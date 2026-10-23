package com.migux97.simon.launcher

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context

/** Hospeda widgets de otras apps (reloj del sistema, Spotify, Google…) dentro del launcher. */
object WidgetHost {
    private const val HOST_ID = 0x5150
    private var host: AppWidgetHost? = null

    fun get(context: Context): AppWidgetHost =
        host ?: AppWidgetHost(context.applicationContext, HOST_ID).also { host = it }

    fun providers(context: Context): List<AppWidgetProviderInfo> =
        try {
            AppWidgetManager.getInstance(context).installedProviders
                .filter { it.provider.packageName != context.packageName }
                .sortedBy { it.loadLabel(context.packageManager).lowercase() }
        } catch (e: Exception) {
            emptyList()
        }

    fun allocate(context: Context): Int = get(context).allocateAppWidgetId()

    fun delete(context: Context, id: Int) {
        if (id >= 0) try {
            get(context).deleteAppWidgetId(id)
        } catch (_: Exception) {
        }
    }

    fun createView(context: Context, id: Int): AppWidgetHostView? {
        val info = AppWidgetManager.getInstance(context).getAppWidgetInfo(id) ?: return null
        return try {
            get(context).createView(context, id, info).apply { setAppWidget(id, info) }
        } catch (e: Exception) {
            null
        }
    }
}
