package com.migux97.simon.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.LruCache
import java.io.File
import java.io.FileOutputStream

/**
 * Copia las fotos y videos que elige el usuario dentro de la app, así los
 * widgets y el fondo animado los pueden leer aunque se pierda el permiso
 * sobre el archivo original.
 */
object ImageStore {
    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }

    private fun dir(context: Context, name: String) =
        File(context.filesDir, name).apply { mkdirs() }

    /** Guarda una foto reducida (lado mayor máx. [maxSide]) y devuelve su ruta. */
    fun importImage(context: Context, uri: Uri, maxSide: Int = 1600): String? {
        return try {
            val bmp = decodeUri(context, uri, maxSide) ?: return null
            val file = File(dir(context, "images"), "${newId()}.jpg")
            FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /** Guarda una imagen ya armada (por ejemplo tu fondo actual) y devuelve su ruta. */
    fun saveBitmap(context: Context, bmp: Bitmap, png: Boolean = false): String? {
        return try {
            val file = File(dir(context, "images"), "${newId()}.${if (png) "png" else "jpg"}")
            FileOutputStream(file).use { bmp.compress(if (png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, 92, it) }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /** Copia un video a la carpeta privada de la app. */
    fun importVideo(context: Context, uri: Uri): String? {
        return try {
            val folder = dir(context, "videos")
            val file = File(folder, "${newId()}.mp4")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { input.copyTo(it) }
            } ?: return null
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun decodeUri(context: Context, uri: Uri, maxSide: Int): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    val w = info.size.width
                    val h = info.size.height
                    val scale = maxSide.toFloat() / maxOf(w, h)
                    if (scale < 1f) decoder.setTargetSize((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1))
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = false
                }
            } else {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                val opts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxSide)
                }
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Carga una foto guardada, reducida al tamaño pedido, con caché. */
    fun load(path: String?, maxSide: Int = 1024): Bitmap? {
        if (path.isNullOrBlank()) return null
        val key = "$path@$maxSide"
        cache.get(key)?.let { return it }
        val file = File(path)
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxSide)
        }
        val bmp = BitmapFactory.decodeFile(path, opts) ?: return null
        cache.put(key, bmp)
        return bmp
    }

    /** Olvida las versiones en caché de un archivo que cambió. */
    fun evict(path: String) {
        cache.snapshot().keys.filter { it.startsWith("$path@") }.forEach { cache.remove(it) }
    }

    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        File(path).delete()
    }

    private fun sampleSize(w: Int, h: Int, maxSide: Int): Int {
        var sample = 1
        var side = maxOf(w, h)
        while (side / 2 >= maxSide) {
            side /= 2
            sample *= 2
        }
        return sample
    }
}
