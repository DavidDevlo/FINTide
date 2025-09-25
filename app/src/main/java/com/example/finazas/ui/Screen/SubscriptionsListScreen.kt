package com.example.finazas.ui.Screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.finazas.data.local.entity.Subscription
import com.example.finazas.navigation.AppRoute
import com.example.finazas.ui.subscriptions.SubscriptionViewModel
import java.util.*
import kotlin.math.abs
import kotlin.math.ceil
import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.work.*
import com.example.finazas.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.max


 // pon false cuando termines de probar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsListScreen(
    navController: NavHostController,
    vm: SubscriptionViewModel = viewModel(),
    onBack: () -> Unit

) {
    // Lista viva (solo activas por defecto; ajusta según tu VM)
    val subs by vm.subscriptions.collectAsStateWithLifecycle(initialValue = emptyList())
    val appCtx = LocalContext.current

    // Permiso (Android 13+) y canal al arrancar
    EnsurePostNotificationsPermission()
    LaunchedEffect(Unit) { ensureChannel(appCtx) }

// 2) (Re)programa recordatorios para TODAS las suscripciones activas cada vez que cambie la lista
    val ctx = LocalContext.current
    LaunchedEffect(subs) {
        subs.filter { it.isActive && it.nextDueAt > 0L }.forEach { sub ->
            scheduleSubscriptionReminders(appCtx, sub)                    // real (9:00am)
            cancelSubscriptionTestReminders(appCtx, sub.id)               // evita duplicados de prueba
            if(ENABLE_TEST_NOTIF) {
                cancelSubscriptionTestReminders(appCtx, sub.id)
                scheduleSubscriptionTest(appCtx, sub, 10)      // prueba: una notificación en 10s
            }
        }
    }




    // Estados de diálogos
    var confirmDeleteId by remember { mutableStateOf<Long?>(null) }
    var confirmCancelId by remember { mutableStateOf<Long?>(null) }
    var payDialog by remember { mutableStateOf<PayDialogState?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Suscripciones y recibos", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(AppRoute.SubscriptionNew.create()) }) {
                        Icon(Icons.Outlined.Add, contentDescription = "Nueva")
                    }
                }
            )
        }
    ) { inner ->
        if (subs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay suscripciones. Toca + para agregar.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(subs.size, key = { subs[it].id }) { idx ->
                    val s = subs[idx]
                    SubscriptionRow(
                        sub = s,
                        onPay = {
                            // Si es variable pedimos monto; si es fijo confirmamos con default
                            payDialog = PayDialogState(
                                id = s.id,
                                title = s.title,
                                variable = s.variableAmount || s.amountCents == null,
                                defaultAmountCents = s.amountCents
                            )
                        },
                        onEdit = { navController.navigate(AppRoute.SubscriptionNew.edit(s.id)) },
                        onCancel = { confirmCancelId = s.id },
                        onDelete = { confirmDeleteId = s.id }
                    )
                }
            }
        }
    }

    // --- Diálogo: Pagar ---
    payDialog?.let { st ->
        PayDialog(
            state = st,
            onDismiss = { payDialog = null },
            onConfirm = { cents ->
                // VM debe crear Movement ligado a la sub y adelantar nextDueAt
                vm.pay(st.id, cents)
                payDialog = null
            }
        )
    }

    // --- Confirmar cancelar (soft) ---
    confirmCancelId?.let { id ->
        AlertDialog(
            onDismissRequest = { confirmCancelId = null },
            title = { Text("Cancelar suscripción") },
            text = { Text("La suscripción se marcará como inactiva, pero el historial se conserva.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.cancel(id)                 // tu lógica (soft delete / isActive=false)
                    cancelSubscriptionReminders(appCtx, id)
                    confirmCancelId = null
                }) { Text("Cancelar suscripción") }
            },
            dismissButton = {
                TextButton(onClick = { confirmCancelId = null }) { Text("Volver") }
            }
        )
    }

    // --- Confirmar eliminar (hard) ---
    confirmDeleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { confirmDeleteId = null },
            title = { Text("Eliminar suscripción") },
            text = { Text("Esto eliminará el registro de suscripción. No se eliminarán movimientos históricos ya pagados.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(id)                 // tu lógica (hard delete)
                    cancelSubscriptionReminders(appCtx, id)
                    confirmDeleteId = null
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteId = null }) { Text("Cancelar") }
            }
        )
    }
}
private const val ENABLE_TEST_NOTIF = true
private fun SubscriptionViewModel.cancel(id: Long) {}

private fun SubscriptionViewModel.pay(id: Long, cents: Long) {}

