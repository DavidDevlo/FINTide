package com.example.finazas.ui.Screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.finazas.data.local.entity.PaymentCard
import com.example.finazas.ui.profile.CardFormState
import com.example.finazas.ui.profile.CardsViewModel

enum class CardTab { DEBIT, CREDIT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardsScreen(
    nav: NavHostController,
    vm: CardsViewModel = viewModel()
) {
    val cards by vm.cards.collectAsStateWithLifecycle()
    val form by vm.form.collectAsStateWithLifecycle()

    var tab by remember { mutableStateOf(CardTab.DEBIT) }
    var showSheet by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var toDeleteId by remember { mutableStateOf<Long?>(null) }

    val palette = listOf("#2563EB", "#F59E0B", "#10B981", "#7C3AED", "#EF4444", "#3B82F6")

    // 🔧 Mover los derivados FUERA del LazyColumn (sin remember dentro del builder)
    val want = if (tab == CardTab.DEBIT) "DEBIT" else "CREDIT"
    val filtered = cards.filter { it.cardType.equals(want, ignoreCase = true) }
    val (physical, virtual) = filtered.partition { it.isPhysical }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis tarjetas", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { nav.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingId = null
                vm.startCreate()
                vm.onFormChange(cardType = if (tab == CardTab.DEBIT) "DEBIT" else "CREDIT", isPhysical = true)
                showSheet = true
            }) { Icon(Icons.Outlined.AddCircle, contentDescription = "Agregar") }
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            // Segmented Débito / Crédito
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    SegChip(
                        text = "Débito",
                        selected = tab == CardTab.DEBIT,
                        onClick = { tab = CardTab.DEBIT }
                    )
                    Spacer(Modifier.width(10.dp))
                    SegChip(
                        text = "Crédito",
                        selected = tab == CardTab.CREDIT,
                        onClick = { tab = CardTab.CREDIT }
                    )
                }
            }

            // Sección Física
            item {
                SectionWithAdd(
                    title = "Tarjeta física",
                    onAdd = {
                        editingId = null
                        vm.startCreate()
                        vm.onFormChange(cardType = if (tab == CardTab.DEBIT) "DEBIT" else "CREDIT", isPhysical = true)
                        showSheet = true
                    }
                )
            }
            if (physical.isEmpty()) {
                item { Text("No tienes tarjetas físicas en esta categoría") }
            } else {
                items(physical.size) { i ->
                    CardRow(
                        card = physical[i],
                        onEdit = {
                            editingId = physical[i].id
                            vm.loadForEdit(physical[i].id)
                            showSheet = true
                        },
                        onMakeDefault = { vm.setDefault(physical[i].id) },
                        onDelete = { toDeleteId = physical[i].id }
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }

            // Sección Virtual
            item { Spacer(Modifier.height(6.dp)) }
            item {
                SectionWithAdd(
                    title = "Tarjeta virtual",
                    onAdd = {
                        editingId = null
                        vm.startCreate()
                        vm.onFormChange(cardType = if (tab == CardTab.DEBIT) "DEBIT" else "CREDIT", isPhysical = false)
                        showSheet = true
                    }
                )
            }
            if (virtual.isEmpty()) {
                item { Text("No tienes tarjetas virtuales en esta categoría") }
            } else {
                items(virtual.size) { i ->
                    CardRow(
                        card = virtual[i],
                        onEdit = {
                            editingId = virtual[i].id
                            vm.loadForEdit(virtual[i].id)
                            showSheet = true
                        },
                        onMakeDefault = { vm.setDefault(virtual[i].id) },
                        onDelete = { toDeleteId = virtual[i].id }
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            CardForm(
                isEdit = editingId != null,
                form = form,
                palette = palette,
                onChange = { holder, nick, pan, mm, yy, color, isDef, type, phys ->
                    vm.onFormChange(
                        holderName = holder,
                        nickname = nick,
                        panInput = pan,
                        expMonth = mm,
                        expYear = yy,
                        colorHex = color,
                        isDefault = isDef,
                        cardType = type,
                        isPhysical = phys
                    )
                },
                onSave = {
                    vm.save {
                        showSheet = false
                        editingId = null
                    }
                },
                onCancel = {
                    showSheet = false
                    editingId = null
                },
                onToggleType = { newType -> vm.onFormChange(cardType = newType) },
                onTogglePhysical = { newVal -> vm.onFormChange(isPhysical = newVal) }
            )
        }
    }

    if (toDeleteId != null) {
        AlertDialog(
            onDismissRequest = { toDeleteId = null },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(toDeleteId!!)
                    toDeleteId = null
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { toDeleteId = null }) { Text("Cancelar") } },
            title = { Text("Eliminar tarjeta") },
            text = { Text("No afectará movimientos históricos.") }
        )
    }
}


/* -------------------- UI pieces (segmented, section, row, form) -------------------- */

