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

import android.text.method.LinkMovementMethod
import android.widget.TextView
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import androidx.navigation.NavController
import com.chudzikiewicz.smarthrfancontrol.R
import com.chudzikiewicz.smarthrfancontrol.core.preferences.AlgorithmSettings
import com.chudzikiewicz.smarthrfancontrol.ui.MainUiState
import com.chudzikiewicz.smarthrfancontrol.ui.MainViewModel
import com.chudzikiewicz.smarthrfancontrol.ui.SettingsManager
import com.chudzikiewicz.smarthrfancontrol.ui.components.FanCurveChart
import com.chudzikiewicz.smarthrfancontrol.ui.components.HrAlgorithmInputFields
import com.chudzikiewicz.smarthrfancontrol.ui.components.generateChartPoints
import com.chudzikiewicz.smarthrfancontrol.ui.components.HtmlText

private enum class SettingsTab {
    FAN, HR, INFO
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val settingsManager = remember { viewModel.settingsManager }

    var showResetFanDialog by remember { mutableStateOf(false) }
    var showResetAlgoDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        settingsManager.syncInputStateWithActiveState()
        viewModel.events.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    if (showResetFanDialog) {
        ResetConfirmationDialog(
            title = "Reset Fan Config?",
            text = "This will clear the saved Fan IP and Token data. Are you sure?",
            onDismiss = { showResetFanDialog = false },
            onConfirm = {
                settingsManager.resetFanConfig()
                showResetFanDialog = false
            }
        )
    }

    if (showResetAlgoDialog) {
        ResetConfirmationDialog(
            title = "Reset Auto Mode Config?",
            text = "This will restore all algorithm settings (HR, Speed, etc.) to their defaults. Are you sure?",
            onDismiss = { showResetAlgoDialog = false },
            onConfirm = {
                settingsManager.resetAutoModeConfig()
                showResetAlgoDialog = false
            }
        )
    }

    var selectedTab by rememberSaveable { mutableStateOf(SettingsTab.FAN) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "SmartHR FanControl",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                windowInsets = WindowInsets(0)
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SettingsTabRow(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
            when (selectedTab) {
                SettingsTab.FAN -> FanConfigContent(
                    uiState = uiState,
                    settingsManager = settingsManager,
                    onResetClick = { showResetFanDialog = true }
                )

                SettingsTab.HR -> HrConfigContent(
                    uiState = uiState,
                    settingsManager = settingsManager,
                    onResetClick = { showResetAlgoDialog = true }
                )

                SettingsTab.INFO -> InfoContent(navController = navController)
            }
        }
    }
}

@Composable
private fun SettingsTabRow(selectedTab: SettingsTab, onTabSelected: (SettingsTab) -> Unit) {
    val tabs = enumValues<SettingsTab>()
    TabRow(selectedTabIndex = tabs.indexOf(selectedTab), modifier = Modifier.fillMaxWidth()) {
        tabs.forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                text = {
                    val title = when (tab) {
                        SettingsTab.FAN -> "Fan Config"
                        SettingsTab.HR -> "Auto Mode"
                        SettingsTab.INFO -> "Info"
                    }
                    Text(title)
                }
            )
        }
    }
}

