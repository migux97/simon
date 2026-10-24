package com.migux97.simon.custom

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.text.Layout
import androidx.core.graphics.drawable.toBitmap
import com.migux97.simon.data.CustomWidget
import com.migux97.simon.data.ElementType
import com.migux97.simon.data.ImageStore
import com.migux97.simon.data.Lookup
import com.migux97.simon.data.MediaLibrary
import com.migux97.simon.data.ProgressSource
import com.migux97.simon.data.ProgressStyle
import com.migux97.simon.data.Repository
import com.migux97.simon.data.ShapeKind
import com.migux97.simon.data.StudioStore
import com.migux97.simon.data.WElement
import com.migux97.simon.data.WidgetDesign
import com.migux97.simon.data.WidgetKind
import com.migux97.simon.data.WidgetSize
import com.migux97.simon.launcher.AppCatalog
import com.migux97.simon.ui.components.MotionMath
import com.migux97.simon.wallpaper.Scene
import com.migux97.simon.widget.Draw
import com.migux97.simon.widget.RenderEnv
import com.migux97.simon.widget.WidgetRenderer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** Reemplaza las etiquetas {hora}, {bateria}, {clima}… por datos en vivo. */
object Tokens {
    val all = listOf(
        "{hora}" to "Hora (24 h)",
        "{hora12}" to "Hora (12 h)",
        "{min}" to "Minutos",
        "{seg}" to "Segundos",
        "{ampm}" to "a. m. / p. m.",
        "{dia}" to "Día de la semana",
        "{diacorto}" to "Día corto",
        "{num}" to "Número de día",
        "{mes}" to "Mes",
        "{año}" to "Año",
        "{fecha}" to "Fecha completa",
        "{bateria}" to "Batería %",
        "{cargando}" to "Estado de carga",
        "{clima}" to "Temperatura",
        "{cielo}" to "Estado del cielo",
        "{ciudad}" to "Ciudad",
        "{maxmin}" to "Máx / mín",
        "{saludo}" to "Saludo según la hora",
        "{semana}" to "Semana del año",
        "{progresodia}" to "% del día"
    )

    private val locale: Locale = WidgetRenderer.locale

    fun resolve(text: String, env: RenderEnv, widget: CustomWidget?): String {
        if (!text.contains('{')) return text
        val now = env.now
        val time = now.time
        fun f(p: String) = SimpleDateFormat(p, locale).format(time)
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val weather = widget?.takeIf { it.city.isNotBlank() }?.let { Repository.weatherFor(it.lat, it.lon) }
        var out = text
        val map = mapOf(
            "{hora}" to f("HH:mm"),
            "{hora12}" to f("h:mm"),
            "{min}" to f("mm"),
            "{seg}" to f("ss"),
            "{ampm}" to if (now.get(Calendar.AM_PM) == Calendar.AM) "a. m." else "p. m.",
            "{dia}" to WidgetRenderer.capitalize(f("EEEE")),
            "{diacorto}" to WidgetRenderer.capitalize(f("EEE")).trimEnd('.'),
            "{num}" to now.get(Calendar.DAY_OF_MONTH).toString(),
            "{mes}" to f("MMMM"),
            "{año}" to now.get(Calendar.YEAR).toString(),
            "{fecha}" to f("d 'de' MMMM 'de' yyyy"),
            "{bateria}" to "${env.batteryLevel}%",
            "{cargando}" to if (env.charging) "Cargando" else "",
            "{clima}" to (weather?.let { "${it.temperature.roundToInt()}°" } ?: "--°"),
            "{cielo}" to (weather?.let { WidgetRenderer.weatherDescription(it.code) } ?: ""),
            "{ciudad}" to (widget?.city ?: ""),
            "{maxmin}" to (weather?.let { "${it.max.roundToInt()}° / ${it.min.roundToInt()}°" } ?: ""),
            "{saludo}" to when (hour) {
                in 5..11 -> "Buenos días"
                in 12..19 -> "Buenas tardes"
                else -> "Buenas noches"
            },
            "{semana}" to now.get(Calendar.WEEK_OF_YEAR).toString(),
            "{progresodia}" to "${((hour * 60 + now.get(Calendar.MINUTE)) / 14.4f).roundToInt()}%"
        )
        map.forEach { (k, v) -> out = out.replace(k, v) }
        return out
    }

    fun needsSeconds(text: String) = text.contains("{seg}")
}

/** Dibuja un elemento de un widget creado en un Canvas común. */
object ElementPainter {

