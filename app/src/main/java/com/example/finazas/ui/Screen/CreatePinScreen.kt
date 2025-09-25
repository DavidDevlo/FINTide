package com.example.finazas.ui.Screen

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finazas.ui.profile.PinSixInput

// package com.fintide.app.ui
@Composable
fun CreatePinScreen(
    onPinSet: (String) -> Unit,
    onBack: (() -> Unit)? = null
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Crea tu PIN", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Este PIN de 6 dígitos servirá para abrir FINTide.", textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            PinSixInput(onComplete = onPinSet)
        }
        if (onBack != null) {
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Text("Volver")
            }
        }
    }
}

@Composable
fun UnlockScreen(
    error: String?,
    onUnlock: (String) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Introduce tu PIN", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
            PinSixInput(onComplete = onUnlock)
        }
        Spacer(Modifier.height(1.dp))
    }
}
