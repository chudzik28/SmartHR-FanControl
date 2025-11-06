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
import kotlin.math.pow
import kotlin.math.roundToInt

class FanSpeedController {
    private var smoothedHeartRate: Double? = null

    fun processNewHeartRate(newHeartRate: Int, settings: AlgorithmSettings): Int {
        updateSmoothedHeartRate(newHeartRate.toDouble(), settings)
        val currentSmoothedHr = smoothedHeartRate ?: return settings.minSpeed
        return calculateNonLinearSpeed(currentSmoothedHr, settings)
    }

    private fun updateSmoothedHeartRate(newRawHr: Double, settings: AlgorithmSettings) {
        val previousSmoothedHr = smoothedHeartRate
        if (previousSmoothedHr == null) {
            smoothedHeartRate = newRawHr
        } else {
            smoothedHeartRate =
                (newRawHr * settings.smoothingFactor) + (previousSmoothedHr * (1.0 - settings.smoothingFactor))
        }
    }

    private fun calculateNonLinearSpeed(hr: Double, settings: AlgorithmSettings): Int {
        if (hr <= settings.minHr) return settings.minSpeed
        if (hr >= settings.maxHr) return settings.maxSpeed

        val hrProgress = (hr - settings.minHr) / (settings.maxHr - settings.minHr)
        val nonLinearProgress = hrProgress.pow(settings.exponent)
        val speedRange = settings.maxSpeed - settings.minSpeed
        val calculatedSpeed = settings.minSpeed + (nonLinearProgress * speedRange)

        return calculatedSpeed.roundToInt()
    }

    fun reset() {
        smoothedHeartRate = null
    }
}