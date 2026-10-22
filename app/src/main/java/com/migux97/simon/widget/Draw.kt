package com.migux97.simon.widget

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.migux97.simon.data.FontStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Utilidades de dibujo compartidas por widgets, íconos y fondos. */
object Draw {

    private val typefaces = HashMap<FontStyle, Typeface>()

    fun typeface(font: FontStyle): Typeface = typefaces.getOrPut(font) {
        Typeface.create(font.family, if (font.bold) Typeface.BOLD else Typeface.NORMAL)
    }

    fun textPaint(font: FontStyle, color: Int, size: Float, bold: Boolean = false): TextPaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = if (bold && !font.bold) Typeface.create(typeface(font), Typeface.BOLD) else typeface(font)
            this.color = color
            textSize = size
        }

    fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))

    fun mix(a: Int, b: Int, t: Float): Int {
        val k = t.coerceIn(0f, 1f)
        return Color.argb(
            (Color.alpha(a) + (Color.alpha(b) - Color.alpha(a)) * k).toInt(),
            (Color.red(a) + (Color.red(b) - Color.red(a)) * k).toInt(),
            (Color.green(a) + (Color.green(b) - Color.green(a)) * k).toInt(),
            (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * k).toInt()
        )
    }

    fun luminance(color: Int): Float =
        (0.299f * Color.red(color) + 0.587f * Color.green(color) + 0.114f * Color.blue(color)) / 255f

    /** Degradado lineal en un ángulo (grados) que cubre el rectángulo. */
    fun angledGradient(rect: RectF, angle: Int, colors: IntArray): LinearGradient {
        val rad = Math.toRadians(angle.toDouble())
        val cx = rect.centerX()
        val cy = rect.centerY()
        val half = (max(rect.width(), rect.height()) / 2f)
        val dx = (cos(rad) * half).toFloat()
        val dy = (sin(rad) * half).toFloat()
        return LinearGradient(cx - dx, cy - dy, cx + dx, cy + dy, colors, null, Shader.TileMode.CLAMP)
    }

    /** Dibuja un bitmap recortado al centro para llenar [dst]. */
    fun drawCenterCrop(canvas: Canvas, bmp: Bitmap, dst: RectF, paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)) {
        val scale = max(dst.width() / bmp.width, dst.height() / bmp.height)
        val m = Matrix()
        m.setScale(scale, scale)
        m.postTranslate(
            dst.left + (dst.width() - bmp.width * scale) / 2f,
            dst.top + (dst.height() - bmp.height * scale) / 2f
        )
        canvas.save()
        canvas.clipRect(dst)
        canvas.drawBitmap(bmp, m, paint)
        canvas.restore()
    }

    /** Foto recortada en un círculo. */
    fun drawCirclePhoto(canvas: Canvas, bmp: Bitmap, cx: Float, cy: Float, r: Float) {
        val shader = BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val scale = max(2 * r / bmp.width, 2 * r / bmp.height)
        val m = Matrix()
        m.setScale(scale, scale)
        m.postTranslate(cx - bmp.width * scale / 2f, cy - bmp.height * scale / 2f)
        shader.setLocalMatrix(m)
        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { this.shader = shader }
        canvas.drawCircle(cx, cy, r, p)
    }

    /** Texto en una línea, achicado si no entra en [maxWidth]. */
    fun fitText(canvas: Canvas, text: String, x: Float, baseline: Float, paint: Paint, maxWidth: Float, align: Paint.Align = Paint.Align.CENTER) {
        val p = Paint(paint)
        p.textAlign = align
        val w = p.measureText(text)
        if (w > maxWidth && w > 0f) p.textSize = p.textSize * maxWidth / w
        canvas.drawText(text, x, baseline, p)
    }

    /** Ajusta el tamaño de letra para que [text] entre en una sola línea. */
    fun fittedSize(text: String, paint: Paint, maxWidth: Float, maxSize: Float): Float {
        val p = Paint(paint)
        p.textSize = maxSize
        val w = p.measureText(text)
        return if (w > maxWidth && w > 0f) maxSize * maxWidth / w else maxSize
    }

    /**
     * Texto de varias líneas centrado dentro de [box], achicando la letra
     * hasta que entre.
     */
    fun drawMultiline(
        canvas: Canvas,
        text: String,
        box: RectF,
        paint: TextPaint,
        maxSize: Float,
        minSize: Float,
        align: Layout.Alignment = Layout.Alignment.ALIGN_CENTER,
        verticalCenter: Boolean = true
    ): Float {
        val p = TextPaint(paint)
        var size = maxSize
        var layout: StaticLayout
        while (true) {
            p.textSize = size
            layout = StaticLayout.Builder.obtain(text, 0, text.length, p, box.width().toInt().coerceAtLeast(1))
                .setAlignment(align)
                .setLineSpacing(0f, 1.05f)
                .setIncludePad(false)
                .build()
            if (layout.height <= box.height() || size <= minSize) break
            size *= 0.92f
        }
        canvas.save()
        val top = if (verticalCenter) box.top + (box.height() - layout.height) / 2f else box.top
        canvas.translate(box.left, top)
        canvas.clipRect(0f, 0f, box.width(), box.height() + (box.top - top))
        layout.draw(canvas)
        canvas.restore()
        return layout.height.toFloat()
    }

    fun heartPath(cx: Float, cy: Float, size: Float): Path {
        val s = size / 2f
        return Path().apply {
            moveTo(cx, cy + s * 0.85f)
            cubicTo(cx - s * 1.25f, cy + s * 0.05f, cx - s * 0.75f, cy - s * 0.95f, cx, cy - s * 0.35f)
            cubicTo(cx + s * 0.75f, cy - s * 0.95f, cx + s * 1.25f, cy + s * 0.05f, cx, cy + s * 0.85f)
            close()
        }
    }

    fun starPath(cx: Float, cy: Float, outer: Float, inner: Float = outer * 0.45f, points: Int = 5): Path {
        val path = Path()
        val step = PI / points
        for (i in 0 until points * 2) {
            val r = if (i % 2 == 0) outer else inner
            val a = -PI / 2 + i * step
            val x = cx + (cos(a) * r).toFloat()
            val y = cy + (sin(a) * r).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return path
    }

    /** Destello de cuatro puntas. */
    fun sparklePath(cx: Float, cy: Float, r: Float): Path = Path().apply {
        moveTo(cx, cy - r)
        quadTo(cx, cy, cx + r, cy)
        quadTo(cx, cy, cx, cy + r)
        quadTo(cx, cy, cx - r, cy)
        quadTo(cx, cy, cx, cy - r)
        close()
    }

    fun boltPath(cx: Float, cy: Float, h: Float): Path {
        val w = h * 0.6f
        return Path().apply {
            moveTo(cx + w * 0.1f, cy - h / 2)
            lineTo(cx - w / 2, cy + h * 0.08f)
            lineTo(cx - w * 0.02f, cy + h * 0.08f)
            lineTo(cx - w * 0.15f, cy + h / 2)
            lineTo(cx + w / 2, cy - h * 0.1f)
            lineTo(cx + w * 0.02f, cy - h * 0.1f)
            close()
        }
    }

    /** Polígono regular (hexágono, etc.). */
    fun polygonPath(cx: Float, cy: Float, r: Float, sides: Int, rotation: Double = -PI / 2): Path {
        val path = Path()
        for (i in 0 until sides) {
            val a = rotation + i * 2 * PI / sides
            val x = cx + (cos(a) * r).toFloat()
            val y = cy + (sin(a) * r).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return path
    }

    /** Nube simple hecha con círculos. */
    fun cloudPath(cx: Float, cy: Float, w: Float): Path {
        val h = w * 0.55f
        val path = Path()
        val base = RectF(cx - w / 2, cy - h * 0.1f, cx + w / 2, cy + h / 2)
        path.addRoundRect(base, h * 0.3f, h * 0.3f, Path.Direction.CW)
        path.addCircle(cx - w * 0.18f, cy, w * 0.22f, Path.Direction.CW)
        path.addCircle(cx + w * 0.12f, cy - h * 0.18f, w * 0.28f, Path.Direction.CW)
        path.op(Path(), Path.Op.UNION)
        return path
    }

    fun clamp01(v: Float) = min(1f, max(0f, v))
}
