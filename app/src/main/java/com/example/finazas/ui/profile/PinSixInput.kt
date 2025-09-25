package com.example.finazas.ui.profile

// package com.fintide.app.ui
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PinSixInput(
    modifier: Modifier = Modifier,
    onComplete: (String) -> Unit
) {
    var raw by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }

    Box(modifier = modifier) {
        // Campo oculto, captura los números
        OutlinedTextField(
            value = raw,
            onValueChange = {
                val cleaned = it.filter { c -> c.isDigit() }.take(6)
                raw = cleaned
                if (cleaned.length == 6) onComplete(cleaned)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            textStyle = TextStyle(fontSize = 1.sp), // diminuto para "ocultarlo"
            modifier = Modifier
                .size(1.dp) // prácticamente invisible
                .focusRequester(focus)
        )

        // 6 cajitas espejando el contenido
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { focus.requestFocus() }
        ) {
            repeat(6) { idx ->
                val ch = raw.getOrNull(idx)?.toString() ?: ""
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ch,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
        }
    }
}
