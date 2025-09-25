package com.example.finazas



import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.room.Room
import com.example.finazas.data.local.AppDatabase
import com.example.finazas.data.repo.AuthRepository
import com.example.finazas.navigation.AppNav
import com.example.finazas.navigation.AppRoute
import com.example.finazas.ui.Screen.OnboardingFlow
import com.example.finazas.ui.profile.AuthViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

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

        // Room + Repo
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "fintide.db"
        ).build()
        val repo = AuthRepository(db.userDao())

        // VM factory
        val factory = viewModelFactory {
            initializer { AuthViewModel(repo) }
        }

        setContent {
            val authVm: AuthViewModel = viewModel(factory = factory)
            AppRoot(authVm = authVm)
        }
    }
}

@Composable
fun AppRoot(authVm: AuthViewModel) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,            // usa tus colores existentes
            onPrimary = Color.Black,
            background = DarkBg,
            onBackground = TextOnDark,
            surface = DarkBg,
            onSurface = TextOnDark
        )
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            var authorized by remember { mutableStateOf(false) }

            if (!authorized) {
                // Gate de acceso: Login -> CreatePin -> Onboarding -> Unlock
                OnboardingFlow(
                    vm = authVm,
                    onDone = { authorized = true }
                )
            } else {
                // Tu navegación normal de la app
                AppNav(startDestination = AppRoute.Home.route)
            }
        }
    }
}
