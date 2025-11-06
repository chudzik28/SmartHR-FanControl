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
package com.chudzikiewicz.smarthrfancontrol.core.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import com.chudzikiewicz.smarthrfancontrol.core.preferences.PairedHrDevice
import com.chudzikiewicz.smarthrfancontrol.core.preferences.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit

@SuppressLint("MissingPermission")
class BleDeviceManager(
    private val btAdapter: BluetoothAdapter,
    private val repository: UserPreferencesRepository,
    private val scope: CoroutineScope
) {
    private val _scannedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val scannedDevices = _scannedDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val _scanStatus = MutableStateFlow("Ready to scan")
    val scanStatus = _scanStatus.asStateFlow()

    private val scanResults = mutableMapOf<String, BluetoothDevice>()
    private val scanHandler = Handler(Looper.getMainLooper())
    private val SCAN_PERIOD: Long = TimeUnit.SECONDS.toMillis(10)
    private val UUID_HEART_RATE_SERVICE = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")

    fun startBleScan() {
        if (_isScanning.value) return
        val bleScanner = btAdapter.bluetoothLeScanner ?: run {
            _scanStatus.value = "Cannot access BLE scanner"; return
        }
        if (!btAdapter.isEnabled) {
            _scanStatus.value = "Bluetooth is disabled"; return
        }

        scanResults.clear()
        _scannedDevices.value = emptyList()
        scanHandler.postDelayed({ stopBleScan() }, SCAN_PERIOD)

        val filters =
            listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(UUID_HEART_RATE_SERVICE)).build())
        val settings =
            ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()

        bleScanner.startScan(filters, settings, scanCallback)
        _isScanning.value = true
        _scanStatus.value = "Scanning..."
    }

    fun stopBleScan() {
        if (!_isScanning.value) return
        btAdapter.bluetoothLeScanner?.stopScan(scanCallback)
        _isScanning.value = false
        _scanStatus.value = "Scanning Completed"
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (!scanResults.containsKey(result.device.address)) {
                scanResults[result.device.address] = result.device
                _scannedDevices.value = scanResults.values.toList()
            }
        }

        override fun onScanFailed(errorCode: Int) {
            _isScanning.value = false
            _scanStatus.value = "Scanning Error: $errorCode"
        }
    }

    fun pairDevice(device: BluetoothDevice) {
        scope.launch {
            val currentPaired = repository.pairedHrDevicesFlow.first().toMutableList()
            if (currentPaired.none { it.address == device.address }) {
                currentPaired.add(PairedHrDevice(address = device.address, name = device.name))
                repository.savePairedHrDevices(currentPaired)
            }
        }
    }

    fun removeDevice(device: PairedHrDevice) {
        scope.launch {
            val currentPaired = repository.pairedHrDevicesFlow.first().toMutableList()
            currentPaired.removeAll { it.address == device.address }
            repository.savePairedHrDevices(currentPaired)

            if (repository.selectedHrDeviceAddressFlow.first() == device.address) {
                repository.saveSelectedHrDevice(null)
            }
            _scannedDevices.value = _scannedDevices.value.filterNot { it.address == device.address }
        }
    }

    fun chooseDevice(device: PairedHrDevice) {
        scope.launch {
            val currentSelected = repository.selectedHrDeviceAddressFlow.first()
            val newSelected = if (currentSelected == device.address) null else device.address
            repository.saveSelectedHrDevice(newSelected)
        }
    }
}