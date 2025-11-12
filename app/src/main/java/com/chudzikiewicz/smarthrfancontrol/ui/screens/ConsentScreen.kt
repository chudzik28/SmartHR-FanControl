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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.chudzikiewicz.smarthrfancontrol.R
import com.chudzikiewicz.smarthrfancontrol.ui.components.HtmlText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsentScreen(
    onContinueClicked: () -> Unit
) {
    var privacyPolicyChecked by remember { mutableStateOf(false) }
    var termsOfUseChecked by remember { mutableStateOf(false) }
    val allAccepted = privacyPolicyChecked && termsOfUseChecked

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Welcome to SmartHR FanControl",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        bottomBar = {
            // 👇 dolny pasek z bezpiecznym marginesem nad systemową nawigacją
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars) // bezpieczna strefa
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onContinueClicked,
                    enabled = allAccepted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("ACCEPT & CONTINUE")
                }
            }
        },
        contentWindowInsets = WindowInsets(0) // 👈 wyłącz domyślne insets w Scaffoldzie
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Hey! I'm really happy you decided to download my app. " +
                        "It's a hobby project shared for free with everyone. " +
                        "I hope you'll find it useful! " +
                        "I do not receive any income for creating it. " +
                        "If you appreciate my work you can always support me by clicking the button on main screen.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                "Before you begin, please review and accept our Privacy Policy and Terms of Use.",
                style = MaterialTheme.typography.bodyMedium
            )

            ExpandableText(
                title = stringResource(id = R.string.info_privacy_policy_title),
                content = stringResource(id = R.string.info_privacy_policy_content)
            )
            CheckboxRow(
                checked = privacyPolicyChecked,
                onCheckedChange = { privacyPolicyChecked = it },
                label = "I have read and accept the Privacy Policy"
            )

            Spacer(modifier = Modifier.height(16.dp))

            ExpandableText(
                title = stringResource(id = R.string.info_terms_of_use_title),
                content = stringResource(id = R.string.info_terms_of_use_content)
            )
            CheckboxRow(
                checked = termsOfUseChecked,
                onCheckedChange = { termsOfUseChecked = it },
                label = "I have read and accept the Terms of Use"
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ExpandableText(
    title: String,
    content: String
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 8.dp),
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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                HtmlText(
                    html = content,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun CheckboxRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}
