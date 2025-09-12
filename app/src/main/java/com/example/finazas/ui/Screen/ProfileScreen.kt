package com.example.finazas.ui.Screen


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(nav: NavHostController) {
    val subtle = MaterialTheme.colorScheme.onSurfaceVariant
    val pinColor = Color(0xFF15C6DF)

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
            // Ubicación (placeholder)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
            ) {
                Icon(Icons.Outlined.Place, contentDescription = null, tint = pinColor)
                Spacer(Modifier.width(6.dp))
                Text("Juliaca, Perú", fontSize = 14.sp)
            }

            ProfileCard(
                username = "daviddddd",
                name = "David",
                lastName = "Naira",
                email = "davidddd@gmail.com",
                password = "supersecret"
            )

            Spacer(Modifier.height(16.dp))

            // Botón principal: Mis tarjetas
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
}

@Composable
private fun ProfileCard(
    username: String,
    name: String,
    lastName: String,
    email: String,
    password: String
) {
    val subtle = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {

            // Encabezado con nombre de usuario y lápiz
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(username, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                Icon(
                    Icons.Outlined.Edit, contentDescription = "Editar usuario",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { /* TODO editar username */ }
                )
            }

            Divider(Modifier.padding(vertical = 12.dp))

            ProfileField(label = "Nombre", value = name, onEdit = { /* TODO */ })
            Divider()
            ProfileField(label = "Apellido", value = lastName, onEdit = { /* TODO */ })
            Divider()
            ProfileField(label = "Correo electrónico", value = email, onEdit = { /* TODO */ })
            Divider()
            PasswordField(label = "Contraseña", password = password)
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
            Text(
                value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Outlined.Edit,
                contentDescription = "Editar $label",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onEdit() }
            )
        }
    }
}

@Composable
private fun PasswordField(label: String, password: String) {
    val subtle = MaterialTheme.colorScheme.onSurfaceVariant
    var visible by remember { mutableStateOf(false) }
    Column(Modifier.padding(vertical = 10.dp)) {
        Text(label, color = subtle, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (visible) password else "************",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                contentDescription = if (visible) "Ocultar" else "Mostrar",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { visible = !visible }
            )
        }
    }
}
