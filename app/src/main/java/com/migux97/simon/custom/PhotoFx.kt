package com.migux97.simon.custom

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.LruCache
import com.migux97.simon.data.PhotoAdjust
import com.migux97.simon.data.PhotoFilter
import kotlin.math.roundToInt

/**
 * Recorte y ajustes de color de fotos y dibujos. El resultado se guarda en
 * memoria: el mismo recorte con los mismos ajustes no se vuelve a calcular.
 */
object PhotoFx {
    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    fun matrix(a: PhotoAdjust): ColorMatrix? {
        if (!a.hasColor) return null
        val m = ColorMatrix()
        when (a.filter) {
            PhotoFilter.NONE -> Unit
            PhotoFilter.BW -> m.setSaturation(0f)
            PhotoFilter.SEPIA -> {
                m.setSaturation(0f)
                m.postConcat(ColorMatrix(floatArrayOf(1.07f, 0f, 0f, 0f, 18f, 0f, 0.96f, 0f, 0f, 6f, 0f, 0f, 0.78f, 0f, -12f, 0f, 0f, 0f, 1f, 0f)))
            }
            PhotoFilter.WARM -> m.set(floatArrayOf(1.1f, 0f, 0f, 0f, 10f, 0f, 1.02f, 0f, 0f, 4f, 0f, 0f, 0.88f, 0f, -6f, 0f, 0f, 0f, 1f, 0f))
            PhotoFilter.COOL -> m.set(floatArrayOf(0.9f, 0f, 0f, 0f, -6f, 0f, 1f, 0f, 0f, 2f, 0f, 0f, 1.12f, 0f, 12f, 0f, 0f, 0f, 1f, 0f))
            PhotoFilter.VINTAGE -> {
                m.setSaturation(0.55f)
                m.postConcat(ColorMatrix(floatArrayOf(0.9f, 0f, 0f, 0f, 30f, 0f, 0.85f, 0f, 0f, 22f, 0f, 0f, 0.7f, 0f, 10f, 0f, 0f, 0f, 1f, 0f)))
            }
            PhotoFilter.VIVID -> m.setSaturation(1.5f)
            PhotoFilter.FADE -> {
                m.setSaturation(0.75f)
                m.postConcat(ColorMatrix(floatArrayOf(0.85f, 0f, 0f, 0f, 30f, 0f, 0.85f, 0f, 0f, 30f, 0f, 0f, 0.85f, 0f, 30f, 0f, 0f, 0f, 1f, 0f)))
            }
        }
        if (a.saturation != 1f) m.postConcat(ColorMatrix().apply { setSaturation(a.saturation.coerceIn(0f, 2f)) })
        if (a.contrast != 1f) {
            val c = a.contrast.coerceIn(0f, 2f)
            val t = 128f * (1f - c)
            m.postConcat(ColorMatrix(floatArrayOf(c, 0f, 0f, 0f, t, 0f, c, 0f, 0f, t, 0f, 0f, c, 0f, t, 0f, 0f, 0f, 1f, 0f)))
        }
        if (a.brightness != 0f) {
            val b = 255f * a.brightness.coerceIn(-1f, 1f) * 0.6f
            m.postConcat(ColorMatrix(floatArrayOf(1f, 0f, 0f, 0f, b, 0f, 1f, 0f, 0f, b, 0f, 0f, 1f, 0f, b, 0f, 0f, 0f, 1f, 0f)))
        }
        return m
    }

    /** Devuelve la imagen recortada y con los ajustes. [key] identifica la imagen original. */
    fun process(src: Bitmap, a: PhotoAdjust, key: String): Bitmap {
        if (a.isDefault) return src
        val k = "$key|${src.width}x${src.height}|$a"
        cache.get(k)?.let { return it }
        val l = (a.cropL.coerceIn(0f, 1f) * src.width).roundToInt()
        val t = (a.cropT.coerceIn(0f, 1f) * src.height).roundToInt()
        val r = (a.cropR.coerceIn(0f, 1f) * src.width).roundToInt().coerceAtLeast(l + 1).coerceAtMost(src.width)
        val b = (a.cropB.coerceIn(0f, 1f) * src.height).roundToInt().coerceAtLeast(t + 1).coerceAtMost(src.height)
        val out = Bitmap.createBitmap((r - l).coerceAtLeast(1), (b - t).coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        matrix(a)?.let { paint.colorFilter = ColorMatrixColorFilter(it) }
        Canvas(out).drawBitmap(src, -l.toFloat(), -t.toFloat(), paint)
        cache.put(k, out)
        return out
    }
}
