package com.example.finazas.ui.Screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch

import com.example.finazas.ui.goals.GoalViewModel
import com.example.finazas.ui.goals.GoalFormState
import com.example.finazas.data.local.entity.Goal
import com.example.finazas.navigation.AppRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreenEntry(
    navController: NavHostController,
    onOpenDrawer: () -> Unit
) {
    val vm: GoalViewModel = viewModel()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            // AppBar minimal con botón Atrás
            CenterAlignedTopAppBar(
                title = { Text("Metas") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        },
        // FAB eliminado SOLO en esta pantalla (como pediste)
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        GoalsScreen(
            vm = vm,
            navController = navController,
            showMessage = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}

@Composable
fun GoalsScreen(
    vm: GoalViewModel = viewModel(),
    navController: NavHostController,
    showMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val goals by vm.goals.collectAsStateWithLifecycle()
    val form  by vm.form.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope() // <-- scope válido en composable

    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        item {
            Text("Metas (CRUD)", style = MaterialTheme.typography.headlineSmall)
        }

        // ---------- FORM ----------
        item {
            GoalForm(
                form = form,
                onChange = { title, target, current, color, active ->
                    vm.onFormChange(title, target, current, color, active)
                },
                onSave = {
                    vm.save {
                        showMessage("Meta guardada")
                        navController.navigate(AppRoute.Metas.route) {
                            popUpTo(AppRoute.Pruebas.route) { inclusive = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                onCancel = {
                    vm.startCreate()
                    showMessage("Cambios cancelados")
                }
            )
        }

        // ---------- LISTA ----------
        if (goals.isEmpty()) {
            item {
                Text(
                    "Aún no hay metas. Crea una con el formulario de arriba.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            items(goals, key = { it.id }) { g ->
                GoalRow(
                    goal = g,
                    onEdit = {
                        // Cargar datos al formulario sin LaunchedEffect dentro de items{}
                        scope.launch { vm.loadForEdit(g.id) }
                    },
                    onDelete = {
                        vm.delete(g.id)
                        showMessage("Meta eliminada")
                    },
                    onQuickAdd = { centsToAdd ->
                        val newAmount = g.currentAmount + centsToAdd
                        vm.updateGoalAmount(g.id, newAmount)
                        showMessage("Se agregó S/ ${"%.2f".format(centsToAdd/100.0)} a “${g.title}”")
                    }
                )
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun GoalForm(
    form: GoalFormState,
    onChange: (title: String?, target: String?, current: String?, color: String?, active: Boolean?) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                if (form.errors.isEmpty()) "Formulario de meta" else "Revisa los campos",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = form.title,
                onValueChange = { onChange(it, null, null, null, null) },
                label = { Text("Título") },
                isError = form.errors.containsKey("title"),
                supportingText = { form.errors["title"]?.let { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = form.targetAmount,
                onValueChange = { onChange(null, it, null, null, null) },
                label = { Text("Meta (S/ con decimales)") },
                isError = form.errors.containsKey("targetAmount"),
                supportingText = { form.errors["targetAmount"]?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = form.currentAmount,
                onValueChange = { onChange(null, null, it, null, null) },
                label = { Text("Progreso actual (S/)") },
                isError = form.errors.containsKey("currentAmount"),
                supportingText = { form.errors["currentAmount"]?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = form.colorHex,
                    onValueChange = { onChange(null, null, null, it, null) },
                    label = { Text("Color HEX (#RRGGBB)") },
                    isError = form.errors.containsKey("colorHex"),
                    supportingText = { form.errors["colorHex"]?.let { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                val previewColor = remember(form.colorHex) {
                    runCatching {
                        val clean = form.colorHex.trim().removePrefix("#")
                        val argb = when (clean.length) {
                            6 -> 0xFF000000 or clean.toLong(16)
                            8 -> clean.toLong(16)
                            else -> 0xFF888888
                        }
                        Color(argb.toULong())
                    }.getOrElse { Color(0xFF888888) }
                }
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(previewColor)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = form.isActive,
                    onCheckedChange = { onChange(null, null, null, null, it) }
                )
                Spacer(Modifier.width(8.dp))
                Text("Activo")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSave) { Text("Guardar") }
                OutlinedButton(onClick = onCancel) { Text("Cancelar") }
            }
        }
    }
}

@Composable
private fun GoalRow(
    goal: Goal,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onQuickAdd: (centsToAdd: Long) -> Unit
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(goal.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    val pct = if (goal.targetAmount <= 0) 0f
                    else (goal.currentAmount.toFloat() / goal.targetAmount.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(progress = pct, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "S/ ${"%.2f".format(goal.currentAmount / 100.0)} / S/ ${"%.2f".format(goal.targetAmount / 100.0)}  •  ${if (goal.isActive) "Activo" else "Archivado"}"
                    )
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Editar")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AssistChip(onClick = { onQuickAdd(10_00) }, label = { Text("+10") })
                AssistChip(onClick = { onQuickAdd(50_00) }, label = { Text("+50") })
            }
        }
    }
}
