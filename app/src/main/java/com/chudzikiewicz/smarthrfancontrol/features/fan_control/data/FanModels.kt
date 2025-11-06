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

import com.chudzikiewicz.smarthrfancontrol.core.miio_protocol.MiotPropertyResult
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

enum class OperationMode(val value: Int) {
    Normal(0),
    Nature(1);

    companion object {
        fun fromValue(value: Int?) = entries.find { it.value == value } ?: Normal
    }
}

enum class MoveDirection {
    Left,
    Right
}

interface FanStatus {
    val isOn: Boolean
    val mode: OperationMode
    val speed: Int
    val oscillate: Boolean
    val delayOffCountdown: Int
    val isLedOn: Boolean
    val isBuzzerOn: Boolean
    val isChildLockOn: Boolean
    val power: String get() = if (isOn) "on" else "off"
}

data class FanStatusMiot(
    override val isOn: Boolean,
    override val mode: OperationMode,
    override val speed: Int,
    override val oscillate: Boolean,
    val angle: Int,
    override val delayOffCountdown: Int,
    override val isLedOn: Boolean,
    override val isBuzzerOn: Boolean,
    override val isChildLockOn: Boolean
) : FanStatus

internal fun parseMiotResult(results: List<MiotPropertyResult>): Map<String, Any?> {
    return results.filter { it.code == 0 && it.value != null }.associate {
        val primitiveValue = when {
            it.value!!.jsonPrimitive.isString -> it.value.jsonPrimitive.content
            it.value.jsonPrimitive.booleanOrNull != null -> it.value.jsonPrimitive.booleanOrNull
            it.value.jsonPrimitive.intOrNull != null -> it.value.jsonPrimitive.intOrNull
            else -> it.value.toString()
        }
        it.did to primitiveValue
    }
}