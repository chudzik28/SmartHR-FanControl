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


import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class FanStatusResult(
    val power: String,
    val mode: String,
    val fan_level: Int,
    val model: String? = null
)

@Serializable
data class MiotSetProperty(
    val did: String,
    val siid: Int,
    val piid: Int,
    val value: JsonElement
)

@Serializable
data class MiotGetProperty(val did: String, val siid: Int, val piid: Int)

@Serializable
data class MiotPropertyResult(
    val did: String,
    val siid: Int,
    val piid: Int,
    val code: Int,
    val value: JsonElement? = null
)