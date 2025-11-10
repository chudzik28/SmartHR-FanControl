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

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chudzikiewicz.smarthrfancontrol.core.preferences.PairedHrDevice
import com.chudzikiewicz.smarthrfancontrol.ui.MainUiState
import com.chudzikiewicz.smarthrfancontrol.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScanScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()

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
        }
    ) { paddingValues ->
        DeviceScanContent(
            uiState = uiState,
            onStartScan = { viewModel.startBleScan() },
            onPairDevice = { viewModel.pairDevice(it) },
            onChooseDevice = { viewModel.chooseDevice(it) },
            onRemoveDevice = { viewModel.removeDevice(it) },
            onToggleHrSharing = { viewModel.toggleHrSharing() },
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeviceScanContent(
    uiState: MainUiState,
    onStartScan: () -> Unit,
    onPairDevice: (BluetoothDevice) -> Unit,
    onChooseDevice: (PairedHrDevice) -> Unit,
    onRemoveDevice: (PairedHrDevice) -> Unit,
    onToggleHrSharing: () -> Unit,
    modifier: Modifier = Modifier
) {
    val newDevices = uiState.scannedHrDevices.filterNot { scanned ->
        uiState.pairedHrDevices.any { paired -> paired.address == scanned.address }
    }
    val isScannedListVisible = uiState.isScanning || newDevices.isNotEmpty()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.elevatedCardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Heart Rate Sharing",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = uiState.gattServerStatus,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = uiState.isHrSharingEnabled,
                        onCheckedChange = { onToggleHrSharing() },
                        enabled = uiState.isBluetoothEnabled && uiState.selectedHrDeviceAddress != null
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Text(
                "Paired Sensors",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        items(uiState.pairedHrDevices, key = { it.address }) { device ->
            PairedDeviceItem(
                modifier = Modifier.animateItem(),
                device = device,
                isSelected = device.address == uiState.selectedHrDeviceAddress,
                onChoose = { onChooseDevice(device) },
                onRemove = { onRemoveDevice(device) }
            )
        }
        item {
            Column(
                modifier = Modifier.padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onStartScan,
                    enabled = !uiState.isScanning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(if (uiState.isScanning) "SCANNING..." else "SEARCH HEART RATE SENSORS")
                }
                if (uiState.isScanning) {
                    LinearProgressIndicator(modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp))
                } else {
                    Spacer(modifier = Modifier
                        .height(4.dp)
                        .padding(top = 4.dp))
                }
            }
        }
        if (isScannedListVisible) {
            item {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Detected Devices",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            items(newDevices, key = { it.address }) { device ->
                ScannedDeviceItem(
                    modifier = Modifier.animateItem(),
                    device = device,
                    onPair = { onPairDevice(device) }
                )
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun PairedDeviceItem(
    modifier: Modifier = Modifier,
    device: PairedHrDevice,
    isSelected: Boolean,
    onChoose: () -> Unit,
    onRemove: () -> Unit
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onChoose() }) {
                Text(
                    text = device.name ?: "Unknown Device",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(text = device.address, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalButton(
                onClick = onChoose,
                colors = if (isSelected) ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) else ButtonDefaults.filledTonalButtonColors()
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "Unselect",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Unselect")
                } else {
                    Text("Select")
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun ScannedDeviceItem(
    modifier: Modifier = Modifier,
    device: BluetoothDevice,
    onPair: () -> Unit
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name ?: "Unknown Device",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(text = device.address, style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onPair) { Text("Pair") }
        }
    }
}