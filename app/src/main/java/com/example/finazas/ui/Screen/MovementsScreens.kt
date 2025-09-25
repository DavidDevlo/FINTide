package com.example.finazas.ui.screens.movements

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
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
            TopAppBar(
                title = { Text("Movimientos", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {

            item {
                // Resumen
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryPill("Ingresos", incomeStr, Color(0xFF22C55E))
                    SummaryPill("Egresos", outcomeStr, Color(0xFFE95555))
                }
            }

            item {
                // Gráfico real (hoy / semana / mes)
                InOutChartCard(
                    data = movements,
                    getMillis = { it.createdAt },   // cambia si tu campo se llama distinto
                    getType = { it.type },
                    getAmountCents = { it.amount }
                )
            }

            // Próximos vencimientos
            item { Text("Próximos a vencer", style = MaterialTheme.typography.titleMedium) }
            item {
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
            }

            // Lista de movimientos
            if (uiList.isEmpty()) {
                item { Text("Aún no hay movimientos. Usa el botón verde para agregar uno.", color = Color(0xFFB9BEC5)) }
            } else {
                items(uiList) { item -> MovementRow(item) }
            }
        }
    }}


/* ==================== CHART: Ingresos vs Egresos ==================== */

private enum class ChartRange { TODAY, WEEK, MONTH }

@Composable
private fun <T> InOutChartCard(
    data: List<T>,
    getMillis: (T) -> Long,
    getType: (T) -> String,
    getAmountCents: (T) -> Long
) {
    var range by remember { mutableStateOf(ChartRange.WEEK) }

    val chart = remember(data, range) {
        buildChartData(
            data = data,
            range = range,
            getMillis = getMillis,
            getType = getType,
            getAmountCents = getAmountCents
        )
    }

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            // Selector: Hoy / Semana / Mes
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RangeChip("Hoy",    selected = range == ChartRange.TODAY)  { range = ChartRange.TODAY }
                RangeChip("Semana", selected = range == ChartRange.WEEK)   { range = ChartRange.WEEK }
                RangeChip("Mes",    selected = range == ChartRange.MONTH)  { range = ChartRange.MONTH }

            }

            Spacer(Modifier.height(10.dp))

            Text(
                "Balance de ${when(range){
                    ChartRange.TODAY -> "hoy"
                    ChartRange.WEEK  -> "esta semana"
                    ChartRange.MONTH -> "este mes"
                }}",
                style = MaterialTheme.typography.titleMedium
            )

            val net = (chart.totalIncomeCents - chart.totalExpenseCents) / 100.0
            Spacer(Modifier.height(4.dp))
            Text("S/ " + String.format(Locale.US, "%,.2f", net), style = MaterialTheme.typography.headlineSmall)

            Spacer(Modifier.height(10.dp))

            LineChart(
                labels = chart.labels,
                income = chart.incomePoints,
                expense = chart.expensePoints
            )

            Spacer(Modifier.height(10.dp))

            // Leyenda
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                LegendDot(text = "Ingresos", color = Color(0xFF22C55E))
                LegendDot(text = "Egresos",  color = Color(0xFFE95555))
            }
        }
    }
}
@Composable
private fun RangeChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else Color(0xFF1E1F22),
        label = "rangechip-bg"
    )
    val fg by animateColorAsState(
        if (selected) Color.Black else MaterialTheme.colorScheme.onSurface,
        label = "rangechip-fg"
    )

    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text, color = fg, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun LegendDot(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(color))
        Spacer(Modifier.width(6.dp))
        Text(text)
    }
}

/* ------------------------- Data builder -------------------------- */

private data class ChartData(
    val labels: List<String>,
    val incomePoints: List<Float>,
    val expensePoints: List<Float>,
    val totalIncomeCents: Long,
    val totalExpenseCents: Long
)

