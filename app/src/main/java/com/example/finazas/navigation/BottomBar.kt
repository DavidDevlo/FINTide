package com.example.finazas.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material3.MaterialTheme
import com.example.finazas.navigation.AppRoute


@Composable
fun BottomBar(navController: NavController) {
    val items = listOf(AppRoute.Home, AppRoute.Movimientos, AppRoute.Metas, AppRoute.Perfil)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val current = navBackStackEntry?.destination

    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        items.forEach { screen ->
            val selected = current?.hierarchy?.any { it.route == screen.route } == true

            val icon = when (screen) {
                AppRoute.Home        -> Icons.Outlined.Home
                AppRoute.Movimientos -> Icons.Outlined.ReceiptLong
                AppRoute.Metas       -> Icons.Outlined.Flag
                AppRoute.Perfil      -> Icons.Outlined.AccountCircle
                else -> Icons.Outlined.Home
            }
            val label = when (screen) {
                AppRoute.Home        -> "Inicio"
                AppRoute.Movimientos -> "Movimientos"
                AppRoute.Metas       -> "Metas"
                AppRoute.Perfil      -> "Perfil"
                else -> ""
            }

            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Box(
                        modifier = if (selected) {
                            Modifier.size(40.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        } else Modifier,
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (selected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                },
                label = {
                    Text(
                        text = label,
                        color = if (selected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.0f)
                )
            )
        }
    }
}
