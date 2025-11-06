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

data class MiotProperty(val siid: Int, val piid: Int)

object MiotMappings {

    private const val MODEL_FAN_P9 = "dmaker.fan.p9"
    private const val MODEL_FAN_P10 = "dmaker.fan.p10"
    private const val MODEL_FAN_P11 = "dmaker.fan.p11"
    private const val MODEL_FAN_P15 = "dmaker.fan.p15"
    private const val MODEL_FAN_P18 = "dmaker.fan.p18"
    private const val MODEL_FAN_P33 = "dmaker.fan.p33"

    private val FAN_P9_MAPPING = mapOf(
        "power" to MiotProperty(siid = 2, piid = 1),
        "fan_level" to MiotProperty(siid = 2, piid = 2),
        "child_lock" to MiotProperty(siid = 3, piid = 1),
        "fan_speed" to MiotProperty(siid = 2, piid = 11),
        "swing_mode" to MiotProperty(siid = 2, piid = 5),
        "swing_mode_angle" to MiotProperty(siid = 2, piid = 6),
        "power_off_time" to MiotProperty(siid = 2, piid = 8),
        "buzzer" to MiotProperty(siid = 2, piid = 7),
        "light" to MiotProperty(siid = 2, piid = 9),
        "mode" to MiotProperty(siid = 2, piid = 4),
        "set_move" to MiotProperty(siid = 2, piid = 10)
    )

    private val FAN_P10_MAPPING = mapOf(
        "power" to MiotProperty(siid = 2, piid = 1),
        "fan_level" to MiotProperty(siid = 2, piid = 2),
        "child_lock" to MiotProperty(siid = 3, piid = 1),
        "fan_speed" to MiotProperty(siid = 2, piid = 10),
        "swing_mode" to MiotProperty(siid = 2, piid = 4),
        "swing_mode_angle" to MiotProperty(siid = 2, piid = 5),
        "power_off_time" to MiotProperty(siid = 2, piid = 6),
        "buzzer" to MiotProperty(siid = 2, piid = 8),
        "light" to MiotProperty(siid = 2, piid = 7),
        "mode" to MiotProperty(siid = 2, piid = 3),
        "set_move" to MiotProperty(siid = 2, piid = 9)
    )

    private val FAN_P11_MAPPING = mapOf(
        "power" to MiotProperty(siid = 2, piid = 1),
        "fan_level" to MiotProperty(siid = 2, piid = 2),
        "mode" to MiotProperty(siid = 2, piid = 3),
        "swing_mode" to MiotProperty(siid = 2, piid = 4),
        "swing_mode_angle" to MiotProperty(siid = 2, piid = 5),
        "fan_speed" to MiotProperty(siid = 2, piid = 6),
        "light" to MiotProperty(siid = 4, piid = 1),
        "buzzer" to MiotProperty(siid = 5, piid = 1),
        "child_lock" to MiotProperty(siid = 7, piid = 1),
        "power_off_time" to MiotProperty(siid = 3, piid = 1),
        "set_move" to MiotProperty(siid = 6, piid = 1)
    )

    private val FAN_P33_MAPPING = mapOf(
        "power" to MiotProperty(siid = 2, piid = 1),
        "fan_level" to MiotProperty(siid = 2, piid = 2),
        "mode" to MiotProperty(siid = 2, piid = 3),
        "swing_mode" to MiotProperty(siid = 2, piid = 4),
        "swing_mode_angle" to MiotProperty(siid = 2, piid = 5),
        "fan_speed" to MiotProperty(siid = 2, piid = 6),
        "light" to MiotProperty(siid = 4, piid = 1),
        "buzzer" to MiotProperty(siid = 5, piid = 1),
        "child_lock" to MiotProperty(siid = 7, piid = 1),
        "power_off_time" to MiotProperty(siid = 3, piid = 1),
        "set_move" to MiotProperty(siid = 6, piid = 1)
    )

    val MAPPINGS: Map<String, Map<String, MiotProperty>> = mapOf(
        MODEL_FAN_P9 to FAN_P9_MAPPING,
        MODEL_FAN_P10 to FAN_P10_MAPPING,
        MODEL_FAN_P11 to FAN_P11_MAPPING,
        MODEL_FAN_P33 to FAN_P33_MAPPING,

        MODEL_FAN_P15 to FAN_P11_MAPPING,
        MODEL_FAN_P18 to FAN_P10_MAPPING
    )


    val SUPPORTED_ANGLES: Map<String, List<Int>> = mapOf(
        MODEL_FAN_P9 to listOf(30, 60, 90, 120, 150),
        MODEL_FAN_P10 to listOf(30, 60, 90, 120, 140),
        MODEL_FAN_P11 to listOf(30, 60, 90, 120, 140),
        MODEL_FAN_P33 to listOf(30, 60, 90, 120, 140)
    )
}