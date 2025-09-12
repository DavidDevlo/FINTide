// ui/profile/CardsViewModel.kt
package com.example.finazas.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.finazas.data.local.AppDatabase
import com.example.finazas.data.local.entity.PaymentCard
import com.example.finazas.data.local.dao.PaymentCardDao
import com.example.finazas.data.repo.PaymentCardRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale

data class CardFormState(
    val holderName: String = "",
    val nickname: String = "",
    val panInput: String = "",        // SOLO para detectar brand/last4 (NO se guarda)
    val brand: String = "UNKNOWN",    // autocalculada
    val last4Preview: String = "",    // vista previa
    val expMonth: String = "",        // "1".."12"
    val expYear: String = "",         // "2025"
    val colorHex: String = defaultColorForBrand("UNKNOWN"),
    val isDefault: Boolean = false,
    val cardType: String = "DEBIT",   // "DEBIT" | "CREDIT"
    val isPhysical: Boolean = true,

    val errors: Map<String, String> = emptyMap()
)

class CardsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = PaymentCardRepository(AppDatabase.get(app).paymentCardDao())

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    val cards: StateFlow<List<PaymentCard>> =
        _query.debounce(250)
            .flatMapLatest { q -> if (q.isBlank()) repo.observeActive() else repo.search(q) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _form = MutableStateFlow(CardFormState())
    val form: StateFlow<CardFormState> = _form

    private var editingId: Long? = null
    private var loadJob: Job? = null

    fun setQuery(q: String) { _query.value = q }
    fun startCreate() { editingId = null; _form.value = CardFormState() }

    fun loadForEdit(id: Long) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val c = repo.get(id) ?: return@launch
            editingId = c.id
            _form.value = CardFormState(
                holderName = c.holderName,
                nickname = c.nickname.orEmpty(),
                brand = c.brand,
                last4Preview = c.panLast4,
                expMonth = c.expMonth.toString(),
                expYear = c.expYear.toString(),
                colorHex = c.colorHex,
                isDefault = c.isDefault,
                cardType = c.cardType,            // NUEVO
                isPhysical = c.isPhysical         // NUEVO
            )
        }
    }

    fun onFormChange(
        holderName: String? = null,
        nickname: String? = null,
        panInput: String? = null,
        expMonth: String? = null,
        expYear: String? = null,
        colorHex: String? = null,
        isDefault: Boolean? = null,
        cardType: String? = null,        // NUEVO
        isPhysical: Boolean? = null      // NUEVO
    ) {
        val prev = _form.value
        val pan = panInput ?: prev.panInput
        val brand = detectBrand(pan)
        val last4 = pan.takeLast(4)
        val color = colorHex ?: if (prev.brand != brand && prev.colorHex == defaultColorForBrand(prev.brand)) {
            defaultColorForBrand(brand)
        } else prev.colorHex

        _form.value = prev.copy(
            holderName = holderName ?: prev.holderName,
            nickname = nickname ?: prev.nickname,
            panInput = pan,
            brand = brand,
            last4Preview = last4,
            expMonth = expMonth ?: prev.expMonth,
            expYear = expYear ?: prev.expYear,
            colorHex = color,
            isDefault = isDefault ?: prev.isDefault,
            cardType = (cardType ?: prev.cardType).uppercase(),
            isPhysical = isPhysical ?: prev.isPhysical
        )
    }

    private fun validate(forCreate: Boolean): Boolean {
        val f = _form.value
        val errs = mutableMapOf<String, String>()

        if (f.holderName.isBlank()) errs["holderName"] = "Nombre en tarjeta obligatorio"
        val m = f.expMonth.toIntOrNull()
        if (m == null || m !in 1..12) errs["expMonth"] = "Mes 1..12"
        val y = f.expYear.toIntOrNull()
        if (y == null || y < 2020 || y > 2100) errs["expYear"] = "Año inválido"

        if (f.cardType.uppercase() !in setOf("DEBIT","CREDIT")) errs["cardType"] = "Tipo inválido"
        // validación de PAN solo en creación
        if (forCreate) {
            val digits = f.panInput.filter { it.isDigit() }
            if (digits.length !in 12..19) errs["panInput"] = "PAN inválido (12–19 dígitos)"
            else if (!luhnValid(digits)) errs["panInput"] = "PAN no supera validación"
        }
        val color = f.colorHex.trim()
        if (!color.matches(Regex("^#([0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$")))
            errs["colorHex"] = "Color HEX inválido"

        _form.value = f.copy(errors = errs)
        return errs.isEmpty()
    }

    fun save(onDone: (Long?) -> Unit) = viewModelScope.launch {
        val create = (editingId == null)
        if (!validate(forCreate = create)) return@launch
        val f = _form.value

        if (create) {
            val panDigits = f.panInput.filter { it.isDigit() }
            val id = repo.create(
                holderName = f.holderName,
                nickname = f.nickname.ifBlank { null },
                brand = detectBrand(panDigits),
                panLast4 = panDigits.takeLast(4),
                expMonth = f.expMonth.toInt(),
                expYear = f.expYear.toInt(),
                colorHex = f.colorHex,
                isDefault = f.isDefault,
                cardType = f.cardType,            // NUEVO
                isPhysical = f.isPhysical         // NUEVO
            )
            onDone(id)
        } else {
            repo.updateMeta(
                id = editingId!!,
                nickname = f.nickname.ifBlank { null },
                colorHex = f.colorHex,
                setDefault = f.isDefault
            )
            onDone(editingId)
        }
        startCreate()
    }


    fun delete(id: Long) = viewModelScope.launch { repo.delete(id) }
    fun setDefault(id: Long) = viewModelScope.launch { repo.setDefault(id) }

    // -------- Helpers de marca / Luhn / color --------

    private fun detectBrand(pan: String): String {
        val d = pan.filter { it.isDigit() }
        return when {
            d.startsWith("4") -> "VISA"
            d.startsWith("34") || d.startsWith("37") -> "AMEX"
            d.take(2).toIntOrNull() in 51..55 -> "MASTERCARD"
            d.length >= 4 && d.take(4).toIntOrNull() in 2200..2299 -> "MIR"
            d.startsWith("36") || d.startsWith("38") || d.startsWith("30") -> "DINERS"
            else -> "UNKNOWN"
        }
    }

    private fun luhnValid(pan: String): Boolean {
        var sum = 0
        var alt = false
        for (i in pan.length - 1 downTo 0) {
            var n = pan[i] - '0'
            if (alt) {
                n *= 2
                if (n > 9) n -= 9
            }
            sum += n
            alt = !alt
        }
        return sum % 10 == 0
    }
}

private fun defaultColorForBrand(brand: String): String = when (brand.uppercase(Locale.US)) {
    "VISA" -> "#2563EB"        // azul
    "MASTERCARD" -> "#F59E0B"  // naranja
    "AMEX" -> "#10B981"        // verde
    "DINERS" -> "#7C3AED"      // púrpura
    else -> "#3B82F6"          // azul por defecto
}
