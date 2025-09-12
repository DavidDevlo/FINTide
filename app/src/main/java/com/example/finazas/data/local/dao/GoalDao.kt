package com.example.finazas.data.local.dao


import androidx.room.*
import com.example.finazas.data.local.entity.Goal
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    // Lista reactiva de metas activas, ordenadas por última actualización
    @Query("SELECT * FROM goals WHERE isActive = 1 ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<Goal>>

    // Detalle reactivo por id (útil para pantallas de edición)
    @Query("SELECT * FROM goals WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<Goal?>

    // Búsqueda puntual (no reactiva)
    @Query("SELECT * FROM goals WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): Goal?

    @Insert
    suspend fun insert(goal: Goal): Long


    // @Update NO cambia updatedAt por sí solo; por eso dejamos
    // una query dedicada que asegura el timestamp
    @Update
    suspend fun updateRaw(goal: Goal)
    @Query("""
    SELECT * FROM goals 
    WHERE isActive = 1 AND title LIKE '%' || :q || '%' 
    ORDER BY updatedAt DESC
""")
    fun search(q: String): Flow<List<Goal>>

    // Actualiza currentAmount y updatedAt de forma atómica
    @Query("UPDATE goals SET currentAmount = :amount, updatedAt = :ts WHERE id = :id")
    suspend fun setAmount(id: Long, amount: Long, ts: Long = System.currentTimeMillis())

    // Incremento/decremento atómico (evita lecturas previas)
    @Query("UPDATE goals SET currentAmount = currentAmount + :delta, updatedAt = :ts WHERE id = :id")
    suspend fun addToAmount(id: Long, delta: Long, ts: Long = System.currentTimeMillis())

    // Cambia título y color, forzando updatedAt
    @Query("UPDATE goals SET title = :title, colorHex = :colorHex, updatedAt = :ts WHERE id = :id")
    suspend fun setTitleAndColor(id: Long, title: String, colorHex: String, ts: Long = System.currentTimeMillis())

    // Soft delete
    @Query("UPDATE goals SET isActive = 0, updatedAt = :ts WHERE id = :id")
    suspend fun softDelete(id: Long, ts: Long = System.currentTimeMillis())
}