    /** Valor 0..1 para los elementos de progreso. */
    fun progressValue(e: WElement, env: RenderEnv): Float {
        val c = env.now
        return when (e.progress) {
            ProgressSource.BATTERY -> env.batteryLevel / 100f
            ProgressSource.DAY -> (c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)) / 1440f
            ProgressSource.WEEK -> (((c.get(Calendar.DAY_OF_WEEK) + 5) % 7) + c.get(Calendar.HOUR_OF_DAY) / 24f) / 7f
            ProgressSource.MONTH -> (c.get(Calendar.DAY_OF_MONTH) - 1 + c.get(Calendar.HOUR_OF_DAY) / 24f) / c.getActualMaximum(Calendar.DAY_OF_MONTH)
            ProgressSource.YEAR -> (c.get(Calendar.DAY_OF_YEAR) - 1f) / c.getActualMaximum(Calendar.DAY_OF_YEAR)
            ProgressSource.CUSTOM -> e.customValue
        }.coerceIn(0f, 1f)
    }

    fun shapePath(kind: ShapeKind, r: RectF, corner: Float): Path {
        val cx = r.centerX(); val cy = r.centerY()
        val m = min(r.width(), r.height())
        return when (kind) {
            ShapeKind.RECT -> Path().apply { addRect(r, Path.Direction.CW) }
            ShapeKind.ROUNDED -> Path().apply { addRoundRect(r, m * corner, m * corner, Path.Direction.CW) }
            ShapeKind.CIRCLE -> Path().apply { addOval(r, Path.Direction.CW) }
            ShapeKind.STAR -> Draw.starPath(cx, cy, m / 2, m * 0.22f)
            ShapeKind.HEART -> Draw.heartPath(cx, cy + m * 0.04f, m * 0.95f)
            ShapeKind.TRIANGLE -> Path().apply { moveTo(cx, r.top); lineTo(r.right, r.bottom); lineTo(r.left, r.bottom); close() }
            ShapeKind.HEXAGON -> Draw.polygonPath(cx, cy, m / 2, 6, 0.0)
            ShapeKind.LINE -> Path().apply { addRect(RectF(r.left, cy - r.height() * 0.08f, r.right, cy + r.height() * 0.08f), Path.Direction.CW) }
            ShapeKind.SPARKLE -> Draw.sparklePath(cx, cy, m / 2)
        }
    }

    private fun fillPaint(e: WElement, r: RectF): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        if (e.gradient) shader = Draw.angledGradient(r, e.gradientAngle, intArrayOf(e.fill1, e.fill2)) else color = e.fill1
    }

    /**
     * Dibuja [e] dentro de [r]. [timeMs] mueve animaciones y escenas; para bitmaps
     * estáticos se usa 0. Los videos se dibujan con su primer cuadro.
     */
    fun draw(context: Context?, c: Canvas, e: WElement, r: RectF, env: RenderEnv, widget: CustomWidget?, timeMs: Long, scenes: MutableMap<String, Scene>? = null) {
        if (e.hidden) return
        when (e.type) {
            ElementType.TEXT -> {
                val text = Tokens.resolve(e.text.ifBlank { " " }, env, widget)
                val tp = Draw.textPaint(e.font, e.textColor, r.height() * 0.8f, e.bold)
                if (e.shadow) tp.setShadowLayer(r.height() * 0.06f, 0f, r.height() * 0.03f, Draw.withAlpha(Color.BLACK, 140))
                val align = when (e.align) {
                    0 -> Layout.Alignment.ALIGN_NORMAL
                    2 -> Layout.Alignment.ALIGN_OPPOSITE
                    else -> Layout.Alignment.ALIGN_CENTER
                }
                if (!text.contains('\n') && tp.measureText(text) <= r.width() * 1.6f) {
                    // Una línea: se ajusta para ocupar el alto del cuadro.
                    tp.textSize = Draw.fittedSize(text, tp, r.width(), r.height() * 0.78f)
                    val x = when (e.align) { 0 -> r.left; 2 -> r.right; else -> r.centerX() }
                    tp.textAlign = when (e.align) { 0 -> Paint.Align.LEFT; 2 -> Paint.Align.RIGHT; else -> Paint.Align.CENTER }
                    val fm = tp.fontMetrics
                    c.drawText(text, x, r.centerY() - (fm.ascent + fm.descent) / 2, tp)
                } else {
                    Draw.drawMultiline(c, text, r, tp, r.height() * 0.5f, r.height() * 0.05f, align)
                }
            }
            ElementType.CLOCK -> {
                val rad = min(r.width(), r.height()) / 2
                val cx = r.centerX(); val cy = r.centerY()
                val face = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Draw.withAlpha(e.fill1, (255 * 0.25f).toInt()) }
                c.drawCircle(cx, cy, rad, face)
                val tick = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = e.textColor; strokeCap = Paint.Cap.ROUND; strokeWidth = rad * 0.04f }
                for (i in 0 until 12) {
                    val a = i / 12.0 * 2 * PI
                    c.drawLine(cx + (sin(a) * rad * 0.8).toFloat(), cy - (cos(a) * rad * 0.8).toFloat(), cx + (sin(a) * rad * 0.92).toFloat(), cy - (cos(a) * rad * 0.92).toFloat(), tick)
                }
                val now = env.now
                val sec = now.get(Calendar.SECOND) + (if (timeMs > 0) (System.currentTimeMillis() % 1000) / 1000.0 else 0.0)
                val minute = now.get(Calendar.MINUTE) + sec / 60.0
                val hour = now.get(Calendar.HOUR) + minute / 60.0
                val hand = Paint(tick)
                hand.strokeWidth = rad * 0.08f
                c.drawLine(cx, cy, cx + (sin(hour / 12 * 2 * PI) * rad * 0.5).toFloat(), cy - (cos(hour / 12 * 2 * PI) * rad * 0.5).toFloat(), hand)
                hand.strokeWidth = rad * 0.05f
                c.drawLine(cx, cy, cx + (sin(minute / 60 * 2 * PI) * rad * 0.74).toFloat(), cy - (cos(minute / 60 * 2 * PI) * rad * 0.74).toFloat(), hand)
                if (timeMs > 0) {
                    hand.color = e.fill2; hand.strokeWidth = rad * 0.02f
                    c.drawLine(cx, cy, cx + (sin(sec / 60 * 2 * PI) * rad * 0.8).toFloat(), cy - (cos(sec / 60 * 2 * PI) * rad * 0.8).toFloat(), hand)
                }
                c.drawCircle(cx, cy, rad * 0.07f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = e.fill2 })
            }
            ElementType.SHAPE -> {
                val path = shapePath(e.shape, r, e.corner)
                c.drawPath(path, fillPaint(e, r))
                if (e.strokeWidth > 0f) {
                    val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        color = e.strokeColor
                        strokeWidth = min(r.width(), r.height()) * e.strokeWidth * 0.1f
                    }
                    c.drawPath(path, sp)
                }
            }
            ElementType.IMAGE, ElementType.DRAWING, ElementType.VIDEO, ElementType.ANIMATION -> {
                val bmp: Bitmap? = when (e.type) {
                    ElementType.IMAGE -> ImageStore.load(e.mediaPath, 900)
                    ElementType.DRAWING -> MediaLibrary.loadDrawing(e.mediaId, 900)
                    ElementType.VIDEO -> MediaLibrary.videoThumb(e.mediaPath)
                    else -> {
                        val frames = MediaLibrary.loadFrames(e.mediaId, 600)
                        frames.getOrNull(MediaLibrary.frameIndex(StudioStore.animation(e.mediaId), frames.size, timeMs))
                    }
                }
                if (bmp != null) {
                    c.save()
                    c.clipPath(shapePath(e.shape, r, e.corner))
                    if (e.type == ElementType.DRAWING || e.type == ElementType.ANIMATION) drawFit(c, bmp, r) else Draw.drawCenterCrop(c, bmp, r)
                    c.restore()
                } else {
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Draw.withAlpha(Color.WHITE, 60) }
                    c.drawRoundRect(r, min(r.width(), r.height()) * 0.15f, min(r.width(), r.height()) * 0.15f, p)
                }
            }
            ElementType.SCENE -> {
                val live = e.liveId?.let { Lookup.live(it) }
                if (live != null) {
                    val key = e.id
                    val scene = scenes?.getOrPut(key) { Scene.create(live) } ?: Scene.create(live).also { s ->
                        s.resize(r.width().toInt().coerceAtLeast(1), r.height().toInt().coerceAtLeast(1))
                        repeat(30) { s.step(0.06f) }
                    }
                    scene.resize(r.width().toInt().coerceAtLeast(1), r.height().toInt().coerceAtLeast(1))
                    c.save()
                    c.clipPath(shapePath(e.shape, r, e.corner))
                    c.translate(r.left, r.top)
                    scene.render(c)
                    c.restore()
                }
            }
            ElementType.PROGRESS -> {
                val v = progressValue(e, env)
                val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Draw.withAlpha(e.fill2, 70) }
                val fill = fillPaint(e, r)
                when (e.progressStyle) {
                    ProgressStyle.BAR -> {
                        val rad = r.height() / 2
                        c.drawRoundRect(r, rad, rad, track)
                        if (v > 0f) c.drawRoundRect(RectF(r.left, r.top, r.left + (r.width() * v).coerceAtLeast(r.height()), r.bottom), rad, rad, fill)
                    }
                    ProgressStyle.RING -> {
                        val m = min(r.width(), r.height())
                        val stroke = m * 0.12f
                        val oval = RectF(r.centerX() - m / 2 + stroke / 2, r.centerY() - m / 2 + stroke / 2, r.centerX() + m / 2 - stroke / 2, r.centerY() + m / 2 - stroke / 2)
                        track.style = Paint.Style.STROKE; track.strokeWidth = stroke
                        fill.style = Paint.Style.STROKE; fill.strokeWidth = stroke; fill.strokeCap = Paint.Cap.ROUND
                        c.drawArc(oval, 0f, 360f, false, track)
                        c.drawArc(oval, -90f, 360f * v, false, fill)
                    }
                    ProgressStyle.DOTS -> {
                        val n = 10
                        val step = r.width() / n
                        val rad = min(step, r.height()) * 0.35f
                        for (i in 0 until n) {
                            c.drawCircle(r.left + step * (i + 0.5f), r.centerY(), rad, if ((i + 0.5f) / n <= v) fill else track)
                        }
                    }
                }
            }
            ElementType.APP_ICON -> {
                val app = AppCatalog.find(e.appKey)
                val bmp = app?.icon?.toBitmap(256, 256)
                if (bmp != null) drawFit(c, bmp, r)
            }
            ElementType.STICKER -> StudioStore.sticker(e.mediaId)?.let { StickerPainter.draw(c, it, r, timeMs) }
            ElementType.WEATHER_ICON -> {
                val info = widget?.takeIf { it.city.isNotBlank() }?.let { Repository.weatherFor(it.lat, it.lon) }
                val stub = WidgetDesign(name = "", kind = WidgetKind.WEATHER, size = WidgetSize.SMALL, color1 = 0, color2 = 0)
                WidgetRenderer.weatherIcon(c, stub, r.centerX(), r.centerY(), min(r.width(), r.height()), info?.code ?: 2, info?.isDay ?: true)
            }
        }
    }

    /** Dibuja el bitmap completo, sin recortar, centrado en [r]. */
    fun drawFit(c: Canvas, bmp: Bitmap, r: RectF) {
        val s = min(r.width() / bmp.width, r.height() / bmp.height)
        val w = bmp.width * s; val h = bmp.height * s
        val dst = RectF(r.centerX() - w / 2, r.centerY() - h / 2, r.centerX() + w / 2, r.centerY() + h / 2)
        c.drawBitmap(bmp, null, dst, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    }
}