@Composable
private fun SubscriptionRow(
    sub: Subscription,
    onPay: () -> Unit,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    val stripeColor = ColorFromHex(sub.colorHex)
    val (dueLabel, dueTint) = dueTextAndTint(sub.nextDueAt)
    val amountText = when {
        sub.variableAmount || sub.amountCents == null -> "Monto variable"
        else -> formatMoney(sub.amountCents)
    }

    ElevatedCard(
        onClick = onEdit,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Franja de color
            Box(
                Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(stripeColor)
            )

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(sub.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(amountText, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = MaterialTheme.typography.bodySmall.fontSize)
                Spacer(Modifier.height(4.dp))
                Text(dueLabel, color = dueTint, fontSize = MaterialTheme.typography.bodySmall.fontSize)
            }

            // Acciones rápidas
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(onClick = onPay, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)) {
                    Icon(Icons.Outlined.Payments, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Pagar")
                }

                var menu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Más")
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Editar") },
                            onClick = { menu = false; onEdit() },
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Cancelar suscripción") },
                            onClick = { menu = false; onCancel() }
                        )
                        DropdownMenuItem(
                            text = { Text("Eliminar") },
                            onClick = { menu = false; onDelete() },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) }
                        )
                    }
                }
            }
        }
    }
}

/* ===================== Diálogo de pago ===================== */

private data class PayDialogState(
    val id: Long,
    val title: String,
    val variable: Boolean,
    val defaultAmountCents: Long?
)

@Composable
private fun PayDialog(
    state: PayDialogState,
    onDismiss: () -> Unit,
    onConfirm: (amountCents: Long) -> Unit
) {
    var amountText by remember {
        mutableStateOf(
            if (state.variable || state.defaultAmountCents == null) ""
            else centsToText(state.defaultAmountCents)
        )
    }
    val isError = state.variable && amountText.isBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pagar ${state.title}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.variable) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Monto (S/)") },
                        singleLine = true,
                        isError = isError,
                        supportingText = { if (isError) Text("Ingresa un monto") }
                    )
                } else {
                    Text("Monto: ${centsToText(state.defaultAmountCents!!)}")
                }
                Text("Se registrará un movimiento y se reprogramará el próximo vencimiento.")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cents = if (state.variable) parseAmountToCents(amountText) else (state.defaultAmountCents ?: 0L)
                if (cents > 0) onConfirm(cents)
            }) { Text("Confirmar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

/* ===================== Helpers UI/Domain ===================== */

@Composable
private fun dueTextAndTint(nextDueAt: Long): Pair<String, Color> {
    val now = System.currentTimeMillis()
    val oneDay = 86_400_000L
    val days = ceil((nextDueAt - startOfDay(now)) / oneDay.toDouble()).toInt()

    return when {
        days < 0 -> {
            val dd = abs(days)
            "Vencida hace $dd ${if (dd == 1) "día" else "días"}" to Color(0xFFE95555)
        }
        days == 0 -> "Vence hoy" to Color(0xFFF59E0B)
        days == 1 -> "Vence mañana" to Color(0xFFF59E0B)
        else -> "Faltan $days días" to MaterialTheme.colorScheme.onSurfaceVariant
    }
}

private fun startOfDay(ms: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = ms
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun formatMoney(cents: Long): String = "S/ " + String.format(Locale.US, "%,.2f", cents / 100.0)
private fun centsToText(cents: Long): String = String.format(Locale.US, "%.2f", cents / 100.0)
private fun parseAmountToCents(txt: String): Long = runCatching {
    val norm = txt.trim().replace("S/", "", ignoreCase = true).replace(",", ".")
    (norm.toDouble() * 100).toLong()
}.getOrDefault(0L)

/**
 * Color seguro en sRGB desde "#RRGGBB" o "#AARRGGBB".
 * Usa ctor Int (ARGB) – evita packed ULong para no crashear.
 */
private fun ColorFromHex(hex: String): Color {
    val s = hex.trim()
    return try {
        val withHash = if (s.startsWith("#")) s else "#$s"
        val argbInt = android.graphics.Color.parseColor(withHash)
        Color(argbInt)
    } catch (_: Exception) {
        Color(0xFF888888.toInt())
    }
}
private const val CHANNEL_ID = "subs_reminders"
private const val CHANNEL_NAME = "Recordatorios de suscripciones"
private const val CHANNEL_DESC = "Te avisa cuando una suscripción está por vencer"
private const val WORK_TAG_PREFIX = "sub-" // tag por suscripción: sub-<id>

/** Crea el canal si no existe. Idempotente. */
private fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= 26) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val exists = nm.getNotificationChannel(CHANNEL_ID) != null
        if (!exists) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                    description = CHANNEL_DESC
                    enableVibration(true)
                }
            )
        }
    }
}

