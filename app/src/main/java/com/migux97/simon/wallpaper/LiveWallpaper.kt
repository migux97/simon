package com.migux97.simon.wallpaper

import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.migux97.simon.data.LiveConfig
import com.migux97.simon.data.Repository

object LiveWallpaper {

    fun isActive(context: Context): Boolean =
        WallpaperManager.getInstance(context).wallpaperInfo?.packageName == context.packageName

    /**
     * Guarda la configuración y, si el fondo animado de Simón no está activo,
     * abre la pantalla del sistema para activarlo. Devuelve true si ya quedó aplicado.
     */
    fun apply(context: Context, config: LiveConfig): Boolean {
        Repository.setLive(config)
        if (isActive(context)) return true
        val component = ComponentName(context, SimonWallpaperService::class.java)
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            try {
                context.startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: Exception) {
            }
        }
        return false
    }
}
