package com.migux97.simon.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.migux97.simon.MainActivity
import com.migux97.simon.R
import com.migux97.simon.data.Repository
import com.migux97.simon.data.WidgetDesign
import com.migux97.simon.data.WidgetKind
import com.migux97.simon.data.WidgetSize
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt

// ------------------------------------------------------------------ proveedores

abstract class SimonWidgetProvider(val size: WidgetSize) : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        Repository.init(context)
        ids.forEach { WidgetUpdater.update(context, manager, it) }
        WidgetUpdater.scheduleTick(context)
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, newOptions: Bundle) {
        Repository.init(context)
        WidgetUpdater.update(context, manager, id)
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        Repository.init(context)
        Repository.unbindWidgets(ids)
        WidgetUpdater.scheduleTick(context)
    }

    override fun onEnabled(context: Context) {
        Repository.init(context)
        RefreshWorker.schedule(context)
    }
}

class SmallWidget : SimonWidgetProvider(WidgetSize.SMALL)
class MediumWidget : SimonWidgetProvider(WidgetSize.MEDIUM)
class LargeWidget : SimonWidgetProvider(WidgetSize.LARGE)

// -------------------------------------------------------------- actualización

object WidgetUpdater {

    private const val EXTRA_DESIGN = "design_id"
    const val ACTION_TICK = "com.migux97.simon.TICK"

    fun providerFor(size: WidgetSize): Class<out SimonWidgetProvider> = when (size) {
        WidgetSize.SMALL -> SmallWidget::class.java
        WidgetSize.MEDIUM -> MediumWidget::class.java
        WidgetSize.LARGE -> LargeWidget::class.java
    }

    fun sizeForProvider(className: String?): WidgetSize = when (className) {
        MediumWidget::class.java.name -> WidgetSize.MEDIUM
        LargeWidget::class.java.name -> WidgetSize.LARGE
        else -> WidgetSize.SMALL
    }

    fun allIds(context: Context): IntArray {
        val mgr = AppWidgetManager.getInstance(context)
        return WidgetSize.entries.flatMap { size ->
            mgr.getAppWidgetIds(ComponentName(context, providerFor(size))).toList()
        }.toIntArray()
    }

    fun updateAll(context: Context, onlyKinds: Set<WidgetKind>? = null) {
        Repository.init(context)
        val mgr = AppWidgetManager.getInstance(context)
        allIds(context).forEach { id ->
            val kind = Repository.designForWidget(id)?.kind
            if (onlyKinds == null || (kind != null && kind in onlyKinds)) update(context, mgr, id)
        }
    }

    /** Vuelve a dibujar los widgets que usan un diseño (después de editarlo). */
    fun updateDesign(context: Context, designId: String) {
        val mgr = AppWidgetManager.getInstance(context)
        Repository.widgetsUsingDesign(designId).forEach { update(context, mgr, it) }
    }

