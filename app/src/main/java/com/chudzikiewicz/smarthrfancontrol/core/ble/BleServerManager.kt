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
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.delay
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

@SuppressLint("MissingPermission")
class BleServerManager(
    private val context: Context,
    private val btManager: BluetoothManager,
    private val btAdapter: BluetoothAdapter,
    private val statusUpdateListener: (String) -> Unit
) {

    companion object {
        private const val TAG = "BleBridge.Server"
        private val UUID_HEART_RATE_SERVICE =
            UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        private val UUID_HEART_RATE_MEASUREMENT =
            UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        private val UUID_CLIENT_CHARACTERISTIC_CONFIG =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private var advertiser: BluetoothLeAdvertiser? = null
    private var gattServer: BluetoothGattServer? = null
    private var hrCharacteristicServer: BluetoothGattCharacteristic? = null
    private val subscribedDevices = mutableMapOf<BluetoothDevice, Boolean>()
    private var lastHrPacket: ByteArray = byteArrayOf(0x06, 0x00)

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            val safeDevice = device ?: return
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                subscribedDevices[safeDevice] = false
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                subscribedDevices.remove(safeDevice)
            }
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice?, requestId: Int, descriptor: BluetoothGattDescriptor?,
            preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?
        ) {
            val safeDevice = device ?: return
            if (descriptor?.uuid == UUID_CLIENT_CHARACTERISTIC_CONFIG) {
                val isEnable =
                    value?.contentEquals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) == true
                subscribedDevices[safeDevice] = isEnable
                if (responseNeeded) {
                    gattServer?.sendResponse(
                        safeDevice,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        offset,
                        value
                    )
                }
            } else {
                if (responseNeeded) {
                    gattServer?.sendResponse(
                        safeDevice,
                        requestId,
                        BluetoothGatt.GATT_WRITE_NOT_PERMITTED,
                        0,
                        null
                    )
                }
            }
        }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice?,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic?
        ) {
            val safeDevice = device ?: return
            if (characteristic?.uuid == UUID_HEART_RATE_MEASUREMENT) {
                gattServer?.sendResponse(
                    safeDevice,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    offset,
                    lastHrPacket
                )
            } else {
                gattServer?.sendResponse(
                    safeDevice,
                    requestId,
                    BluetoothGatt.GATT_READ_NOT_PERMITTED,
                    0,
                    null
                )
            }
        }

        override fun onNotificationSent(device: BluetoothDevice?, status: Int) { /* ... */
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            statusUpdateListener("Advertising as HR peripheral")
        }

        override fun onStartFailure(errorCode: Int) {
            statusUpdateListener("Advertising failed: $errorCode")
        }
    }

    @Suppress("DEPRECATION")
    suspend fun startServer() {
        var retries = 3
        while (retries > 0) {
            if (btAdapter.isMultipleAdvertisementSupported) {
                break
            }
            retries--
            delay(200)
        }

        if (!btAdapter.isMultipleAdvertisementSupported) {
            statusUpdateListener("BLE peripheral advertising not supported.")
            return
        }

        stopServer()

        advertiser = btAdapter.bluetoothLeAdvertiser
        gattServer = btManager.openGattServer(context, gattServerCallback)
        val service =
            BluetoothGattService(UUID_HEART_RATE_SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        hrCharacteristicServer = BluetoothGattCharacteristic(
            UUID_HEART_RATE_MEASUREMENT,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )

        lastHrPacket = byteArrayOf(0x06, 0x00)
        hrCharacteristicServer?.setValue(lastHrPacket)

        val cccDescriptor = BluetoothGattDescriptor(
            UUID_CLIENT_CHARACTERISTIC_CONFIG,
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
        )
        hrCharacteristicServer?.addDescriptor(cccDescriptor)
        service.addCharacteristic(hrCharacteristicServer)
        gattServer?.addService(service)
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .build()
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addServiceUuid(ParcelUuid(UUID_HEART_RATE_SERVICE))
            .build()
        advertiser?.startAdvertising(settings, data, advertiseCallback)
    }

    fun stopServer() {
        try {
            advertiser?.stopAdvertising(advertiseCallback)
            gattServer?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error while stopping server: ${e.message}")
        } finally {
            advertiser = null
            gattServer = null
            subscribedDevices.clear()
        }
    }

    @Suppress("DEPRECATION")
    fun forwardHeartRateToLocalClients(hr: Int) {
        val chr = hrCharacteristicServer ?: return
        val packet = if (hr < 256) {
            byteArrayOf(0x06, hr.toByte())
        } else {
            val bb = ByteBuffer.allocate(3).order(ByteOrder.LITTLE_ENDIAN)
            bb.put(0x07.toByte()).putShort(hr.toShort())
            bb.array()
        }

        lastHrPacket = packet
        chr.setValue(packet)

        for ((device, isSubscribed) in subscribedDevices) {
            if (isSubscribed) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gattServer?.notifyCharacteristicChanged(device, chr, false, packet)
                } else {
                    gattServer?.notifyCharacteristicChanged(device, chr, false)
                }
            }
        }
    }
}