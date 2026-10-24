package com.migux97.simon.tools

import android.app.AlarmManager
import android.app.KeyguardManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import com.migux97.simon.data.ActionType
import com.migux97.simon.data.EffectConfig
import com.migux97.simon.data.EffectStyle
import com.migux97.simon.data.ImageStore
import com.migux97.simon.data.Lookup
import com.migux97.simon.data.MediaLibrary
import com.migux97.simon.data.Repository
import com.migux97.simon.data.Routine
import com.migux97.simon.data.RoutineAction
import com.migux97.simon.data.StudioStore
import com.migux97.simon.data.TriggerType
import com.migux97.simon.effects.EffectPayload
import com.migux97.simon.effects.EffectsHub
import com.migux97.simon.launcher.AppCatalog
import com.migux97.simon.launcher.SystemActions
import com.migux97.simon.wallpaper.LiveWallpaper
import com.migux97.simon.wallpaper.StaticWallpapers
import com.migux97.simon.wallpaper.WallpaperTarget
import java.util.Calendar
import kotlin.concurrent.thread

/**
 * Punto central de los eventos del sistema: los reparte entre rutinas,
 * pantalla de carga, pantalla de bloqueo y fondos automáticos.
 */
object SimonRuntime {
    private val main = Handler(Looper.getMainLooper())
    private var networkRegistered = false
    private var onWifi: Boolean? = null

    fun onEvent(context: Context, type: TriggerType) {
        val app = context.applicationContext
        Repository.init(app)
        RoutineEngine.trigger(app, type)
        when (type) {
            TriggerType.CHARGER_ON -> ChargingLauncher.maybeShow(app)
            TriggerType.UNLOCK -> if (StudioStore.rotationSettings.onUnlock) thread { WallpaperRotator.next(app) }
            else -> Unit
        }
    }

    /** Pantalla encendida (antes de desbloquear): pantalla de bloqueo propia. */
    fun onScreenOn(context: Context) {
        LockLauncher.maybeShow(context.applicationContext)
    }

    /** Escucha cuándo el teléfono entra o sale de un Wi-Fi. */
    fun registerNetwork(context: Context) {
        if (networkRegistered) return
        networkRegistered = true
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return
        try {
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                    val wifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    update(context, wifi)
                }

                override fun onLost(network: Network) = update(context, false)
            })
        } catch (_: Exception) {
        }
    }

    private fun update(context: Context, wifi: Boolean) {
        val before = onWifi
        onWifi = wifi
        // La primera lectura solo fija el estado; no dispara rutinas.
        if (before == null || before == wifi) return
        main.post { onEvent(context, if (wifi) TriggerType.WIFI_ON else TriggerType.WIFI_OFF) }
    }

    fun isLocked(context: Context): Boolean =
        context.getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true
}

// ======================================================================
//  Rutinas
// ======================================================================

object RoutineEngine {

    /** Prefijos de [RoutineAction.value] para "Poner un fondo fijo" con una foto o un dibujo propio. */
    const val PHOTO_PREFIX = "photo:"
    const val DRAWING_PREFIX = "drawing:"

    fun trigger(context: Context, type: TriggerType) {
        StudioStore.routineList.filter { it.enabled && it.trigger.type == type }.forEach { run(context, it) }
    }

    /**
     * Ejecuta las acciones. Con [record] anota la hora en la rutina guardada
     * (solo si ya existe: probar una rutina sin guardar no la crea).
     */
    fun run(context: Context, routine: Routine, record: Boolean = true) {
        routine.actions.forEach { a ->
            try {
                runAction(context, a)
            } catch (_: Exception) {
            }
        }
        if (record) {
            val saved = StudioStore.routineList.firstOrNull { it.id == routine.id }
            if (saved != null) StudioStore.saveRoutine(saved.copy(lastRun = System.currentTimeMillis()))
        }
    }

