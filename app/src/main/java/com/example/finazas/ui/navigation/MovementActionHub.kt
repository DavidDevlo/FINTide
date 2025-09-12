package com.example.finazas.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MovementActionHub(onSelect: (String) -> Unit) {
    Column(Modifier.padding(16.dp)) {
        Text("Nuevo movimiento", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))

        MovementActionItem("Ingreso", "Añade dinero recibido") { onSelect("Ingreso") }
        Spacer(Modifier.height(8.dp))

        MovementActionItem("Egreso", "Un gasto puntual") { onSelect("Egreso") }
        Spacer(Modifier.height(8.dp))

        MovementActionItem("Suscripción / Recibo", "Pago recurrente o servicio") { onSelect("Suscripción") }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun MovementActionItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(subtitle) }
        )
    }
}
