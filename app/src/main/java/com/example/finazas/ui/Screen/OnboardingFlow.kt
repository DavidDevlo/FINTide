package com.example.finazas.ui.Screen

// package com.fintide.app.ui
import androidx.compose.runtime.*
import com.example.finazas.ui.profile.AuthStep
import com.example.finazas.ui.profile.AuthViewModel

@Composable
fun OnboardingFlow(
    vm: AuthViewModel,
    onDone: () -> Unit
) {
    val ui = vm.ui.collectAsState().value

    when (ui.step) {
        AuthStep.Onboarding -> OnboardingScreen(
            onSkip   = vm::onOnboardingSeen,
            onFinish = vm::onOnboardingSeen
        )

        AuthStep.Login -> LoginScreen(
            loading = ui.loading,
            error = ui.error,
            onGoogle = { /* lo maneja internamente con Credential Manager y luego vm.onGoogleSignedIn(...) */ },
            onFacebook = {}, // pendiente si deseas
            onTwitter = {},  // pendiente si deseas
            onManualRegister = vm::onManualRegister,
            onGoogleSigned = vm::onGoogleSignedIn
        )

        AuthStep.CreatePin -> CreatePinScreen(onPinSet = vm::onSetPin)

        AuthStep.Unlock -> UnlockScreen(
            error = ui.error,
            onUnlock = vm::onUnlock
        )

        AuthStep.Done -> onDone()
    }
}