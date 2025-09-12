package com.example.finazas.ui.Screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.finazas.ui.Movement.MovementViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovementIncomeScreen(
    navController: NavHostController,
    vm: MovementViewModel = viewModel()
) {
    // Al abrir, forzamos tipo "Ingreso" y color verde si aún no está
    val form by vm.form.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        vm.startCreate()
        vm.onFormChange(type = "Ingreso", stripeColorHex = "#22C55E")  // verde por defecto
    }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    val palette = listOf("#22C55E", "#10B981", "#3B82F6", "#7C3AED", "#F59E0B", "#EF4444")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo ingreso", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { inner ->
        // único contenedor scrolleable
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Text("Detalles del ingreso", style = MaterialTheme.typography.titleMedium)
            }

            // Sugerencias de título
            item { TitleSuggestions(onPick = { vm.onFormChange(title = it) }) }

            // Formulario
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = form.title,
                            onValueChange = { vm.onFormChange(title = it) },
                            label = { Text("Título (ej. Sueldo, Reembolso, Venta)") },
                            isError = form.errors.containsKey("title"),
                            supportingText = { form.errors["title"]?.let { Text(it) } },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = form.amount,
                            onValueChange = { vm.onFormChange(amount = it) },
                            label = { Text("Monto (S/)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = form.errors.containsKey("amount"),
                            supportingText = { form.errors["amount"]?.let { Text(it) } },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // montos rápidos
                        QuickAmountsRow(onPick = { picked ->
                            val current = form.amount.trim()
                            val newText = if (current.isBlank()) picked else try {
                                val sum = (current.replace(",", ".").toDoubleOrNull() ?: 0.0) + picked.toDouble()
                                String.format(Locale.US, "%.2f", sum)
                            } catch (_: Exception) { picked }
                            vm.onFormChange(amount = newText)
                        })

                        OutlinedTextField(
                            value = form.date,
                            onValueChange = { vm.onFormChange(date = it) },
                            label = { Text("Fecha (yyyy-MM-dd)") },
                            trailingIcon = {
                                IconButton(onClick = { showDatePicker = true }) {
                                    Icon(Icons.Outlined.CalendarMonth, contentDescription = "Elegir")
                                }
                            },
                            isError = form.errors.containsKey("date"),
                            supportingText = { form.errors["date"]?.let { Text(it) } },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (showDatePicker) {
                            DatePickerDialog(
                                onDismissRequest = { showDatePicker = false },
                                confirmButton = {
                                    TextButton(onClick = {
                                        val ms = datePickerState.selectedDateMillis
                                        if (ms != null) vm.onFormChange(date = millisToISO(ms))
                                        showDatePicker = false
                                    }) { Text("OK") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
                                }
                            ) {
                                DatePicker(state = datePickerState)
                            }
                        }

                        Text("Color", fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            palette.forEach { hex ->
                                ColorSwatch(
                                    hex = hex,
                                    selected = hex.equals(form.stripeColorHex, ignoreCase = true)
                                ) { vm.onFormChange(stripeColorHex = hex) }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    // Aseguramos tipo "Ingreso" antes de guardar
                                    vm.onFormChange(type = "Ingreso")
                                    vm.save { navController.navigateUp() }
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Guardar ingreso") }

                            OutlinedButton(
                                onClick = { navController.navigateUp() },
                                modifier = Modifier.weight(1f)
                            ) { Text("Cancelar") }
                        }
                    }
                }
            }
        }
    }
}

/* ------------------- Sugerencias & UI helpers ------------------- */

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TitleSuggestions(onPick: (String) -> Unit) {
    val items = listOf("Sueldo", "Reembolso", "Venta", "Intereses", "Devolución", "Transferencia")
    Text("Sugerencias", style = MaterialTheme.typography.titleSmall)
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 3
    ) {
        items.forEach { label ->
            AssistChip(onClick = { onPick(label) }, label = { Text(label) })
        }
    }
}

@Composable
private fun QuickAmountsRow(onPick: (String) -> Unit) {
    val amounts = listOf("50.00", "100.00", "200.00", "500.00")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        amounts.forEach { a ->
            OutlinedButton(onClick = { onPick(a) }) { Text("+ $a") }
        }
    }
}

@Composable
private fun ColorSwatch(hex: String, selected: Boolean, onPick: () -> Unit) {
    val color = colorFromHexSafe(hex)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "swatchPress")
    var mod = Modifier
        .size(28.dp)
        .graphicsLayer(scaleX = scale, scaleY = scale)
        .clip(CircleShape)
        .background(color)
        .clickable(interactionSource = interaction, indication = null) { onPick() }
    if (selected) mod = mod.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
    Box(mod)
}

/* ------------------- Utils seguros ------------------- */

private fun colorFromHexSafe(hex: String): Color = try {
    val withHash = if (hex.startsWith("#")) hex else "#$hex"
    val argb = android.graphics.Color.parseColor(withHash)
    Color(argb)
} catch (_: Exception) {
    Color(0xFF22C55E.toInt())
}

private fun millisToISO(ms: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        .format(Date(ms))