private fun <T> buildChartData(
    data: List<T>,
    range: ChartRange,
    getMillis: (T) -> Long,
    getType: (T) -> String,
    getAmountCents: (T) -> Long
): ChartData {
    val tz = TimeZone.getDefault()
    val cal = java.util.Calendar.getInstance(tz, Locale("es", "PE"))

    fun startOfDay(ms: Long): Long {
        cal.timeInMillis = ms
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    val now = System.currentTimeMillis()
    val today0 = startOfDay(now)

    val bucketCount: Int
    val labels: List<String>
    val indexOf: (Long) -> Int
    val rangeStart: Long
    val rangeEnd: Long

    when (range) {
        ChartRange.TODAY -> {
            bucketCount = 24
            labels = (0 until 24).map { if (it % 3 == 0) "$it" else "" } // cada 3h
            rangeStart = today0
            rangeEnd = today0 + 24*60*60*1000L - 1
            indexOf = { ms ->
                (((ms - today0) / (60*60*1000L)).toInt()).coerceIn(0, 23)
            }
        }
        ChartRange.WEEK -> {
            bucketCount = 7
            labels = listOf("Dom","Lun","Mar","Mie","Jue","Vie","Sab")
            // inicio de semana (domingo)
            cal.timeInMillis = today0
            cal.set(java.util.Calendar.DAY_OF_WEEK, cal.firstDayOfWeek) // suele ser domingo
            rangeStart = startOfDay(cal.timeInMillis)
            rangeEnd = rangeStart + 7*24*60*60*1000L - 1
            indexOf = { ms -> (((startOfDay(ms) - rangeStart) / (24*60*60*1000L)).toInt()).coerceIn(0, 6) }
        }
        ChartRange.MONTH -> {
            bucketCount = 30
            labels = (1..30).map { if (it % 5 == 0) it.toString() else "" } // cada 5 días
            rangeEnd = today0 + 24*60*60*1000L - 1
            rangeStart = rangeEnd - 30L*24*60*60*1000L + 1
            indexOf = { ms -> (((startOfDay(ms) - rangeStart) / (24*60*60*1000L)).toInt()).coerceIn(0, 29) }
        }
    }

    val income = FloatArray(bucketCount)
    val expense = FloatArray(bucketCount)
    var incomeSum = 0L
    var expenseSum = 0L

    data.forEach { item ->
        val ms = getMillis(item)
        if (ms in rangeStart..rangeEnd) {
            val idx = indexOf(ms)
            val cents = getAmountCents(item)
            val type = getType(item).lowercase(Locale.ROOT)
            if (type == "ingreso") {
                income[idx] += (cents / 100f)
                incomeSum += cents
            } else if (type == "egreso" || type == "suscripción" || type == "suscripcion") {
                expense[idx] += (cents / 100f)
                expenseSum += cents
            }
        }
    }

    return ChartData(
        labels = labels,
        incomePoints = income.toList(),
        expensePoints = expense.toList(),
        totalIncomeCents = incomeSum,
        totalExpenseCents = expenseSum
    )
}

/* ------------------------- Canvas chart -------------------------- */

@Composable
private fun LineChart(
    labels: List<String>,
    income: List<Float>,
    expense: List<Float>,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(220.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(Color(0xFF1E1F22))
) {
    // padding interiores para ejes
    val padL = 32f
    val padR = 16f
    val padT = 18f
    val padB = 30f

    val gridColor = Color(0xFF5A5F66)
    val incomeColor = Color(0xFF22C55E)
    val expenseColor = Color(0xFFE95555)

    Canvas(modifier) {
        val w = size.width
        val h = size.height

        val plotW = w - padL - padR
        val plotH = h - padT - padB

        // Máximo Y
        val maxY = maxOf(
            (income.maxOrNull() ?: 0f),
            (expense.maxOrNull() ?: 0f),
            1f
        )
        // grid en 5 niveles
        val steps = 5
        val yStepVal = maxY / steps
        val yStepPx = plotH / steps

        // Grid horizontal (líneas punteadas)
        val dash = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
        for (i in 0..steps) {
            val y = padT + i * yStepPx
            drawLine(
                color = gridColor.copy(alpha = 0.4f),
                start = Offset(padL, y),
                end = Offset(w - padR, y),
                strokeWidth = 1f,
                pathEffect = dash
            )
        }

        // Eje X labels
        val n = labels.size.coerceAtLeast(1)
        val dx = if (n == 1) 0f else plotW / (n - 1)
        labels.forEachIndexed { i, label ->
            if (label.isNotBlank()) {
                drawContext.canvas.nativeCanvas.apply {
                    val p = android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#C8CDD3")
                        textSize = 24f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    drawText(
                        label,
                        padL + i * dx,
                        h - 8f,
                        p
                    )
                }
            }
        }

        fun yOf(v: Float): Float = padT + plotH - (v / maxY) * plotH

        // Línea Ingresos
        if (income.isNotEmpty()) {
            val path = androidx.compose.ui.graphics.Path()
            income.forEachIndexed { i, v ->
                val x = padL + i * dx
                val y = yOf(v)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color = incomeColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
        }

        // Línea Egresos
        if (expense.isNotEmpty()) {
            val path = androidx.compose.ui.graphics.Path()
            expense.forEachIndexed { i, v ->
                val x = padL + i * dx
                val y = yOf(v)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color = expenseColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
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