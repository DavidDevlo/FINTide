package com.example.finazas.navigation



import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.finazas.ui.Movement.MovementViewModel
import com.example.finazas.ui.Screen.CardsScreen
import com.example.finazas.ui.Screen.GoalsScreenEntry
import com.example.finazas.ui.Screen.HomeScreen
import com.example.finazas.ui.Screen.MetasScreen
import com.example.finazas.ui.Screen.MovementIncomeScreen
import com.example.finazas.ui.Screen.ProfileScreen
import com.example.finazas.ui.Screen.SubscriptionWizardScreen
import com.example.finazas.ui.Screen.SubscriptionsListScreen
import com.example.finazas.ui.screens.movements.MovementsScreen
import com.example.finazas.ui.subscriptions.SubscriptionViewModel

fun NavGraphBuilder.appGraph(nav: NavHostController, onOpenDrawer: () -> Unit) {

    composable(AppRoute.Home.route) {
        HomeScreen(navController = nav, onOpenDrawer = onOpenDrawer)
    }
    composable(AppRoute.Pruebas.route) {
        GoalsScreenEntry(navController = nav, onOpenDrawer = onOpenDrawer)
    }
    composable(AppRoute.Metas.route) {
        MetasScreen(navController = nav, onOpenDrawer = onOpenDrawer)
    }

    composable(AppRoute.Movimientos.route) {
        MovementsScreen(navController = nav, onOpenDrawer = onOpenDrawer)
    }

    composable(AppRoute.SubscriptionNew.route,
        arguments = listOf(navArgument("id"){ type = NavType.LongType; defaultValue = -1L })
    ) { back ->
        val id = back.arguments?.getLong("id") ?: -1L
        SubscriptionWizardScreen(navController = nav, editingId = if (id >= 0) id else null)
    }


    composable(AppRoute.Subscriptions.route) {
        val sVm: SubscriptionViewModel = viewModel()
        SubscriptionsListScreen(
            navController = nav,
            vm = sVm,
            onBack = {
                // si no hay back stack (por venir de bottom bar o de otra parte), vuelve a Movimientos
                if (!nav.navigateUp()) {
                    nav.navigate(AppRoute.Movimientos.route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        )
    }

    composable(
        route = AppRoute.MovementNew.route,  // "movement_new?type={type}"
        arguments = listOf(
            navArgument("type") {
                type = NavType.StringType
                nullable = true
                defaultValue = "Ingreso" // si no llega, será Ingreso
            }
        )
    ) { backStackEntry ->
        val vm: MovementViewModel = viewModel()
        val typeArg = backStackEntry.arguments?.getString("type") ?: "Ingreso"

        // Inicializa una sola vez por instancia de este destino
        LaunchedEffect(backStackEntry.id) {
            vm.startCreate()
            vm.onFormChange(type = typeArg) // fija el tipo (Ingreso/Egreso)
        }

        MovementIncomeScreen(
            navController = nav,
            vm = vm)
    }



    composable(AppRoute.Perfil.route) { ProfileScreen(nav) }
    composable(AppRoute.Cards.route) { CardsScreen(nav) }






}

