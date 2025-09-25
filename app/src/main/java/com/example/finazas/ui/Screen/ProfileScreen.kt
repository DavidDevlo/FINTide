package com.example.finazas.ui.Screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.finazas.navigation.AppRoute
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.room.Room
import androidx.compose.ui.platform.LocalContext
import com.example.finazas.data.local.AppDatabase
import com.example.finazas.ui.profile.ProfileViewModel
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.finazas.data.repo.AuthRepository


// Si no los tienes ya:


enum class EditKind { FULL_NAME, NAME, LAST_NAME, EMAIL, PIN }
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(nav: NavHostController) {
    val subtle = MaterialTheme.colorScheme.onSurfaceVariant
    val pinColor = Color(0xFF15C6DF)

    // ViewModel + DB + Repo
    val ctx = LocalContext.current
    val vm: ProfileViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val db = Room.databaseBuilder(
                    ctx.applicationContext,
                    AppDatabase::class.java,
                    "fintide.db"
                ).build()
                val repo = AuthRepository(db.userDao())
                ProfileViewModel(db.userDao(), repo)
            }
        }
    )
    val ui by vm.ui.collectAsState()

    // Estado de diálogos

    var editKind by remember { mutableStateOf<EditKind?>(null) }
    var t1 by remember { mutableStateOf("") }
    var t2 by remember { mutableStateOf("") }

    // Feedback simple
    ui.message?.let {
        LaunchedEffect(it) {
            // podrías mostrar Snackbar; por simpleza, limpiamos luego
            vm.clearFeedback()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { nav.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Atrás")
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
            ) {
                Icon(Icons.Outlined.Place, contentDescription = null, tint = pinColor)
                Spacer(Modifier.width(6.dp))
                Text("Juliaca, Perú", fontSize = 14.sp)
            }

            if (ui.loading) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp)),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) { Column(Modifier.padding(16.dp)) { Text("Cargando perfil…") } }
            } else {
                ProfileCard(
                    username = ui.username.ifBlank { "usuario" },
                    name = ui.name,
                    lastName = ui.lastName,
                    email = ui.email,
                    onEditUsername = {
                        t1 = ui.name
                        t2 = ui.lastName
                        editKind = EditKind.FULL_NAME
                    },
                    onEditName = {
                        t1 = ui.name
                        editKind = EditKind.NAME
                    },
                    onEditLastName = {
                        t1 = ui.lastName
                        editKind = EditKind.LAST_NAME
                    },
                    onEditEmail = {
                        t1 = ui.email
                        editKind = EditKind.EMAIL
                    },
                    onChangePin = {
                        t1 = ""
                        t2 = ""
                        editKind = EditKind.PIN
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { nav.navigate(AppRoute.Cards.route) },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(Icons.Outlined.CreditCard, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Mis tarjetas", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Administra tus tarjetas, bancos y métodos de pago.",
                color = subtle,
                fontSize = 12.sp
            )
        }
    }

    // --- Diálogo de edición ---
    when (editKind) {
        EditKind.FULL_NAME -> {
            EditTwoFieldsDialog(
                title = "Editar nombre y apellido",
                label1 = "Nombre",
                label2 = "Apellido",
                value1 = t1,
                value2 = t2,
                onValue1 = { t1 = it },
                onValue2 = { t2 = it },
                onDismiss = { editKind = null },
                onConfirm = {
                    if (t1.isBlank() || t2.isBlank()) return@EditTwoFieldsDialog
                    vm.updateFullName(t1.trim(), t2.trim())
                    editKind = null
                }
            )
        }
        EditKind.NAME -> {
            EditOneFieldDialog(
                title = "Editar nombre",
                label = "Nombre",
                value = t1,
                onValue = { t1 = it },
                onDismiss = { editKind = null },
                onConfirm = {
                    if (t1.isBlank()) return@EditOneFieldDialog
                    vm.updateName(t1.trim()); editKind = null
                }
            )
        }
        EditKind.LAST_NAME -> {
            EditOneFieldDialog(
                title = "Editar apellido",
                label = "Apellido",
                value = t1,
                onValue = { t1 = it },
                onDismiss = { editKind = null },
                onConfirm = {
                    if (t1.isBlank()) return@EditOneFieldDialog
                    vm.updateLastName(t1.trim()); editKind = null
                }
            )
        }
        EditKind.EMAIL -> {
            EditOneFieldDialog(
                title = "Editar correo",
                label = "Correo",
                value = t1,
                onValue = { t1 = it },
                keyboardEmail = true,
                onDismiss = { editKind = null },
                onConfirm = {
                    val v = t1.trim()
                    if (v.isBlank() || !v.contains("@")) return@EditOneFieldDialog
                    vm.updateEmail(v); editKind = null
                }
            )
        }
        EditKind.PIN -> {
            EditTwoFieldsDialog(
                title = "Cambiar PIN",
                label1 = "Nuevo PIN (6 dígitos)",
                label2 = "Repite el PIN",
                value1 = t1,
                value2 = t2,
                onValue1 = { t1 = it.filter { c -> c.isDigit() }.take(6) },
                onValue2 = { t2 = it.filter { c -> c.isDigit() }.take(6) },
                isPassword = true,
                numeric = true,
                onDismiss = { editKind = null },
                onConfirm = {
                    vm.changePin(t1, t2)
                    editKind = null
                }
            )
        }
        null -> Unit
    }
}

