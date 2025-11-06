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
package com.chudzikiewicz.smarthrfancontrol.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.chudzikiewicz.smarthrfancontrol.ui.MainUiState
import com.chudzikiewicz.smarthrfancontrol.ui.SettingsManager

@Composable
fun HrAlgorithmInputFields(
    viewModelUiState: MainUiState,
    settingsManager: SettingsManager
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val onDoneAction: (String, (String) -> Unit) -> Unit = { value, flowAction ->
        flowAction(value)
        keyboardController?.hide()
    }
    Column {
        SettingsRow(label = "Heart Rate Range") {
            ControlledNumericSettingField(
                flowValue = viewModelUiState.minHrInput,
                onFlowAction = settingsManager::onMinHrChanged,
                onSaveAndHide = onDoneAction,
                label = { Text("HR Min") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            ControlledNumericSettingField(
                flowValue = viewModelUiState.maxHrInput,
                onFlowAction = settingsManager::onMaxHrChanged,
                onSaveAndHide = onDoneAction,
                label = { Text("HR Max") },
                modifier = Modifier.weight(1f)
            )
            InfoIconWithTooltip(
                title = "Heart Rate (Min/Max)",
                text = "This defines your 'effort zone.' Below HR Min, the fan operates at its minimum, above HR Max at its maximum. Clause: HR Min < HR Max."
            )
        }
        SettingsRow(label = "Fan Speed Range") {
            ControlledNumericSettingField(
                flowValue = viewModelUiState.minSpeedInput,
                onFlowAction = settingsManager::onMinSpeedChanged,
                onSaveAndHide = onDoneAction,
                label = { Text("Speed Min") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            ControlledNumericSettingField(
                flowValue = viewModelUiState.maxSpeedInput,
                onFlowAction = settingsManager::onMaxSpeedChanged,
                onSaveAndHide = onDoneAction,
                label = { Text("Speed Max") },
                modifier = Modifier.weight(1f)
            )
            InfoIconWithTooltip(
                title = "Fan Speed (Min/Max)",
                text = "Defines the minimum and maximum speed (in %) for auto mode. Clause: Speed Min < Speed Max."
            )
        }

        SettingsRow(label = "Reaction Curve") {
            ControlledDecimalSettingField(
                flowValue = viewModelUiState.exponentInput,
                onFlowAction = settingsManager::onExponentChanged,
                onSaveAndHide = onDoneAction,
                label = { Text("Exponent") },
                modifier = Modifier.weight(1f)
            )
            InfoIconWithTooltip(
                title = "Exponent",
                text = "Defines the nonlinearity of the response. A value > 1.0 causes the speed to increase more rapidly at higher heart rates. Range: 1.0 - 3.0."
            )
        }
        SettingsRow(label = "Smoothing Factor") {
            ControlledDecimalSettingField(
                flowValue = viewModelUiState.smoothingInput,
                onFlowAction = settingsManager::onSmoothingChanged,
                onSaveAndHide = onDoneAction,
                label = { Text("") },
                modifier = Modifier.weight(1f)
            )
            InfoIconWithTooltip(
                title = "Smoothing Factor",
                text = "To ensure comfortable and stable cooling, the application uses smart heart rate smoothing (EMA). This allows the fan to react quickly to genuine increases in effort while simultaneously ignoring temporary, accidental heart rate spikes and eliminating unpleasant, sudden changes in speed. Higher the value higher the  responsiveness. Range: 0.1-1.0"
            )
        }
    }
}

@Composable
private fun ControlledNumericSettingField(
    flowValue: String,
    onFlowAction: (String) -> Unit,
    onSaveAndHide: (String, (String) -> Unit) -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier
) {
    var localValue by remember(flowValue) { mutableStateOf(flowValue) }

    LaunchedEffect(flowValue) {
        localValue = flowValue
    }

    OutlinedTextField(
        value = localValue,
        onValueChange = { newValue ->
            localValue = newValue.filter { it.isDigit() }
        },
        label = label,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                if (!focusState.isFocused) {
                    localValue = flowValue
                }
            },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                onSaveAndHide(localValue, onFlowAction)
            }
        )
    )
}

@Composable
private fun ControlledDecimalSettingField(
    flowValue: String,
    onFlowAction: (String) -> Unit,
    onSaveAndHide: (String, (String) -> Unit) -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier
) {
    var localValue by remember(flowValue) { mutableStateOf(flowValue) }

    LaunchedEffect(flowValue) {
        localValue = flowValue
    }

    OutlinedTextField(
        value = localValue,
        onValueChange = { newValue ->
            val cleanValue = newValue.replace(',', '.')

            if (isValidDecimal(cleanValue)) {
                localValue = cleanValue
            }
        },
        label = label,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                if (!focusState.isFocused) {
                    localValue = flowValue
                }
            },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                onSaveAndHide(localValue, onFlowAction)
            }
        )
    )
}

private fun isValidDecimal(input: String): Boolean {
    if (input.isEmpty()) return true
    input.toDoubleOrNull() ?: return false

    val parts = input.split('.')
    return if (parts.size > 1) {
        parts[1].length <= 1
    } else {
        true
    }
}

@Composable
private fun SettingsRow(
    label: String,
    content: @Composable RowScope.() -> Unit
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            content()
        }
    }
}

@Composable
private fun InfoIconWithTooltip(title: String, text: String) {
    var showDialog by remember { mutableStateOf(false) }
    IconButton(onClick = { showDialog = true }) {
        Icon(imageVector = Icons.Default.Info, contentDescription = "Info")
    }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title) },
            text = { Text(text) },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}