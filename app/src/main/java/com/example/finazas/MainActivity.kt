package com.example.finazas



import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.finazas.navigation.AppNav
import com.example.finazas.navigation.AppRoute
import com.example.finazas.ui.theme.FinazasTheme

private val DarkBg = Color(0xFF0E0F11)
private val SurfaceGray = Color(0xFF2B2F35)
private val TextOnDark = Color(0xFFECECEC)
private val Subtle = Color(0xFFB9BEC5)
private val Accent = Color(0xFFFFA726)
private val Success = Color(0xFF22C55E)
private val Danger = Color(0xFFE95555)
private val Purple = Color(0xFF7C3AED)

// MainActivity.kt
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppRoot()   // 👈 Un único theme + un único NavHost
        }
    }
}

@Composable
fun AppRoot() {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            onPrimary = Color.Black,
            background = DarkBg,
            onBackground = TextOnDark,
            surface = DarkBg,
            onSurface = TextOnDark
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AppNav()   // 👈 Tu NavHost único
        }
    }
}
