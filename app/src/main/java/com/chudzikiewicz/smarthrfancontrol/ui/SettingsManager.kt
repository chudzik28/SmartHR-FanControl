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
package com.chudzikiewicz.smarthrfancontrol.ui

import com.chudzikiewicz.smarthrfancontrol.core.preferences.AlgorithmSettings
import com.chudzikiewicz.smarthrfancontrol.core.preferences.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.*

class SettingsManager(
    private val uiState: MutableStateFlow<MainUiState>,
    private val repository: UserPreferencesRepository,
    private val scope: CoroutineScope,
    private val events: Channel<String>,
    private val onSettingsSaved: () -> Unit
) {
    private fun Int.isValidHrValue(): Boolean = this in 50..200
    private fun Int.isValidSpeedValue(): Boolean = this in 1..100
    private fun Double.isValidSmoothingFactor(): Boolean = this in 0.1..1.0
    private fun Double.isValidExponent(): Boolean = this in 1.0..3.0
    private fun sendEvent(message: String) {
        scope.launch { events.send(message) }
    }
    private fun Double.toFormattedDecimalString(): String {
        return String.format(Locale.US, "%.1f", this)
    }

    fun onMinHrChanged(newMinHr: String) {
        val hr = newMinHr.toIntOrNull()
        if (newMinHr.isBlank()) {
            uiState.update { it.copy(minHrInput = newMinHr) }
        } else if (hr != null && hr.isValidHrValue()) {
            uiState.update { it.copy(minHrInput = newMinHr) }
        } else {
            sendEvent("Error: HR must be between 50 and 200")
            uiState.update { it.copy(minHrInput = it.minHr.toString()) }
        }
    }

    fun onMaxHrChanged(newMaxHr: String) {
        val hr = newMaxHr.toIntOrNull()
        if (newMaxHr.isBlank()) {
            uiState.update { it.copy(maxHrInput = newMaxHr) }
        } else if (hr != null && hr.isValidHrValue()) {
            uiState.update { it.copy(maxHrInput = newMaxHr) }
        } else {
            sendEvent("Error: HR must be between 50 and 200")
            uiState.update { it.copy(maxHrInput = it.maxHr.toString()) }
        }
    }

    fun onMinSpeedChanged(newMinSpeed: String) {
        val speed = newMinSpeed.toIntOrNull()
        if (newMinSpeed.isBlank()) {
            uiState.update { it.copy(minSpeedInput = newMinSpeed) }
        } else if (speed != null && speed.isValidSpeedValue()) {
            uiState.update { it.copy(minSpeedInput = newMinSpeed) }
        } else {
            sendEvent("Error: Speed must be between 1 and 100")
            uiState.update { it.copy(minSpeedInput = it.minSpeed.toString()) }
        }
    }

    fun onMaxSpeedChanged(newMaxSpeed: String) {
        val speed = newMaxSpeed.toIntOrNull()
        if (newMaxSpeed.isBlank()) {
            uiState.update { it.copy(maxSpeedInput = newMaxSpeed) }
        } else if (speed != null && speed.isValidSpeedValue()) {
            uiState.update { it.copy(maxSpeedInput = newMaxSpeed) }
        } else {
            sendEvent("Error: Speed must be between 1 and 100")
            uiState.update { it.copy(maxSpeedInput = it.maxSpeed.toString()) }
        }
    }

    fun onSmoothingChanged(newSmoothing: String) {
        val smoothing = newSmoothing.replace(',', '.').toDoubleOrNull()
        if (newSmoothing.isBlank() || newSmoothing == ".") {
            uiState.update { it.copy(smoothingInput = newSmoothing) }
        } else if (smoothing != null && smoothing.isValidSmoothingFactor()) {
            uiState.update { it.copy(smoothingInput = newSmoothing) }
        } else {
            sendEvent("Error: Smoothing Factor must be between 0.1 and 1.0")
            uiState.update { it.copy(smoothingInput = it.smoothingFactor.toFormattedDecimalString()) }
        }
    }

    fun onExponentChanged(newExponent: String) {
        val exponent = newExponent.replace(',', '.').toDoubleOrNull()
        if (newExponent.isBlank() || newExponent == ".") {
            uiState.update { it.copy(exponentInput = newExponent) }
        } else if (exponent != null && exponent.isValidExponent()) {
            uiState.update { it.copy(exponentInput = newExponent) }
        } else {
            sendEvent("Error: Exponent must be between 1.0 and 3.0")
            uiState.update { it.copy(exponentInput = it.exponent.toFormattedDecimalString()) }
        }
    }

    fun onFanIpChanged(newIp: String) = uiState.update { it.copy(fanIpInput = newIp) }
    fun onFanTokenChanged(newToken: String) = uiState.update { it.copy(fanTokenInput = newToken) }

    fun syncInputStateWithActiveState() {
        val currentState = uiState.value
        uiState.update {
            it.copy(
                fanIpInput = currentState.fanIpAddress,
                fanTokenInput = currentState.fanToken,
                minHrInput = currentState.minHr.toString(),
                maxHrInput = currentState.maxHr.toString(),
                minSpeedInput = currentState.minSpeed.toString(),
                maxSpeedInput = currentState.maxSpeed.toString(),
                smoothingInput = currentState.smoothingFactor.toFormattedDecimalString(),
                exponentInput = currentState.exponent.toFormattedDecimalString()
            )
        }
    }
    fun resetFanConfig() {
        scope.launch {
            repository.saveFanSettings("", "")
            uiState.update {
                it.copy(
                    fanIpAddress = "", fanToken = "",
                    fanIpInput = "", fanTokenInput = ""
                )
            }
            events.send("Fan Config has been reset")
            onSettingsSaved()
        }
    }
    fun resetAutoModeConfig() {
        scope.launch {
            val defaultSettings = AlgorithmSettings(
                minHr = 80, maxHr = 170, minSpeed = 10, maxSpeed = 100,
                smoothingFactor = 0.3, exponent = 2.2
            )
            repository.saveAlgorithmSettings(defaultSettings)
            uiState.update {
                it.copy(
                    minHr = defaultSettings.minHr, maxHr = defaultSettings.maxHr,
                    minSpeed = defaultSettings.minSpeed, maxSpeed = defaultSettings.maxSpeed,
                    smoothingFactor = defaultSettings.smoothingFactor, exponent = defaultSettings.exponent,
                    minHrInput = defaultSettings.minHr.toString(),
                    maxHrInput = defaultSettings.maxHr.toString(),
                    minSpeedInput = defaultSettings.minSpeed.toString(),
                    maxSpeedInput = defaultSettings.maxSpeed.toString(),
                    smoothingInput = defaultSettings.smoothingFactor.toFormattedDecimalString(),
                    exponentInput = defaultSettings.exponent.toFormattedDecimalString()
                )
            }
            events.send("Auto Mode settings have been reset")
        }
    }

    fun saveFanSettings() {
        scope.launch {
            val state = uiState.value
            if (state.fanIpInput.isBlank() || state.fanTokenInput.isBlank()) {
                events.send("Error: IP & Token can't be empty"); return@launch
            }
            if (state.fanTokenInput.length != 32) {
                events.send("Error: Token must be 32 signs"); return@launch
            }
            repository.saveFanSettings(state.fanIpInput, state.fanTokenInput)
            uiState.update { it.copy(fanIpAddress = state.fanIpInput, fanToken = state.fanTokenInput) }
            events.send("Fan Settings Saved")
            onSettingsSaved()
        }
    }
    fun saveAlgorithmSettings() {
        scope.launch {
            val state = uiState.value
            val minHr = state.minHrInput.toIntOrNull()
            val maxHr = state.maxHrInput.toIntOrNull()
            val minSpeed = state.minSpeedInput.toIntOrNull()
            val maxSpeed = state.maxSpeedInput.toIntOrNull()
            val smoothing = state.smoothingInput.replace(',', '.').toDoubleOrNull()
            val exponent = state.exponentInput.replace(',', '.').toDoubleOrNull()

            if (minHr == null || maxHr == null || minSpeed == null || maxSpeed == null || smoothing == null || exponent == null) {
                events.send("Error: All fields must contain valid numbers"); return@launch
            }
            if (!minHr.isValidHrValue() || !maxHr.isValidHrValue()) {
                events.send("Error: HR outside the range of 50-200 BPM."); return@launch
            }
            if (!minSpeed.isValidSpeedValue() || !maxSpeed.isValidSpeedValue()) {
                events.send("Error: Speed outside the range 1-100%."); return@launch
            }
            if (!smoothing.isValidSmoothingFactor()) {
                events.send("Error: Smoothing value must be in the range 0.1 - 1.0."); return@launch
            }
            if (!exponent.isValidExponent()) {
                events.send("Error: Exponent value must be in the range 1.0 - 3.0."); return@launch
            }
            if (minHr >= maxHr) {
                events.send("Error: HR Min must be lower HR Max"); return@launch
            }
            if (minSpeed >= maxSpeed) {
                events.send("Error: Speed Min must be lower than Speed Max"); return@launch
            }

            val algoSettings = AlgorithmSettings(
                minHr = minHr, maxHr = maxHr, minSpeed = minSpeed, maxSpeed = maxSpeed,
                smoothingFactor = smoothing, exponent = exponent
            )
            repository.saveAlgorithmSettings(algoSettings)
            uiState.update {
                it.copy(
                    minHr = algoSettings.minHr, maxHr = algoSettings.maxHr,
                    minSpeed = algoSettings.minSpeed, maxSpeed = algoSettings.maxSpeed,
                    smoothingFactor = algoSettings.smoothingFactor, exponent = algoSettings.exponent
                )
            }
            events.send("Auto Mode Settings Saved")
        }
    }
}