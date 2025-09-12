package com.example.finazas.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Suscripción / Recibo recurrente
 * - amountCents puede ser null si el monto es variable (variableAmount = true).
 * - frequency: MONTHLY | WEEKLY | YEARLY | CUSTOM
 * - intervalDays: días para CUSTOM (>=1)
 * - nextDueAt: timestamp (ms) del próximo vencimiento (recomendado: inicio de día)
 */
@Entity(
    tableName = "subscriptions",
    indices = [
        Index(value = ["isActive", "nextDueAt"]),
        Index(value = ["title"])
    ]
)
data class Subscription(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amountCents: Long? = null,
    val variableAmount: Boolean = false,

    val frequency: String,           // MONTHLY | WEEKLY | YEARLY | CUSTOM
    val intervalDays: Int? = null,   // válido cuando frequency = CUSTOM (>=1)

    val nextDueAt: Long,             // ms (UTC o local; sé consistente)
    val autoPay: Boolean = false,
    val colorHex: String = "#7C3AED",

    val isActive: Boolean = true,

    val lastPaidAt: Long? = null,
    val lastPaidAmountCents: Long? = null,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
