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

import java.nio.ByteBuffer
import java.nio.ByteOrder

const val MIIO_HEADER_SIZE = 32
const val MIIO_MAGIC_V2 = 0x2131


/**
 * Represents Miio packet (header + payload).
 *
 * @property magic Magic bytes (0x2131).
 * @property length Length of whole packet
 * @property unknown Unknown Field (mostly 0xFFFFFFFF for encrypted, 0x00000000 for handshake).
 * @property deviceId Device ID.
 * @property timestamp Time of last communication (for encrypt).
 * @property checksum Conrol sum of MD5 header + encrypted payload
 * @property payload encrypted data.
 */
data class MiioPacket(
    val magic: Short = MIIO_MAGIC_V2.toShort(),
    var length: Short = MIIO_HEADER_SIZE.toShort(),
    val unknown: Int = 0,
    var deviceId: Int = 0,
    var timestamp: Int = 0,
    var checksum: ByteArray = ByteArray(16),
    var payload: ByteArray = ByteArray(0)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MiioPacket

        if (magic != other.magic) return false
        if (length != other.length) return false
        if (unknown != other.unknown) return false
        if (deviceId != other.deviceId) return false
        if (timestamp != other.timestamp) return false
        if (!checksum.contentEquals(other.checksum)) return false
        if (!payload.contentEquals(other.payload)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = magic.hashCode()
        result = 31 * result + length.hashCode()
        result = 31 * result + unknown.hashCode()
        result = 31 * result + deviceId.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + checksum.contentHashCode()
        result = 31 * result + payload.contentHashCode()
        return result
    }

    fun serialize(): ByteArray {
        length = (MIIO_HEADER_SIZE + payload.size).toShort()
        val buffer = ByteBuffer.allocate(length.toInt()).order(ByteOrder.BIG_ENDIAN)

        buffer.putShort(magic)
        buffer.putShort(length)
        buffer.putInt(unknown)
        buffer.putInt(deviceId)
        buffer.putInt(timestamp)
        buffer.put(checksum)
        buffer.put(payload)

        return buffer.array()
    }

    companion object {
        fun deserialize(data: ByteArray): MiioPacket {
            val buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)

            if (data.size < MIIO_HEADER_SIZE) {
                throw IllegalArgumentException("Data too short for header MiioPacket.")
            }

            val magic = buffer.getShort()
            val length = buffer.getShort()

            if (length > data.size || length < MIIO_HEADER_SIZE) {
                throw IllegalArgumentException("Header packet length ($length) inconsistent with the actual data length (${data.size}).")
            }


            val unknown = buffer.getInt()
            val deviceId = buffer.getInt()
            val timestamp = buffer.getInt()
            val checksum = ByteArray(16)
            buffer.get(checksum)

            val payloadSize = length - MIIO_HEADER_SIZE
            val payload = if (payloadSize > 0) {

                if (buffer.remaining() >= payloadSize) {
                    ByteArray(payloadSize).also { buffer.get(it) }
                } else {
                    println("WARNING: Packet Miio thinks, payload size is $payloadSize, but available only ${buffer.remaining()} bytes.")
                    ByteArray(0)
                }
            } else {
                ByteArray(0)
            }

            return MiioPacket(magic, length, unknown, deviceId, timestamp, checksum, payload)
        }
    }
}