@Composable
private fun FanConfigContent(
    uiState: MainUiState,
    settingsManager: SettingsManager,
    onResetClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Fan Connection Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = uiState.fanIpInput,
                    onValueChange = { settingsManager.onFanIpChanged(it) },
                    label = { Text("Fan IP Address") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                OutlinedTextField(
                    value = uiState.fanTokenInput,
                    onValueChange = { settingsManager.onFanTokenChanged(it) },
                    label = { Text("Fan Token (32 signs)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onResetClick, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Text("RESET FAN CONFIG")
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = { settingsManager.saveFanSettings() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("SAVE FAN SETTINGS", fontSize = MaterialTheme.typography.titleMedium.fontSize)
        }
    }
}

@Composable
private fun HrConfigContent(
    uiState: MainUiState,
    settingsManager: SettingsManager,
    onResetClick: () -> Unit
) {
    val settingsForChart = remember(
        uiState.minHrInput, uiState.maxHrInput,
        uiState.minSpeedInput, uiState.maxSpeedInput,
        uiState.exponentInput, uiState.smoothingInput
    ) {
        val minHr = uiState.minHrInput.toIntOrNull()
        val maxHr = uiState.maxHrInput.toIntOrNull()
        val minSpeed = uiState.minSpeedInput.toIntOrNull()
        val maxSpeed = uiState.maxSpeedInput.toIntOrNull()
        val smoothingFactor = uiState.smoothingInput.replace(',', '.').toDoubleOrNull()
        val exponent = uiState.exponentInput.replace(',', '.').toDoubleOrNull()

        val isInputStateValid = minHr != null && maxHr != null && minSpeed != null && maxSpeed != null &&
                smoothingFactor != null && exponent != null &&
                minHr < maxHr && minSpeed < maxSpeed

        if (isInputStateValid) {
            AlgorithmSettings(minHr, maxHr, minSpeed, maxSpeed, smoothingFactor, exponent)
        } else {
            AlgorithmSettings(
                minHr = uiState.minHr,
                maxHr = uiState.maxHr,
                minSpeed = uiState.minSpeed,
                maxSpeed = uiState.maxSpeed,
                smoothingFactor = uiState.smoothingFactor,
                exponent = uiState.exponent
            )
        }
    }

    val chartPoints = remember(settingsForChart) { generateChartPoints(settingsForChart) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Auto Mode Parameters",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                HrAlgorithmInputFields(
                    viewModelUiState = uiState,
                    settingsManager = settingsManager
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onResetClick, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Text("RESET AUTO MODE CONFIG")
                }
            }
        }
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Speed-HR Control Graph",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                FanCurveChart(chartPoints = chartPoints, settings = settingsForChart)
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = { settingsManager.saveAlgorithmSettings() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                "SAVE AUTO MODE SETTINGS",
                fontSize = MaterialTheme.typography.titleMedium.fontSize
            )
        }
    }
}

@Composable
private fun InfoContent(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ExpandableInfoCard(title = stringResource(id = R.string.info_instructions_title)) {
            HtmlText(html = stringResource(id = R.string.info_instructions_content))
        }
        ExpandableInfoCard(title = stringResource(id = R.string.info_supported_devices_title)) {
            HtmlText(html = stringResource(id = R.string.info_supported_devices_content))
        }
        ExpandableInfoCard(title = stringResource(id = R.string.info_privacy_policy_title)) {
            HtmlText(html = stringResource(id = R.string.info_privacy_policy_content))
        }
        ExpandableInfoCard(title = stringResource(id = R.string.info_terms_of_use_title)) {
            HtmlText(html = stringResource(id = R.string.info_terms_of_use_content))
        }
        ExpandableInfoCard(title = stringResource(id = R.string.info_gpl_license_title)) {
            HtmlText(html = stringResource(id = R.string.info_gpl_license_content))
        }

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    navController.navigate("licenses")
                },
            elevation = CardDefaults.elevatedCardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(id = R.string.info_oss_licenses_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.info_oss_licenses_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(id = R.string.info_acknowledgements_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                HtmlText(
                    html = stringResource(id = R.string.info_acknowledgements_content),
                )
            }
        }
    }
}

@Composable
private fun ExpandableInfoCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                )
            }
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))
                    content()
                }
            }
        }
    }
}

@Composable
fun ResetConfirmationDialog(
    title: String,
    text: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = text) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun HtmlText(html: String, modifier: Modifier = Modifier) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val linkColor = MaterialTheme.colorScheme.primary

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            TextView(context).apply {
                movementMethod = LinkMovementMethod.getInstance()
                setTextColor(textColor.toArgb())
                setLinkTextColor(linkColor.toArgb())
            }
        },
        update = {
            it.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
        }
    )
}