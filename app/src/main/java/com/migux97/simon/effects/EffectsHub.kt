package com.migux97.simon.effects

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.migux97.simon.data.AppFilter
import com.migux97.simon.data.EffectConfig
import com.migux97.simon.data.Repository
import com.migux97.simon.data.StudioStore
import com.migux97.simon.data.TriggerType

/**
 * Muestra los efectos en pantalla y escucha los eventos del sistema
 * (desbloqueo, pantalla encendida, carga). Lo arrancan el servicio de
 * notificaciones y el de accesibilidad, que el sistema mantiene vivos.
 */
object EffectsHub {
    private val main = Handler(Looper.getMainLooper())
    private var registered = false
    private var showing = 0
    private var lastShown = 0L

    fun canDrawOverlays(context: Context) = Settings.canDrawOverlays(context)

    /** Registra los eventos del sistema una sola vez por proceso. */
    fun register(context: Context) {
        if (registered) return
        registered = true
        val app = context.applicationContext
        Repository.init(app)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                val fx = StudioStore.effectsSettings
                val rt = com.migux97.simon.tools.SimonRuntime
                when (intent.action) {
                    Intent.ACTION_USER_PRESENT -> {
                        if (fx.unlock.enabled) show(app, fx.unlock, EffectPayload(title = fx.unlock.text))
                        rt.onEvent(app, TriggerType.UNLOCK)
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        if (fx.screenOn.enabled) main.postDelayed({ show(app, fx.screenOn, EffectPayload(title = fx.screenOn.text)) }, 150)
                        rt.onScreenOn(app)
                    }
                    Intent.ACTION_SCREEN_OFF -> rt.onEvent(app, TriggerType.SCREEN_OFF)
                    Intent.ACTION_POWER_CONNECTED -> {
                        if (fx.charging.enabled) {
                            val bm = app.getSystemService(Context.BATTERY_SERVICE) as? android.os.BatteryManager
                            val level = bm?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 0
                            show(app, fx.charging, EffectPayload(title = fx.charging.text.ifBlank { "Cargando" }, text = "$level%"))
                        }
                        rt.onEvent(app, TriggerType.CHARGER_ON)
                    }
                    Intent.ACTION_POWER_DISCONNECTED -> rt.onEvent(app, TriggerType.CHARGER_OFF)
                    Intent.ACTION_BATTERY_LOW -> rt.onEvent(app, TriggerType.BATTERY_LOW)
                    Intent.ACTION_HEADSET_PLUG -> {
                        // El sistema manda este aviso también al registrarse: se ignora el primero.
                        if (!isInitialStickyBroadcast) rt.onEvent(app, if (intent.getIntExtra("state", 0) == 1) TriggerType.HEADSET_ON else TriggerType.HEADSET_OFF)
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_HEADSET_PLUG)
        }
        com.migux97.simon.tools.SimonRuntime.registerNetwork(app)
        ContextCompat.registerReceiver(app, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    /** Decide si una app pasa el filtro configurado. */
    fun allowed(config: EffectConfig, packageName: String): Boolean = when (config.filter) {
        AppFilter.ALL -> true
        AppFilter.ONLY -> config.apps.any { it.substringBefore('/') == packageName }
        AppFilter.EXCEPT -> config.apps.none { it.substringBefore('/') == packageName }
    }

    fun appIcon(context: Context, packageName: String): Bitmap? = try {
        context.packageManager.getApplicationIcon(packageName).toBitmap(160, 160)
    } catch (e: Exception) {
        null
    }

    /** Color promedio (saturado) de un ícono, para teñir el efecto con el color de la app. */
    fun dominantColor(bmp: Bitmap?): Int? {
        if (bmp == null) return null
        val small = Bitmap.createScaledBitmap(bmp, 16, 16, true)
        var r = 0L; var g = 0L; var b = 0L; var n = 0L
        for (x in 0 until 16) for (y in 0 until 16) {
            val c = small.getPixel(x, y)
            if (Color.alpha(c) < 128) continue
            val hsv = FloatArray(3); Color.colorToHSV(c, hsv)
            if (hsv[1] < 0.25f || hsv[2] < 0.25f) continue
            r += Color.red(c); g += Color.green(c); b += Color.blue(c); n++
        }
        if (n == 0L) return null
        val hsv = FloatArray(3)
        Color.colorToHSV(Color.rgb((r / n).toInt(), (g / n).toInt(), (b / n).toInt()), hsv)
        hsv[1] = (hsv[1] * 1.3f).coerceAtMost(1f); hsv[2] = hsv[2].coerceAtLeast(0.8f)
        return Color.HSVToColor(hsv)
    }

    /** Muestra un efecto. Usa la capa de accesibilidad si está activa (sirve hasta en la pantalla de bloqueo). */
    fun show(context: Context, config: EffectConfig, payload: EffectPayload, force: Boolean = false) {
        main.post {
            val now = System.currentTimeMillis()
            if (!force && (showing > 1 || now - lastShown < 400)) return@post
            val access = SimonAccessibilityService.instance
            val ctx: Context = access ?: context.applicationContext
            val type = when {
                access != null -> WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
                canDrawOverlays(context) -> WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else -> return@post
            }
            val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            var view: EffectView? = null
            view = EffectView(ctx, config, payload) {
                try { view?.let { wm.removeView(it) } } catch (_: Exception) {}
                showing--
            }
            try {
                wm.addView(view, params)
                showing++
                lastShown = now
            } catch (_: Exception) {
            }
        }
    }
}
