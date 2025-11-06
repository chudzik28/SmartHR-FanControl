/*
 * SmartHR FanControl
 * Copyright (C) [2025] [Marcin Chudzikiewicz]
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.chudzikiewicz.smarthrfancontrol.core.miio_protocol

import com.chudzikiewicz.smarthrfancontrol.core.miio_protocol.MiioCrypto.decodeHex
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.BoundDatagramSocket
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.aSocket
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.readBytes
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import mu.KotlinLogging
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.time.Duration.Companion.seconds
import io.ktor.network.sockets.InetSocketAddress as KtorInetSocketAddress


private val logger = KotlinLogging.logger {}

fun Json.encodeDynamicToJsonElement(value: Any): JsonElement {
    return when (value) {
        is List<*> -> JsonArray(value.map { if (it != null) this.encodeDynamicToJsonElement(it) else JsonNull })
        is Map<*, *> -> JsonObject(value.mapValues {
            if (it.value != null) this.encodeDynamicToJsonElement(
                it.value!!
            ) else JsonNull
        }.filterKeys { it is String }.mapKeys { it.key as String })

        is String -> JsonPrimitive(value)
        is Int -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        is Double -> JsonPrimitive(value)
        is Float -> JsonPrimitive(value)
        is Long -> JsonPrimitive(value)
        else -> JsonPrimitive(value.toString())
    }
}

@Serializable
data class MiioResponse(
    val id: Int,
    val result: JsonElement? = null,
    val error: JsonElement? = null
)

@Serializable
data class MiioInfoResponse(val model: String)

open class MiioDevice(
    val ip: String,
    val token: String,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : CoroutineScope by scope {
    private val miioPort = 54321
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val tokenBytes: ByteArray = token.decodeHex()
    private var deviceId: Int = 0
    private var deviceTimestamp: Int = 0
    private var lastPacketReceivedMs: Long = 0L
    private var lastSequenceId: Int = 0
    private lateinit var aesKey: SecretKeySpec
    private lateinit var aesIv: IvParameterSpec
    private val selectorManager = SelectorManager(Dispatchers.IO)
    private var socket: BoundDatagramSocket? = null
    open var deviceModel: String? = null

    companion object {
        private const val MIIO_HEADER_SIZE: Int = 32
        val HANDSHAKE_PACKET_BYTES: ByteArray =
            "21310020ffffffffffffffffffffffffffffffffffffffffffffffffffffffff".decodeHex()
    }

    open suspend fun initialize() {
        try {

            val (key, iv) = MiioCrypto.generateKeyAndIv(token)
            this.aesKey = key
            this.aesIv = iv
            socket = aSocket(selectorManager).udp().bind(KtorInetSocketAddress("0.0.0.0", 0))

            performHandshake()
            val infoResponse = sendCommand("miIO.info", emptyList<Any>())

            if (infoResponse.result != null && infoResponse.result !is JsonNull) {
                val infoResult = json.decodeFromJsonElement<MiioInfoResponse>(infoResponse.result)
                deviceModel = infoResult.model
            } else {
                val errorDetails = infoResponse.error?.toString() ?: "no details"
                throw IllegalStateException("Cannot read Fan model. Error: $errorDetails")
            }

        } catch (e: SocketTimeoutException) {
            throw Exception("Check Fan IP/Token")

        } catch (e: Exception) {
            throw Exception("Check Fan IP/Token")
        }
    }

    private suspend fun performHandshake() {
        for (attempt in 1..3) {
            try {
                sendRawData(HANDSHAKE_PACKET_BYTES)
                val response = withTimeout(2.seconds) { receiveRawPacket() }
                updateTimestamps(response.timestamp)
                deviceId = response.deviceId
                return
            } catch (e: Exception) {
                delay(1.seconds)
            }
        }
        throw Exception("Check Fan IP/Token")
    }

    open suspend fun sendCommand(method: String, params: Any): MiioResponse {
        lastSequenceId++
        val paramsElement = when (params) {
            is JsonElement -> params
            else -> json.encodeDynamicToJsonElement(params)
        }
        val miioCommand = MiioCommand(lastSequenceId, method, paramsElement)
        val jsonPayload = json.encodeToString(miioCommand).toByteArray(Charsets.UTF_8)
        val encryptedPayload = MiioCrypto.encrypt(jsonPayload, aesKey, aesIv)
        val elapsedSeconds =
            if (lastPacketReceivedMs == 0L) 0 else (System.currentTimeMillis() - lastPacketReceivedMs) / 1000
        val timestampToSend = deviceTimestamp + elapsedSeconds.toInt() + 1
        val commandPacket = buildPacket(encryptedPayload, timestampToSend)
        sendRawData(commandPacket)
        val responsePacket =
            withTimeout(5.seconds) { receiveRawPacket(expectedDeviceId = deviceId) }
        updateTimestamps(responsePacket.timestamp)
        val decryptedPayload = MiioCrypto.decrypt(responsePacket.payload, aesKey, aesIv)
        val jsonString = decryptedPayload.toString(Charsets.UTF_8).trimEnd('\u0000', ' ')

        if (jsonString.isBlank()) return MiioResponse(
            lastSequenceId,
            error = JsonPrimitive("Empty load after decrypt")
        )
        return json.decodeFromString(jsonString)
    }

    private fun buildPacket(payload: ByteArray, timestamp: Int): ByteArray {
        val packetLength = MIIO_HEADER_SIZE + payload.size

        val headerPrefix = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN)
            .putShort(0x2131)
            .putShort(packetLength.toShort())
            .putInt(0) // Unknown
            .putInt(deviceId)
            .putInt(timestamp)
            .array()

        val digest = MessageDigest.getInstance("MD5")
        digest.update(headerPrefix)
        digest.update(tokenBytes)
        digest.update(payload)
        val checksum = digest.digest()

        val finalHeader = ByteBuffer.allocate(MIIO_HEADER_SIZE).order(ByteOrder.BIG_ENDIAN)
            .put(headerPrefix)
            .put(checksum)
            .array()

        return finalHeader + payload
    }

    private suspend fun receiveRawPacket(expectedDeviceId: Int? = null): MiioPacket {
        while (coroutineContext.isActive) {
            val d = socket!!.receive()
            val a = d.packet.readBytes();if (a.size < MIIO_HEADER_SIZE) continue
            val p = try {
                MiioPacket.deserialize(a)
            } catch (e: Exception) {
                continue
            };if (expectedDeviceId != null && p.deviceId != expectedDeviceId) continue;return p
        };throw CancellationException()
    }

    private suspend fun sendRawData(data: ByteArray) {
        socket?.send(Datagram(ByteReadPacket(data), KtorInetSocketAddress(ip, miioPort)))
    }

    private fun updateTimestamps(newDeviceTimestamp: Int) {
        if (newDeviceTimestamp > 0) {
            this.deviceTimestamp = newDeviceTimestamp
            this.lastPacketReceivedMs = System.currentTimeMillis()
        }
    }

    open suspend fun setPropertyMiot(propertyName: String, value: Any): MiioResponse {
        val modelMapping = MiotMappings.MAPPINGS[deviceModel]
            ?: throw IllegalStateException("No mapping for $deviceModel")

        val property = modelMapping[propertyName]
            ?: throw IllegalArgumentException("Unknown feature '$propertyName' for $deviceModel")

        val jsonValue = when (value) {
            is Boolean -> JsonPrimitive(value)
            is Int -> JsonPrimitive(value)
            is Float -> JsonPrimitive(value)
            is Double -> JsonPrimitive(value)
            is String -> JsonPrimitive(value)
            else -> {
                JsonPrimitive(value.toString())
            }
        }

        val params = listOf(
            MiotSetProperty(
                did = propertyName,
                siid = property.siid,
                piid = property.piid,
                value = jsonValue
            )
        )

        val paramsElement =
            json.encodeToJsonElement(ListSerializer(MiotSetProperty.serializer()), params)

        return sendCommand("set_properties", paramsElement)
    }


    open suspend fun getPropertiesMiot(propertyNames: List<String>): MiioResponse {
        val modelMapping =
            MiotMappings.MAPPINGS[deviceModel] ?: throw IllegalStateException("No mappinng")
        val params = propertyNames.map { name ->
            val property = modelMapping[name] ?: throw IllegalArgumentException("Unknown feature")
            mapOf("siid" to property.siid, "piid" to property.piid)
        }
        return sendCommand("get_properties", params)
    }

    open fun close() {
        socket?.close()
        selectorManager.close()
    }
}