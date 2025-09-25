package com.example.finazas.ui.Screen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

// VM + Entity (ajusta paquetes)
import com.example.finazas.ui.goals.GoalViewModel
import com.example.finazas.data.local.entity.Goal
import com.example.finazas.navigation.AppRoute

/* ----------------------------- THEME ----------------------------- */
private val DarkBg = Color(0xFF0E0F11)
private val SurfaceGray = Color(0xFF2B2F35)
private val TextOnDark = Color(0xFFECECEC)
private val Accent = Color(0xFFFFA726)
private val Success = Color(0xFF22C55E)
private val Subtle = Color(0xFFB9BEC5)

/* ------------------------------ APP WRAPPER ----------------------------- */
@Composable
fun MetasApp() {
    val nav = rememberNavController()
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            onPrimary = Color.Black,
            background = DarkBg,
            onBackground = TextOnDark,
            surface = DarkBg,
            onSurface = TextOnDark
        )
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            MetasScreen(
                navController = nav,
                onOpenDrawer = { /* no-op */ }
            )
        }
    }
}

/* ------------------------------ DATA MOCK (gráfico) ----------------------------- */
private val months = listOf("Ene","Feb","Mar","Abr","May","Jun","Jul")
private val bars = listOf(0.25f, 0.35f, 0.68f, 0.52f, 0.60f, 0.40f, 0.85f) // 0..1

/* --------------------------- UI MODEL PARA EL GRID ------------------------- */
data class GoalTileUi(
    val id: Long,
    val title: String,
    val amountText: String,  // "S/ 1,500.00 / S/ 2,000.00"
    val progress: Float,     // 0f..1f
    val color: Color,
    val selected: Boolean
)

private fun Goal.toTileUi(selected: Boolean): GoalTileUi {
    val progress = if (targetAmount > 0) (currentAmount.toFloat() / targetAmount.toFloat()).coerceIn(0f,1f) else 0f
    val amountText = "S/ ${"%.2f".format(currentAmount/100.0)} / S/ ${"%.2f".format(targetAmount/100.0)}"
    return GoalTileUi(
        id = id,
        title = title,
        amountText = amountText,
        progress = progress,
        color = hexToColor(colorHex),
        selected = selected
    )
}

private fun hexToColor(hex: String): Color = runCatching {
    val clean = hex.removePrefix("#")
    val argb = when (clean.length) {
        6 -> 0xFF000000 or clean.toLong(16)
        8 -> clean.toLong(16)
        else -> 0xFF888888
    }
    Color(argb.toULong())
}.getOrElse { Color(0xFF888888) }

/* --------------------------- MAIN SCREEN ------------------------- */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetasScreen(
    navController: NavHostController,
    onOpenDrawer: () -> Unit,
    goalsVm: GoalViewModel = viewModel()
) {
    val goals by goalsVm.goals.collectAsStateWithLifecycle()
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(goals) {
        if (selectedId == null && goals.isNotEmpty()) selectedId = goals.first().id
        if (goals.none { it.id == selectedId }) selectedId = goals.firstOrNull()?.id
    }

    val tiles = remember(goals, selectedId) {
        goals.map { it.toTileUi(selected = (it.id == selectedId)) }
    }
    val selectedGoal = goals.firstOrNull { it.id == selectedId }
    val canGoBack = navController.previousBackStackEntry != null

    // --- Dialog agregar monto ---
    var showAddDialog by rememberSaveable { mutableStateOf(false) }

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Metas de ahorro", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (canGoBack) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Atrás")
                        }
                    } else {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "Menú")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {
            /* GRID 2 columnas */
            if (tiles.isEmpty()) {
                Text("Agrega metas para verlas aquí.", color = Subtle)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    userScrollEnabled = false
                ) {
                    items(tiles, key = { it.id }) { t ->
                        GoalTile(
                            goal = t,
                            onClick = { selectedId = t.id }
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            AddNewGoal(onClick = {
                navController.navigate(AppRoute.Pruebas.route) {
                    launchSingleTop = true
                    restoreState = true
                }
            })

            Spacer(Modifier.height(16.dp))

            if (selectedGoal != null) {
                GoalDetailCard(
                    title = selectedGoal.title,
                    currentText = "S/ ${"%.2f".format(selectedGoal.currentAmount/100.0)}",
                    targetText = "S/ ${"%.2f".format(selectedGoal.targetAmount/100.0)}",
                    percent = if (selectedGoal.targetAmount > 0)
                        (selectedGoal.currentAmount.toFloat() / selectedGoal.targetAmount.toFloat()).coerceIn(0f,1f)
                    else 0f,
                    onProgressClick = { showAddDialog = true } // <-- botón del aro
                )
            } else if (tiles.isNotEmpty()) {
                GoalDetailCard(
                    title = tiles.first().title,
                    currentText = "--",
                    targetText = "--",
                    percent = tiles.first().progress,
                    onProgressClick = { /* no-op si no hay meta */ }
                )
            }
        }

        // --- Dialog Agregar Monto ---
        if (showAddDialog && selectedGoal != null) {
            AddAmountDialog(
                title = "Agregar a «${selectedGoal.title}»",
                onDismiss = { showAddDialog = false },
                onConfirmAmount = { amountInSoles ->
                    val centsToAdd = parseSolesToCents(amountInSoles)
                    if (centsToAdd <= 0) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Ingresa un monto válido mayor a 0")
                        }
                        return@AddAmountDialog
                    }
                    val newAmount = selectedGoal.currentAmount + centsToAdd
                    val reached = newAmount >= selectedGoal.targetAmount

                    // >>> Ajusta estos métodos a tu VM si tienen otro nombre <<<
                    if (reached) {
                        // Borra meta y felicita
                        goalsVm.delete(selectedGoal.id)
                        showAddDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar("Meta lograda, felicidades 🎉")
                        }
                    } else {
                        goalsVm.updateGoalAmount(selectedGoal.id, newAmount)
                        showAddDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "Se agregó S/ ${"%.2f".format(centsToAdd/100.0)} a la meta"
                            )
                        }
                    }
                }
            )
        }
    }
}