@Composable
private fun SegChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else Color(0xFF1D2024), label = "seg-bg")
    val fg by animateColorAsState(if (selected) Color.Black else MaterialTheme.colorScheme.onSurface, label = "seg-fg")
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Text(text, color = fg, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SectionWithAdd(title: String, onAdd: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onAdd() }) {
            Text("Agregar", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Outlined.AddCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun CardRow(
    card: PaymentCard,
    onEdit: () -> Unit,
    onMakeDefault: () -> Unit,
    onDelete: () -> Unit
) {
    val stripe = colorFromHexSafe(card.colorHex)
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().heightIn(min = 72.dp)) {
            Box(Modifier.width(6.dp).fillMaxHeight().background(stripe))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text((card.nickname ?: card.holderName), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    if (card.isDefault) AssistChip(onClick = {}, label = { Text("Predeterminada") }, leadingIcon = {
                        Icon(Icons.Outlined.CreditCard, null)
                    })
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "${card.brand}  •••• ${card.panLast4}  •  ${card.expMonth.toString().padStart(2,'0')}/${(card.expYear % 100).toString().padStart(2,'0')}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, null); Spacer(Modifier.width(6.dp)); Text("Editar") }
                    OutlinedButton(onClick = onMakeDefault, enabled = !card.isDefault) { Text(if (card.isDefault) "Predeterminada" else "Hacer predeterminada") }
                    IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, contentDescription = "Eliminar") }
                }
            }
        }
    }
}

@Composable
private fun CardForm(
    isEdit: Boolean,
    form: CardFormState,
    palette: List<String>,
    onChange: (
        holderName: String?, nickname: String?, panInput: String?, expMonth: String?, expYear: String?,
        colorHex: String?, isDefault: Boolean?, cardType: String?, isPhysical: Boolean?
    ) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onToggleType: (String) -> Unit,
    onTogglePhysical: (Boolean) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (isEdit) "Editar tarjeta" else "Nueva tarjeta", style = MaterialTheme.typography.titleMedium)

        // Toggle tipo: Débito / Crédito
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SegChip("Débito", selected = form.cardType.equals("DEBIT", true), onClick = { onToggleType("DEBIT") })
            SegChip("Crédito", selected = form.cardType.equals("CREDIT", true), onClick = { onToggleType("CREDIT") })
        }

        // Toggle física / virtual
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SegChip("Física", selected = form.isPhysical, onClick = { onTogglePhysical(true) })
            SegChip("Virtual", selected = !form.isPhysical, onClick = { onTogglePhysical(false) })
        }

        OutlinedTextField(
            value = form.holderName,
            onValueChange = { onChange(it, null, null, null, null, null, null, null, null) },
            label = { Text("Nombre en la tarjeta") },
            isError = form.errors.containsKey("holderName"),
            supportingText = { form.errors["holderName"]?.let { Text(it) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = form.nickname,
            onValueChange = { onChange(null, it, null, null, null, null, null, null, null) },
            label = { Text("Apodo (opcional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (!isEdit) {
            OutlinedTextField(
                value = form.panInput,
                onValueChange = { onChange(null, null, it, null, null, null, null, null, null) },
                label = { Text("Número de tarjeta (solo para detectar)") },
                placeholder = { Text("•••• •••• •••• ••••") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = form.errors.containsKey("panInput"),
                supportingText = {
                    val msg = buildString {
                        append("Detectado: ${form.brand}")
                        if (form.last4Preview.isNotBlank()) append("  •••• ${form.last4Preview}")
                    }
                    Text(msg)
                    form.errors["panInput"]?.let { Text(it) }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            AssistChip(onClick = {}, label = { Text("${form.brand} •••• ${form.last4Preview}") }, leadingIcon = {
                Icon(Icons.Outlined.CreditCard, null)
            })
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = form.expMonth,
                onValueChange = { onChange(null, null, null, it, null, null, null, null, null) },
                label = { Text("Mes") },
                isError = form.errors.containsKey("expMonth"),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = form.expYear,
                onValueChange = { onChange(null, null, null, null, it, null, null, null, null) },
                label = { Text("Año") },
                isError = form.errors.containsKey("expYear"),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Text("Color", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            palette.forEach { hex ->
                ColorSwatch(hex = hex, selected = hex.equals(form.colorHex, true)) {
                    onChange(null, null, null, null, null, hex, null, null, null)
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = form.isDefault, onCheckedChange = { onChange(null, null, null, null, null, null, it, null, null) })
            Spacer(Modifier.width(6.dp))
            Text("Marcar como predeterminada")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onSave, modifier = Modifier.weight(1f)) { Text(if (isEdit) "Guardar" else "Agregar") }
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancelar") }
        }

        Spacer(Modifier.height(8.dp))
    }
}

/* Helpers */
@Composable
private fun ColorSwatch(hex: String, selected: Boolean, onPick: () -> Unit) {
    val color = colorFromHexSafe(hex)
    var mod = Modifier.size(28.dp).clip(CircleShape).background(color).clickable { onPick() }
    if (selected) mod = mod.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
    Box(mod)
}

private fun colorFromHexSafe(hex: String): Color = runCatching {
    val withHash = if (hex.startsWith("#")) hex else "#$hex"
    Color(android.graphics.Color.parseColor(withHash))
}.getOrElse { Color(0xFF3B82F6.toInt()) }


