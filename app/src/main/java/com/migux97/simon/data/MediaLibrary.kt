package com.migux97.simon.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.LruCache
import java.io.File
import java.io.FileOutputStream

/** Archivos de dibujos, animaciones y miniaturas de video. */
object MediaLibrary {
    private lateinit var root: File

    private val frameCache = object : LruCache<String, List<Bitmap>>(48 * 1024 * 1024) {
        override fun sizeOf(key: String, value: List<Bitmap>) = value.sumOf { it.allocationByteCount }.coerceAtLeast(1)
    }
    private val videoThumbs = LruCache<String, Bitmap>(12)

    fun init(context: Context) {
        if (::root.isInitialized) return
        root = context.applicationContext.filesDir
    }

    // -------------------------------------------------------------- dibujos

    fun drawingFile(id: String) = File(File(root, "drawings").apply { mkdirs() }, "$id.png")

    fun saveDrawing(info: DrawingInfo, bitmap: Bitmap) {
        FileOutputStream(drawingFile(info.id)).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        ImageStore.evict(drawingFile(info.id).absolutePath)
        StudioStore.saveDrawing(info.copy(updatedAt = System.currentTimeMillis()))
    }

    fun drawingPath(id: String?): String? = id?.let { drawingFile(it).takeIf { f -> f.exists() }?.absolutePath }

    fun loadDrawing(id: String?, maxSide: Int = 1200): Bitmap? = ImageStore.load(drawingPath(id), maxSide)

    fun deleteDrawing(id: String) {
        drawingFile(id).delete()
        StudioStore.deleteDrawing(id)
    }

    // ----------------------------------------------------------- animaciones

    private fun animDir(id: String) = File(File(root, "anims"), id)

    fun frameFile(id: String, index: Int) = File(animDir(id).apply { mkdirs() }, "f_%03d.png".format(index))

    fun saveAnimation(info: AnimationInfo, frames: List<Bitmap>) {
        val dir = animDir(info.id)
        dir.listFiles()?.forEach { it.delete() }
        dir.mkdirs()
        frames.forEachIndexed { i, bmp ->
            FileOutputStream(frameFile(info.id, i)).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        frameCache.snapshot().keys.filter { it.startsWith(info.id) }.forEach { frameCache.remove(it) }
        StudioStore.saveAnimation(info.copy(frames = frames.size, updatedAt = System.currentTimeMillis()))
    }

    /** Carga los cuadros (reducidos) de una animación, con caché. */
    fun loadFrames(id: String?, maxSide: Int = 720): List<Bitmap> {
        if (id == null) return emptyList()
        val info = StudioStore.animation(id) ?: return emptyList()
        val key = "$id@$maxSide@${info.updatedAt}"
        frameCache.get(key)?.let { return it }
        val list = (0 until info.frames).mapNotNull { i ->
            val f = frameFile(id, i)
            if (!f.exists()) return@mapNotNull null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(f.absolutePath, bounds)
            var sample = 1
            var side = maxOf(bounds.outWidth, bounds.outHeight)
            while (side / 2 >= maxSide) { side /= 2; sample *= 2 }
            BitmapFactory.decodeFile(f.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
        }
        if (list.isNotEmpty()) frameCache.put(key, list)
        return list
    }

    /** Qué cuadro mostrar en un momento dado, según fps, bucle e ida y vuelta. */
    fun frameIndex(info: AnimationInfo?, count: Int, timeMs: Long): Int {
        if (count <= 1) return 0
        val fps = (info?.fps ?: 8).coerceIn(1, 60)
        val raw = (timeMs * fps / 1000L).toInt()
        val bounce = info?.bounce == true
        val loop = info?.loop != false
        return if (bounce) {
            val period = (count - 1) * 2
            val p = if (loop) raw % period else raw.coerceAtMost(count - 1)
            if (p < count) p else period - p
        } else {
            if (loop) raw % count else raw.coerceAtMost(count - 1)
        }
    }

    fun deleteAnimation(id: String) {
        animDir(id).deleteRecursively()
        StudioStore.deleteAnimation(id)
    }

    // ----------------------------------------------------------------- video

    /** Primer cuadro de un video, para vistas estáticas (widgets del sistema). */
    fun videoThumb(path: String?): Bitmap? {
        if (path.isNullOrBlank() || !File(path).exists()) return null
        videoThumbs.get(path)?.let { return it }
        return try {
            val r = MediaMetadataRetriever()
            r.setDataSource(path)
            val bmp = r.getFrameAtTime(0)
            r.release()
            bmp?.also { videoThumbs.put(path, it) }
        } catch (e: Exception) {
            null
        }
    }
}
