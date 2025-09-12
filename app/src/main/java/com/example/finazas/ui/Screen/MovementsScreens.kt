package com.example.finazas.ui.screens.movements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.finazas.ui.Movement.MovementViewModel
import com.example.finazas.ui.subscriptions.SubscriptionViewModel
import com.example.finazas.data.local.entity.Subscription
import com.example.finazas.navigation.AppRoute
import com.example.finazas.ui.model.MovementRowUi
import com.example.finazas.ui.model.toRowUi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovementsScreen(
    navController: NavHostController,
    vm: MovementViewModel = viewModel(),
    subVm: SubscriptionViewModel = viewModel(),
    onOpenDrawer: () -> Unit
) {
    val movements = vm.movements.collectAsStateWithLifecycle().value
    val uiList = remember(movements) { movements.map { it.toRowUi() } }

    // Totales
    val incomeCents = remember(movements) {
        movements.filter { it.type.equals("Ingreso", true) }.sumOf { it.amount }
    }
    val outcomeCents = remember(movements) {
        movements.filter { it.type.equals("Egreso", true) || it.type.equals("Suscripción", true) }.sumOf { it.amount }
    }
    val incomeStr = "S/ " + String.format("%.2f", incomeCents / 100.0)
    val outcomeStr = "S/ " + String.format("%.2f", outcomeCents / 100.0)

    // Suscripciones próximas (30 días)
    val subsAll = subVm.subscriptions.collectAsStateWithLifecycle().value
    val now = remember { System.currentTimeMillis() }
    val in30 = remember(now) { now + 30L * 24 * 60 * 60 * 1000 }
    val dueSoon = remember(subsAll) { subsAll.filter { it.isActive && it.nextDueAt in now..in30 } }
        .sortedBy { it.nextDueAt }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Movimientos", fontWeight = FontWeight.SemiBold) })
        }
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Resumen
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryPill("Ingresos", incomeStr, Color(0xFF22C55E))
                SummaryPill("Egresos", outcomeStr, Color(0xFFE95555))
            }

            Spacer(Modifier.height(12.dp))

            // Gráfico (placeholder)
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Balance de esta semana", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("S/ 6,420.00", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    Text("Gráfico próximamente…", color = Color(0xFFB9BEC5))
                }
            }

            Spacer(Modifier.height(12.dp))

            // Próximos vencimientos de suscripciones
            Text("Próximos a vencer", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (dueSoon.isEmpty()) {
                Text("No hay suscripciones", color = Color(0xFFB9BEC5))
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(dueSoon) { s ->
                        SubscriptionChip(
                            sub = s,
                            onPay = {
                                navController.navigate(AppRoute.Subscriptions.route) {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Lista de movimientos
            if (uiList.isEmpty()) {
                Text("Aún no hay movimientos. Usa el botón verde para agregar uno.", color = Color(0xFFB9BEC5))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiList) { item -> MovementRow(item) }
                }
            }
        }
    }
}

@Composable
private fun SummaryPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(label, color = accent)
            Spacer(Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}


/* ---------- Suscripciones próximas (chip card) ---------- */
@Composable
private fun SubscriptionChip(
    sub: Subscription,
    onPay: () -> Unit
) {
    val days = daysLeft(sub.nextDueAt)
    val chipColor = when {
        days < 0  -> Color(0xFFE95555) // vencido
        days == 0 -> Color(0xFFFFA726) // hoy
        days <= 3 -> Color(0xFFFFA726) // pronto
        else      -> Color(0xFF8C9096)
    }

    ElevatedCard(
        modifier = Modifier
            .widthIn(min = 260.dp, max = 320.dp) // un poco más ancho para evitar cortes
            .defaultMinSize(minHeight = 96.dp)
    ) {
        Column(Modifier.padding(12.dp)) {

            // Fila: color + título (1 línea, elipsis)
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(hexToColor(sub.colorHex))
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    sub.title,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(6.dp))

            // Fila separada solo para el chip (a la derecha)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            when {
                                days < 0  -> "Vencido ${-days}d"
                                days == 0 -> "Hoy"
                                else      -> "Faltan ${days}d"
                            }
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = chipColor.copy(alpha = 0.18f),
                        labelColor = chipColor
                    )
                )
            }

            Spacer(Modifier.height(8.dp))

            // Importe + acción Pagar
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = sub.amountCents?.let { "S/ " + String.format("%.2f", it / 100.0) } ?: "~ S/ ?",
                    color = Color(0xFFECECEC)
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onPay) { Text("Pagar") }
            }
        }
    }
}

/* ---------- Row de movimientos (simple; usa tu versión si ya la tienes) ---------- */
@Composable
private fun MovementRow(m: MovementRowUi) {
    ListItem(
        headlineContent = { Text(m.title) },
        supportingContent = { Text("${m.subtitle} • ${m.date}", color = Color(0xFFB9BEC5)) },
        trailingContent = {
            Text(
                m.amountText,
                color = if (m.positive) Color(0xFF22C55E) else Color(0xFFE95555),
                style = MaterialTheme.typography.titleMedium
            )
        }
    )
    Divider()
}

/* ---------- Helpers ---------- */
private fun daysLeft(dueAt: Long): Int {
    val oneDay = 24 * 60 * 60 * 1000L
    val now = System.currentTimeMillis()
    return ((dueAt - now) / oneDay).toInt()
}


private fun ColorFromHex(hex: String): Color {
    val s = hex.trim()
    return try {
        val withHash = if (s.startsWith("#")) s else "#$s"
        val argbInt = android.graphics.Color.parseColor(withHash) // Int ARGB
        Color(argbInt)
    } catch (_: Exception) {
        Color(0xFF888888.toInt())
    }
}
@Suppress("FunctionName")
private fun hexToColor(hex: String): Color = ColorFromHex(hex)

private fun millisToISO(ms: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        .format(Date(ms))