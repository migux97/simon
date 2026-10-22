package com.migux97.simon.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.migux97.simon.data.StaticPattern
import com.migux97.simon.data.StaticWallpaper
import com.migux97.simon.widget.Draw
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

enum class WallpaperTarget(val label: String) { HOME("Inicio"), LOCK("Bloqueo"), BOTH("Ambas") }

object StaticWallpapers {

    fun render(sw: StaticWallpaper, width: Int, height: Int): Bitmap {
        val bmp = Bitmap.createBitmap(width.coerceAtLeast(8), height.coerceAtLeast(8), Bitmap.Config.ARGB_8888)
        draw(Canvas(bmp), sw, bmp.width.toFloat(), bmp.height.toFloat())
        return bmp
    }

    fun draw(c: Canvas, sw: StaticWallpaper, w: Float, h: Float) {
        val rect = RectF(0f, 0f, w, h)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val rnd = Random(sw.id.hashCode())
        val s = min(w, h)
        when (sw.pattern) {
            StaticPattern.LINEAR -> {
                p.shader = Draw.angledGradient(rect, 120, intArrayOf(sw.color1, Draw.mix(sw.color1, sw.color2, 0.5f), sw.color2))
                c.drawRect(rect, p)
            }
            StaticPattern.RADIAL -> {
                p.shader = LinearGradient(0f, 0f, 0f, h, sw.color1, sw.color2, Shader.TileMode.CLAMP)
                c.drawRect(rect, p)
                p.shader = RadialGradient(w * 0.5f, h * 0.38f, max(w, h) * 0.55f, Draw.withAlpha(sw.color3, 190), Draw.withAlpha(sw.color3, 0), Shader.TileMode.CLAMP)
                c.drawRect(rect, p)
            }
            StaticPattern.MESH -> {
                p.color = sw.color1
                c.drawRect(rect, p)
                val spots = listOf(
                    Triple(0.15f, 0.2f, sw.color2), Triple(0.9f, 0.35f, sw.color3),
                    Triple(0.3f, 0.85f, sw.color3), Triple(0.8f, 0.9f, sw.color2), Triple(0.55f, 0.55f, Draw.mix(sw.color2, sw.color3, 0.5f))
                )
                spots.forEach { (x, y, col) ->
                    p.shader = RadialGradient(w * x, h * y, max(w, h) * 0.55f, Draw.withAlpha(col, 220), Draw.withAlpha(col, 0), Shader.TileMode.CLAMP)
                    c.drawRect(rect, p)
                }
            }
            StaticPattern.WAVES -> {
                p.shader = LinearGradient(0f, 0f, 0f, h, Draw.mix(sw.color1, Color.WHITE, 0.2f), sw.color1, Shader.TileMode.CLAMP)
                c.drawRect(rect, p)
                p.shader = null
                for (i in 0 until 6) {
                    val k = i / 5f
                    val path = Path()
                    val baseY = h * (0.35f + k * 0.55f)
                    path.moveTo(0f, h)
                    var x = 0f
                    while (x <= w + 10) {
                        path.lineTo(x, baseY + sin(x / w * 2 * PI * (1.2 + i * 0.35) + i).toFloat() * s * 0.04f)
                        x += 8f
                    }
                    path.lineTo(w, h)
                    path.close()
                    p.color = Draw.mix(sw.color2, sw.color3, k)
                    p.alpha = 150 + (105 * k).toInt()
                    c.drawPath(path, p)
                }
            }
            StaticPattern.DOTS -> {
                p.shader = LinearGradient(0f, 0f, w, h, sw.color1, Draw.mix(sw.color1, sw.color2, 0.3f), Shader.TileMode.CLAMP)
                c.drawRect(rect, p)
                p.shader = null
                val step = s * 0.09f
                var y = step / 2
                var row = 0
                while (y < h + step) {
                    var x = if (row % 2 == 0) step / 2 else step
                    while (x < w + step) {
                        p.color = Draw.withAlpha(if ((row + (x / step).toInt()) % 5 == 0) sw.color3 else sw.color2, 150)
                        c.drawCircle(x, y, step * 0.16f, p)
                        x += step
                    }
                    y += step * 0.87f
                    row++
                }
            }
            StaticPattern.STRIPES -> {
                p.color = sw.color1
                c.drawRect(rect, p)
                c.save()
                c.rotate(-30f, w / 2, h / 2)
                val band = s * 0.12f
                var x = -h * 2
                var i = 0
                while (x < w + h * 2) {
                    p.color = if (i % 2 == 0) sw.color2 else sw.color1
                    c.drawRect(x, -h, x + band, h * 2, p)
                    p.color = Draw.withAlpha(sw.color3, 200)
                    c.drawRect(x + band - band * 0.08f, -h, x + band, h * 2, p)
                    x += band
                    i++
                }
                c.restore()
                p.shader = LinearGradient(0f, 0f, 0f, h, Color.TRANSPARENT, Draw.withAlpha(Color.BLACK, 90), Shader.TileMode.CLAMP)
                c.drawRect(rect, p)
            }
            StaticPattern.BLOBS -> {
                p.shader = LinearGradient(0f, 0f, 0f, h, sw.color1, Draw.mix(sw.color1, sw.color2, 0.4f), Shader.TileMode.CLAMP)
                c.drawRect(rect, p)
                p.shader = null
                repeat(9) { k ->
                    val col = if (k % 2 == 0) sw.color2 else sw.color3
                    val cx = rnd.nextFloat() * w
                    val cy = rnd.nextFloat() * h
                    val r = s * (0.15f + rnd.nextFloat() * 0.3f)
                    val path = Path()
                    val pts = 8
                    for (j in 0..pts) {
                        val a = j * 2 * PI / pts
                        val rr = r * (0.8f + rnd.nextFloat() * 0.35f)
                        val px = cx + (kotlin.math.cos(a) * rr).toFloat()
                        val py = cy + (sin(a) * rr).toFloat()
                        if (j == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    path.close()
                    p.color = Draw.withAlpha(col, 110)
                    p.pathEffect = android.graphics.CornerPathEffect(r * 0.6f)
                    c.drawPath(path, p)
                }
                p.pathEffect = null
            }
            StaticPattern.SUNSET -> {
                p.shader = LinearGradient(0f, 0f, 0f, h * 0.75f, intArrayOf(sw.color1, sw.color2, Draw.mix(sw.color2, sw.color3, 0.4f)), null, Shader.TileMode.CLAMP)
                c.drawRect(rect, p)
                p.shader = RadialGradient(w / 2, h * 0.55f, s * 0.6f, Draw.withAlpha(Color.WHITE, 120), Draw.withAlpha(Color.WHITE, 0), Shader.TileMode.CLAMP)
                c.drawRect(rect, p)
                p.shader = null
                p.color = Draw.mix(sw.color2, Color.WHITE, 0.55f)
                c.drawCircle(w / 2, h * 0.55f, s * 0.2f, p)
                for (layer in 0 until 3) {
                    val path = Path()
                    val baseY = h * (0.62f + layer * 0.1f)
                    path.moveTo(0f, h)
                    var x = 0f
                    while (x <= w + 10) {
                        val y = baseY - (sin(x / w * PI * (2 + layer) + layer * 2).toFloat() * 0.5f + 0.5f) * s * (0.12f - layer * 0.025f)
                        path.lineTo(x, y)
                        x += 8f
                    }
                    path.lineTo(w, h)
                    path.close()
                    p.color = Draw.mix(sw.color3, Color.BLACK, 0.35f + layer * 0.22f)
                    c.drawPath(path, p)
                }
            }
        }
    }

    /** Pone un bitmap como fondo de pantalla. */
    fun apply(context: Context, bitmap: Bitmap, target: WallpaperTarget): Boolean = try {
        val wm = WallpaperManager.getInstance(context)
        val flags = when (target) {
            WallpaperTarget.HOME -> WallpaperManager.FLAG_SYSTEM
            WallpaperTarget.LOCK -> WallpaperManager.FLAG_LOCK
            WallpaperTarget.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
        }
        wm.setBitmap(bitmap, null, true, flags)
        true
    } catch (e: Exception) {
        false
    }

    /** Tamaño ideal para generar el fondo (pantalla completa). */
    fun screenSize(context: Context): Pair<Int, Int> {
        val dm = context.resources.displayMetrics
        val w = min(dm.widthPixels, dm.heightPixels)
        val h = max(dm.widthPixels, dm.heightPixels)
        return w to h
    }
}
