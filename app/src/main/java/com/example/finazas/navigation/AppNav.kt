package com.example.finazas.navigation



import android.content.Context
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotApplyResult
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import com.example.finazas.navigation.appGraph // si usas el builder externo
import com.example.finazas.ui.Screen.OnboardingScreen
import com.example.finazas.ui.navigation.CentralActionButton
import com.example.finazas.ui.navigation.MovementActionHub
import com.example.finazas.ui.theme.*
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.graph.SuccessorsFunction

// navigation/AppNav.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNav(startDestination: String) {
    val nav = rememberNavController()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val isOnboarding = backStackEntry?.destination
        ?.hierarchy
        ?.any { it.route == AppRoute.Onboarding.route } == true

    // 👇 Estado para el hub del botón verde
    var showHub by rememberSaveable { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = rememberDrawerState(DrawerValue.Closed),
        gesturesEnabled = !isOnboarding,
        drawerContent = { if (!isOnboarding) { /* tu drawer */ } }
    ) {
        Scaffold(
            bottomBar = {
                if (!isOnboarding) {
                    BottomBar(navController = nav)
                }
            },
            // 👇 pinta el FAB SOLO si no estamos en Onboarding
            floatingActionButton = {
                if (!isOnboarding) {
                    FloatingActionButton(
                        onClick = { showHub = true },
                        containerColor = Color.Green,           // tu verde (o el color que uses)
                        contentColor = Color.Black
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Acciones")
                    }
                }
            },
            floatingActionButtonPosition = FabPosition.Center   // 👈 centrado sobre el bottom bar
        ) { innerPadding ->
            NavHost(
                navController = nav,
                startDestination = startDestination,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                appGraph(nav, onOpenDrawer = { /* ... */ })

                composable(AppRoute.Onboarding.route) {
                    val ctx = LocalContext.current
                    OnboardingScreen(
                        onSkip = {
                            markOnboardingDone(ctx)
                            nav.navigate(AppRoute.Home.route) {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onFinish = {
                            markOnboardingDone(ctx)
                            nav.navigate(AppRoute.Home.route) {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    }

    // 👇 pinta el sheet del hub SOLO si no estamos en Onboarding
    if (showHub && !isOnboarding) {
        ModalBottomSheet(
            onDismissRequest = { showHub = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            MovementActionHub(
                onSelect = { action ->
                    showHub = false
                    when (action) {
                        "Ingreso" -> nav.navigate(AppRoute.MovementNew.withType("Ingreso")) {
                            launchSingleTop = true; restoreState = true
                        }
                        "Suscripción" -> nav.navigate(AppRoute.SubscriptionNew.create()) {
                            launchSingleTop = true; restoreState = true
                        }
                        "Egreso" -> nav.navigate(AppRoute.Subscriptions.route) {
                            launchSingleTop = true; restoreState = true
                        }
                    }
                }
            )
        }
    }
}

// NO composable
private fun markOnboardingDone(ctx: Context) {
    ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        .edit().putBoolean("onboarding_done", true).apply()
}