/** Worker que muestra la notificación. */
class SubscriptionReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.Default) {
        val id = inputData.getLong("id", -1L)
        val title = inputData.getString("title") ?: return@withContext Result.failure()
        val days = inputData.getInt("days", -1) // 5..1 y 0 (hoy)

        ensureChannel(applicationContext)

        // Al tocar la notificación abrimos la pantalla de Suscripciones
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("deeplink", "subscriptions") // maneja esto en tu Main para navegar a la pantalla
        }
        val flags = if (Build.VERSION.SDK_INT >= 23)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT

        val pi = PendingIntent.getActivity(
            applicationContext,
            (id * 10 + days).toInt(),
            intent,
            flags
        )

        val text = when {
            days > 1  -> "Faltan $days días para: $title"
            days == 1 -> "Falta 1 día para: $title"
            else      -> "Hoy vence: $title"
        }

        // Android 13+: verifica permiso antes de notificar
        if (Build.VERSION.SDK_INT >= 33) {
            val ok = ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS
            ) == PERMISSION_GRANTED
            if (!ok) return@withContext Result.success()
        }

        NotificationManagerCompat.from(applicationContext).notify(
            (id * 10 + days).toInt(),
            NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // usa tu ícono (o R.drawable.ic_notification)
                .setContentTitle("Suscripción")
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build()
        )
        Result.success()
    }


}

/** Programa 6 recordatorios: 5,4,3,2,1 días antes y HOY (a las 9:00 am hora local). */
fun scheduleSubscriptionReminders(context: Context, sub: Subscription) {
    val wm = WorkManager.getInstance(context)
    // Cancelar cualquier recordatorio previo de esta suscripción
    wm.cancelAllWorkByTag("$WORK_TAG_PREFIX${sub.id}")

    // Si está inactiva o sin fecha, no programar
    if (!sub.isActive || sub.nextDueAt <= 0L) return

    val now = System.currentTimeMillis()
    val dueAt9 = setHourLocal(sub.nextDueAt, hour = 9) // Notificar a las 9am

    // Días faltantes: 5..1 y 0 (hoy)
    val days = listOf(5, 4, 3, 2, 1, 0)
    for (d in days) {
        val fireAt = dueAt9 - d * DAY_MS
        val delayMs = max(0L, fireAt - now)
        if (delayMs < 60_000L) continue // evita colas inmediatas o pasadas (<1 min)

        val data = workDataOf(
            "id" to sub.id,
            "title" to sub.title,
            "days" to d
        )

        val req = OneTimeWorkRequestBuilder<SubscriptionReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .addTag("$WORK_TAG_PREFIX${sub.id}")
            .setInputData(data)
            .build()

        wm.enqueue(req)
    }
}

/** Cancela todos los recordatorios de una suscripción. */
fun cancelSubscriptionReminders(context: Context, subId: Long) {
    WorkManager.getInstance(context).cancelAllWorkByTag("$WORK_TAG_PREFIX$subId")
}

private const val DAY_MS = 86_400_000L

/** Ajusta un timestamp (ms) al mismo día a la hora local indicada (ej. 9:00). */
private fun setHourLocal(ms: Long, hour: Int): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = ms
    cal.set(Calendar.HOUR_OF_DAY, hour)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

/* ======== Permiso runtime para Android 13+ (Compose puro, sin libs extra) ======== */

@Composable
private fun EnsurePostNotificationsPermission() {
    if (Build.VERSION.SDK_INT < 33) return
    val ctx = LocalContext.current
    val granted = ContextCompat.checkSelfPermission(
        ctx, Manifest.permission.POST_NOTIFICATIONS
    ) == PERMISSION_GRANTED

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* ignore: si niega, simplemente no mostramos */ }

    LaunchedEffect(Unit) {
        if (!granted && ctx is Activity) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
fun scheduleSubscriptionTest(context: Context, sub: Subscription, seconds: Long) {
    ensureChannel(context)
    val tag = "sub-test-${sub.id}"
    WorkManager.getInstance(context).cancelAllWorkByTag(tag)

    val data = workDataOf(
        "id" to sub.id,
        "title" to sub.title,
        "days" to 0 // solo afecta el texto "Hoy vence: ..."
    )

    val req = OneTimeWorkRequestBuilder<SubscriptionReminderWorker>()
        .setInitialDelay(seconds, TimeUnit.SECONDS)
        .addTag(tag)
        .setInputData(data)
        .build()

    WorkManager.getInstance(context).enqueue(req)
}

fun cancelSubscriptionTestReminders(context: Context, subId: Long) {
    WorkManager.getInstance(context).cancelAllWorkByTag("sub-test-$subId")
}
