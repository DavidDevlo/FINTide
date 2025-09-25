package com.example.finazas.ui.Screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.finazas.navigation.AppRoute
import com.example.finazas.ui.Movement.MovementViewModel
import com.example.finazas.ui.goals.GoalViewModel
import com.example.finazas.ui.model.GoalTileUi
import com.example.finazas.ui.model.toTileUi
import com.example.finazas.ui.model.toRowUi

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.room.Room
import com.example.finazas.data.local.AppDatabase
import com.example.finazas.data.local.entity.PaymentCard
import com.example.finazas.ui.profile.CardsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale



/* ----------------------------- THEME ----------------------------- */
private val DarkBg = Color(0xFF0E0F11)
private val SurfaceGray = Color(0xFF2B2F35)
private val TextOnDark = Color(0xFFECECEC)
private val Subtle = Color(0xFFB9BEC5)
private val Accent = Color(0xFFFFA726)
private val Success = Color(0xFF22C55E)
private val Danger = Color(0xFFE95555)
private val Purple = Color(0xFF7C3AED)



@Composable
fun HomeApp() {
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
            HomeScreen(
                navController = nav,
                onOpenDrawer = { /* no-op aquí */ }
            )
        }
    }
}

/* ---------------------------- MAIN SCREEN ------------------------ */

@Composable
fun HomeScreen(
    navController: NavHostController,
    onOpenDrawer: () -> Unit,
    goalsVm: GoalViewModel = viewModel(),
    movementsVm: MovementViewModel = viewModel(),
    cardsVm: CardsViewModel = viewModel()     // ⬅️ añade es
) {
    // Datos reales
    val mvm: MovementViewModel = viewModel()

    // 1) Lo que pintas en la lista (UI ya mapeada)
    val movementRows by mvm.rows.collectAsStateWithLifecycle()

    val goalsEntity by goalsVm.goals.collectAsStateWithLifecycle()
    val movementsEntity by movementsVm.movements.collectAsStateWithLifecycle()
    val cards by cardsVm.cards.collectAsStateWithLifecycle()
    // Mappers a modelos UI para los composables de abajo
    val goalsUi by remember(goalsEntity) {
        mutableStateOf(goalsEntity.map { it.toTileUi() })
    }
    val movementsUi by remember(movementsEntity) {
        mutableStateOf(movementsEntity.map { it.toRowUi() })
    }
    if (goalsUi.isEmpty()) {
        Text("Agrega metas para verlas aquí.", color = Subtle)
    } else {
        GoalsGrid(goals = goalsUi) // tu composable que espera GoalTileUi
    }

    if (movementRows.isEmpty()) {
        Text("Agrega movimientos para verlos aquí.", color = Subtle)
    } else {
        movementRows.forEach { MovementRow(it) } // tu composable que espera MovementRowUi
    }

    // Totales para InOutSummary
    val incomeCents = remember(movementsEntity) {
        movementsEntity.filter { it.type.equals("Ingreso", true) }.sumOf { it.amount }
    }
    val outcomeCents = remember(movementsEntity) {
        movementsEntity.filter { it.type.equals("Egreso", true) }.sumOf { it.amount }
    }
    val incomeStr = "S/ " + String.format("%.2f", incomeCents / 100.0)
    val outcomeStr = "S/ " + String.format("%.2f", outcomeCents / 100.0)

    Scaffold(
        topBar = {}
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            GreetingCard(username = "davidd")
            Spacer(Modifier.height(12.dp))

            HomeCardsCarousel(cards = cards, staticAmount = 6420.00) // el monto fijo que quieras


            Spacer(Modifier.height(10.dp))
            InOutSummary(income = incomeStr, outcome = outcomeStr)
            Spacer(Modifier.height(16.dp))

            SectionHeader(
                title = "Metas de ahorro",
                action = "Ver más"
            ) {
                navController.navigate(AppRoute.Metas.route) {
                    launchSingleTop = true
                    restoreState = true
                }
            }

            if (goalsUi.isEmpty()) {
                Text("Agrega metas para verlas aquí.", color = Subtle)
            } else {
                GoalsGrid(goals = goalsUi)
            }

            Spacer(Modifier.height(14.dp))
            SectionHeader(
                title = "Movimientos",
                action = "Ver más"
            ) {
                navController.navigate(AppRoute.Movimientos.route) {
                    launchSingleTop = true
                    restoreState = true
                }
            }

            Spacer(Modifier.height(6.dp))

            if (movementRows.isEmpty()) {
                Text("Agrega movimientos para verlos aquí.", color = Subtle)
            } else {
                movementRows.forEach { MovementRow(it) }
            }

            Spacer(Modifier.height(18.dp))
        }
    }
}

/* ===================== MODELOS UI + MAPPERS ====================== */

// Imports (ajusta el paquete)


// En tu HomeScreen, ya tienes:



/* --------------------------- COMPONENTES UI ---------------------- */
@Composable
fun GreetingCard(username: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp)),
        color = SurfaceGray
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Buenos días!", color = Subtle, fontSize = 12.sp)
                Text(username, fontWeight = FontWeight.SemiBold)
            }
            Icon(Icons.Outlined.Alarm, contentDescription = "Recordatorios", tint = TextOnDark)
        }
    }
}

/* ----------------------- BALANCE + CAROUSEL ---------------------- */
enum class BalanceStyle { ORANGE, WHITE }

