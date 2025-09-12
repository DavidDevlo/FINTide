package com.example.finazas.navigation



import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.example.finazas.navigation.AppRoute
import kotlinx.coroutines.launch

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import com.example.finazas.navigation.appGraph // si usas el builder externo
import com.example.finazas.ui.navigation.CentralActionButton
import com.example.finazas.ui.navigation.MovementActionHub
import com.example.finazas.ui.theme.*

// navigation/AppNav.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNav(startDestination: String = AppRoute.Home.route) {
    val nav = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val onOpenDrawer: () -> Unit = { scope.launch { drawerState.open() } }

    // estado del hub (botón verde)
    var showHub by rememberSaveable { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            /* ... tu Drawer ... */
        }
    ) {
        Scaffold(
            bottomBar = { BottomBar(navController = nav) },
            floatingActionButton = {
                CentralActionButton(onClick = { showHub = true }) // botón verde centrado
            },
            floatingActionButtonPosition = FabPosition.Center
        ) { innerPadding ->
            NavHost(
                navController = nav,
                startDestination = startDestination,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                appGraph(nav, onOpenDrawer)
            }
        }
    }

    // HUB de acciones (Ingreso / Egreso / Suscripción)
    if (showHub) {
        ModalBottomSheet(
            onDismissRequest = { showHub = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            MovementActionHub(
                onSelect = { action ->
                    showHub = false // cierra el sheet primero
                    when (action) {
                        "Ingreso" -> {
                            nav.navigate(AppRoute.MovementNew.withType("Ingreso")) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        "Suscripción" -> {
                            nav.navigate(AppRoute.SubscriptionNew.create()) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        "Egreso" -> {
                            // lleva a la LISTA de suscripciones/recibos
                            nav.navigate(AppRoute.Subscriptions.route) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                }
            )
        }
    }
}

