package com.migux97.simon.draw

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.migux97.simon.widget.Draw
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

enum class Tool(val label: String) {
    PEN("Lápiz"),
    MARKER("Marcador"),
    NEON("Neón"),
    SPRAY("Aerosol"),
    ERASER("Goma"),
    LINE("Línea"),
    RECT("Rectángulo"),
    CIRCLE("Círculo"),
    HEART("Corazón"),
    STAR("Estrella"),
    FILL("Rellenar todo")
}

/** Un trazo, con coordenadas relativas (0..1) para que no dependa del tamaño en pantalla. */
data class Stroke(
    val tool: Tool,
    val color: Int,
    val width: Float,
    val alpha: Float,
    val points: List<Offset>,
    val filled: Boolean = false,
    val seed: Int = Random.nextInt()
)

object DrawCore {

    private fun paintFor(s: Stroke, w: Float): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = s.color
        alpha = (255 * s.alpha).toInt().coerceIn(0, 255)
        strokeWidth = s.width * w
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        style = if (s.filled) Paint.Style.FILL else Paint.Style.STROKE
        when (s.tool) {
            Tool.ERASER -> xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            Tool.MARKER -> { strokeCap = Paint.Cap.SQUARE; alpha = (alpha * 0.55f).toInt() }
            Tool.NEON -> setShadowLayer(s.width * w * 1.4f, 0f, 0f, s.color)
            else -> Unit
        }
    }

    fun drawStroke(c: Canvas, s: Stroke, w: Float, h: Float) {
        if (s.points.isEmpty()) return
        val p = paintFor(s, w)
        val pts = s.points.map { Offset(it.x * w, it.y * h) }
        val a = pts.first(); val b = pts.last()
        val rect = RectF(min(a.x, b.x), min(a.y, b.y), max(a.x, b.x), max(a.y, b.y))
        when (s.tool) {
            Tool.FILL -> { p.style = Paint.Style.FILL; c.drawRect(0f, 0f, w, h, p) }
            Tool.LINE -> c.drawLine(a.x, a.y, b.x, b.y, p)
            Tool.RECT -> c.drawRoundRect(rect, s.width * w, s.width * w, p)
            Tool.CIRCLE -> c.drawOval(rect, p)
            Tool.HEART -> c.drawPath(Draw.heartPath(rect.centerX(), rect.centerY(), max(rect.width(), rect.height())), p)
            Tool.STAR -> c.drawPath(Draw.starPath(rect.centerX(), rect.centerY(), max(rect.width(), rect.height()) / 2), p)
            Tool.SPRAY -> {
                val rnd = Random(s.seed)
                p.style = Paint.Style.FILL
                val radius = s.width * w * 2.5f
                pts.forEach { pt ->
                    repeat(14) {
                        val dx = (rnd.nextFloat() - 0.5f) * 2 * radius
                        val dy = (rnd.nextFloat() - 0.5f) * 2 * radius
                        if (dx * dx + dy * dy <= radius * radius) c.drawCircle(pt.x + dx, pt.y + dy, max(1f, s.width * w * 0.08f), p)
                    }
                }
            }
            else -> {
                if (pts.size == 1) {
                    p.style = Paint.Style.FILL
                    c.drawCircle(a.x, a.y, p.strokeWidth / 2, p)
                } else {
                    val path = Path()
                    path.moveTo(pts[0].x, pts[0].y)
                    for (i in 1 until pts.size) {
                        val prev = pts[i - 1]; val cur = pts[i]
                        path.quadTo(prev.x, prev.y, (prev.x + cur.x) / 2, (prev.y + cur.y) / 2)
                    }
                    path.lineTo(b.x, b.y)
                    c.drawPath(path, p)
                }
            }
        }
    }

    /** Dibuja fondo + imagen base + trazos. La goma borra solo la capa de dibujo. */
    fun render(c: Canvas, w: Float, h: Float, bgColor: Int?, bgImage: Bitmap?, base: Bitmap?, strokes: List<Stroke>) {
        bgColor?.let { c.drawColor(it) }
        bgImage?.let { Draw.drawCenterCrop(c, it, RectF(0f, 0f, w, h)) }
        val layer = c.saveLayer(0f, 0f, w, h, null)
        base?.let { c.drawBitmap(it, null, RectF(0f, 0f, w, h), Paint(Paint.FILTER_BITMAP_FLAG)) }
        strokes.forEach { drawStroke(c, it, w, h) }
        c.restoreToCount(layer)
    }

    fun toBitmap(width: Int, height: Int, bgColor: Int?, bgImage: Bitmap?, base: Bitmap?, strokes: List<Stroke>): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        render(Canvas(bmp), width.toFloat(), height.toFloat(), bgColor, bgImage, base, strokes)
        return bmp
    }
}

/**
 * Lienzo táctil. [current] es el trazo en curso; al soltar se llama [onStroke].
 * [onion] se muestra semitransparente detrás (cuadro anterior de una animación).
 */
@Composable
fun DrawingSurface(
    strokes: List<Stroke>,
    base: Bitmap?,
    bgColor: Int?,
    bgImage: Bitmap?,
    onion: Bitmap?,
    tool: Tool,
    color: Int,
    width: Float,
    alpha: Float,
    filled: Boolean,
    current: Stroke?,
    onCurrent: (Stroke?) -> Unit,
    onStroke: (Stroke) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier
            .pointerInput(tool, color, width, alpha, filled) {
                detectTapGestures { pos ->
                    val pt = Offset(pos.x / size.width, pos.y / size.height)
                    onStroke(Stroke(tool, color, width, alpha, listOf(pt, pt), filled))
                }
            }
            .pointerInput(tool, color, width, alpha, filled) {
                var pts = mutableListOf<Offset>()
                detectDragGestures(
                    onDragStart = { pos ->
                        pts = mutableListOf(Offset(pos.x / size.width, pos.y / size.height))
                        onCurrent(Stroke(tool, color, width, alpha, pts.toList(), filled))
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val pt = Offset(change.position.x / size.width, change.position.y / size.height)
                        val last = pts.lastOrNull()
                        if (last == null || abs(last.x - pt.x) + abs(last.y - pt.y) > 0.002f) {
                            pts.add(pt)
                            onCurrent(Stroke(tool, color, width, alpha, pts.toList(), filled))
                        }
                    },
                    onDragEnd = {
                        if (pts.isNotEmpty()) onStroke(Stroke(tool, color, width, alpha, pts.toList(), filled))
                        onCurrent(null)
                    },
                    onDragCancel = { onCurrent(null) }
                )
            }
    ) {
        drawIntoCanvas { c ->
            val nc = c.nativeCanvas
            val w = size.width; val h = size.height
            // Cuadriculado para ver la transparencia
            if (bgColor == null && bgImage == null) {
                val cell = w / 20f
                val p = Paint().apply { this.color = 0x22888888 }
                var y = 0
                while (y * cell < h) {
                    var x = y % 2
                    while (x * cell < w) { nc.drawRect(x * cell, y * cell, (x + 1) * cell, (y + 1) * cell, p); x += 2 }
                    y++
                }
            }
            onion?.let { nc.drawBitmap(it, null, RectF(0f, 0f, w, h), Paint().apply { this.alpha = 70 }) }
            DrawCore.render(nc, w, h, bgColor, bgImage, base, if (current != null) strokes + current else strokes)
        }
    }
}
