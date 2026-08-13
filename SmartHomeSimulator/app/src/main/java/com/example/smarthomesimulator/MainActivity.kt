package com.example.smarthomesimulator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.smarthomesimulator.domain.model.dynamicFloors
import com.example.smarthomesimulator.domain.model.dynamicLayouts
import com.example.smarthomesimulator.ui.navigation.AppBottomBar
import com.example.smarthomesimulator.ui.screens.*
import com.example.smarthomesimulator.ui.theme.SmartHomeSimulatorTheme
import com.example.smarthomesimulator.ui.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartHomeSimulatorTheme {
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        val routesWithoutBottomBar = listOf("welcome", "login", null)
                        if (currentRoute !in routesWithoutBottomBar) {
                            AppBottomBar(navController)
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "welcome",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("welcome") {
                            WelcomeScreen(
                                onEnter = {
                                    navController.navigate("login") {
                                        popUpTo("welcome") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = {
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("home") {
                            val homeViewModel: HomeViewModel = hiltViewModel()
                            HomeScreen(
                                viewModel = homeViewModel,
                                onDeviceClick = { device ->
                                    navController.navigate("device/${device.id}")
                                },
                                onAddLayout = {
                                    navController.navigate("add_layout")
                                }
                            )
                        }
                        composable("reports") {
                            ReportsScreen(viewModel = hiltViewModel())
                        }
                        composable("floorplan") {
                            FloorPlanScreen(
                                viewModel = hiltViewModel(),
                                onDeviceClick = { device ->
                                    navController.navigate("device/${device.id}")
                                },
                                onAddLayout = {
                                    navController.navigate("add_layout")
                                }
                            )
                        }
                        composable("add_layout") {
                            AddLayoutScreen(
                                onLayoutAdded = { floor, layouts ->
                                    dynamicFloors.add(floor)
                                    dynamicLayouts[floor.id] = layouts
                                    navController.popBackStack()
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("edit_layout/{floorId}") { backStackEntry ->
                            val floorId = backStackEntry.arguments?.getString("floorId")
                            AddLayoutScreen(
                                floorToEditId = floorId,
                                onLayoutAdded = { floor, layouts ->
                                    val index = dynamicFloors.indexOfFirst { it.id == floor.id }
                                    if (index != -1) {
                                        dynamicFloors[index] = floor
                                    }
                                    dynamicLayouts[floor.id] = layouts
                                    navController.popBackStack()
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("alerts") {
                            AlertsScreen(viewModel = hiltViewModel())
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = hiltViewModel(),
                                onEditFloor = { floorId ->
                                    navController.navigate("edit_layout/$floorId")
                                },
                                onEditDevice = { deviceId ->
                                    navController.navigate("device/$deviceId")
                                },
                                onLogout = {
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("device/{deviceId}") {
                            DeviceDetailScreen(
                                viewModel = hiltViewModel(),
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
