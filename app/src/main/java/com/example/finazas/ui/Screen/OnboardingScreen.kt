package com.example.finazas.ui.Screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// 👇 IMPORTS CLAVE para el delegado `by`
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun OnboardingScreen(
    onSkip: () -> Unit,    // <-- lambda normal
    onFinish: () -> Unit   //
) {
    val pages = listOf(
        OnbPage(
            title = "Bienvenido a FINTide",
            subtitle = "Tu asistente para ordenar tus finanzas personales.",
            emoji = "💙"
        ),
        OnbPage(
            title = "Todo en un solo lugar",
            subtitle = "Administra gastos, metas y suscripciones sin complicarte.",
            emoji = "📊"
        ),
        OnbPage(
            title = "Toma el control",
            subtitle = "Recibe recordatorios y paga a tiempo. Ahorra mejor.",
            emoji = "⚡"
        )
    )
    var page by rememberSaveable { mutableStateOf(0) }
    val isLast = page == pages.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top bar: Skip
        Row (Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (!isLast) {
                TextButton(onClick = onSkip) { Text("Saltar") }
            }
        }

        // Ilustración + textos
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                pages[page].emoji,
                fontSize = 64.sp
            )
            Spacer(Modifier.height(24.dp))
            Text(
                pages[page].title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                pages[page].subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }

        // Indicadores + Botones
        Column {
            // Dots
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                pages.indices.forEach { i ->
                    val active = i == page
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (active) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                            )
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            if (isLast) {
                Button(
                    onClick = onFinish ,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Empezar") }
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = onSkip,
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Saltar") }

                    Button(
                        onClick = { page++ },
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Siguiente") }
                }
            }
        }
    }
}

private data class OnbPage(
    val title: String,
    val subtitle: String,
    val emoji: String
)
