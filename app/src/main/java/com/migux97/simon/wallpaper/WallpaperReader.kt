package com.migux97.simon.wallpaper

import android.Manifest
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.max

/**
 * Lee la imagen de tu fondo de inicio y de bloqueo. Android solo lo permite
 * con permisos: hasta Android 12 el de lectura; desde Android 13, el de fotos
 * y además «Acceso a todos los archivos».
 */
object WallpaperReader {

    /** Lo que falta para poder leer el fondo, en el orden en que se pide. */
    enum class Missing { PHOTOS, ALL_FILES }

    private val photosPermission: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE

    val PERMISSION: String get() = photosPermission

    fun missing(context: Context): Missing? = when {
        ContextCompat.checkSelfPermission(context, photosPermission) != PackageManager.PERMISSION_GRANTED -> Missing.PHOTOS
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Environment.isExternalStorageManager() -> Missing.ALL_FILES
        else -> null
    }

    fun hasAccess(context: Context): Boolean = missing(context) == null

    /** Pantalla de Ajustes para dar «Acceso a todos los archivos» a Simón. Se abre encima de Simón: «Atrás» vuelve a la app. */
    fun allFilesIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${context.packageName}"))

    /** Los ajustes de Simón, por si el permiso de fotos se negó para siempre. */
    fun appSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))

    /** ¿Hay un fondo animado (de cualquier app) en la pantalla de inicio? */
    fun isLive(context: Context): Boolean = try {
        WallpaperManager.getInstance(context).wallpaperInfo != null
    } catch (_: Exception) {
        false
    }

    /**
     * La imagen del fondo de inicio ([lock] = false) o de bloqueo. Null si no
     * hay permiso o si es un fondo animado. Si el bloqueo usa el mismo fondo que
     * el inicio, devuelve el del inicio.
     */
    fun load(context: Context, lock: Boolean, maxSide: Int = 2600): Bitmap? {
        if (!hasAccess(context)) return null
        val wm = WallpaperManager.getInstance(context)
        val bmp = try {
            if (lock) fromFile(wm, WallpaperManager.FLAG_LOCK) ?: fromFile(wm, WallpaperManager.FLAG_SYSTEM) ?: wm.drawable?.toBitmap()
            else fromFile(wm, WallpaperManager.FLAG_SYSTEM) ?: if (isLive(context)) null else wm.drawable?.toBitmap()
        } catch (_: Exception) {
            null
        } ?: return null
        val side = max(bmp.width, bmp.height)
        if (side <= maxSide) return bmp
        val s = maxSide.toFloat() / side
        return Bitmap.createScaledBitmap(bmp, (bmp.width * s).toInt().coerceAtLeast(1), (bmp.height * s).toInt().coerceAtLeast(1), true)
    }

    private fun fromFile(wm: WallpaperManager, which: Int): Bitmap? = try {
        wm.getWallpaperFile(which)?.use { pfd -> BitmapFactory.decodeFileDescriptor(pfd.fileDescriptor) }
    } catch (_: Exception) {
        null
    }

    /** Id que Android le da al fondo puesto (cambia cada vez que se pone otro). */
    fun currentId(context: Context, lock: Boolean): Int = try {
        WallpaperManager.getInstance(context).getWallpaperId(if (lock) WallpaperManager.FLAG_LOCK else WallpaperManager.FLAG_SYSTEM)
    } catch (_: Exception) {
        0
    }
}
