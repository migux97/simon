package com.migux97.simon.effects

import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.graphics.drawable.toBitmap
import com.migux97.simon.data.Repository
import com.migux97.simon.data.StudioStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Una notificación lista para mostrar en el panel de Simón. */
data class PanelNotification(
    val key: String,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val time: Long,
    val icon: Bitmap?,
    val color: Int,
    val clearable: Boolean,
    val contentIntent: PendingIntent?
)

/**
 * Escucha las notificaciones para disparar efectos y para mostrarlas en el
 * panel deslizable propio. Requiere el permiso "Acceso a notificaciones".
 */
class SimonNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        Repository.init(this)
        EffectsHub.register(this)
        refresh()
    }

    override fun onListenerDisconnected() {
        if (instance === this) instance = null
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        refresh()
        if (sbn.packageName == packageName) return
        val n = sbn.notification
        if (sbn.isOngoing || (n.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return
        // Ignorar actualizaciones de la misma notificación (barras de progreso, etc.)
        val sig = sbn.key + "|" + (n.extras.getCharSequence(Notification.EXTRA_TEXT) ?: "")
        if (sig == lastSignature && System.currentTimeMillis() - lastAt < 4000) return
        lastSignature = sig; lastAt = System.currentTimeMillis()

        com.migux97.simon.tools.SoundPlayer.onNotification(
            this, sbn.packageName,
            n.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty(),
            n.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        )

        val fx = StudioStore.effectsSettings.notification
        if (!fx.enabled || !EffectsHub.allowed(fx, sbn.packageName)) return
        val icon = EffectsHub.appIcon(this, sbn.packageName)
        val color = n.color.takeIf { it != 0 && it != Notification.COLOR_DEFAULT } ?: EffectsHub.dominantColor(icon)
        val title = n.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: appLabel(this, sbn.packageName)
        val text = n.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        EffectsHub.show(this, fx, EffectPayload(icon = icon, title = title, text = text, color = color))
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        refresh()
    }

    private fun refresh() {
        val list = try {
            activeNotifications?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        _notifications.value = list
            .filter { (it.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0 }
            .sortedByDescending { it.postTime }
            .map { sbn ->
                val n = sbn.notification
                PanelNotification(
                    key = sbn.key,
                    packageName = sbn.packageName,
                    appLabel = appLabel(this, sbn.packageName),
                    title = n.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "",
                    text = (n.extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: n.extras.getCharSequence(Notification.EXTRA_TEXT))?.toString() ?: "",
                    time = sbn.postTime,
                    icon = iconCache.getOrPut(sbn.packageName) { EffectsHub.appIcon(this, sbn.packageName) },
                    color = n.color,
                    clearable = sbn.isClearable,
                    contentIntent = n.contentIntent
                )
            }
    }

    companion object {
        @Volatile
        var instance: SimonNotificationListener? = null
            private set
        private val _notifications = MutableStateFlow<List<PanelNotification>>(emptyList())
        val notifications: StateFlow<List<PanelNotification>> = _notifications.asStateFlow()
        private val iconCache = HashMap<String, Bitmap?>()
        private var lastSignature = ""
        private var lastAt = 0L

        fun appLabel(context: Context, pkg: String): String = try {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (e: Exception) {
            pkg
        }

        fun isEnabled(context: Context): Boolean {
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
            val me = ComponentName(context, SimonNotificationListener::class.java).flattenToString()
            return flat.split(":").any { it == me }
        }

        fun dismiss(key: String) {
            try { instance?.cancelNotification(key) } catch (_: Exception) {}
        }

        fun dismissAll() {
            try { instance?.cancelAllNotifications() } catch (_: Exception) {}
        }

        fun open(n: PanelNotification) {
            try {
                val pi = n.contentIntent
                if (pi != null && android.os.Build.VERSION.SDK_INT >= 34) {
                    val opts = android.app.ActivityOptions.makeBasic()
                        .setPendingIntentBackgroundActivityStartMode(android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                        .toBundle()
                    pi.send(null, 0, null, null, null, null, opts)
                } else pi?.send()
            } catch (_: Exception) {}
            if (n.clearable) dismiss(n.key)
        }
    }
}
