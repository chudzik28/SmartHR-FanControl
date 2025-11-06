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
package com.chudzikiewicz.smarthrfancontrol.core.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map

data class FanSettings(val ip: String, val token: String)
data class PairedHrDevice(val address: String, val name: String?)
data class AlgorithmSettings(
    val minHr: Int, val maxHr: Int, val minSpeed: Int, val maxSpeed: Int,
    val smoothingFactor: Double, val exponent: Double
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        const val FAN_IP = "fan_ip_address"
        const val FAN_TOKEN = "fan_token"
        const val PAIRED_HR_DEVICES_DATA = "paired_hr_devices_data_v2"
        const val SELECTED_HR_DEVICE_ADDRESS = "selected_hr_device_address"
        const val HR_MIN = "hr_min"
        const val HR_MAX = "hr_max"
        const val SPEED_MIN = "speed_min"
        const val SPEED_MAX = "speed_max"
        const val SMOOTHING_FACTOR = "smoothing_factor"
        const val EXPONENT = "exponent"
        const val IS_HR_SHARING_ENABLED = "is_hr_sharing_enabled"
        const val TERMS_ACCEPTED = "terms_accepted"
    }

    private val encryptedPrefs: SharedPreferences by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "secure_user_settings",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun <T> SharedPreferences.observeKey(key: String, default: T): Flow<T> {
        return callbackFlow {
            val sendUpdate = {
                @Suppress("UNCHECKED_CAST")
                val value: T = when (default) {
                    is String? -> getString(key, default) as T
                    is Int -> getInt(key, default) as T
                    is Boolean -> getBoolean(key, default) as T
                    is Float -> getFloat(key, default) as T
                    is Long -> getLong(key, default) as T
                    is Set<*> -> (getStringSet(key, default as? Set<String>)
                        ?: emptySet<String>()) as T

                    else -> throw IllegalArgumentException("Unsupported type for SharedPreferences")
                }
                trySend(value)
            }
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, updatedKey ->
                if (updatedKey == key) {
                    sendUpdate()
                }
            }
            registerOnSharedPreferenceChangeListener(listener)
            sendUpdate()
            awaitClose { unregisterOnSharedPreferenceChangeListener(listener) }
        }
    }

    // --- Fan Settings ---
    val fanSettingsFlow: Flow<FanSettings> =
        encryptedPrefs.observeKey(PreferencesKeys.FAN_IP, "").map {
            FanSettings(
                ip = encryptedPrefs.getString(PreferencesKeys.FAN_IP, "") ?: "",
                token = encryptedPrefs.getString(PreferencesKeys.FAN_TOKEN, "") ?: ""
            )
        }

    fun saveFanSettings(ip: String, token: String) {
        encryptedPrefs.edit {
            putString(PreferencesKeys.FAN_IP, ip)
            putString(PreferencesKeys.FAN_TOKEN, token)
        }
    }

    // --- HR Devices Settings ---
    val pairedHrDevicesFlow: Flow<List<PairedHrDevice>> =
        encryptedPrefs.observeKey(PreferencesKeys.PAIRED_HR_DEVICES_DATA, emptySet<String>())
            .map { deviceStrings ->
                deviceStrings.map { deviceString ->
                    val parts = deviceString.split('|')
                    PairedHrDevice(
                        parts.getOrElse(0) { "" },
                        parts.getOrNull(1)?.takeIf { it != "null" })
                }.filter { it.address.isNotEmpty() }
            }

    val selectedHrDeviceAddressFlow: Flow<String?> =
        encryptedPrefs.observeKey(PreferencesKeys.SELECTED_HR_DEVICE_ADDRESS, null)

    fun savePairedHrDevices(devices: List<PairedHrDevice>) {
        val deviceStrings = devices.map { "${it.address}|${it.name}" }.toSet()
        encryptedPrefs.edit { putStringSet(PreferencesKeys.PAIRED_HR_DEVICES_DATA, deviceStrings) }
    }

    fun saveSelectedHrDevice(address: String?) {
        encryptedPrefs.edit { putString(PreferencesKeys.SELECTED_HR_DEVICE_ADDRESS, address) }
    }

    // --- Algorithm Settings ---
    val algorithmSettingsFlow: Flow<AlgorithmSettings> =
        encryptedPrefs.observeKey(PreferencesKeys.HR_MIN, 80).map {
            AlgorithmSettings(
                minHr = encryptedPrefs.getInt(PreferencesKeys.HR_MIN, 80),
                maxHr = encryptedPrefs.getInt(PreferencesKeys.HR_MAX, 170),
                minSpeed = encryptedPrefs.getInt(PreferencesKeys.SPEED_MIN, 10),
                maxSpeed = encryptedPrefs.getInt(PreferencesKeys.SPEED_MAX, 100),
                smoothingFactor = encryptedPrefs.getFloat(PreferencesKeys.SMOOTHING_FACTOR, 0.3f)
                    .toDouble(),
                exponent = encryptedPrefs.getFloat(PreferencesKeys.EXPONENT, 2.2f).toDouble()
            )
        }

    fun saveAlgorithmSettings(settings: AlgorithmSettings) {
        encryptedPrefs.edit {
            putInt(PreferencesKeys.HR_MIN, settings.minHr)
            putInt(PreferencesKeys.HR_MAX, settings.maxHr)
            putInt(PreferencesKeys.SPEED_MIN, settings.minSpeed)
            putInt(PreferencesKeys.SPEED_MAX, settings.maxSpeed)
            putFloat(PreferencesKeys.SMOOTHING_FACTOR, settings.smoothingFactor.toFloat())
            putFloat(PreferencesKeys.EXPONENT, settings.exponent.toFloat())
        }
    }

    val isHrSharingEnabledFlow: Flow<Boolean> =
        encryptedPrefs.observeKey(PreferencesKeys.IS_HR_SHARING_ENABLED, false)

    fun saveHrSharingEnabled(isEnabled: Boolean) {
        encryptedPrefs.edit { putBoolean(PreferencesKeys.IS_HR_SHARING_ENABLED, isEnabled) }
    }

    val termsAcceptedFlow: Flow<Boolean> =
        encryptedPrefs.observeKey(PreferencesKeys.TERMS_ACCEPTED, false)

    fun saveTermsAccepted(accepted: Boolean) {
        encryptedPrefs.edit {
            putBoolean(PreferencesKeys.TERMS_ACCEPTED, accepted)
        }
    }
}