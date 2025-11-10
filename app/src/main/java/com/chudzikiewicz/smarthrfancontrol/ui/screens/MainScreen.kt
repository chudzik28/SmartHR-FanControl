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

package com.chudzikiewicz.smarthrfancontrol.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chudzikiewicz.smarthrfancontrol.ui.MainUiState
import com.chudzikiewicz.smarthrfancontrol.ui.MainViewModel
import com.chudzikiewicz.smarthrfancontrol.ui.theme.WarningSalmon
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(key1 = true) {
        viewModel.events.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    val isFanConnected = uiState.fanConnectionStatus.startsWith("Connected")

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "SmartHR FanControl",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ConnectivityStatusRow(
                isWifiEnabled = uiState.isWifiEnabled,
                isBluetoothEnabled = uiState.isBluetoothEnabled
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CompactStatusCard(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    title = "Fan Status",
                    status = uiState.fanConnectionStatus,
                    isConnecting = uiState.isFanConnecting
                )
                CompactStatusCard(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    title = "HR Sensor Status",
                    status = uiState.hrDeviceStatus,
                    isConnecting = uiState.isHrConnecting
                )
            }

            ReconnectAlerts(
                isFanVisible = uiState.isFanReconnectVisible,
                onFanClick = { viewModel.reconnectFan() },
                isHrVisible = uiState.isHrReconnectVisible,
                onHrClick = { viewModel.reconnectHr() }
            )

            DataDisplayCard(
                heartRate = uiState.currentHeartRate,
                fanSpeed = uiState.currentFanSpeed
            )

            SupportButton(url = "https://buymeacoffee.com/chudzim")

            Spacer(modifier = Modifier.weight(1f))

            FanControlPanel(
                uiState = uiState,
                isPanelEnabled = isFanConnected && uiState.isFanOn,
                onAutoModeToggle = { viewModel.toggleAutoMode() },
                onManualSpeedChange = { viewModel.setManualFanSpeed(it) }
            )

            MasterToggleButton(
                isFanOn = uiState.isFanOn,
                isEnabled = isFanConnected,
                onClick = { viewModel.toggleFan() }
            )
        }
    }
}

@Composable
private fun ReconnectAlerts(
    isFanVisible: Boolean,
    onFanClick: () -> Unit,
    isHrVisible: Boolean,
    onHrClick: () -> Unit
) {
    if (isFanVisible || isHrVisible) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isFanVisible) {
                ReconnectButton(modifier = Modifier.weight(1f), label = "Reconnect Fan", onClick = onFanClick)
            }
            if (isHrVisible) {
                ReconnectButton(modifier = Modifier.weight(1f), label = "Reconnect HR sensor", onClick = onHrClick)
            }
        }
    }
}

@Composable
private fun ReconnectButton(modifier: Modifier = Modifier, label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CompactStatusCard(
    modifier: Modifier = Modifier,
    title: String,
    status: String,
    isConnecting: Boolean
) {
    ElevatedCard(
        modifier = modifier.heightIn(min = 100.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = status,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 3
            )
            Spacer(modifier = Modifier.weight(1f))
            if (isConnecting) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .padding(top = 4.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp).padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun FanControlPanel(
    uiState: MainUiState,
    isPanelEnabled: Boolean,
    onAutoModeToggle: () -> Unit,
    onManualSpeedChange: (Int) -> Unit
) {
    val isHrSensorActive = uiState.selectedHrDeviceAddress != null && uiState.hrDeviceStatus.startsWith("Connected")

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            if (!isHrSensorActive) {
                Text(
                    text = "Connect HR sensor to use Auto Mode",
                    color = WarningSalmon,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Manual Mode",
                    color = if (uiState.isAutoModeEnabled || !isPanelEnabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Switch(
                    checked = uiState.isAutoModeEnabled,
                    onCheckedChange = { onAutoModeToggle() },
                    enabled = isPanelEnabled && isHrSensorActive,
                )
                Text(
                    "Auto Mode",
                    color = if (!uiState.isAutoModeEnabled && isPanelEnabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "SPEED: ${uiState.currentFanSpeed}%",
                style = MaterialTheme.typography.titleMedium,
                color = if (!isPanelEnabled || uiState.isAutoModeEnabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = uiState.currentFanSpeed.toFloat(),
                onValueChange = { onManualSpeedChange(it.roundToInt()) },
                valueRange = 1f..100f,
                enabled = isPanelEnabled && !uiState.isAutoModeEnabled,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MasterToggleButton(
    isFanOn: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = isEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isFanOn) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
    ) {
        Text(
            text = if (isFanOn) "FAN OFF" else "FAN ON",
            fontSize = MaterialTheme.typography.titleMedium.fontSize,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ConnectivityStatusRow(isWifiEnabled: Boolean, isBluetoothEnabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ConnectivityStatusItem(label = "Wi-Fi", isEnabled = isWifiEnabled)
        ConnectivityStatusItem(label = "Bluetooth", isEnabled = isBluetoothEnabled)
    }
}

@Composable
private fun ConnectivityStatusItem(label: String, isEnabled: Boolean) {
    val icon = when (label) {
        "Wi-Fi" -> if (isEnabled) Icons.Default.Wifi else Icons.Default.WifiOff
        "Bluetooth" -> if (isEnabled) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled
        else -> if (isEnabled) Icons.Default.Wifi else Icons.Default.WifiOff
    }
    val color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val text = if (isEnabled) "Enabled" else "Disabled"

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$label: $text",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

@Composable
private fun DataDisplayCard(heartRate: Int, fanSpeed: Int) {
    val hrToDisplay = if (heartRate <= 0) "--" else heartRate.toString()

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DataColumn(label = "HEART RATE", value = hrToDisplay, unit = "BPM")
            HorizontalDivider(
                modifier = Modifier
                    .height(60.dp)
                    .width(1.dp),
                thickness = DividerDefaults.Thickness, color = DividerDefaults.color
            )
            DataColumn(label = "FAN SPEED", value = fanSpeed.toString(), unit = "%")
        }
    }
}

@Composable
private fun DataColumn(label: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(text = unit, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun SupportButton(url: String) {
    val uriHandler = LocalUriHandler.current

    OutlinedButton(
        onClick = { uriHandler.openUri(url) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.Coffee,
            contentDescription = "Buy me a coffee",
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )
        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
        Text("Buy me a coffee")
    }
}