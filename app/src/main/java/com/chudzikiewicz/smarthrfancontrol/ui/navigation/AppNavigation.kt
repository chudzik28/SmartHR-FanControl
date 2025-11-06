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
package com.chudzikiewicz.smarthrfancontrol.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.chudzikiewicz.smarthrfancontrol.ui.MainViewModel
import com.chudzikiewicz.smarthrfancontrol.ui.screens.ConsentScreen
import com.chudzikiewicz.smarthrfancontrol.ui.screens.DeviceScanScreen
import com.chudzikiewicz.smarthrfancontrol.ui.screens.LibsScreen
import com.chudzikiewicz.smarthrfancontrol.ui.screens.MainScreen
import com.chudzikiewicz.smarthrfancontrol.ui.screens.SettingsScreen
import com.chudzikiewicz.smarthrfancontrol.R

sealed class Screen(val route: String, val label: String, val iconRes: Int) {
    object Main : Screen("main", "Main", R.drawable.ic_home)
    object HrDevices : Screen("hr_devices", "HR Sensors", R.drawable.ic_bluetooth)
    object Settings : Screen("settings", "Settings", R.drawable.ic_settings)
}

val items = listOf(
    Screen.Main,
    Screen.HrDevices,
    Screen.Settings,
)

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()

    if (!uiState.isReady) {
        return
    }

    val startDestination = if (uiState.haveTermsBeenAccepted) {
        "main_tabs"
    } else {
        "consent"
    }

    NavHost(navController = navController, startDestination = startDestination) {

        composable("consent") {
            ConsentScreen(
                onContinueClicked = {
                    viewModel.onTermsAccepted()
                    navController.navigate("main_tabs") {
                        popUpTo("consent") { inclusive = true }
                    }
                }
            )
        }

        composable("main_tabs") {
            MainTabsScreen(viewModel = viewModel)
        }
    }
}

@Composable
private fun MainTabsScreen(viewModel: MainViewModel) {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                painterResource(id = screen.iconRes),
                                contentDescription = null
                            )
                        },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController,
            startDestination = Screen.Main.route,
            Modifier.padding(innerPadding)
        ) {
            composable(Screen.Main.route) { MainScreen(viewModel) }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    navController = navController,
                    viewModel = viewModel
                )
            }
            composable(Screen.HrDevices.route) { DeviceScanScreen(viewModel) }
            composable("licenses") { LibsScreen(onNavigateUp = { navController.popBackStack() }) }
        }
    }
}