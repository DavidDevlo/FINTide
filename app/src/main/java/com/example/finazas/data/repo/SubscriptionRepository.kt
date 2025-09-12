// data/repo/SubscriptionRepository.kt
package com.example.finazas.data.repo

import com.example.finazas.data.local.dao.MovementDao
import com.example.finazas.data.local.dao.SubscriptionDao
import com.example.finazas.data.local.entity.Movement
import com.example.finazas.data.local.entity.Subscription
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class SubscriptionRepository(
    private val dao: SubscriptionDao,
    private val movementDao: MovementDao? = null   // opcional: para crear Movement al pagar
) {
    fun observeAll(): Flow<List<Subscription>> = dao.observeAll()
    fun search(q: String): Flow<List<Subscription>> = dao.search(q)
    fun observeDueBetween(start: Long, end: Long): Flow<List<Subscription>> = dao.observeDueBetween(start, end)

    suspend fun getById(id: Long): Subscription? = dao.findById(id)

    suspend fun create(
        title: String,
        amountCents: Long?,            // null si variable
        variableAmount: Boolean,
        frequency: String,             // MONTHLY | WEEKLY | YEARLY | CUSTOM
        intervalDays: Int?,            // requerido si CUSTOM
        nextDueAt: Long,
        autoPay: Boolean,
        colorHex: String
    ): Long {
        val now = System.currentTimeMillis()
        val s = Subscription(
            title = title.trim(),
            amountCents = amountCents,
            variableAmount = variableAmount,
            frequency = frequency.trim(),
            intervalDays = intervalDays,
            nextDueAt = nextDueAt,
            autoPay = autoPay,
            colorHex = colorHex.trim(),
            createdAt = now,
            updatedAt = now
        )
        return dao.insert(s)
    }

    suspend fun update(s: Subscription) {
        dao.updateRaw(s.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun setCoreFields(
        id: Long,
        title: String,
        amountCents: Long?,
        variableAmount: Boolean,
        frequency: String,
        intervalDays: Int?,
        nextDueAt: Long,
        autoPay: Boolean,
        colorHex: String
    ) = dao.setCoreFields(id, title.trim(), amountCents, variableAmount, frequency.trim(), intervalDays, nextDueAt, autoPay, colorHex.trim())

    suspend fun delete(id: Long) = dao.softDelete(id)

    /**
     * Marca pagado:
     * - registra Movement si movementDao != null (tipo "Suscripción")
     * - actualiza lastPaid*, nextDueAt (según frecuencia)
     */
    suspend fun markPaid(
        id: Long,
        paidAmountCents: Long?,    // si null y la sub tiene amountCents, se usa ese
        paidAt: Long = System.currentTimeMillis()
    ) {
        val s = dao.findById(id) ?: return
        val amount = paidAmountCents ?: s.amountCents
        requireNotNull(amount) { "Monto requerido para suscripción de monto variable." }

        // Crear movement (opcional)
        movementDao?.let { mdao ->
            val movement = Movement(
                title = s.title,
                type = "Egreso",
                amount = amount,
                date = paidAt,
                stripeColorHex = s.colorHex,
                isActive = true
            )
            val insertedId = mdao.insert(movement)        // 👈 capturamos el ID
            android.util.Log.d(
                "SubsRepo",
                "markPaid(): movimiento creado id=$insertedId, subId=${s.id}, monto=$amount, fecha=$paidAt"
            )
        }

        val next = computeNextDue(s, paidAt)
        dao.markPaidInternal(id, amount, paidAt, next)
    }

    // -------- Helpers de fecha para próximo vencimiento ----------
    private fun computeNextDue(s: Subscription, fromMillis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = fromMillis
        when (s.frequency.uppercase()) {
            "MONTHLY" -> cal.add(Calendar.MONTH, 1)
            "WEEKLY"  -> cal.add(Calendar.DAY_OF_YEAR, 7)
            "YEARLY"  -> cal.add(Calendar.YEAR, 1)
            "CUSTOM"  -> cal.add(Calendar.DAY_OF_YEAR, (s.intervalDays ?: 1).coerceAtLeast(1))
            else      -> cal.add(Calendar.MONTH, 1) // fallback
        }
        // opcional: normalizar a inicio de día
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
