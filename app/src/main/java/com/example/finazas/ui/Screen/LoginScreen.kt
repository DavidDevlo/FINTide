package com.example.finazas.ui.Screen

// package com.fintide.app.ui
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finazas.ui.profile.GoogleProfile
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*

import androidx.compose.ui.platform.LocalContext
import com.example.finazas.ui.profile.getGoogleProfileViaCredentialManager
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    loading: Boolean,
    error: String?,
    onGoogle: () -> Unit, // no usado, queda por compatibilidad
    onFacebook: () -> Unit,
    onTwitter: () -> Unit,
    onManualRegister: (String, String, String) -> Unit,
    onGoogleSigned: (sub: String, given: String?, family: String?, email: String, picture: String?) -> Unit
) {
    val given = remember { mutableStateOf("") }
    val family = remember { mutableStateOf("") }
    val email = remember { mutableStateOf("") }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "¡Bienvenido a FINTide!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {

            // --- Google Sign-In real ---
            Button(
                enabled = !loading,
                onClick = {
                    scope.launch {
                        val profile: GoogleProfile? = getGoogleProfileViaCredentialManager(ctx)
                        if (profile != null) {
                            onGoogleSigned(
                                profile.sub, profile.givenName, profile.familyName, profile.email, profile.picture
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Continuar con Google") }

            OutlinedButton(
                enabled = !loading,
                onClick = onFacebook,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Continuar con Facebook") }

            OutlinedButton(
                enabled = !loading,
                onClick = onTwitter,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Continuar con X (Twitter)") }

            Divider(Modifier.padding(vertical = 16.dp))
            Text("o regístrate con tu correo", fontSize = 14.sp)

            OutlinedTextField(
                value = given.value, onValueChange = { given.value = it },
                label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = family.value, onValueChange = { family.value = it },
                label = { Text("Apellido") }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = email.value, onValueChange = { email.value = it },
                label = { Text("Correo") }, modifier = Modifier.fillMaxWidth()
            )
            Button(
                enabled = !loading && given.value.isNotBlank() && family.value.isNotBlank() && email.value.isNotBlank(),
                onClick = { onManualRegister(given.value.trim(), family.value.trim(), email.value.trim()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Crear cuenta") }

            if (error != null) {
                Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }
        }

        Text("Al continuar aceptas nuestros Términos y Política de Privacidad.", fontSize = 12.sp)
    }
}

