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
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

@SuppressLint("MissingPermission")
class BleClientManager(
    private val context: Context,
    private val btAdapter: BluetoothAdapter,
    private var targetDeviceAddress: String?,
    private val hrUpdateListener: (Int) -> Unit,
    private val statusUpdateListener: (String) -> Unit
) {

    companion object {
        private const val TAG = "BleBridge.Client"
        private val UUID_HEART_RATE_SERVICE =
            UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        private val UUID_HEART_RATE_MEASUREMENT =
            UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        private val UUID_CLIENT_CHARACTERISTIC_CONFIG =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private var connectedGatt: BluetoothGatt? = null
    private var isDisconnectInitiatedByViewModel: Boolean = false

    fun setTargetDeviceAddress(address: String?) {
        this.targetDeviceAddress = address
        Log.d(TAG, "Target device address updated to: $address")
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.i(
                TAG,
                "Connection state changed for ${gatt.device.address}, status=$status newState=$newState"
            )

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                isDisconnectInitiatedByViewModel = false
                statusUpdateListener("Connected ${gatt.device.name ?: gatt.device.address}")
                Log.d(TAG, "Discovering services on target device...")
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from ${gatt.device.address}.")
                if (!isDisconnectInitiatedByViewModel) {
                    statusUpdateListener("Disconnected")
                }
                isDisconnectInitiatedByViewModel = false
                hrUpdateListener(0)
                gatt.close()
                if (gatt == connectedGatt) {
                    connectedGatt = null
                }
            }
        }

        @Suppress("DEPRECATION")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Log.i(TAG, "Services discovered on ${gatt.device.address}, status=$status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(UUID_HEART_RATE_SERVICE)
                val chr = service?.getCharacteristic(UUID_HEART_RATE_MEASUREMENT)
                if (chr == null) {
                    statusUpdateListener("No HR service at sensor")
                    return
                }

                gatt.setCharacteristicNotification(chr, true)
                val ccc = chr.getDescriptor(UUID_CLIENT_CHARACTERISTIC_CONFIG)
                if (ccc != null) {
                    ccc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(ccc)
                }
            } else {
                statusUpdateListener("Searching for HR services failed: $status")
            }
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid == UUID_HEART_RATE_MEASUREMENT) {
                val value = characteristic.value
                if (value != null) {
                    val hr = parseHeartRate(value)
                    if (hr > 0) {
                        statusUpdateListener("Connected")
                        hrUpdateListener(hr)
                    }
                }
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                statusUpdateListener("Subscribed to HR")
            } else {
                statusUpdateListener("HR Subscription error: $status")
            }
        }
    }

    fun connect() {
        val address = targetDeviceAddress
        if (address == null) {
            Log.w(TAG, "Cannot connect, targetDeviceAddress is null.")
            statusUpdateListener("No HR sensor selected")
            return
        }
        if (connectedGatt != null && connectedGatt!!.device.address != address) {
            Log.i(
                TAG,
                "Switching device. Disconnecting from ${connectedGatt!!.device.address} first."
            )
            disconnect()
        }
        if (connectedGatt != null && connectedGatt!!.device.address == address) {
            Log.d(TAG, "Already connected or connecting to the correct device. Ignoring.")
            return
        }
        isDisconnectInitiatedByViewModel = false
        val device = btAdapter.getRemoteDevice(address)
        statusUpdateListener("Connecting ${device.name ?: address}")
        connectedGatt = device.connectGatt(context, false, gattCallback)
    }

    fun disconnect() {
        if (connectedGatt == null) {
            hrUpdateListener(0)
            return
        }
        isDisconnectInitiatedByViewModel = true
        connectedGatt?.disconnect()
        connectedGatt?.close()
        connectedGatt = null
        hrUpdateListener(0)
    }

    private fun parseHeartRate(value: ByteArray): Int {
        if (value.isEmpty()) return 0
        val flags = value[0].toInt()
        val hrFormatUint16 = (flags and 0x01) != 0
        return if (hrFormatUint16 && value.size >= 3) {
            ByteBuffer.wrap(value, 1, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xFFFF
        } else if (value.size >= 2) {
            value[1].toInt() and 0xFF
        } else {
            0
        }
    }
}