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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
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

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

// VM + Entity (ajusta paquetes)
import com.example.finazas.ui.goals.GoalViewModel
import com.example.finazas.data.local.entity.Goal
import com.example.finazas.navigation.AppRoute
import com.example.finazas.navigation.BottomBar

// Tu bottom bar y colores/estilos (ajusta paquetes según tu proyecto)


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
                onOpenDrawer = { /* no-op aquí */ }
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

    // Meta seleccionada (persistente en recomposiciones)
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    // Si no hay selección pero sí hay datos, selecciona la primera
    LaunchedEffect(goals) {
        if (selectedId == null && goals.isNotEmpty()) selectedId = goals.first().id
        if (goals.none { it.id == selectedId }) selectedId = goals.firstOrNull()?.id
    }

    val tiles = remember(goals, selectedId) {
        goals.map { it.toTileUi(selected = (it.id == selectedId)) }
    }
    val selectedGoal = goals.firstOrNull { it.id == selectedId }
    val canGoBack = navController.previousBackStackEntry != null

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
                        // Pantalla top-level: no hay back; abrimos el drawer
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "Menú")
                        }
                    }
                }
            )
        }
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
                navController.navigate(AppRoute.Pruebas.route) { // <- ir al CRUD
                    launchSingleTop = true
                    restoreState = true
                }
            })

            Spacer(Modifier.height(16.dp))

            // Detalle de la meta seleccionada (solo si hay alguna)
            if (selectedGoal != null) {
                GoalDetailCard(
                    title = selectedGoal.title,
                    currentText = "S/ ${"%.2f".format(selectedGoal.currentAmount/100.0)}",
                    targetText = "S/ ${"%.2f".format(selectedGoal.targetAmount/100.0)}",
                    percent = if (selectedGoal.targetAmount > 0)
                        (selectedGoal.currentAmount.toFloat() / selectedGoal.targetAmount.toFloat()).coerceIn(0f,1f)
                    else 0f
                )
            } else if (tiles.isNotEmpty()) {
                // fallback si por timing aún no hay selectedGoal consistente
                GoalDetailCard(title = tiles.first().title, currentText = "--", targetText = "--", percent = tiles.first().progress)
            }
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
            // indicador cuadrado a la izquierda
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
    percent: Float
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)),
        color = SurfaceGray
    ) {
        Column(Modifier.padding(14.dp)) {

            /* Encabezado con icono cuadrado y badge de progreso */
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                )
                Spacer(Modifier.width(10.dp))
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                ProgressBadge(percent = percent)
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
fun ProgressBadge(percent: Float) {
    // pequeño anillo con porcentaje (ej. 75%)
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 6f
            val diameter = kotlin.math.min(size.width, size.height) - stroke
            val topLeft = androidx.compose.ui.geometry.Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            // fondo
            drawArc(
                color = Color(0x33FFFFFF),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                size = androidx.compose.ui.geometry.Size(diameter, diameter),
                topLeft = topLeft
            )
            // progreso
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
        // barras
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
        // etiquetas
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




/* ----------------------------- PREVIEW --------------------------- */
@Preview(showBackground = true, backgroundColor = 0xFF0E0F11)
@Composable
fun PreviewMetas() { MetasApp() }