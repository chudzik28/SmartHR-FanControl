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
package com.chudzikiewicz.smarthrfancontrol.features.fan_control.data

import com.chudzikiewicz.smarthrfancontrol.core.miio_protocol.MiioDevice
import com.chudzikiewicz.smarthrfancontrol.core.miio_protocol.MiotMappings
import com.chudzikiewicz.smarthrfancontrol.core.miio_protocol.MiotPropertyResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

class Fan(ip: String, token: String, scope: CoroutineScope) : MiioDevice(ip, token, scope) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun status(): FanStatus {
        val modelMapping = MiotMappings.MAPPINGS[deviceModel]
            ?: throw IllegalStateException("No mapping for $deviceModel")

        val propertyNames = modelMapping.keys.toList()
        val response = getPropertiesMiot(propertyNames)

        if (response.result == null || response.result.jsonArray.isEmpty()) {
            throw IllegalStateException("Received empty device status. Error: ${response.error}")
        }

        val results = json.decodeFromJsonElement(
            ListSerializer(MiotPropertyResult.serializer()),
            response.result
        )

        val reverseMapping = modelMapping.entries.associate { (name, prop) ->
            Pair(prop.siid, prop.piid) to name
        }

        val data = results
            .filter { it.code == 0 && it.value != null }
            .mapNotNull { result ->
                val name = reverseMapping[Pair(result.siid, result.piid)]
                if (name != null) {
                    val jsonValue = result.value!!
                    val value = when {
                        jsonValue.jsonPrimitive.booleanOrNull != null -> jsonValue.jsonPrimitive.booleanOrNull
                        jsonValue.jsonPrimitive.intOrNull != null -> jsonValue.jsonPrimitive.intOrNull
                        else -> jsonValue.jsonPrimitive.content
                    }
                    name to value
                } else {
                    null
                }
            }
            .toMap()

        return FanStatusMiot(
            isOn = data["power"] as? Boolean ?: false,
            mode = OperationMode.Companion.fromValue(data["mode"] as? Int),
            speed = data["fan_speed"] as? Int ?: 0,
            oscillate = data["swing_mode"] as? Boolean ?: false,
            angle = data["swing_mode_angle"] as? Int ?: 0,
            delayOffCountdown = data["power_off_time"] as? Int ?: 0,
            isLedOn = data["light"] as? Boolean ?: false,
            isBuzzerOn = data["buzzer"] as? Boolean ?: false,
            isChildLockOn = data["child_lock"] as? Boolean ?: false
        )
    }


    suspend fun on() = setPropertyMiot("power", true)

    suspend fun off() = setPropertyMiot("power", false)

    suspend fun setSpeed(speed: Int) {
        require(speed in 0..100) { "Improper speed: $speed. Allowed range: 0-100." }
        setPropertyMiot("fan_speed", speed)
    }
}