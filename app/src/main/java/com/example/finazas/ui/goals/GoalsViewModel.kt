package com.example.finazas.ui.goals

import com.example.finazas.data.repo.GoalRepository
import com.example.finazas.data.local.entity.Goal
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.finazas.data.local.AppDatabase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

data class GoalFormState(
    val title: String = "",
    val targetAmount: String = "",   // texto -> centavos
    val currentAmount: String = "",  // texto -> centavos
    val colorHex: String = "#FF9800",
    val isActive: Boolean = true,
    val errors: Map<String, String> = emptyMap()
)

class GoalViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = GoalRepository(AppDatabase.get(app).goalDao())

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    // Lista reactiva: si no hay query -> observeAll(), si hay query -> search(q)
    val goals: StateFlow<List<Goal>> = _query
        .debounce(250)
        .flatMapLatest { q -> if (q.isBlank()) repo.observeAll() else repo.search(q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _form = MutableStateFlow(GoalFormState())
    val form: StateFlow<GoalFormState> = _form

    private var editingId: Long? = null
    private var loadJob: Job? = null

    fun setQuery(q: String) { _query.value = q }

    fun startCreate() {
        editingId = null
        _form.value = GoalFormState()
    }

    suspend fun loadForEdit(id: Long) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val g = repo.getById(id) ?: return@launch
            editingId = g.id
            _form.value = GoalFormState(
                title = g.title,
                targetAmount = centsToText(g.targetAmount),
                currentAmount = centsToText(g.currentAmount),
                colorHex = g.colorHex,
                isActive = g.isActive
            )
        }
    }

    fun onFormChange(
        title: String? = null,
        targetAmount: String? = null,
        currentAmount: String? = null,
        colorHex: String? = null,
        isActive: Boolean? = null
    ) {
        _form.value = _form.value.copy(
            title = title ?: _form.value.title,
            targetAmount = targetAmount ?: _form.value.targetAmount,
            currentAmount = currentAmount ?: _form.value.currentAmount,
            colorHex = colorHex ?: _form.value.colorHex,
            isActive = isActive ?: _form.value.isActive
        )
    }

    private fun validate(): Boolean {
        val f = _form.value
        val errs = mutableMapOf<String, String>()

        if (f.title.isBlank()) errs["title"] = "Título obligatorio"

        val targetCents = parseMoneyToCents(f.targetAmount)
        if (targetCents == null || targetCents < 0) errs["targetAmount"] = "Meta inválida"

        val currentCents = if (f.currentAmount.isBlank()) 0L else parseMoneyToCents(f.currentAmount)
        if (currentCents == null || currentCents < 0) errs["currentAmount"] = "Progreso inválido"

        val color = f.colorHex.trim()
        if (!color.matches(Regex("^#([0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$")))
            errs["colorHex"] = "Color HEX inválido (usa #RRGGBB o #AARRGGBB)"

        _form.value = f.copy(errors = errs)
        return errs.isEmpty()
    }

    fun save(onDone: (Long?) -> Unit) = viewModelScope.launch {
        if (!validate()) return@launch
        val f = _form.value
        val title = f.title.trim()
        val target = parseMoneyToCents(f.targetAmount) ?: 0L
        val current = if (f.currentAmount.isBlank()) 0L else (parseMoneyToCents(f.currentAmount) ?: 0L)
        val color = f.colorHex.trim()
        val active = f.isActive

        val id = editingId
        if (id == null) {
            val newId = repo.create(title, target, color)
            if (current != 0L) repo.setAmount(newId, current)
            onDone(newId)
        } else {
            val existing = repo.getById(id) ?: return@launch
            val updated = existing.copy(
                title = title,
                targetAmount = target,
                currentAmount = current,
                colorHex = color,
                isActive = active
            )
            repo.update(updated)
            onDone(id)
        }
        startCreate() // limpia form
    }

    fun delete(id: Long) = viewModelScope.launch {
        repo.delete(id) // soft delete
        if (editingId == id) startCreate()
    }
    // ---------- Métodos añadidos para que compile el UI ----------
    fun deleteGoal(id: Long) = delete(id)

    fun updateGoalAmount(id: Long, newAmount: Long) = viewModelScope.launch {
        // Actualiza el monto actual (en centavos)
        repo.setAmount(id, newAmount)
        // Nota: la lógica de "si llegó a 100% borrar" la maneja la UI.
        // Si prefieres moverla aquí, avísame y la encapsulamos.
    }

    // Helpers de conversión
    private fun parseMoneyToCents(text: String): Long? {
        // Acepta "200", "200.00", "200,00"
        val normalized = text.trim().replace(",", ".")
        val d = normalized.toDoubleOrNull() ?: return null
        return (d * 100).toLong()
    }

    private fun centsToText(cents: Long): String {
        return String.format("%.2f", cents / 100.0)
    }
}