    private fun runAction(context: Context, a: RoutineAction) {
        when (a.type) {
            ActionType.STATIC_WALLPAPER -> {
                val target = WallpaperTarget.entries[a.number.coerceIn(0, WallpaperTarget.entries.lastIndex)]
                when {
                    a.value.startsWith(PHOTO_PREFIX) -> thread {
                        val (w, h) = StaticWallpapers.screenSize(context)
                        ImageStore.load(a.value.removePrefix(PHOTO_PREFIX), maxOf(w, h))?.let { StaticWallpapers.apply(context, it, target) }
                    }
                    a.value.startsWith(DRAWING_PREFIX) -> thread {
                        val (w, h) = StaticWallpapers.screenSize(context)
                        MediaLibrary.loadDrawing(a.value.removePrefix(DRAWING_PREFIX), maxOf(w, h))?.let { StaticWallpapers.apply(context, it, target) }
                    }
                    else -> Lookup.static(a.value)?.let { sw ->
                        thread {
                            val (w, h) = StaticWallpapers.screenSize(context)
                            StaticWallpapers.apply(context, StaticWallpapers.render(sw, w, h), target)
                        }
                    }
                }
            }
            ActionType.LIVE_WALLPAPER -> Lookup.live(a.value)?.let {
                // Si el fondo animado de Simón ya está puesto, cambia al instante; si no, queda listo.
                if (LiveWallpaper.isActive(context)) Repository.setLive(it) else Repository.saveLiveToCollection(it)
            }
            ActionType.THEME -> Lookup.theme(a.value)?.let { theme ->
                Repository.setAppliedTheme(theme.id)
                theme.widgets.forEach { w -> if (Repository.design(w.id) == null) Repository.saveDesign(w) }
                thread {
                    val (w, h) = StaticWallpapers.screenSize(context)
                    StaticWallpapers.apply(context, StaticWallpapers.render(theme.wallpaper, w, h), WallpaperTarget.BOTH)
                }
            }
            ActionType.NEXT_WALLPAPER -> thread { WallpaperRotator.next(context) }
            ActionType.RINGER -> {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                am.ringerMode = when (a.number) {
                    1 -> AudioManager.RINGER_MODE_VIBRATE
                    2 -> AudioManager.RINGER_MODE_SILENT
                    else -> AudioManager.RINGER_MODE_NORMAL
                }
            }
            ActionType.DND -> {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (nm.isNotificationPolicyAccessGranted) {
                    nm.setInterruptionFilter(if (a.number == 1) NotificationManager.INTERRUPTION_FILTER_PRIORITY else NotificationManager.INTERRUPTION_FILTER_ALL)
                }
            }
            ActionType.BRIGHTNESS -> if (Settings.System.canWrite(context)) {
                Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, (a.number * 255 / 100).coerceIn(1, 255))
            }
            ActionType.MEDIA_VOLUME -> {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                am.setStreamVolume(AudioManager.STREAM_MUSIC, (max * a.number / 100).coerceIn(0, max), 0)
            }
            ActionType.OPEN_APP -> AppCatalog.launchKey(context, a.value.ifBlank { null })
            ActionType.EFFECT -> {
                val style = EffectStyle.entries.getOrElse(a.number) { EffectStyle.SPARKLES }
                EffectsHub.show(context, EffectConfig(enabled = true, style = style, showIcon = false, text = a.value), EffectPayload(title = a.value), force = true)
            }
            ActionType.MESSAGE -> Handler(Looper.getMainLooper()).post { Toast.makeText(context, a.value, Toast.LENGTH_LONG).show() }
            ActionType.FLASHLIGHT_OFF -> if (SystemActions.torchOn) SystemActions.toggleFlashlight(context)
            ActionType.CHARGING_SCREEN -> ChargingLauncher.show(context)
        }
    }
}

/** Programa las rutinas por hora con alarmas que funcionan aunque Simón esté cerrado. */
object RoutineAlarms {

    private fun pending(context: Context, routine: Routine): PendingIntent {
        val intent = Intent(context, RoutineAlarmReceiver::class.java).putExtra(RoutineAlarmReceiver.EXTRA_ID, routine.id)
        return PendingIntent.getBroadcast(context, routine.id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    /** Próxima vez que toca la rutina, según hora y días elegidos. */
    fun nextTime(routine: Routine, from: Calendar = Calendar.getInstance()): Long? {
        val days = routine.trigger.days.ifEmpty { return null }
        val c = (from.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, routine.trigger.hour)
            set(Calendar.MINUTE, routine.trigger.minute)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        repeat(8) {
            if (c.after(from) && c.get(Calendar.DAY_OF_WEEK) in days) return c.timeInMillis
            c.add(Calendar.DAY_OF_YEAR, 1)
        }
        return null
    }

    fun scheduleAll(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        StudioStore.routineList.forEach { r ->
            val pi = pending(context, r)
            am.cancel(pi)
            if (r.enabled && r.trigger.type == TriggerType.TIME) {
                nextTime(r)?.let { at ->
                    // Inexacta pero con permiso de despertar: llega dentro de pocos minutos aun en reposo.
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                }
            }
        }
    }

    fun cancel(context: Context, routine: Routine) {
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pending(context, routine))
    }
}

class RoutineAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Repository.init(context)
        val id = intent.getStringExtra(EXTRA_ID)
        StudioStore.routineList.firstOrNull { it.id == id && it.enabled }?.let { RoutineEngine.run(context, it) }
        RoutineAlarms.scheduleAll(context)
    }

    companion object {
        const val EXTRA_ID = "routine_id"
    }
}
