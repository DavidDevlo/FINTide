package com.example.finazas.navigation



sealed class AppRoute(val route: String) {
    data object Pruebas : AppRoute("prueba")
    data object Home : AppRoute("home")
    data object Movimientos : AppRoute("movimientos")
    data object Metas : AppRoute("metas")
    data object Perfil : AppRoute("perfil")
    data object Cards : AppRoute("cards")     // "Mis tarjetas

    data object MovementNew : AppRoute("movement_new?type={type}") {
        fun withType(type: String) = "movement_new?type=$type"
    }
    data object GoalNew : AppRoute("goal_new")

    data object SubscriptionNew : AppRoute("subscription_new?id={id}") {
        fun create() = "subscription_new?id=-1"
        fun edit(id: Long) = "subscription_new?id=$id"
    }

    data object Subscriptions : AppRoute("subscriptions")

    data object Onboarding : AppRoute("onboarding")
}
