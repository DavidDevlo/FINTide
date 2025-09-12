package com.example.finazas.ui.model

import androidx.compose.ui.graphics.Color
import com.example.finazas.data.local.entity.Goal
import com.example.finazas.data.local.entity.Movement

data class GoalTileUi(
    val id: Long,
    val title: String,
    val amountText: String,  // "S/ 1,500.00 / S/ 2,000.00"
    val progress: Float,     // 0f..1f
    val color: Color,
    val selected: Boolean = false     // <- CLAVE: default false
)

data class MovementRowUi(
    val title: String,
    val subtitle: String,   // "Ingreso" / "Egreso"
    val amountText: String, // "+ S/ 100.00" o "- S/ 50.00"
    val positive: Boolean,
    val date: String,       // dd/MM/yyyy
    val stripeColor: Color
)

fun Goal.toTileUi(selected: Boolean = false): GoalTileUi {
    val progress = if (targetAmount > 0)
        (currentAmount.toFloat() / targetAmount.toFloat()).coerceIn(0f, 1f)
    else 0f
    val amountText = "S/ ${"%.2f".format(currentAmount / 100.0)} / S/ ${"%.2f".format(targetAmount / 100.0)}"
    return GoalTileUi(
        id = id,
        title = title,
        amountText = amountText,
        progress = progress,
        color = hexToColor(colorHex),
        selected = selected
    )
}

fun Movement.toRowUi(): MovementRowUi {
    val positive = type.equals("Ingreso", ignoreCase = true)
    val sign = if (positive) "+" else "-"
    val amountText = "$sign S/ ${"%.2f".format(amount / 100.0)}"
    val dateStr = java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date(date))
    return MovementRowUi(
        title = title,
        subtitle = type,
        amountText = amountText,
        positive = positive,
        date = dateStr,
        stripeColor = hexToColor(stripeColorHex)
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
