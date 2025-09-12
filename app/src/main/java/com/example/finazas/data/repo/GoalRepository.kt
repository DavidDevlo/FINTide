package com.example.finazas.data.repo


import com.example.finazas.data.local.dao.GoalDao
import com.example.finazas.data.local.entity.Goal
import kotlinx.coroutines.flow.Flow

class GoalRepository(
    private val dao: GoalDao
) {
    fun observeAll(): Flow<List<Goal>> = dao.observeAll()

    fun observeById(id: Long): Flow<Goal?> = dao.observeById(id)

    suspend fun getById(id: Long): Goal? = dao.findById(id)

    suspend fun create(title: String, targetAmountCents: Long, colorHex: String): Long {
        val now = System.currentTimeMillis()
        val goal = Goal(
            title = title.trim(),
            targetAmount = targetAmountCents,
            colorHex = colorHex,
            createdAt = now,
            updatedAt = now
        )
        return dao.insert(goal)
    }

    /**
     * Update completo: RECUERDA actualizar updatedAt.
     * Usa updateRaw del DAO para mantener semántica de Room,
     * pero aseguramos el timestamp aquí.
     */
    suspend fun update(goal: Goal) {
        dao.updateRaw(goal.copy(updatedAt = System.currentTimeMillis()))
    }
    // GoalRepository.kt (añade este //método)

    fun search(q: String): Flow<List<Goal>> = dao.search(q)


    /** Setea el monto exacto (sobrescribe) */
    suspend fun setAmount(id: Long, amountCents: Long) {
        dao.setAmount(id, amountCents)
    }

    /** Suma/resta de forma atómica (delta puede ser negativo) */
    suspend fun addToAmount(id: Long, deltaCents: Long) {
        dao.addToAmount(id, deltaCents)
    }

    /** Cambia título y color */
    suspend fun updateTitleAndColor(id: Long, title: String, colorHex: String) {
        dao.setTitleAndColor(id, title.trim(), colorHex)
    }

    /** Baja lógica */
    suspend fun delete(id: Long) {
        dao.softDelete(id)
    }
}