@Composable
fun HomeCardsCarousel(cards: List<PaymentCard>, staticAmount: Double = 0.0) {
    if (cards.isEmpty()) {
        Text(
            "Aún no has agregado tarjetas.",
            color = Subtle,               // usa tu color ya definido en Home
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    // Orden: predeterminada primero, luego por última actualización/creación
    val rows = remember(cards) {
        cards.sortedWith(
            compareByDescending<PaymentCard> { it.isDefault }
                .thenByDescending { maxOf(it.updatedAt, it.createdAt) }
        )
    }

    val sdf = remember { java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("es", "PE")) }
    var page by remember { mutableStateOf(0) }

    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        rows.forEachIndexed { i, card ->
            if (i > 0) Spacer(Modifier.width(14.dp))
            val dateStr = sdf.format(java.util.Date(maxOf(card.updatedAt, card.createdAt)))

            if (card.isDefault) {
                // Naranja + marca visible
                BalanceCardOrange(
                    amount = staticAmount,
                    date = dateStr,
                    brand = card.brand
                ) { page = i }
            } else {
                // Blanca (tu versión no muestra brand)
                BalanceCardWhite(
                    amount = staticAmount,
                    date = dateStr
                ) { page = i }
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    DotsIndicator(count = rows.size, selected = page)
}



@Composable
fun BalanceCardOrange(amount: Double, date: String, brand: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .width(320.dp)
            .height(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = Color.Transparent
    ) {
        Box(
            Modifier.background(
                Brush.linearGradient(listOf(Color(0xFFFFA726), Color(0xFFF57C00))),
                shape = RoundedCornerShape(16.dp)
            ).padding(16.dp)
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Saldo", color = Color.Black.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Filled.MoreVert, contentDescription = null, tint = Color.Black.copy(alpha = 0.6f))
                }
                Spacer(Modifier.height(4.dp))
                Text(date, color = Color.Black.copy(alpha = 0.6f), fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                Text("$${"%,.2f".format(amount)}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(brand, color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, modifier = Modifier.align(Alignment.End))
            }
        }
    }
}

@Composable
fun BalanceCardWhite(amount: Double, date: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .width(320.dp)
            .height(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = Color(0xFFF7F7F7)
    ) {
        Box(Modifier.fillMaxSize().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Saldo", color = Purple, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Filled.MoreVert, contentDescription = null, tint = Color(0xFF6B7280))
            }
            Text(date, color = Color(0xFF6B7280), fontSize = 12.sp, modifier = Modifier.padding(top = 26.dp))
            Text(
                "$${"%,.2f".format(amount)}",
                color = Purple,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterStart).padding(top = 16.dp)
            )
        }
    }
}

@Composable
fun DotsIndicator(count: Int, selected: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        repeat(count) { i ->
            Box(
                Modifier
                    .padding(horizontal = 3.dp)
                    .size(if (i == selected) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (i == selected) Color.White else Color.White.copy(alpha = 0.4f))
            )
        }
    }
}

/* ----------------------- INGRESOS / EGRESOS ---------------------- */
@Composable
fun InOutSummary(income: String, outcome: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(12.dp)),
        color = SurfaceGray
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.ArrowDownward, null, tint = Success)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Ingresos", color = Subtle, fontSize = 12.sp)
                    Text(income, fontWeight = FontWeight.SemiBold)
                }
            }
            Box(
                Modifier
                    .width(1.dp)
                    .height(28.dp)
                    .background(Color.White.copy(alpha = 0.15f))
            )
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.ArrowUpward, null, tint = Danger)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Egresos", color = Subtle, fontSize = 12.sp)
                    Text(outcome, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/* --------------------------- METAS (2x2) ------------------------- */
@Composable
fun SectionHeader(title: String, action: String, onAction: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        Text(
            action,
            color = Accent,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable { onAction() }
        )
    }
}

@Composable
fun GoalsGrid(goals: List<GoalTileUi>) {
    // Muestra hasta 4, en 2x2, sin crashear si hay menos
    val a = goals.getOrNull(0)
    val b = goals.getOrNull(1)
    val c = goals.getOrNull(2)
    val d = goals.getOrNull(3)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            if (a != null) GoalTile(a, Modifier.weight(1f)) else Box(Modifier.weight(1f))
            if (b != null) GoalTile(b, Modifier.weight(1f)) else Box(Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            if (c != null) GoalTile(c, Modifier.weight(1f)) else Box(Modifier.weight(1f))
            if (d != null) GoalTile(d, Modifier.weight(1f)) else Box(Modifier.weight(1f))
        }
    }
}

@Composable
fun GoalTile(goal: GoalTileUi, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .height(86.dp)
            .clip(RoundedCornerShape(12.dp)),
        color = SurfaceGray
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        goal.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(goal.amountText, color = Subtle, fontSize = 13.sp)
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = Subtle)
            }
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF43484E))
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = goal.progress)
                        .background(goal.color)
                )
            }
        }
    }
}


/* --------------------------- MOVIMIENTOS ------------------------- */
@Composable
fun MovementRow(m: com.example.finazas.ui.Movement.MovementRowUi) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(RoundedCornerShape(12.dp))
            .padding(vertical = 5.dp),
        color = SurfaceGray
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(m.stripeColor)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(m.title, fontWeight = FontWeight.SemiBold)
                Text(m.subtitle, color = Subtle, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    m.amountText,
                    color = if (m.positive) Success else Danger,
                    fontWeight = FontWeight.SemiBold
                )
                Text(m.date, color = Subtle, fontSize = 12.sp)
            }
        }
    }
}
/* ----------------------------- BOTTOM BAR - home ------------------------ */


  /* ------------------------------- PREVIEW ------------------------- */
@Preview(showBackground = true, backgroundColor = 0xFF0E0F11)
@Composable
fun PreviewHome() {
    HomeApp()
                }