/* ---------- Card y campos ---------- */

@Composable
private fun ProfileCard(
    username: String,
    name: String,
    lastName: String,
    email: String,
    onEditUsername: () -> Unit,
    onEditName: () -> Unit,
    onEditLastName: () -> Unit,
    onEditEmail: () -> Unit,
    onChangePin: () -> Unit
) {
    val subtle = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(username, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                Icon(
                    Icons.Outlined.Edit, contentDescription = "Editar usuario",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp).clickable { onEditUsername() }
                )
            }

            Divider(Modifier.padding(vertical = 12.dp))

            ProfileField(label = "Nombre", value = name, onEdit = onEditName)
            Divider()
            ProfileField(label = "Apellido", value = lastName, onEdit = onEditLastName)
            Divider()
            ProfileField(label = "Correo electrónico", value = email, onEdit = onEditEmail)
            Divider()
            PinMaskedField(label = "PIN", masked = "********", onChange = onChangePin)
        }
    }
}

@Composable
private fun ProfileField(label: String, value: String, onEdit: () -> Unit) {
    val subtle = MaterialTheme.colorScheme.onSurfaceVariant
    Column(Modifier.padding(vertical = 10.dp)) {
        Text(label, color = subtle, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Icon(
                Icons.Outlined.Edit, contentDescription = "Editar $label",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp).clickable { onEdit() }
            )
        }
    }
}

@Composable
private fun PinMaskedField(label: String, masked: String, onChange: () -> Unit) {
    val subtle = MaterialTheme.colorScheme.onSurfaceVariant
    Column(Modifier.padding(vertical = 10.dp)) {
        Text(label, color = subtle, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(masked, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Icon(
                Icons.Outlined.Edit, contentDescription = "Cambiar PIN",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp).clickable { onChange() }
            )
        }
    }
}

/* ---------- Diálogos reutilizables ---------- */

@Composable
private fun EditOneFieldDialog(
    title: String,
    label: String,
    value: String,
    onValue: (String) -> Unit,
    keyboardEmail: Boolean = false,
    isPassword: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValue,
                label = { Text(label) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = when {
                        keyboardEmail -> KeyboardType.Email
                        isPassword -> KeyboardType.NumberPassword
                        else -> KeyboardType.Text
                    }
                ),
                visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun EditTwoFieldsDialog(
    title: String,
    label1: String,
    label2: String,
    value1: String,
    value2: String,
    onValue1: (String) -> Unit,
    onValue2: (String) -> Unit,
    numeric: Boolean = false,
    isPassword: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = value1,
                    onValueChange = onValue1,
                    label = { Text(label1) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = when {
                            numeric || isPassword -> KeyboardType.NumberPassword
                            else -> KeyboardType.Text
                        }
                    ),
                    visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None
                )
                OutlinedTextField(
                    value = value2,
                    onValueChange = onValue2,
                    label = { Text(label2) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = when {
                            numeric || isPassword -> KeyboardType.NumberPassword
                            else -> KeyboardType.Text
                        }
                    ),
                    visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None
                )
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
