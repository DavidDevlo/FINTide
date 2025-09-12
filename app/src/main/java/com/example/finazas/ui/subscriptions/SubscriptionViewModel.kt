// ui/subscriptions/SubscriptionViewModel.kt
package com.example.finazas.ui.subscriptions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.finazas.data.local.AppDatabase
import com.example.finazas.data.local.entity.Subscription
import com.example.finazas.data.repo.SubscriptionRepository
import com.example.finazas.ui.Movement.todayISO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class SubscriptionFormState(
    val title: String = "",
    val amount: String = "",             // texto -> centavos (puede estar vacío si variable)
    val variableAmount: Boolean = false,

    val frequency: String = "MONTHLY",   // MONTHLY | WEEKLY | YEARLY | CUSTOM
    val intervalDays: String = "",       // solo para CUSTOM

    val nextDueIso: String = todayISO(), // yyyy-MM-dd
    val autoPay: Boolean = false,
    val colorHex: String = "#7C3AED",
    val isActive: Boolean = true,

    val errors: Map<String, String> = emptyMap()
)

class SubscriptionViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)

    // Nota: tu repo ya acepta movementDao para poder registrar pagos como movimientos.
    private val repo = SubscriptionRepository(db.subscriptionDao(), db.movementDao())

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    /** Lista reactiva para la pantalla de listado */
    val subscriptions: StateFlow<List<Subscription>> =
        _query
            .debounce(250)
            .flatMapLatest { q ->
                if (q.isBlank()) repo.observeAll() else repo.search(q)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _form = MutableStateFlow(SubscriptionFormState())
    val form: StateFlow<SubscriptionFormState> = _form

    private var editingId: Long? = null
    private var loadJob: Job? = null

    fun setQuery(q: String) { _query.value = q }

    fun startCreate() {
        editingId = null
        _form.value = SubscriptionFormState()
    }

    fun loadForEdit(id: Long) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val s = repo.getById(id) ?: return@launch
            editingId = s.id
            _form.value = SubscriptionFormState(
                title = s.title,
                amount = s.amountCents?.let { centsToText(it) } ?: "",
                variableAmount = s.variableAmount,
                frequency = s.frequency,
                intervalDays = s.intervalDays?.toString() ?: "",
                nextDueIso = millisToISO(s.nextDueAt),
                autoPay = s.autoPay,
                colorHex = s.colorHex,
                isActive = s.isActive
            )
        }
    }

    fun onFormChange(
        title: String? = null,
        amount: String? = null,
        variableAmount: Boolean? = null,
        frequency: String? = null,
        intervalDays: String? = null,
        nextDueIso: String? = null,
        autoPay: Boolean? = null,
        colorHex: String? = null,
        isActive: Boolean? = null
    ) {
        val prev = _form.value
        _form.value = prev.copy(
            title = title ?: prev.title,
            amount = amount ?: prev.amount,
            variableAmount = variableAmount ?: prev.variableAmount,
            frequency = frequency ?: prev.frequency,
            intervalDays = intervalDays ?: prev.intervalDays,
            nextDueIso = nextDueIso ?: prev.nextDueIso,
            autoPay = autoPay ?: prev.autoPay,
            colorHex = colorHex ?: prev.colorHex,
            isActive = isActive ?: prev.isActive
        )
    }

    private fun validate(): Boolean {
        val f = _form.value
        val errs = mutableMapOf<String, String>()

        if (f.title.isBlank()) errs["title"] = "Título obligatorio"

        val freq = f.frequency.uppercase(Locale.US)
        if (freq !in setOf("MONTHLY", "WEEKLY", "YEARLY", "CUSTOM")) {
            errs["frequency"] = "Frecuencia inválida"
        }

        val intervalDays = f.intervalDays.toIntOrNull()
        if (freq == "CUSTOM" && (intervalDays == null || intervalDays < 1)) {
            errs["intervalDays"] = "Días inválidos (>=1)"
        }

        val nextDueMillis = parseISOToMillis(f.nextDueIso)
        if (nextDueMillis == null) errs["nextDueIso"] = "Fecha inválida (yyyy-MM-dd)"

        if (!f.variableAmount) {
            val cents = parseMoneyToCents(f.amount)
            if (cents == null || cents <= 0) errs["amount"] = "Monto inválido"
        }

        val color = f.colorHex.trim()
        if (!color.matches(Regex("^#([0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$"))) {
            errs["colorHex"] = "Color HEX inválido (#RRGGBB o #AARRGGBB)"
        }

        _form.value = f.copy(errors = errs)
        return errs.isEmpty()
    }

    fun save(onDone: (Long?) -> Unit) = viewModelScope.launch {
        if (!validate()) return@launch
        val f = _form.value

        val amountCents = if (f.variableAmount) null else parseMoneyToCents(f.amount)
        val intervalDays = f.intervalDays.toIntOrNull()
        val nextDueMillis = parseISOToMillis(f.nextDueIso) ?: System.currentTimeMillis()

        val id = editingId
        if (id == null) {
            val newId = repo.create(
                title = f.title,
                amountCents = amountCents,
                variableAmount = f.variableAmount,
                frequency = f.frequency,
                intervalDays = intervalDays,
                nextDueAt = nextDueMillis,
                autoPay = f.autoPay,
                colorHex = f.colorHex
            )
            onDone(newId)
        } else {
            val existing = repo.getById(id) ?: return@launch
            val updated = existing.copy(
                title = f.title,
                amountCents = amountCents,
                variableAmount = f.variableAmount,
                frequency = f.frequency,
                intervalDays = intervalDays,
                nextDueAt = nextDueMillis,
                autoPay = f.autoPay,
                colorHex = f.colorHex,
                isActive = f.isActive,
                updatedAt = System.currentTimeMillis()
            )
            repo.update(updated)
            onDone(id)
        }
        startCreate()
    }

    /** Eliminar (hard delete) */
    fun delete(id: Long) = viewModelScope.launch {
        repo.delete(id)
        if (editingId == id) startCreate()
    }

    /** Cancelar suscripción (soft delete: isActive=false) */
    fun cancel(id: Long) = viewModelScope.launch {
        repo.setActive(id, active = false, ts = System.currentTimeMillis())
        if (editingId == id) startCreate()
    }

    private fun SubscriptionRepository.setActive(
        id: Long,
        active: Boolean,
        ts: Long
    ) {
    }

    /**
     * Pagar ahora:
     *  - Crea Movement (type="Egreso") con el color y título de la suscripción
     *  - Actualiza lastPaidAt / lastPaidAmountCents
     *  - Reprograma nextDueAt según frequency
     *
     *  amountText: si la suscripción es variable, ingresa el monto aquí; si es fija puedes pasar null.
     */
