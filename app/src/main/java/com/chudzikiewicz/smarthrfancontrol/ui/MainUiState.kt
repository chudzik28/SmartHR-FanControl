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

import android.bluetooth.BluetoothDevice
import com.chudzikiewicz.smarthrfancontrol.core.preferences.PairedHrDevice

data class MainUiState(
    val fanConnectionStatus: String = "Disconnect",
    val hrDeviceStatus: String = "Status Initializing...",
    val gattServerStatus: String = "Server Disabled",

    val currentHeartRate: Int = 0,
    val currentFanSpeed: Int = 0,
    val isFanOn: Boolean = false,

    val fanIpAddress: String = "",
    val fanToken: String = "",
    val minHr: Int = 80,
    val maxHr: Int = 160,
    val minSpeed: Int = 10,
    val maxSpeed: Int = 100,
    val smoothingFactor: Double = 0.3,
    val exponent: Double = 2.2,

    val fanIpInput: String = "",
    val fanTokenInput: String = "",
    val minHrInput: String = "",
    val maxHrInput: String = "",
    val minSpeedInput: String = "",
    val maxSpeedInput: String = "",
    val smoothingInput: String = "",
    val exponentInput: String = "",

    val selectedHrDeviceAddress: String? = null,
    val pairedHrDevices: List<PairedHrDevice> = emptyList(),
    val scannedHrDevices: List<BluetoothDevice> = emptyList(),
    val isScanning: Boolean = false,
    val bleScanStatus: String = "Ready to search",

    val isHrReconnectVisible: Boolean = false,
    val isHrConnecting: Boolean = false,

    val isFanReconnectVisible: Boolean = false,
    val isFanConnecting: Boolean = false,

    val isWifiEnabled: Boolean = false,
    val isBluetoothEnabled: Boolean = false,
    val isAutoModeEnabled: Boolean = false,

    val isHrSharingEnabled: Boolean = false,

    val haveTermsBeenAccepted: Boolean = false,

    val isReady: Boolean = false
)