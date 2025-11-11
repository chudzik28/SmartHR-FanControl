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

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chudzikiewicz.smarthrfancontrol.core.ble.BleClientManager
import com.chudzikiewicz.smarthrfancontrol.core.ble.BleDeviceManager
import com.chudzikiewicz.smarthrfancontrol.core.ble.BleServerManager
import com.chudzikiewicz.smarthrfancontrol.core.ble.BluetoothStateReceiver
import com.chudzikiewicz.smarthrfancontrol.core.preferences.PairedHrDevice
import com.chudzikiewicz.smarthrfancontrol.core.preferences.UserPreferencesRepository
import com.chudzikiewicz.smarthrfancontrol.features.fan_control.data.Fan
import com.chudzikiewicz.smarthrfancontrol.features.fan_control.domain.FanController
import com.chudzikiewicz.smarthrfancontrol.features.fan_control.domain.FanSpeedController
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale

@SuppressLint("MissingPermission")
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _events = Channel<String>()
    val events = _events.receiveAsFlow()

    private val userPreferencesRepository = UserPreferencesRepository(application)

    lateinit var settingsManager: SettingsManager
    private lateinit var fanController: FanController
    private lateinit var bleDeviceManager: BleDeviceManager
    private lateinit var bleClient: BleClientManager
    private lateinit var bleServer: BleServerManager

    private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var wifiNetworkCallback: ConnectivityManager.NetworkCallback? = null

    private val bluetoothStateReceiver = BluetoothStateReceiver()

    init {
        val btManager = application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val btAdapter: BluetoothAdapter? = btManager.adapter

        settingsManager = SettingsManager(
            uiState = _uiState,
            repository = userPreferencesRepository,
            scope = viewModelScope,
            events = _events,
            onSettingsSaved = { initializeFanConnection() }
        )

        if (btAdapter != null) {
            bleDeviceManager = BleDeviceManager(btAdapter, userPreferencesRepository, viewModelScope)
            observeBleDeviceManagerChanges()
        }

        observeUiStateChanges()
        checkInitialStates()
        registerStateReceivers()
        observeHrReconnectVisibility()
        observeFanReconnectVisibility()

        viewModelScope.launch {
            val accepted = userPreferencesRepository.termsAcceptedFlow.first()
            _uiState.update { it.copy(haveTermsBeenAccepted = accepted) }
        }

        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        application.registerReceiver(bluetoothStateReceiver, filter)

        bluetoothStateReceiver.isBluetoothEnabled
            .onEach { isEnabled ->
                _uiState.update { it.copy(isBluetoothEnabled = isEnabled) }
                if (::bleClient.isInitialized) {
                    if (isEnabled) {
                        val selectedAddress = _uiState.value.selectedHrDeviceAddress
                        if (selectedAddress != null) {
                            _uiState.update { it.copy(hrDeviceStatus = "Connecting...") }
                            bleClient.connect()
                        } else {
                            _uiState.update { it.copy(hrDeviceStatus = "No HR sensor selected", isHrConnecting = false) }
                        }
                    } else {
                        _uiState.update { it.copy(hrDeviceStatus = "Enable Bluetooth to connect", isHrConnecting = false) }
                        bleClient.disconnect()
                        if (_uiState.value.isAutoModeEnabled && ::fanController.isInitialized) {
                            fanController.toggleAutoMode()
                            _uiState.update { it.copy(isAutoModeEnabled = false) }
                        }
                    }
                }
                if (::bleServer.isInitialized) {
                    viewModelScope.launch {
                        if (isEnabled && _uiState.value.isHrSharingEnabled && _uiState.value.selectedHrDeviceAddress != null) {
                            _uiState.update { it.copy(gattServerStatus = "Starting Server...") }
                            bleServer.startServer()
                        } else {
                            bleServer.stopServer()
                            val newStatus = if (!isEnabled) "Bluetooth disabled" else if (_uiState.value.selectedHrDeviceAddress == null) "HR sensor not selected" else "Server Stopped"
                            _uiState.update { it.copy(gattServerStatus = newStatus) }
                        }
                    }
                }
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            val isHrSharingEnabled = userPreferencesRepository.isHrSharingEnabledFlow.first()
            val fanSettings = userPreferencesRepository.fanSettingsFlow.first()
            val algoSettings = userPreferencesRepository.algorithmSettingsFlow.first()
            val pairedDevices = userPreferencesRepository.pairedHrDevicesFlow.first()
            val selectedAddress = userPreferencesRepository.selectedHrDeviceAddressFlow.first()
            val isBluetoothOn = btAdapter?.isEnabled == true

            bluetoothStateReceiver.updateInitialState(isBluetoothOn)

            _uiState.update { it.copy(isBluetoothEnabled = isBluetoothOn, isHrSharingEnabled = isHrSharingEnabled) }
            _uiState.update {
                it.copy(
                    fanIpAddress = fanSettings.ip, fanToken = fanSettings.token,
                    fanIpInput = fanSettings.ip, fanTokenInput = fanSettings.token,
                    minHr = algoSettings.minHr, maxHr = algoSettings.maxHr,
                    minSpeed = algoSettings.minSpeed, maxSpeed = algoSettings.maxSpeed,
                    smoothingFactor = algoSettings.smoothingFactor, exponent = algoSettings.exponent,
                    minHrInput = algoSettings.minHr.toString(), maxHrInput = algoSettings.maxHr.toString(),
                    minSpeedInput = algoSettings.minSpeed.toString(), maxSpeedInput = algoSettings.maxSpeed.toString(),
                    smoothingInput = String.format(Locale.US, "%.2f", algoSettings.smoothingFactor),
                    exponentInput = String.format(Locale.US, "%.2f", algoSettings.exponent),
                    pairedHrDevices = pairedDevices,
                    selectedHrDeviceAddress = selectedAddress
                )
            }
            if (btAdapter != null) {
                bleClient = BleClientManager(
                    context = application, btAdapter = btAdapter,
                    targetDeviceAddress = selectedAddress,
                    hrUpdateListener = { newHr -> _uiState.update { it.copy(currentHeartRate = newHr) } },
                    statusUpdateListener = { status -> _uiState.update { it.copy(hrDeviceStatus = status) } }
                )
                bleServer = BleServerManager(
                    context = application, btManager = btManager, btAdapter = btAdapter,
                    statusUpdateListener = { status ->
                        val newStatus = if (!_uiState.value.isBluetoothEnabled) {
                            "Bluetooth disabled"
                        } else if (_uiState.value.selectedHrDeviceAddress == null) {
                            "HR sensor not selected"
                        } else {
                            status
                        }
                        _uiState.update { it.copy(gattServerStatus = newStatus) }
                    }
                )
            }
            if (!isBluetoothOn) {
                _uiState.update { it.copy(hrDeviceStatus = "Enable Bluetooth to connect", isHrConnecting = false) }
            } else if (selectedAddress == null) {
                _uiState.update { it.copy(hrDeviceStatus = "No HR sensor selected", isHrConnecting = false) }
            }
            val ipOk = fanSettings.ip.isNotBlank() && fanSettings.token.isNotBlank()
            if (!ipOk) {
                _uiState.update { it.copy(fanConnectionStatus = "Check Fan IP/Token") }
            }
            if (_uiState.value.isWifiEnabled) {
                initializeFanConnection()
            } else if (!ipOk) {
                // ...
            } else {
                _uiState.update { it.copy(fanConnectionStatus = "Enable Wi-Fi to connnect") }
            }
            if (selectedAddress != null && isBluetoothOn) {
                _uiState.update { it.copy(hrDeviceStatus = "Connecting...") }
                bleClient.connect()
            }
            val initialGattStatus = if (!isBluetoothOn) {
                "Bluetooth disabled"
            } else if (selectedAddress == null) {
                "HR sensor not selected"
            } else if (!isHrSharingEnabled) {
                "Server Stopped"
            } else {
                "Starting Server..."
            }
            _uiState.update { it.copy(gattServerStatus = initialGattStatus, isReady = true) }
        }
    }

    private fun observeFanReconnectVisibility() {
        combine(
            uiState.map { it.fanConnectionStatus }.distinctUntilChanged(),
            uiState.map { it.isWifiEnabled }.distinctUntilChanged()
        ) { status, isWifiEnabled ->
            val isErrorState = status.contains("Error") || status.contains("Disconnected")
            val isConnecting = status.startsWith("Connecting")
            val ipOk = _uiState.value.fanIpAddress.isNotBlank() && _uiState.value.fanToken.isNotBlank()
            val shouldBeVisible = ipOk && isWifiEnabled && isErrorState
            _uiState.update {
                it.copy(
                    isFanReconnectVisible = shouldBeVisible && !isConnecting,
                    isFanConnecting = isConnecting
                )
            }
            if (!ipOk && !status.contains("Check Fan IP/Token")) {
                _uiState.update { it.copy(fanConnectionStatus = "Check Fan IP/Token") }
            }
        }.launchIn(viewModelScope)
    }

    private fun observeHrReconnectVisibility() {
        combine(
            uiState.map { it.hrDeviceStatus }.distinctUntilChanged(),
            uiState.map { it.isBluetoothEnabled }.distinctUntilChanged(),
            uiState.map { it.selectedHrDeviceAddress }.distinctUntilChanged()
        ) { status, isBTEnabled, address ->
            val isDisconnected = status == "Disconnected" || status == "No connection with HR sensor"
            val isFailed = status.startsWith("Error") || status.startsWith("No service")
            val isConnecting = status.startsWith("Connecting")
            val shouldBeVisible = isBTEnabled && address != null && (isDisconnected || isFailed) && !isConnecting
            _uiState.update {
                it.copy(
                    isHrReconnectVisible = shouldBeVisible,
                    isHrConnecting = isConnecting
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun observeUiStateChanges() {
        _uiState.map { it.currentHeartRate }.distinctUntilChanged().onEach { hr ->
            if (::fanController.isInitialized && _uiState.value.isAutoModeEnabled && _uiState.value.isFanOn) {
                fanController.updateSpeedBasedOnHr(hr)
            }
            if (::bleServer.isInitialized && _uiState.value.isHrSharingEnabled && _uiState.value.gattServerStatus.startsWith("Advertising")) {
                bleServer.forwardHeartRateToLocalClients(hr)
            }
        }.launchIn(viewModelScope)
        _uiState.map { it.hrDeviceStatus }.distinctUntilChanged().onEach { status ->
            if ((status.contains("Disconnected", ignoreCase = true) || status.startsWith("Error", ignoreCase = true)) &&
                _uiState.value.isAutoModeEnabled && ::fanController.isInitialized) {
                fanController.disableAutoMode()
                _uiState.update { it.copy(isAutoModeEnabled = false) }
                viewModelScope.launch {
                    _events.send("Auto Mode deactivated – no connection with HR sensor.")
                }
            }
        }.launchIn(viewModelScope)
        userPreferencesRepository.selectedHrDeviceAddressFlow.onEach { address ->
            if (_uiState.value.selectedHrDeviceAddress != address) {
                _uiState.update { it.copy(selectedHrDeviceAddress = address) }
                if (::bleClient.isInitialized) {
                    bleClient.setTargetDeviceAddress(address)
                    if (address != null && _uiState.value.isBluetoothEnabled) {
                        _uiState.update { it.copy(hrDeviceStatus = "Connecting...") }
                        bleClient.connect()
                    } else {
                        bleClient.disconnect()
                        val newStatus = if (!_uiState.value.isBluetoothEnabled) "Enable Bluetooth to connect" else "No HR sensor selected"
                        _uiState.update { it.copy(hrDeviceStatus = newStatus, isHrConnecting = false) }
                    }
                }
            }
            if (address == null && _uiState.value.isHrSharingEnabled) {
                toggleHrSharing(forceOff = true)
            }
        }.launchIn(viewModelScope)
    }

    private fun observeBleDeviceManagerChanges() {
        bleDeviceManager.scannedDevices.onEach { _uiState.update { ui -> ui.copy(scannedHrDevices = it) } }.launchIn(viewModelScope)
        bleDeviceManager.isScanning.onEach { _uiState.update { ui -> ui.copy(isScanning = it) } }.launchIn(viewModelScope)
        bleDeviceManager.scanStatus.onEach { _uiState.update { ui -> ui.copy(bleScanStatus = it) } }.launchIn(viewModelScope)
        userPreferencesRepository.pairedHrDevicesFlow.onEach { _uiState.update { ui -> ui.copy(pairedHrDevices = it) } }.launchIn(viewModelScope)
    }

    private fun initializeFanConnection() {
        if (!_uiState.value.isWifiEnabled) {
            _uiState.update { it.copy(fanConnectionStatus = "Enable Wi-Fi to connect", isFanConnecting = false) }
            return
        }
        val ip = _uiState.value.fanIpAddress
        val token = _uiState.value.fanToken
        if (ip.isBlank() || token.isBlank()) {
            _uiState.update { it.copy(fanConnectionStatus = "Check Fan IP/Token", isFanConnecting = false) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(fanConnectionStatus = "Connecting ${ip}...", isFanConnecting = true) }
            val fan = try {
                Fan(ip, token, viewModelScope).apply { initialize() }
            } catch (e: Exception) {
                _uiState.update { it.copy(fanConnectionStatus = "Connection Error: ${e.message}", isFanConnecting = false) }
                null
            }
            if (_uiState.value.fanIpAddress != ip || _uiState.value.fanToken != token) {
                return@launch
            }
            if (fan != null) {
                fanController = FanController(fan, viewModelScope, FanSpeedController(),
                    onStateUpdate = { newState -> _uiState.value = newState },
                    getState = { _uiState.value }
                )
                settingsManager.fanController = fanController
                _uiState.update { it.copy(fanConnectionStatus = "Connected to ${fan.deviceModel}", isFanConnecting = false) }
                fanController.fetchStatus()
            } else {
                settingsManager.fanController = null
            }
        }
    }

    private fun checkInitialStates() {
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        val isWifiOn = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        _uiState.update { it.copy(isWifiEnabled = isWifiOn) }
    }

    private fun registerStateReceivers() {
        val wifiRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        wifiNetworkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                _uiState.update { it.copy(isWifiEnabled = true) }
                initializeFanConnection()
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                _uiState.update { it.copy(isWifiEnabled = false, fanConnectionStatus = "Enable Wi-Fi to connect", isFanConnecting = false) }
            }
        }
        connectivityManager.registerNetworkCallback(wifiRequest, wifiNetworkCallback!!)
    }

    fun reconnectFan() {
        if (_uiState.value.isFanConnecting || !_uiState.value.isWifiEnabled) return
        if (_uiState.value.fanIpAddress.isBlank() || _uiState.value.fanToken.isBlank()) {
            _uiState.update { it.copy(fanConnectionStatus = "Check Fan IP/Token") }
            return
        }
        initializeFanConnection()
    }

    fun reconnectHr() {
        if (!::bleClient.isInitialized || !_uiState.value.isBluetoothEnabled || _uiState.value.isHrConnecting || _uiState.value.selectedHrDeviceAddress == null) {
            if (_uiState.value.selectedHrDeviceAddress == null) {
                _uiState.update { it.copy(hrDeviceStatus = "No HR sensor selected", isHrConnecting = false) }
            } else if (!_uiState.value.isBluetoothEnabled) {
                _uiState.update { it.copy(hrDeviceStatus = "Bluetooth disabled", isHrConnecting = false) }
            }
            return
        }
        _uiState.update { it.copy(hrDeviceStatus = "Connecting...") }
        bleClient.connect()
    }

    fun toggleAutoMode() {
        if (!::fanController.isInitialized) return
        val currentHr = _uiState.value.currentHeartRate
        val isHrValid = currentHr > 0
        if (_uiState.value.isAutoModeEnabled || isHrValid) {
            fanController.toggleAutoMode()
        }
    }

    fun toggleHrSharing(forceOff: Boolean = false) {
        val currentState = _uiState.value.isHrSharingEnabled
        val newState = if (forceOff) false else !currentState
        if (newState == currentState && !forceOff) return

        viewModelScope.launch {
            userPreferencesRepository.saveHrSharingEnabled(newState)
            _uiState.update { it.copy(isHrSharingEnabled = newState) }

            if (::bleServer.isInitialized) {
                if (newState && _uiState.value.isBluetoothEnabled && _uiState.value.selectedHrDeviceAddress != null) {
                    _uiState.update { it.copy(gattServerStatus = "Starting Server...") }
                    bleServer.startServer()
                } else {
                    bleServer.stopServer()
                    val newStatus = if (!_uiState.value.isBluetoothEnabled) {
                        "Bluetooth disabled"
                    } else if (_uiState.value.selectedHrDeviceAddress == null) {
                        "HR sensor not selected"
                    } else {
                        "Server Stopped"
                    }
                    _uiState.update { it.copy(gattServerStatus = newStatus) }
                }
            }
        }
    }

    fun onPermissionsGranted() {
        viewModelScope.launch {
            delay(1000)
            val btManager = getApplication<Application>().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val isBluetoothOn = btManager.adapter?.isEnabled == true
            if (::bleServer.isInitialized && _uiState.value.isBluetoothEnabled && _uiState.value.isHrSharingEnabled && _uiState.value.selectedHrDeviceAddress != null) {
                _uiState.update { it.copy(gattServerStatus = "Starting Server...") }
                bleServer.startServer()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unregisterReceiver(bluetoothStateReceiver)
        wifiNetworkCallback?.let { connectivityManager.unregisterNetworkCallback(it) }
        if (::bleClient.isInitialized) bleClient.disconnect()
        if (::bleServer.isInitialized) bleServer.stopServer()
        if (::bleDeviceManager.isInitialized) bleDeviceManager.stopBleScan()
    }

    fun onTermsAccepted() {
        viewModelScope.launch {
            userPreferencesRepository.saveTermsAccepted(true)
            _uiState.update { it.copy(haveTermsBeenAccepted = true) }
        }
    }
    fun toggleFan() { if (::fanController.isInitialized) fanController.toggleFan() }
    fun setManualFanSpeed(speed: Int) { if (::fanController.isInitialized) fanController.setManualSpeed(speed) }
    fun startBleScan() { if (::bleDeviceManager.isInitialized) bleDeviceManager.startBleScan() }
    fun pairDevice(device: BluetoothDevice) { if (::bleDeviceManager.isInitialized) bleDeviceManager.pairDevice(device) }
    fun removeDevice(device: PairedHrDevice) { if (::bleDeviceManager.isInitialized) bleDeviceManager.removeDevice(device) }
    fun chooseDevice(device: PairedHrDevice) { if (::bleDeviceManager.isInitialized) bleDeviceManager.chooseDevice(device) }
}