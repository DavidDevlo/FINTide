package com.example.finazas.ui.Movement

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.finazas.data.local.AppDatabase
import com.example.finazas.data.local.entity.Movement
import com.example.finazas.data.repo.MovementRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.graphics.Color

fun todayISO(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date())

data class MovementFormState(
    val title: String = "",
    val type: String = "Egreso",     // default
    val amount: String = "",         // texto -> centavos
    val date: String = todayISO(),   // "yyyy-MM-dd"
    val stripeColorHex: String = "#EF4444",
    val isActive: Boolean = true,
    val errors: Map<String, String> = emptyMap()
)

/* ---- UI model para Home ---- */
data class MovementRowUi(
    val title: String,
    val subtitle: String,   // "Ingreso" / "Egreso" / etc.
    val amountText: String, // "+ S/ 100.00" o "- S/ 50.00"
    val positive: Boolean,
    val date: String,       // dd/MM/yyyy
    val stripeColor: Color
)

class MovementViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = MovementRepository(AppDatabase.get(app).movementDao())

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    // "Todos" | "Ingreso" | "Egreso"
    private val _filterType = MutableStateFlow("Todos")
    val filterType: StateFlow<String> = _filterType

    /** Entidades crudas (por si las necesitas en otra pantalla) */
    val movements: StateFlow<List<Movement>> =
        repo.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Lo que Home necesita: lista ya mapeada y filtrada */
    val rows: StateFlow<List<MovementRowUi>> =
        combine(repo.observeAll(), _query.debounce(250), _filterType) { list, q, t ->
            var out = list
            if (q.isNotBlank()) {
                val needle = q.trim().lowercase(Locale.getDefault())
                out = out.filter { it.title.lowercase(Locale.getDefault()).contains(needle) }
            }
            if (!t.equals("Todos", true)) {
                out = out.filter { it.type.equals(t, true) }
            }
            out.sortedByDescending { it.date }
                .map { it.toRowUi() }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _form = MutableStateFlow(MovementFormState())
    val form: StateFlow<MovementFormState> = _form

    private var editingId: Long? = null
    private var loadJob: Job? = null

    fun setQuery(q: String) { _query.value = q }
    fun setFilterType(t: String) { _filterType.value = t }

    fun startCreate() {
        editingId = null
        _form.value = MovementFormState() // reset
    }

    fun loadForEdit(id: Long) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val m = repo.getById(id) ?: return@launch
            editingId = m.id
            _form.value = MovementFormState(
                title = m.title,
                type = m.type,
                amount = centsToText(m.amount),
                date = millisToISO(m.date),
                stripeColorHex = m.stripeColorHex,
                isActive = m.isActive
            )
        }
    }

    fun onFormChange(
        title: String? = null,
        type: String? = null,
        amount: String? = null,
        date: String? = null,            // "yyyy-MM-dd"
        stripeColorHex: String? = null,
        isActive: Boolean? = null
    ) {
        val prev = _form.value
        val newType = type ?: prev.type
        val newColor =
            if (stripeColorHex != null) stripeColorHex else prev.stripeColorHex
        _form.value = prev.copy(
            title = title ?: prev.title,
            type = newType,
            amount = amount ?: prev.amount,
            date = date ?: prev.date,
            stripeColorHex = newColor,
            isActive = isActive ?: prev.isActive
        )
    }

    private fun validate(): Boolean {
        val f = _form.value
        val errs = mutableMapOf<String, String>()

        if (f.title.isBlank()) errs["title"] = "Título obligatorio"

        val t = f.type.trim()
        if (!(t.equals("Ingreso", true) || t.equals("Egreso", true))) {
            errs["type"] = "Tipo inválido (use Ingreso o Egreso)"
        }

        val amountCents = parseMoneyToCents(f.amount)
        if (amountCents == null || amountCents < 0) errs["amount"] = "Monto inválido"

        val dateMillis = parseISOToMillis(f.date)
        if (dateMillis == null) errs["date"] = "Fecha inválida (yyyy-MM-dd)"

        val color = f.stripeColorHex.trim()
        if (!color.matches(Regex("^#([0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$")))
            errs["stripeColorHex"] = "Color HEX inválido (#RRGGBB o #AARRGGBB)"

        _form.value = f.copy(errors = errs)
        return errs.isEmpty()
    }

    fun save(onDone: (Long?) -> Unit) = viewModelScope.launch {
        if (!validate()) return@launch
        val f = _form.value
        val title = f.title.trim()
        val type = f.type.trim()
        val amount = parseMoneyToCents(f.amount) ?: 0L
        val dateMillis = parseISOToMillis(f.date) ?: System.currentTimeMillis()
        val color = f.stripeColorHex.trim()

        val id = editingId
        if (id == null) {
            val newId = repo.create(title, type, amount, dateMillis, color)
            onDone(newId)
        } else {
            val existing = repo.getById(id) ?: return@launch
            val updated = existing.copy(
                title = title,
                type = type,
                amount = amount,
                date = dateMillis,
                stripeColorHex = color,
                isActive = f.isActive
            )
            repo.update(updated)
            onDone(id)
        }
        startCreate()
    }

    fun delete(id: Long) = viewModelScope.launch {
        repo.delete(id)
        if (editingId == id) startCreate()
    }

    // Helpers dinero/fecha
    private fun parseMoneyToCents(text: String): Long? {
        val normalized = text.trim()
            .replace("S/", "", ignoreCase = true)
            .replace("$", "")
            .replace(",", ".")
        val d = normalized.toDoubleOrNull() ?: return null
        return (d * 100).toLong()
    }
    private fun centsToText(cents: Long): String = String.format(Locale.US, "%.2f", cents / 100.0)

    private fun parseISOToMillis(iso: String): Long? = runCatching {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            .parse(iso)?.time
    }.getOrNull()

    private fun millisToISO(ms: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date(ms))
}

/* ---------- Mapper + color seguro ---------- */
private fun Movement.toRowUi(): MovementRowUi {
    val isIncome = type.equals("Ingreso", ignoreCase = true)
    val positive = isIncome // todo lo que no sea "Ingreso" se considera egreso
    val sign = if (positive) "+" else "-"
    val amountText = "$sign S/ " + String.format(Locale.US, "%,.2f", amount / 100.0)
    val dateText = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(date))
    return MovementRowUi(
        title = title,
        subtitle = type,
        amountText = amountText,
        positive = positive,
        date = dateText,
        stripeColor = colorFromHexSafe(stripeColorHex)
    )
}

private fun colorFromHexSafe(hex: String): Color = try {
    val withHash = if (hex.startsWith("#")) hex else "#$hex"
    val argb = android.graphics.Color.parseColor(withHash)
    Color(argb) // ctor Int → sRGB seguro
} catch (_: Exception) {
    Color(0xFF888888.toInt())
}
