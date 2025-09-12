package com.example.finazas.data.repo


import com.example.finazas.data.local.dao.MovementDao
import com.example.finazas.data.local.entity.Movement
import com.example.finazas.data.local.entity.Subscription
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import kotlin.math.max

class MovementRepository(
    private val dao: MovementDao
) {
    fun observeAll(): Flow<List<Movement>> = dao.observeAll()
    fun search(q: String): Flow<List<Movement>> = dao.search(q)
    fun observeByType(type: String): Flow<List<Movement>> = dao.observeByType(type)
    fun observeBetween(start: Long, end: Long): Flow<List<Movement>> = dao.observeBetween(start, end)

    suspend fun getById(id: Long): Movement? = dao.findById(id)

    suspend fun create(
        title: String,
        type: String,              // "Ingreso" | "Egreso"
        amountCents: Long,
        dateMillis: Long = System.currentTimeMillis(),
        stripeColorHex: String = defaultStripeFor(type)
    ): Long {
        val now = System.currentTimeMillis()
        val m = Movement(
            title = title.trim(),
            type = type.trim(),
            amount = amountCents,
            date = dateMillis,
            stripeColorHex = stripeColorHex.trim(),
            createdAt = now,
            updatedAt = now
        )
        return dao.insert(m)
    }

    suspend fun update(movement: Movement) {
        dao.updateRaw(movement.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun setCoreFields(
        id: Long,
        title: String,
        type: String,
        amountCents: Long,
        dateMillis: Long,
        stripeColorHex: String
    ) {
        dao.setCoreFields(id, title.trim(), type.trim(), amountCents, dateMillis, stripeColorHex.trim())
    }

    suspend fun setAmount(id: Long, amountCents: Long) = dao.setAmount(id, amountCents)

    suspend fun delete(id: Long) = dao.softDelete(id)

    companion object {
        fun defaultStripeFor(type: String): String =
            if (type.equals("Ingreso", ignoreCase = true)) "#22C55E" else "#EF4444"
    }
    /** Calcula el siguiente vencimiento posterior a 'from' */
    fun nextDueAfter(sub: Subscription, from: Long = System.currentTimeMillis()): Long {
        var next = max(sub.nextDueAt, from)
        var iters = 0
        while (next <= from && iters < 120) {
            next = when (sub.frequency.uppercase()) {
                "WEEKLY" -> addDays(next, 7)
                "MONTHLY" -> addMonths(next, 1)
                "YEARLY" -> addYears(next, 1)
                "CUSTOM" -> addDays(next, (sub.intervalDays ?: 30).coerceAtLeast(1))
                else -> addDays(next, 30)
            }
            iters++
        }
        return next
    }

    private fun addDays(base: Long, days: Int): Long {
        val c = Calendar.getInstance().apply { timeInMillis = base }
        c.add(Calendar.DAY_OF_YEAR, days)
        return c.timeInMillis
    }
    private fun addMonths(base: Long, months: Int): Long {
        val c = Calendar.getInstance().apply { timeInMillis = base }
        c.add(Calendar.MONTH, months)
        return c.timeInMillis
    }
    private fun addYears(base: Long, years: Int): Long {
        val c = Calendar.getInstance().apply { timeInMillis = base }
        c.add(Calendar.YEAR, years)
        return c.timeInMillis
    }
    fun observeCount(): Flow<Int> = dao.observeCount()


}
