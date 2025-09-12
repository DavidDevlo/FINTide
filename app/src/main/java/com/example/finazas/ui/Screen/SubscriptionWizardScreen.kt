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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.InvertColors
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.LocalMovies
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.NetworkWifi
import androidx.compose.material.icons.outlined.PhoneIphone
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.finazas.ui.subscriptions.SubscriptionViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionWizardScreen(
    navController: NavHostController,
    vm: SubscriptionViewModel = viewModel(),
    editingId: Long? = null
) {
    // Cargar modo edición/creación
    LaunchedEffect(editingId) {
        if (editingId != null && editingId >= 0) vm.loadForEdit(editingId) else vm.startCreate()
    }
    val form by vm.form.collectAsStateWithLifecycle()

    // Tabs blindadas (evita index fuera de rango)
    var tabIndex by rememberSaveable { mutableStateOf(0) } // 0=Suscripción, 1=Recibo
    val tabs = listOf("Suscripción", "Recibo")
    if (tabIndex !in tabs.indices) tabIndex = 0
    val isReceipt = tabIndex == 1

    // Sugerencias
    val subBrands = remember {
        listOf(
            Brand("Netflix", "#E50914", Icons.Outlined.LocalMovies),
            Brand("Spotify Premium", "#22C55E", Icons.Outlined.MusicNote),
            Brand("Disney+", "#3B82F6", Icons.Outlined.PlayCircle),
            Brand("Prime Video", "#60A5FA", Icons.Outlined.LocalMovies),
            Brand("YouTube Premium", "#EF4444", Icons.Outlined.PlayCircle),
            Brand("HBO Max", "#7C3AED", Icons.Outlined.LocalMovies)
        )
    }
    val bills = remember {
        listOf(
            Brand("Luz", "#F59E0B", Icons.Outlined.Bolt),
            Brand("Agua", "#60A5FA", Icons.Outlined.InvertColors),
            Brand("Gas", "#FB923C", Icons.Outlined.LocalFireDepartment),
            Brand("Internet", "#22C55E", Icons.Outlined.NetworkWifi),
            Brand("Teléfono", "#A78BFA", Icons.Outlined.PhoneIphone),
            Brand("Universidad", "#7C3AED", Icons.Outlined.School)
        )
    }

    // Paleta de color sugerida
    val palette = listOf("#22C55E", "#F59E0B", "#EF4444", "#3B82F6", "#7C3AED", "#10B981", "#F472B6")

    // DatePicker
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    // Presets suaves al cambiar de tab
    LaunchedEffect(tabIndex) {
        if (isReceipt && !vm.form.value.variableAmount) vm.onFormChange(variableAmount = true)
        if (!isReceipt && vm.form.value.variableAmount) vm.onFormChange(variableAmount = false)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (editingId != null && editingId >= 0) "Editar suscripción"
                        else "Nueva suscripción / recibo",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { inner ->
        // ÚNICO contenedor scrolleable (evita anidar scrollers)
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Tabs
            item {
                TabRow(selectedTabIndex = tabIndex, containerColor = Color.Transparent) {
                    tabs.forEachIndexed { i, title ->
                        Tab(
                            selected = tabIndex == i,
                            onClick = { tabIndex = i },
                            text = { Text(title) }
                        )
                    }
                }
            }

            // Sugerencias
            item { Text("Sugerencias", style = MaterialTheme.typography.titleMedium) }
            item {
                SuggestionFlow(
                    items = if (isReceipt) bills else subBrands,
                    onSelect = { b ->
                        vm.onFormChange(
                            title = b.name,
                            colorHex = b.colorHex,
                            frequency = "MONTHLY",
                            variableAmount = isReceipt
                        )
                    }
                )
            }

            // Formulario
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Título
                        OutlinedTextField(
                            value = form.title,
                            onValueChange = { vm.onFormChange(title = it) },
                            label = { Text("Título (proveedor)") },
                            singleLine = true,
                            isError = form.errors.containsKey("title"),
                            supportingText = { form.errors["title"]?.let { Text(it) } },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Monto + Variable
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = form.amount,
                                onValueChange = { vm.onFormChange(amount = it) },
                                label = { Text("Monto (S/)") },
                                enabled = !form.variableAmount,
                                keyboardOptions = KeyboardOptions.Default,
                                isError = form.errors.containsKey("amount"),
                                supportingText = {
                                    Text(if (form.variableAmount) "Monto variable" else form.errors["amount"] ?: "")
                                },
                                modifier = Modifier.weight(1f)
                            )
                            Column(
                                modifier = Modifier.wrapContentWidth(),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = form.variableAmount,
                                        onCheckedChange = { vm.onFormChange(variableAmount = it) }
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Variable")
                                }
                            }
                        }

                        // Frecuencia
                        Text("Frecuencia", fontWeight = FontWeight.SemiBold)
                        FrequencyFlow(
                            current = form.frequency,
                            onPick = { vm.onFormChange(frequency = it) }
                        )

                        if (form.frequency.equals("CUSTOM", ignoreCase = true)) {
                            OutlinedTextField(
                                value = form.intervalDays,
                                onValueChange = { vm.onFormChange(intervalDays = it) },
                                label = { Text("Intervalo (días)") },
                                isError = form.errors.containsKey("intervalDays"),
                                supportingText = { form.errors["intervalDays"]?.let { Text(it) } },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Próximo vencimiento
                        OutlinedTextField(
                            value = form.nextDueIso,
                            onValueChange = { vm.onFormChange(nextDueIso = it) },
                            label = { Text("Próximo vencimiento (yyyy-MM-dd)") },
                            isError = form.errors.containsKey("nextDueIso"),
                            supportingText = { form.errors["nextDueIso"]?.let { Text(it) } },
                            trailingIcon = { TextButton(onClick = { showDatePicker = true }) { Text("Elegir") } },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (showDatePicker) {
                            DatePickerDialog(
                                onDismissRequest = { showDatePicker = false },
                                confirmButton = {
                                    TextButton(onClick = {
                                        val ms = datePickerState.selectedDateMillis
                                        if (ms != null) vm.onFormChange(nextDueIso = millisToISO(ms))
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

                        // Autopago
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = form.autoPay, onCheckedChange = { vm.onFormChange(autoPay = it) })
                            Spacer(Modifier.width(8.dp))
                            Text("Autopago")
                        }

                        // Color
                        Text("Color", fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            palette.forEach { hex ->
                                val selected = hex.equals(form.colorHex, ignoreCase = true)
                                ColorSwatch(hex = hex, selected = selected) { vm.onFormChange(colorHex = hex) }
                            }
                        }

                        // Activo (solo en edición)
                        if (editingId != null && editingId >= 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(checked = form.isActive, onCheckedChange = { vm.onFormChange(isActive = it) })
                                Spacer(Modifier.width(8.dp))
                                Text(if (form.isActive) "Activo" else "Inactivo")
                            }
                        }

                        // Acciones
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { vm.save { navController.navigateUp() } },
                                modifier = Modifier.weight(1f)
                            ) { Text("Guardar") }

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

/* ----------------- Sugerencias ----------------- */

private data class Brand(
    val name: String,
    val colorHex: String,
    val icon: ImageVector? = null
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuggestionFlow(
    items: List<Brand>,
    onSelect: (Brand) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        maxItemsInEachRow = 3
    ) {
        items.forEach { b -> BrandTile(b, onSelect) }
    }
}

@Composable
private fun BrandTile(b: Brand, onSelect: (Brand) -> Unit) {
    ElevatedCard(
        onClick = { onSelect(b) },
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.height(72.dp)
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(ColorFromHex(b.colorHex)),
                contentAlignment = Alignment.Center
            ) {
                if (b.icon != null) {
                    Icon(b.icon, null, tint = Color.Black.copy(alpha = 0.8f))
                } else {
                    Text(b.name.first().uppercase(), color = Color.Black, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                b.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/* ----------------- Frecuencia ----------------- */

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FrequencyFlow(current: String, onPick: (String) -> Unit) {
    val options = listOf(
        "Mensual" to "MONTHLY",
        "Semanal" to "WEEKLY",
        "Anual" to "YEARLY",
        "Personalizada" to "CUSTOM"
    )
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 2
    ) {
        options.forEach { (label, value) ->
            FrequencyChip(label, value, current, onPick)
        }
    }
}

@Composable
private fun FrequencyChip(
    label: String,
    value: String,
    current: String,
    onPick: (String) -> Unit
) {
    val selected = current.equals(value, ignoreCase = true)
    AssistChip(
        onClick = { onPick(value) },
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .15f)
            else MaterialTheme.colorScheme.surfaceVariant,
            labelColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

/* ----------------- Picker de color ----------------- */

@Composable
private fun ColorSwatch(hex: String, selected: Boolean, onPick: () -> Unit) {
    val color = ColorFromHex(hex)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (pressed) 0.96f else 1f, label = "swatchPress")

    var base = Modifier
        .size(28.dp)
        .graphicsLayer(scaleX = scale, scaleY = scale)
        .clip(CircleShape)
        .background(color)
        .clickable(interactionSource = interaction, indication = null) { onPick() }

    // Solo borde cuando está seleccionado (evita border 0.dp)
    if (selected) {
        base = base.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
    }
    Box(base)
}

/* ----------------- Utils: Color seguro & fecha ----------------- */

/**
 * Color seguro en sRGB desde "#RRGGBB" o "#AARRGGBB".
 * Usa ctor Int (ARGB) → NO packed ULong (evita crash de color space).
 */
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

/**
 * Si ya tienes otras pantallas usando `hexToColor(...)`, reemplázala por esta.
 * Mantengo la firma para que no tengas que cambiar llamadas.
 */
@Suppress("FunctionName")
private fun hexToColor(hex: String): Color = ColorFromHex(hex)

private fun millisToISO(ms: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        .format(Date(ms))