fun BottomBarMetas(selectedIndex: Int) {}

/* --------------------------- COMPONENTES ------------------------- */

@Composable
fun GoalTile(goal: GoalTileUi, onClick: () -> Unit) {
    val bg = if (goal.selected) Accent else SurfaceGray
    val text = if (goal.selected) Color.Black else TextOnDark
    val priceText = if (goal.selected) Color.Black else Subtle

    Surface(
        modifier = Modifier
            .height(74.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = bg,
        tonalElevation = 0.dp
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (goal.selected) Color.White.copy(alpha = 0.95f) else Color(0xFF50555B))
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(goal.title, color = text, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(goal.amountText, color = priceText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun AddNewGoal(onClick: () -> Unit) {
    val dash = PathEffect.dashPathEffect(floatArrayOf(18f, 14f), 0f)
    Box(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .drawBehind {
                drawRoundRect(
                    color = Accent,
                    style = Stroke(width = 3f, pathEffect = dash),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(22f, 22f)
                )
            }
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Agregar nueva meta de ahorro", color = Accent, fontWeight = FontWeight.SemiBold)
            Icon(Icons.Outlined.Add, null, tint = Accent)
        }
    }
}

@Composable
fun GoalDetailCard(
    title: String,
    currentText: String,
    targetText: String,
    percent: Float,
    onProgressClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)),
        color = SurfaceGray
    ) {
        Column(Modifier.padding(14.dp)) {

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                )
                Spacer(Modifier.width(10.dp))
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                ProgressBadgeButton(percent = percent, onClick = onProgressClick) // <-- ahora botón
            }

            Spacer(Modifier.height(10.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Ahorro actual", color = Subtle, fontSize = 12.sp)
                    Text(currentText, fontWeight = FontWeight.SemiBold)
                }
                Column(Modifier.weight(1f)) {
                    Text("Meta", color = Subtle, fontSize = 12.sp)
                    Text(targetText, color = Success, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(8.dp))
            BarsChart(
                values = bars,
                labels = months,
                barWidth = 20.dp,
                barColor = Color(0xFF8C9096),
                highlightIndex = bars.lastIndex,
                highlightColor = Accent,
                maxBarHeight = 110.dp
            )

            Spacer(Modifier.height(8.dp))
            Text("↓ 8% menos que el mes pasado", color = Color(0xFFE95555), fontSize = 12.sp)
        }
    }
}

@Composable
fun ProgressBadgeButton(percent: Float, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick), // <-- convierte en botón
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 6f
            val diameter = kotlin.math.min(size.width, size.height) - stroke
            val topLeft = androidx.compose.ui.geometry.Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            drawArc(
                color = Color(0x33FFFFFF),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                size = androidx.compose.ui.geometry.Size(diameter, diameter),
                topLeft = topLeft
            )
            drawArc(
                color = Accent,
                startAngle = -90f,
                sweepAngle = 360f * percent,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                size = androidx.compose.ui.geometry.Size(diameter, diameter),
                topLeft = topLeft
            )
        }
        Text("${(percent * 100).toInt()}%", color = Accent, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

@Composable
fun BarsChart(
    values: List<Float>,
    labels: List<String>,
    barWidth: Dp,
    barColor: Color,
    highlightIndex: Int,
    highlightColor: Color,
    maxBarHeight: Dp
) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .height(maxBarHeight),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            values.forEachIndexed { i, v ->
                val h = (maxBarHeight.value * v).dp
                val color = if (i == highlightIndex) highlightColor else barColor
                Box(
                    Modifier
                        .width(barWidth)
                        .height(h)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEach {
                Text(it, color = Subtle, fontSize = 12.sp)
            }
        }
    }
}

/* ----------------------------- DIALOG: AGREGAR MONTO --------------------------- */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAmountDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirmAmount: (String) -> Unit
) {
    var amount by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        error = null
                    },
                    label = { Text("Monto a agregar (S/)") },
                    placeholder = { Text("Ej: 50 o 50.75") },
                    singleLine = true,
                    isError = error != null
                )
                if (error != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (amount.isBlank()) {
                    error = "Este campo es obligatorio"
                } else {
                    onConfirmAmount(amount)
                }
            }) { Text("Agregar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

/* ----------------------------- HELPERS --------------------------- */
private fun parseSolesToCents(input: String): Int {
    // Acepta "50", "50.5", "50,5", "S/ 50.75", "s/50,75"
    val cleaned = input.lowercase()
        .replace("s/", "")
        .replace("s\\/", "")
        .replace("soles", "")
        .replace("sol", "")
        .replace(" ", "")
        .replace(",", ".")
        .trim()
    val value = cleaned.toDoubleOrNull() ?: return 0
    return kotlin.math.round(value * 100).toInt()
}

/* ----------------------------- PREVIEW --------------------------- */
@Preview(showBackground = true, backgroundColor = 0xFF0E0F11)
@Composable
fun PreviewMetas() { MetasApp() }