    /** Tamaño en píxeles del widget, a partir de las opciones del launcher. */
    private fun pixelSize(context: Context, mgr: AppWidgetManager, id: Int, design: WidgetDesign?): Pair<Int, Int> {
        val dm = context.resources.displayMetrics
        val opts = mgr.getAppWidgetOptions(id)
        val portrait = context.resources.configuration.orientation != android.content.res.Configuration.ORIENTATION_LANDSCAPE
        var wDp = if (portrait) opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) else opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
        var hDp = if (portrait) opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT) else opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        if (wDp <= 0 || hDp <= 0) {
            val size = design?.size ?: WidgetSize.SMALL
            wDp = size.cellsW * 80
            hDp = size.cellsH * 80
        }
        var w = wDp * dm.density
        var h = hDp * dm.density
        // Los RemoteViews tienen un límite de memoria para bitmaps: no pasarse de ~1000px.
        val cap = 1000f
        val scale = cap / max(w, h)
        if (scale < 1f) { w *= scale; h *= scale }
        return w.roundToInt().coerceAtLeast(64) to h.roundToInt().coerceAtLeast(64)
    }

    fun update(context: Context, mgr: AppWidgetManager, id: Int) {
        val design = Repository.designForWidget(id)
        val views = RemoteViews(context.packageName, R.layout.widget_root)
        if (design == null) {
            views.setViewVisibility(R.id.widget_image, View.GONE)
            views.setViewVisibility(R.id.widget_empty, View.VISIBLE)
            val configure = Intent(context, WidgetConfigureActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            views.setOnClickPendingIntent(
                R.id.widget_root,
                PendingIntent.getActivity(context, id, configure, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )
        } else {
            val (w, h) = pixelSize(context, mgr, id, design)
            val bmp = try {
                WidgetRenderer.render(design, w, h, RenderEnv.current(context))
            } catch (e: Throwable) {
                null
            }
            views.setViewVisibility(R.id.widget_empty, View.GONE)
            views.setViewVisibility(R.id.widget_image, View.VISIBLE)
            if (bmp != null) views.setImageViewBitmap(R.id.widget_image, bmp)
            views.setContentDescription(R.id.widget_image, design.name)
            val open = Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_EDIT_DESIGN, design.id)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            views.setOnClickPendingIntent(
                R.id.widget_root,
                PendingIntent.getActivity(context, id, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )
        }
        mgr.updateAppWidget(id, views)
    }

    // ---- Reloj: refresco cada minuto mientras haya widgets de hora ----

    private fun hasClockWidgets(context: Context): Boolean =
        allIds(context).any { Repository.designForWidget(it)?.kind?.needsMinuteTick == true }

    fun scheduleTick(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = tickIntent(context)
        if (!hasClockWidgets(context)) {
            am.cancel(pi)
            return
        }
        val next = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 1)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        // RTC (sin despertar): no gasta batería con la pantalla apagada y se
        // entrega apenas se prende el teléfono. setWindow no requiere permiso de alarma exacta.
        am.setWindow(AlarmManager.RTC, next, 1500L, pi)
    }

    private fun tickIntent(context: Context): PendingIntent {
        val intent = Intent(context, TickReceiver::class.java).setAction(ACTION_TICK)
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    // ---- Fijar un widget en la pantalla de inicio desde la app ----

    fun canPin(context: Context): Boolean =
        AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported

    fun requestPin(context: Context, design: WidgetDesign): Boolean {
        val mgr = AppWidgetManager.getInstance(context)
        if (!mgr.isRequestPinAppWidgetSupported) return false
        val callback = Intent(context, WidgetPinReceiver::class.java).putExtra(EXTRA_DESIGN, design.id)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
        val pi = PendingIntent.getBroadcast(context, design.id.hashCode(), callback, flags)
        return mgr.requestPinAppWidget(ComponentName(context, providerFor(design.size)), null, pi)
    }

    internal fun designIdFrom(intent: Intent): String? = intent.getStringExtra(EXTRA_DESIGN)
}

// --------------------------------------------------------------- receptores

class WidgetPinReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Repository.init(context)
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val designId = WidgetUpdater.designIdFrom(intent) ?: return
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return
        Repository.bindWidget(id, designId)
        WidgetUpdater.update(context, AppWidgetManager.getInstance(context), id)
        WidgetUpdater.scheduleTick(context)
        if (Repository.design(designId)?.kind == WidgetKind.WEATHER) RefreshWorker.runNow(context)
    }
}

class TickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Repository.init(context)
        WidgetUpdater.updateAll(context, setOf(WidgetKind.DIGITAL_CLOCK, WidgetKind.ANALOG_CLOCK))
        WidgetUpdater.scheduleTick(context)
    }
}

class SystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Repository.init(context)
        WidgetUpdater.updateAll(context)
        WidgetUpdater.scheduleTick(context)
        RefreshWorker.schedule(context)
    }
}

// ------------------------------------------------------------ tarea periódica

/** Cada 15 minutos: actualiza clima, batería, calendario y cuentas regresivas. */
class RefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Repository.init(applicationContext)
        val ids = WidgetUpdater.allIds(applicationContext)
        val weatherDesigns = ids.mapNotNull { Repository.designForWidget(it) }
            .filter { it.kind == WidgetKind.WEATHER && it.city.isNotBlank() }
            .distinctBy { "%.2f,%.2f".format(java.util.Locale.US, it.lat, it.lon) }
        weatherDesigns.forEach { d ->
            val cached = Repository.weatherFor(d.lat, d.lon)
            val fresh = cached != null && System.currentTimeMillis() - cached.fetchedAt < TimeUnit.MINUTES.toMillis(25)
            if (!fresh) WeatherService.fetch(d.lat, d.lon)?.let { Repository.putWeather(d.lat, d.lon, it) }
        }
        WidgetUpdater.updateAll(applicationContext)
        WidgetUpdater.scheduleTick(applicationContext)
        return Result.success()
    }

    companion object {
        private const val NAME = "simon_refresh"

        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<RefreshWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, req)
        }

        fun runNow(context: Context) {
            val req = OneTimeWorkRequestBuilder<RefreshWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueue(req)
        }
    }
}