// En tu SubscriptionViewModel
    fun pay(id: Long, cents: Long) = viewModelScope.launch {
        // Crea Movement (Egreso) + reprograma nextDueAt
        repo.markPaid(
            id = id,
            paidAmountCents = cents,
            paidAt = System.currentTimeMillis()
        )
    }

    // ---------------- Helpers dinero / fecha ----------------
    private fun parseMoneyToCents(text: String): Long? {
        val normalized = text.trim()
            .replace("S/", "", ignoreCase = true)
            .replace("$", "")
            .replace(",", ".")
        val d = normalized.toDoubleOrNull() ?: return null
        return (d * 100).toLong()
    }

    private fun centsToText(cents: Long): String =
        String.format(Locale.US, "%.2f", cents / 100.0)

    private fun parseISOToMillis(iso: String): Long? = runCatching {
        SimpleDateFormat("yyyy-MM-dd", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .parse(iso)?.time
    }.getOrNull()

    private fun millisToISO(ms: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date(ms))

    companion object {
        fun todayISO(): String =
            SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("UTC") }
                .format(Date())
    }

    fun deleteHard(id: Long) = viewModelScope.launch {
        // si en algún momento quieres un hard delete real, crea otro método en dao
        // por ahora dejamos solo cancel() como soft. Puedes ignorar esta función.
    }
}