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
package com.chudzikiewicz.smarthrfancontrol.features.fan_control.domain

import com.chudzikiewicz.smarthrfancontrol.core.preferences.AlgorithmSettings
import com.chudzikiewicz.smarthrfancontrol.features.fan_control.data.Fan
import com.chudzikiewicz.smarthrfancontrol.ui.MainUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class FanController(
    private val fan: Fan?,
    private val scope: CoroutineScope,
    private val fanSpeedController: FanSpeedController,
    private val onStateUpdate: (MainUiState) -> Unit,
    private val getState: () -> MainUiState
) {
    private val isAutoModeLocked = AtomicBoolean(false)
    private var controlJob: Job? = null

    fun toggleFan() {
        scope.launch {
            val currentState = getState()
            try {
                if (currentState.isFanOn) {
                    fan?.off()
                    onStateUpdate(currentState.copy(isFanOn = false, currentFanSpeed = 0))
                    controlJob?.cancel()
                    isAutoModeLocked.set(false)
                } else {
                    fan?.on()
                    onStateUpdate(currentState.copy(isFanOn = true))
                    delay(200)

                    if (currentState.isAutoModeEnabled) {
                        updateSpeedBasedOnHr(currentState.currentHeartRate, forceUpdate = true)
                    } else {
                        val speedToSet =
                            if (currentState.currentFanSpeed > 0) currentState.currentFanSpeed else 1
                        setManualSpeed(speedToSet)
                    }
                }
            } catch (e: Exception) { /* Handle error */
            }
        }
    }

    fun toggleAutoMode() {
        val currentState = getState()
        if (!currentState.isFanOn) return

        val isEnabling = !currentState.isAutoModeEnabled
        onStateUpdate(currentState.copy(isAutoModeEnabled = isEnabling))

        controlJob?.cancel()
        if (isEnabling) {
            fanSpeedController.reset()
            updateSpeedBasedOnHr(currentState.currentHeartRate, forceUpdate = true)
        } else {
            isAutoModeLocked.set(false)
        }
    }

    fun setManualSpeed(speed: Int) {
        val currentState = getState()
        if (currentState.isAutoModeEnabled || !currentState.isFanOn) return

        controlJob?.cancel()
        controlJob = scope.launch {
            onStateUpdate(currentState.copy(currentFanSpeed = speed))
            delay(400)

            try {
                if (speed == 0) toggleFan() else fan?.setSpeed(speed)
            } catch (e: Exception) { /* Handle error */
            }
        }
    }

    fun updateSpeedBasedOnHr(rawHeartRate: Int, forceUpdate: Boolean = false) {
        val currentState = getState()
        if (rawHeartRate <= 0 || !currentState.isFanOn) return

        val currentAlgoSettings = AlgorithmSettings(
            minHr = currentState.minHr,
            maxHr = currentState.maxHr,
            minSpeed = currentState.minSpeed,
            maxSpeed = currentState.maxSpeed,
            smoothingFactor = currentState.smoothingFactor,
            exponent = currentState.exponent
        )
        val targetSpeed = fanSpeedController.processNewHeartRate(rawHeartRate, currentAlgoSettings)

        if (forceUpdate || targetSpeed != currentState.currentFanSpeed) {
            controlJob?.cancel()
            controlJob = scope.launch {
                try {
                    isAutoModeLocked.set(true)
                    fan?.setSpeed(targetSpeed)
                    onStateUpdate(getState().copy(currentFanSpeed = targetSpeed))
                    delay(3000)
                    isAutoModeLocked.set(false)
                } catch (e: Exception) {
                    isAutoModeLocked.set(false)
                }
            }
        }
    }

    fun fetchStatus() {
        scope.launch {
            try {
                fan?.status()?.let {
                    onStateUpdate(getState().copy(isFanOn = it.isOn, currentFanSpeed = it.speed))
                }
            } catch (e: Exception) { /* Handle error */
            }
        }
    }

    fun resetAutoModeLock() {
        isAutoModeLocked.set(false)
    }

    fun disableAutoMode() {
        val currentState = getState()
        if (!currentState.isAutoModeEnabled) return

        controlJob?.cancel()
        isAutoModeLocked.set(false)

        onStateUpdate(currentState.copy(isAutoModeEnabled = false))

        val manualSpeed = if (currentState.currentFanSpeed > 0) {
            currentState.currentFanSpeed
        } else {
            currentState.minSpeed
        }

        scope.launch {
            try {
                fan?.setSpeed(manualSpeed)
                onStateUpdate(getState().copy(currentFanSpeed = manualSpeed))
            } catch (e: Exception) { /* Ignore, if fan null */
            }
        }
    }
}