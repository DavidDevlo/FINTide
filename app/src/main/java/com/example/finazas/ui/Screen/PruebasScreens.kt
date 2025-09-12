// GoalsScreenEntry.kt
package com.example.finazas.ui.Screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            CenterAlignedTopAppBar(
                title = { Text("Metas (CRUD demo)") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menú")
                    }
                },
                actions = {
                    // Guardar desde AppBar -> guardar y salir a Metas
                    IconButton(onClick = {
                        scope.launch {
                            vm.save {
                                // Ir a Metas y quitar el CRUD del backstack
                                navController.navigate(AppRoute.Metas.route) {
                                    popUpTo(AppRoute.Pruebas.route) { inclusive = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    }) {
                        Icon(Icons.Filled.Save, contentDescription = "Guardar")
                    }
                    // Listar todo (limpia búsqueda)
                    IconButton(onClick = { vm.setQuery("") }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Listar todo")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    vm.startCreate()
                    scope.launch { snackbarHostState.showSnackbar("Formulario para nueva meta") }
                },
                icon = { Icon(Icons.Filled.Add, contentDescription = "Crear") },
                text = { Text("Crear") }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        GoalsScreen(
            vm = vm,
            navController = navController, // 👈 pásalo para navegar desde el botón Guardar del form
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}

@Composable
fun GoalsScreen(
    vm: GoalViewModel = viewModel(),
    navController: NavHostController,  // 👈 agregado
    modifier: Modifier = Modifier
) {
    val goals by vm.goals.collectAsStateWithLifecycle()
    val form  by vm.form.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }

    LaunchedEffect(query) { vm.setQuery(query) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Metas (Goals)", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))

        // Búsqueda + Listar todo
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Buscar por título") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = {
                query = ""
                vm.setQuery("")
            }) { Text("Listar todo") }
        }

        Spacer(Modifier.height(12.dp))

        GoalForm(
            form = form,
            onChange = { title, target, current, color, active ->
                vm.onFormChange(title, target, current, color, active)
            },
            // Guardar desde botón del formulario -> guardar y salir a Metas
            onSave = {
                vm.save {
                    navController.navigate(AppRoute.Metas.route) {
                        popUpTo(AppRoute.Pruebas.route) { inclusive = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            onCancel = { vm.startCreate() }
        )

        Spacer(Modifier.height(16.dp))

        // Lista con altura acotada
        Box(Modifier.weight(1f)) {
            GoalsList(
                goals = goals,
                onEdit = { goal -> scope.launch { vm.loadForEdit(goal.id) } },
                onDelete = { goal -> vm.delete(goal.id) }
            )
        }
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

            OutlinedTextField(
                value = form.colorHex,
                onValueChange = { onChange(null, null, null, it, null) },
                label = { Text("Color HEX (#RRGGBB)") },
                isError = form.errors.containsKey("colorHex"),
                supportingText = { form.errors["colorHex"]?.let { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

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
private fun GoalsList(
    goals: List<Goal>,
    onEdit: (Goal) -> Unit,
    onDelete: (Goal) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(goals) { g ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(g.title, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            val pct = if (g.targetAmount <= 0) 0f
                            else (g.currentAmount.toFloat() / g.targetAmount.toFloat()).coerceIn(0f, 1f)
                            LinearProgressIndicator(progress = pct, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "S/ ${"%.2f".format(g.currentAmount / 100.0)} / S/ ${"%.2f".format(g.targetAmount / 100.0)}  •  ${if (g.isActive) "Activo" else "Archivado"}"
                            )
                        }
                        Row {
                            IconButton(onClick = { onEdit(g) }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Editar")
                            }
                            IconButton(onClick = { onDelete(g) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
                            }
                        }
                    }
                }
            }
        }
    }
}