/** Dibuja un widget creado completo (fondo + elementos). */
object CustomWidgetRenderer {

    fun drawBackground(c: Canvas, w: CustomWidget, width: Float, height: Float) {
        val rect = RectF(0f, 0f, width, height)
        val rad = min(width, height) * w.corner
        val clip = Path().apply { addRoundRect(rect, rad, rad, Path.Direction.CW) }
        c.clipPath(clip)
        val bmp = ImageStore.load(w.bgImage, 1000)
        if (bmp != null) Draw.drawCenterCrop(c, bmp, rect)
        if (w.bgAlpha > 0f) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG)
            if (w.bgGradient) p.shader = Draw.angledGradient(rect, w.bgAngle, intArrayOf(w.bg1, w.bg2)) else p.color = w.bg1
            p.alpha = (255 * w.bgAlpha).toInt().coerceIn(0, 255)
            c.drawRect(rect, p)
        }
    }

    /** Dibujo completo en un Canvas: se usa para el widget del sistema y para miniaturas. */
    fun draw(context: Context?, c: Canvas, w: CustomWidget, width: Float, height: Float, env: RenderEnv, timeMs: Long = 0L) {
        c.save()
        drawBackground(c, w, width, height)
        w.elements.filterNot { it.hidden }.forEach { e ->
            val r = RectF(e.x * width, e.y * height, (e.x + e.w) * width, (e.y + e.h) * height)
            val m = MotionMath.at(e.motion, e.motionSpeed, timeMs, r.width(), r.height())
            c.save()
            c.translate(m.dx, m.dy)
            c.rotate(e.rotation + m.rotation, r.centerX(), r.centerY())
            c.scale(m.scaleX, m.scaleY, r.centerX(), r.centerY())
            val alpha = (255 * e.alpha * m.alpha).toInt().coerceIn(0, 255)
            if (alpha < 255) c.saveLayerAlpha(r.left - r.width(), r.top - r.height(), r.right + r.width(), r.bottom + r.height(), alpha)
            ElementPainter.draw(context, c, e, r, env, w, timeMs)
            if (alpha < 255) c.restore()
            c.restore()
        }
        c.restore()
    }

    fun render(context: Context?, w: CustomWidget, width: Int, height: Int, env: RenderEnv): Bitmap {
        val bmp = Bitmap.createBitmap(width.coerceAtLeast(8), height.coerceAtLeast(8), Bitmap.Config.ARGB_8888)
        draw(context, Canvas(bmp), w, bmp.width.toFloat(), bmp.height.toFloat(), env)
        return bmp
    }
}
