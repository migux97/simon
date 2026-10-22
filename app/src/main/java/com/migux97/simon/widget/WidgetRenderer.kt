package com.migux97.simon.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.os.BatteryManager
import android.text.Layout
import com.migux97.simon.data.BgStyle
import com.migux97.simon.data.Decoration
import com.migux97.simon.data.ImageStore
import com.migux97.simon.data.Repository
import com.migux97.simon.data.WidgetDesign
import com.migux97.simon.data.WidgetKind
import com.migux97.simon.data.WidgetSize
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** Datos del momento que necesita el dibujo (inyectables para vistas previas). */
data class RenderEnv(
    val now: Calendar = Calendar.getInstance(),
    val batteryLevel: Int = 80,
    val charging: Boolean = false
) {
    companion object {
        fun current(context: Context): RenderEnv {
            val bm = context.applicationContext.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.takeIf { it in 0..100 } ?: 80
            val charging = bm?.isCharging ?: false
            return RenderEnv(Calendar.getInstance(), level, charging)
        }
    }
}

/**
 * Dibuja cualquier [WidgetDesign] en un Bitmap. Se usa tanto para el widget
 * real en la pantalla de inicio como para las vistas previas dentro de la app.
 */
object WidgetRenderer {

    val locale: Locale = Locale("es", "AR")

    fun render(design: WidgetDesign, width: Int, height: Int, env: RenderEnv): Bitmap {
        val w = width.coerceAtLeast(16)
        val h = height.coerceAtLeast(16)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        draw(canvas, design, w.toFloat(), h.toFloat(), env)
        return bmp
    }

    fun draw(canvas: Canvas, d: WidgetDesign, w: Float, h: Float, env: RenderEnv) {
        val u = min(w, h)
        val radius = d.cornerRadius / 100f * u * 0.6f
        val rect = RectF(0f, 0f, w, h)
        val clip = Path().apply { addRoundRect(rect, radius, radius, Path.Direction.CW) }

        canvas.save()
        canvas.clipPath(clip)
        drawBackground(canvas, d, rect, u)
        drawDecoration(canvas, d, rect, u)
        val pad = u * 0.09f
        val content = RectF(pad, pad, w - pad, h - pad)
        when (d.kind) {
            WidgetKind.DIGITAL_CLOCK -> digitalClock(canvas, d, content, env)
            WidgetKind.ANALOG_CLOCK -> analogClock(canvas, d, content, env)
            WidgetKind.CALENDAR -> calendar(canvas, d, content, env)
            WidgetKind.BATTERY -> battery(canvas, d, content, env)
            WidgetKind.PHOTO -> photo(canvas, d, rect, u, radius)
            WidgetKind.QUOTE -> quote(canvas, d, content)
            WidgetKind.COUPLE -> couple(canvas, d, content, env)
            WidgetKind.COUNTDOWN -> countdown(canvas, d, content, env)
            WidgetKind.WEATHER -> weather(canvas, d, content)
        }
        canvas.restore()

        if (d.bgStyle == BgStyle.GLASS) {
            val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = u * 0.012f
                color = Draw.withAlpha(Color.WHITE, 110)
            }
            val inset = border.strokeWidth / 2
            canvas.drawRoundRect(RectF(inset, inset, w - inset, h - inset), radius, radius, border)
        }
    }

    // ------------------------------------------------------------------ fondo

    private fun drawBackground(canvas: Canvas, d: WidgetDesign, rect: RectF, u: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        when (d.bgStyle) {
            BgStyle.SOLID -> {
                p.color = Draw.withAlpha(d.color1, d.bgAlpha)
                canvas.drawRect(rect, p)
            }
            BgStyle.GRADIENT -> {
                p.shader = Draw.angledGradient(rect, d.gradientAngle, intArrayOf(d.color1, d.color2))
                p.alpha = d.bgAlpha
                canvas.drawRect(rect, p)
            }
            BgStyle.GLASS -> {
                p.shader = Draw.angledGradient(
                    rect, d.gradientAngle,
                    intArrayOf(Draw.withAlpha(d.color1, (d.bgAlpha * 0.55f).toInt()), Draw.withAlpha(d.color2, (d.bgAlpha * 0.35f).toInt()))
                )
                canvas.drawRect(rect, p)
                // Brillo superior que imita vidrio
                val shine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(0f, 0f, 0f, rect.height() * 0.6f, Draw.withAlpha(Color.WHITE, 70), Color.TRANSPARENT, Shader.TileMode.CLAMP)
                }
                canvas.drawRect(rect, shine)
            }
            BgStyle.PHOTO -> {
                val bmp = if (d.kind == WidgetKind.PHOTO) null else ImageStore.load(d.photoPath, 900)
                if (bmp != null) {
                    Draw.drawCenterCrop(canvas, bmp, rect)
                    // Velo para que el texto se lea sobre cualquier foto
                    p.shader = LinearGradient(
                        0f, 0f, 0f, rect.height(),
                        Draw.withAlpha(d.color1, 70), Draw.withAlpha(d.color2, 150), Shader.TileMode.CLAMP
                    )
                    canvas.drawRect(rect, p)
                } else {
                    p.shader = Draw.angledGradient(rect, d.gradientAngle, intArrayOf(d.color1, d.color2))
                    canvas.drawRect(rect, p)
                }
            }
        }
    }

    private fun drawDecoration(canvas: Canvas, d: WidgetDesign, rect: RectF, u: Float) {
        if (d.decoration == Decoration.NONE) return
        val rnd = Random(d.id.hashCode())
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Draw.withAlpha(d.accentColor, 70) }
        when (d.decoration) {
            Decoration.STARS -> repeat(14) {
                val x = rnd.nextFloat() * rect.width()
                val y = rnd.nextFloat() * rect.height()
                p.alpha = 40 + rnd.nextInt(80)
                canvas.drawPath(Draw.starPath(x, y, u * (0.015f + rnd.nextFloat() * 0.035f)), p)
            }
            Decoration.HEARTS -> repeat(10) {
                val x = rnd.nextFloat() * rect.width()
                val y = rnd.nextFloat() * rect.height()
                p.alpha = 40 + rnd.nextInt(70)
                canvas.drawPath(Draw.heartPath(x, y, u * (0.05f + rnd.nextFloat() * 0.08f)), p)
            }
            Decoration.DOTS -> {
                val step = u * 0.11f
                var y = step / 2
                var row = 0
                while (y < rect.height()) {
                    var x = if (row % 2 == 0) step / 2 else step
                    while (x < rect.width()) {
                        canvas.drawCircle(x, y, u * 0.012f, p)
                        x += step
                    }
                    y += step * 0.87f
                    row++
                }
            }
            Decoration.WAVES -> {
                p.style = Paint.Style.STROKE
                p.strokeWidth = u * 0.018f
                for (i in 0 until 4) {
                    val path = Path()
                    val baseY = rect.height() * (0.55f + i * 0.13f)
                    path.moveTo(0f, baseY)
                    var x = 0f
                    while (x <= rect.width()) {
                        path.lineTo(x, baseY + sin(x / rect.width() * 2 * PI * 1.5 + i).toFloat() * u * 0.05f)
                        x += 6f
                    }
                    p.alpha = 90 - i * 18
                    canvas.drawPath(path, p)
                }
            }
            Decoration.BUBBLES -> repeat(9) {
                val x = rnd.nextFloat() * rect.width()
                val y = rnd.nextFloat() * rect.height()
                val r = u * (0.04f + rnd.nextFloat() * 0.14f)
                p.alpha = 25 + rnd.nextInt(45)
                canvas.drawCircle(x, y, r, p)
            }
            Decoration.SPARKLES -> repeat(9) {
                val x = rnd.nextFloat() * rect.width()
                val y = rnd.nextFloat() * rect.height()
                p.alpha = 60 + rnd.nextInt(100)
                canvas.drawPath(Draw.sparklePath(x, y, u * (0.02f + rnd.nextFloat() * 0.04f)), p)
            }
            Decoration.NONE -> Unit
        }
    }

    // ---------------------------------------------------------------- helpers

    private fun paint(d: WidgetDesign, size: Float, color: Int = d.textColor, bold: Boolean = false) =
        Draw.textPaint(d.font, color, size, bold)

    private fun soft(d: WidgetDesign) = Draw.withAlpha(d.textColor, 200)

    fun capitalize(s: String) = s.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

    private fun fmt(pattern: String, date: Date) = SimpleDateFormat(pattern, locale).format(date)

    private fun timeText(d: WidgetDesign, now: Calendar): Pair<String, String?> {
        return if (d.use24h) fmt("HH:mm", now.time) to null
        else fmt("h:mm", now.time) to (if (now.get(Calendar.AM_PM) == Calendar.AM) "a. m." else "p. m.")
    }

    // -------------------------------------------------------------- reloj dig

    private fun digitalClock(c: Canvas, d: WidgetDesign, r: RectF, env: RenderEnv) {
        val (time, ampm) = timeText(d, env.now)
        val weekday = capitalize(fmt("EEEE", env.now.time))
        val date = fmt("d 'de' MMMM", env.now.time)
        when (d.size) {
            WidgetSize.SMALL -> {
                val tp = paint(d, r.height() * 0.42f)
                tp.textSize = Draw.fittedSize(time, tp, r.width(), r.height() * 0.42f)
                val cy = r.centerY() + tp.textSize * 0.12f
                c.drawText(time, r.centerX(), cy, tp.apply { textAlign = Paint.Align.CENTER })
                ampm?.let { Draw.fitText(c, it, r.centerX(), cy - tp.textSize * 0.85f, paint(d, r.height() * 0.09f, soft(d)), r.width()) }
                Draw.fitText(c, weekday, r.centerX(), cy + r.height() * 0.2f, paint(d, r.height() * 0.11f, d.accentColor, true), r.width())
                Draw.fitText(c, date, r.centerX(), cy + r.height() * 0.34f, paint(d, r.height() * 0.1f, soft(d)), r.width())
            }
            WidgetSize.MEDIUM -> {
                val leftW = r.width() * 0.6f
                val tp = paint(d, r.height() * 0.6f)
                tp.textSize = Draw.fittedSize(time, tp, leftW, r.height() * 0.62f)
                val base = r.centerY() + tp.textSize * 0.36f
                c.drawText(time, r.left, base, tp.apply { textAlign = Paint.Align.LEFT })
                ampm?.let { c.drawText(it, r.left + tp.measureText(time) + r.height() * 0.04f, base, paint(d, r.height() * 0.12f, soft(d))) }
                val x = r.left + leftW + r.width() * 0.05f
                val divider = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Draw.withAlpha(d.textColor, 80); strokeWidth = r.height() * 0.015f }
                c.drawLine(x - r.width() * 0.025f, r.top + r.height() * 0.15f, x - r.width() * 0.025f, r.bottom - r.height() * 0.15f, divider)
                val colW = r.right - x
                Draw.fitText(c, weekday, x, r.centerY() - r.height() * 0.05f, paint(d, r.height() * 0.18f, d.accentColor, true), colW, Paint.Align.LEFT)
                Draw.fitText(c, date, x, r.centerY() + r.height() * 0.18f, paint(d, r.height() * 0.14f, soft(d)), colW, Paint.Align.LEFT)
                Draw.fitText(c, fmt("yyyy", env.now.time), x, r.centerY() + r.height() * 0.38f, paint(d, r.height() * 0.12f, Draw.withAlpha(d.textColor, 140)), colW, Paint.Align.LEFT)
            }
            WidgetSize.LARGE -> {
                val tp = paint(d, r.height() * 0.32f)
                tp.textSize = Draw.fittedSize(time, tp, r.width(), r.height() * 0.32f)
                val base = r.top + r.height() * 0.38f
                c.drawText(time, r.centerX(), base, tp.apply { textAlign = Paint.Align.CENTER })
                ampm?.let { Draw.fitText(c, it, r.centerX(), r.top + r.height() * 0.08f, paint(d, r.height() * 0.06f, soft(d)), r.width()) }
                Draw.fitText(c, "$weekday, $date", r.centerX(), base + r.height() * 0.12f, paint(d, r.height() * 0.07f, soft(d)), r.width())
                weekStrip(c, d, RectF(r.left, r.top + r.height() * 0.62f, r.right, r.bottom), env.now)
            }
        }
    }

    /** Tira con los 7 días de la semana y hoy resaltado. */
    private fun weekStrip(c: Canvas, d: WidgetDesign, r: RectF, now: Calendar) {
        val cal = now.clone() as Calendar
        cal.firstDayOfWeek = Calendar.MONDAY
        val offset = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        cal.add(Calendar.DAY_OF_YEAR, -offset)
        val cellW = r.width() / 7f
        val letters = listOf("L", "M", "M", "J", "V", "S", "D")
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Draw.withAlpha(d.textColor, 30) }
        c.drawRoundRect(r, r.height() * 0.3f, r.height() * 0.3f, bg)
        for (i in 0 until 7) {
            val cx = r.left + cellW * (i + 0.5f)
            val isToday = i == offset
            val dayP = paint(d, r.height() * 0.24f, if (isToday) d.color1 else d.textColor, true).apply { textAlign = Paint.Align.CENTER }
            if (isToday) {
                val hp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = d.accentColor }
                c.drawRoundRect(RectF(cx - cellW * 0.4f, r.top + r.height() * 0.1f, cx + cellW * 0.4f, r.bottom - r.height() * 0.1f), cellW * 0.3f, cellW * 0.3f, hp)
            }
            c.drawText(letters[i], cx, r.top + r.height() * 0.4f, paint(d, r.height() * 0.17f, if (isToday) d.color1 else soft(d)).apply { textAlign = Paint.Align.CENTER })
            c.drawText(cal.get(Calendar.DAY_OF_MONTH).toString(), cx, r.top + r.height() * 0.75f, dayP)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    // ------------------------------------------------------------- reloj anal

    private fun analogClock(c: Canvas, d: WidgetDesign, r: RectF, env: RenderEnv) {
        when (d.size) {
            WidgetSize.SMALL -> dial(c, d, r.centerX(), r.centerY(), min(r.width(), r.height()) / 2f, env.now, false)
            WidgetSize.MEDIUM -> {
                val rad = r.height() / 2f
                dial(c, d, r.left + rad, r.centerY(), rad, env.now, false)
                val x = r.left + rad * 2 + r.width() * 0.07f
                val colW = r.right - x
                val (time, ampm) = timeText(d, env.now)
                Draw.fitText(c, capitalize(fmt("EEEE", env.now.time)), x, r.centerY() - r.height() * 0.12f, paint(d, r.height() * 0.17f, d.accentColor, true), colW, Paint.Align.LEFT)
                Draw.fitText(c, fmt("d 'de' MMMM", env.now.time), x, r.centerY() + r.height() * 0.1f, paint(d, r.height() * 0.14f, soft(d)), colW, Paint.Align.LEFT)
                Draw.fitText(c, time + (ampm?.let { " $it" } ?: ""), x, r.centerY() + r.height() * 0.34f, paint(d, r.height() * 0.13f, Draw.withAlpha(d.textColor, 150)), colW, Paint.Align.LEFT)
            }
            WidgetSize.LARGE -> {
                val rad = min(r.width(), r.height() * 0.8f) / 2f
                dial(c, d, r.centerX(), r.top + rad, rad, env.now, true)
                Draw.fitText(c, capitalize(fmt("EEEE d 'de' MMMM", env.now.time)), r.centerX(), r.bottom - r.height() * 0.03f, paint(d, r.height() * 0.07f, soft(d)), r.width())
            }
        }
    }

    private fun dial(c: Canvas, d: WidgetDesign, cx: Float, cy: Float, rad: Float, now: Calendar, numbers: Boolean) {
        val face = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Draw.withAlpha(d.textColor, 26) }
        c.drawCircle(cx, cy, rad, face)
        val tick = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = d.textColor; strokeCap = Paint.Cap.ROUND }
        for (i in 0 until 60) {
            val a = i / 60.0 * 2 * PI
            val major = i % 5 == 0
            if (!major && rad < 120) continue
            tick.strokeWidth = rad * if (major) 0.035f else 0.012f
            tick.alpha = if (major) 230 else 110
            val r1 = rad * if (major) 0.8f else 0.86f
            val r2 = rad * 0.9f
            c.drawLine(
                cx + (sin(a) * r1).toFloat(), cy - (cos(a) * r1).toFloat(),
                cx + (sin(a) * r2).toFloat(), cy - (cos(a) * r2).toFloat(), tick
            )
        }
        if (numbers) {
            val np = paint(d, rad * 0.14f).apply { textAlign = Paint.Align.CENTER }
            for (n in 1..12) {
                val a = n / 12.0 * 2 * PI
                val rr = rad * 0.66f
                c.drawText(n.toString(), cx + (sin(a) * rr).toFloat(), cy - (cos(a) * rr).toFloat() + np.textSize * 0.35f, np)
            }
        }
        val hour = now.get(Calendar.HOUR) + now.get(Calendar.MINUTE) / 60.0
        val minute = now.get(Calendar.MINUTE).toDouble()
        val hand = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = d.textColor; strokeCap = Paint.Cap.ROUND }
        hand.strokeWidth = rad * 0.075f
        val ha = hour / 12 * 2 * PI
        c.drawLine(cx, cy, cx + (sin(ha) * rad * 0.48f).toFloat(), cy - (cos(ha) * rad * 0.48f).toFloat(), hand)
        hand.strokeWidth = rad * 0.05f
        val ma = minute / 60 * 2 * PI
        c.drawLine(cx, cy, cx + (sin(ma) * rad * 0.72f).toFloat(), cy - (cos(ma) * rad * 0.72f).toFloat(), hand)
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = d.accentColor }
        c.drawCircle(cx, cy, rad * 0.075f, dot)
    }

    // -------------------------------------------------------------- calendario

    private fun calendar(c: Canvas, d: WidgetDesign, r: RectF, env: RenderEnv) {
        val now = env.now
        when (d.size) {
            WidgetSize.SMALL -> dayCard(c, d, r, now)
            WidgetSize.MEDIUM -> {
                dayCard(c, d, RectF(r.left, r.top, r.left + r.width() * 0.36f, r.bottom), now)
                monthGrid(c, d, RectF(r.left + r.width() * 0.42f, r.top, r.right, r.bottom), now, header = false)
            }
            WidgetSize.LARGE -> monthGrid(c, d, r, now, header = true)
        }
    }

    private fun dayCard(c: Canvas, d: WidgetDesign, r: RectF, now: Calendar) {
        Draw.fitText(c, capitalize(fmt("EEEE", now.time)), r.centerX(), r.top + r.height() * 0.2f, paint(d, r.height() * 0.13f, d.accentColor, true), r.width())
        val big = paint(d, r.height() * 0.5f, d.textColor, true)
        Draw.fitText(c, now.get(Calendar.DAY_OF_MONTH).toString(), r.centerX(), r.top + r.height() * 0.68f, big, r.width())
        Draw.fitText(c, capitalize(fmt("MMMM yyyy", now.time)), r.centerX(), r.top + r.height() * 0.9f, paint(d, r.height() * 0.11f, soft(d)), r.width())
    }

    private fun monthGrid(c: Canvas, d: WidgetDesign, r: RectF, now: Calendar, header: Boolean) {
        var top = r.top
        if (header) {
            val hp = paint(d, r.height() * 0.09f, d.textColor, true)
            Draw.fitText(c, capitalize(fmt("MMMM", now.time)), r.left, top + r.height() * 0.08f, hp, r.width() * 0.7f, Paint.Align.LEFT)
            Draw.fitText(c, fmt("yyyy", now.time), r.right, top + r.height() * 0.08f, paint(d, r.height() * 0.07f, d.accentColor, true), r.width() * 0.3f, Paint.Align.RIGHT)
            top += r.height() * 0.14f
        }
        val cal = now.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val firstOffset = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val days = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val rows = (firstOffset + days + 6) / 7
        val cellW = r.width() / 7f
        val cellH = (r.bottom - top) / (rows + 1)
        val letters = listOf("L", "M", "M", "J", "V", "S", "D")
        val lp = paint(d, cellH * 0.45f, Draw.withAlpha(d.textColor, 150), true).apply { textAlign = Paint.Align.CENTER }
        for (i in 0 until 7) c.drawText(letters[i], r.left + cellW * (i + 0.5f), top + cellH * 0.65f, lp)
        val today = now.get(Calendar.DAY_OF_MONTH)
        val np = paint(d, min(cellH, cellW) * 0.5f).apply { textAlign = Paint.Align.CENTER }
        val hl = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = d.accentColor }
        for (day in 1..days) {
            val idx = firstOffset + day - 1
            val col = idx % 7
            val row = idx / 7 + 1
            val cx = r.left + cellW * (col + 0.5f)
            val cy = top + cellH * (row + 0.5f)
            if (day == today) {
                c.drawCircle(cx, cy, min(cellW, cellH) * 0.46f, hl)
                np.color = if (Draw.luminance(d.accentColor) > 0.6f) Color.BLACK else Color.WHITE
                np.isFakeBoldText = true
            } else {
                np.color = if (col >= 5) Draw.withAlpha(d.textColor, 170) else d.textColor
                np.isFakeBoldText = false
            }
            c.drawText(day.toString(), cx, cy + np.textSize * 0.36f, np)
        }
    }

    // ----------------------------------------------------------------- batería

    private fun battery(c: Canvas, d: WidgetDesign, r: RectF, env: RenderEnv) {
        val level = env.batteryLevel
        val status = when {
            env.charging -> "Cargando"
            level <= 15 -> "Batería baja"
            level >= 95 -> "Carga completa"
            else -> "En uso"
        }
        when (d.size) {
            WidgetSize.SMALL -> ring(c, d, r.centerX(), r.centerY(), min(r.width(), r.height()) / 2f, level, env.charging, status)
            WidgetSize.MEDIUM -> {
                val rad = r.height() / 2f
                ring(c, d, r.left + rad, r.centerY(), rad, level, env.charging, null)
                val x = r.left + rad * 2 + r.width() * 0.07f
                val colW = r.right - x
                Draw.fitText(c, "Batería", x, r.top + r.height() * 0.25f, paint(d, r.height() * 0.15f, soft(d)), colW, Paint.Align.LEFT)
                Draw.fitText(c, "$level%", x, r.top + r.height() * 0.62f, paint(d, r.height() * 0.34f, d.textColor, true), colW, Paint.Align.LEFT)
                barGauge(c, d, RectF(x, r.top + r.height() * 0.74f, r.right, r.top + r.height() * 0.86f), level)
                Draw.fitText(c, status, x, r.bottom, paint(d, r.height() * 0.11f, d.accentColor, true), colW, Paint.Align.LEFT)
            }
            WidgetSize.LARGE -> {
                val rad = min(r.width(), r.height() * 0.7f) / 2f
                ring(c, d, r.centerX(), r.top + rad, rad, level, env.charging, status)
                barGauge(c, d, RectF(r.left, r.bottom - r.height() * 0.14f, r.right, r.bottom - r.height() * 0.06f), level)
            }
        }
    }

    private fun ring(c: Canvas, d: WidgetDesign, cx: Float, cy: Float, rad: Float, level: Int, charging: Boolean, label: String?) {
        val stroke = rad * 0.16f
        val oval = RectF(cx - rad + stroke / 2, cy - rad + stroke / 2, cx + rad - stroke / 2, cy + rad - stroke / 2)
        val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = stroke; color = Draw.withAlpha(d.textColor, 45)
        }
        c.drawArc(oval, 0f, 360f, false, track)
        val prog = Paint(track).apply {
            color = if (level <= 15 && !charging) Color.parseColor("#FF5A5F") else d.accentColor
            strokeCap = Paint.Cap.ROUND
        }
        c.drawArc(oval, -90f, 360f * level / 100f, false, prog)
        val tp = paint(d, rad * 0.5f, d.textColor, true)
        Draw.fitText(c, "$level%", cx, cy + rad * 0.15f, tp, rad * 1.2f)
        if (charging) {
            val bolt = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = d.accentColor }
            c.drawPath(Draw.boltPath(cx, cy - rad * 0.38f, rad * 0.26f), bolt)
        }
        label?.let { Draw.fitText(c, it, cx, cy + rad * 0.48f, paint(d, rad * 0.17f, soft(d)), rad * 1.3f) }
    }

    private fun barGauge(c: Canvas, d: WidgetDesign, r: RectF, level: Int) {
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Draw.withAlpha(d.textColor, 45) }
        c.drawRoundRect(r, r.height() / 2, r.height() / 2, bg)
        val fg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = d.accentColor }
        val fill = RectF(r.left, r.top, r.left + r.width() * level / 100f, r.bottom)
        if (fill.width() > r.height()) c.drawRoundRect(fill, r.height() / 2, r.height() / 2, fg)
    }

    // -------------------------------------------------------------------- foto

    private fun photo(c: Canvas, d: WidgetDesign, full: RectF, u: Float, radius: Float) {
        val bmp = ImageStore.load(d.photoPath, 1000)
        if (bmp == null) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Draw.withAlpha(d.textColor, 160) }
            val cx = full.centerX(); val cy = full.centerY() - u * 0.08f
            val s = u * 0.22f
            val frame = RectF(cx - s, cy - s * 0.75f, cx + s, cy + s * 0.75f)
            p.style = Paint.Style.STROKE; p.strokeWidth = u * 0.025f
            c.drawRoundRect(frame, s * 0.2f, s * 0.2f, p)
            p.style = Paint.Style.FILL
            c.drawCircle(cx - s * 0.4f, cy - s * 0.25f, s * 0.15f, p)
            val mountain = Path().apply {
                moveTo(frame.left + s * 0.15f, frame.bottom - s * 0.15f)
                lineTo(cx - s * 0.1f, cy)
                lineTo(cx + s * 0.25f, cy + s * 0.3f)
                lineTo(cx + s * 0.5f, cy + s * 0.05f)
                lineTo(frame.right - s * 0.15f, frame.bottom - s * 0.15f)
                close()
            }
            c.drawPath(mountain, p)
            Draw.fitText(c, "Elegí una foto", full.centerX(), full.centerY() + u * 0.3f, paint(d, u * 0.08f, soft(d)), full.width() * 0.85f)
            return
        }
        // El "marco" es el fondo: la foto va con un margen proporcional al grosor elegido.
        val inset = if (d.bgAlpha < 20) 0f else u * 0.045f
        val inner = RectF(full.left + inset, full.top + inset, full.right - inset, full.bottom - inset)
        val innerRadius = (radius - inset).coerceAtLeast(0f)
        c.save()
        c.clipPath(Path().apply { addRoundRect(inner, innerRadius, innerRadius, Path.Direction.CW) })
        Draw.drawCenterCrop(c, bmp, inner)
        if (d.text.isNotBlank()) {
            val scrim = Paint().apply {
                shader = LinearGradient(0f, inner.bottom - inner.height() * 0.4f, 0f, inner.bottom, Color.TRANSPARENT, Draw.withAlpha(Color.BLACK, 150), Shader.TileMode.CLAMP)
            }
            c.drawRect(inner, scrim)
            Draw.fitText(c, d.text, inner.left + u * 0.07f, inner.bottom - u * 0.08f, paint(d, u * 0.1f, d.textColor, true), inner.width() - u * 0.14f, Paint.Align.LEFT)
        }
        c.restore()
    }

    // ------------------------------------------------------------------- frase

    private fun quote(c: Canvas, d: WidgetDesign, r: RectF) {
        val text = d.text.ifBlank { "Escribí tu frase favorita" }
        val qp = paint(d, min(r.width(), r.height()) * 0.5f, Draw.withAlpha(d.accentColor, 120), true)
        c.drawText("“", r.left, r.top + qp.textSize * 0.7f, qp)
        val hasAuthor = d.text2.isNotBlank()
        val box = RectF(r.left + r.width() * 0.04f, r.top + r.height() * 0.12f, r.right - r.width() * 0.04f, r.bottom - if (hasAuthor) r.height() * 0.2f else r.height() * 0.06f)
        val tp = paint(d, r.height() * 0.2f)
        Draw.drawMultiline(c, text, box, tp, r.height() * 0.2f, r.height() * 0.06f, Layout.Alignment.ALIGN_CENTER)
        if (hasAuthor) Draw.fitText(c, "— ${d.text2}", r.centerX(), r.bottom - r.height() * 0.02f, paint(d, r.height() * 0.09f, d.accentColor, true), r.width())
    }

    // ------------------------------------------------------------------ pareja

    fun daysBetween(fromMillis: Long, to: Calendar): Long {
        val from = Calendar.getInstance().apply {
            timeInMillis = fromMillis
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val t = (to.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return TimeUnit.MILLISECONDS.toDays(t.timeInMillis - from.timeInMillis + TimeUnit.HOURS.toMillis(2))
    }

    private fun couple(c: Canvas, d: WidgetDesign, r: RectF, env: RenderEnv) {
        val start = if (d.dateMillis == 0L) env.now.timeInMillis else d.dateMillis
        val days = daysBetween(start, env.now).coerceAtLeast(0)
        val name1 = d.text.ifBlank { "Vos" }
        val name2 = d.text2.ifBlank { "Yo" }
        val heart = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = d.accentColor }
        when (d.size) {
            WidgetSize.SMALL -> {
                c.drawPath(Draw.heartPath(r.centerX(), r.top + r.height() * 0.17f, r.height() * 0.22f), heart)
                Draw.fitText(c, days.toString(), r.centerX(), r.top + r.height() * 0.66f, paint(d, r.height() * 0.38f, d.textColor, true), r.width())
                Draw.fitText(c, if (days == 1L) "día juntos" else "días juntos", r.centerX(), r.top + r.height() * 0.82f, paint(d, r.height() * 0.11f, soft(d)), r.width())
                Draw.fitText(c, "$name1 & $name2", r.centerX(), r.bottom, paint(d, r.height() * 0.09f, d.accentColor, true), r.width())
            }
            WidgetSize.MEDIUM -> {
                val rad = r.height() * 0.32f
                avatar(c, d, d.photoPath, r.left + rad, r.top + rad + r.height() * 0.04f, rad, name1)
                avatar(c, d, d.photoPath2, r.right - rad, r.top + rad + r.height() * 0.04f, rad, name2)
                Draw.fitText(c, name1, r.left + rad, r.bottom, paint(d, r.height() * 0.12f, soft(d), true), rad * 2.2f)
                Draw.fitText(c, name2, r.right - rad, r.bottom, paint(d, r.height() * 0.12f, soft(d), true), rad * 2.2f)
                val midW = r.width() - rad * 4.4f
                c.drawPath(Draw.heartPath(r.centerX(), r.top + r.height() * 0.17f, r.height() * 0.2f), heart)
                Draw.fitText(c, days.toString(), r.centerX(), r.top + r.height() * 0.66f, paint(d, r.height() * 0.38f, d.textColor, true), midW)
                Draw.fitText(c, "días juntos", r.centerX(), r.top + r.height() * 0.86f, paint(d, r.height() * 0.12f, soft(d)), midW)
            }
            WidgetSize.LARGE -> {
                val rad = r.width() * 0.17f
                val cy = r.top + rad + r.height() * 0.02f
                avatar(c, d, d.photoPath, r.centerX() - rad * 1.45f, cy, rad, name1)
                avatar(c, d, d.photoPath2, r.centerX() + rad * 1.45f, cy, rad, name2)
                c.drawPath(Draw.heartPath(r.centerX(), cy + rad * 0.15f, rad * 0.7f), heart)
                Draw.fitText(c, "$name1 & $name2", r.centerX(), cy + rad + r.height() * 0.12f, paint(d, r.height() * 0.07f, d.textColor, true), r.width())
                Draw.fitText(c, "Juntos hace", r.centerX(), r.top + r.height() * 0.62f, paint(d, r.height() * 0.06f, soft(d)), r.width())
                Draw.fitText(c, "$days días", r.centerX(), r.top + r.height() * 0.8f, paint(d, r.height() * 0.17f, d.accentColor, true), r.width())
                Draw.fitText(c, breakdown(start, env.now), r.centerX(), r.bottom - r.height() * 0.02f, paint(d, r.height() * 0.055f, soft(d)), r.width())
            }
        }
    }

    /** "1 año, 3 meses y 5 días". */
    private fun breakdown(startMillis: Long, now: Calendar): String {
        val s = Calendar.getInstance().apply { timeInMillis = startMillis }
        var years = now.get(Calendar.YEAR) - s.get(Calendar.YEAR)
        var months = now.get(Calendar.MONTH) - s.get(Calendar.MONTH)
        var days = now.get(Calendar.DAY_OF_MONTH) - s.get(Calendar.DAY_OF_MONTH)
        if (days < 0) {
            months -= 1
            val prev = (now.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
            days += prev.getActualMaximum(Calendar.DAY_OF_MONTH)
        }
        if (months < 0) { years -= 1; months += 12 }
        if (years < 0) return ""
        val parts = mutableListOf<String>()
        if (years > 0) parts += if (years == 1) "1 año" else "$years años"
        if (months > 0) parts += if (months == 1) "1 mes" else "$months meses"
        if (days > 0 || parts.isEmpty()) parts += if (days == 1) "1 día" else "$days días"
        return when (parts.size) {
            1 -> parts[0]
            2 -> "${parts[0]} y ${parts[1]}"
            else -> "${parts[0]}, ${parts[1]} y ${parts[2]}"
        }
    }

    private fun avatar(c: Canvas, d: WidgetDesign, path: String?, cx: Float, cy: Float, rad: Float, name: String) {
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = d.textColor }
        c.drawCircle(cx, cy, rad, ring)
        val bmp = ImageStore.load(path, 400)
        if (bmp != null) {
            Draw.drawCirclePhoto(c, bmp, cx, cy, rad * 0.9f)
        } else {
            val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Draw.mix(d.color1, d.color2, 0.5f) }
            c.drawCircle(cx, cy, rad * 0.9f, fill)
            val initial = name.trim().firstOrNull()?.uppercase() ?: "?"
            val tp = paint(d, rad * 0.9f, d.textColor, true).apply { textAlign = Paint.Align.CENTER }
            c.drawText(initial, cx, cy + tp.textSize * 0.35f, tp)
        }
    }

    // --------------------------------------------------------- cuenta regresiva

    private fun countdown(c: Canvas, d: WidgetDesign, r: RectF, env: RenderEnv) {
        val title = d.text.ifBlank { "Mi evento" }
        val target = if (d.dateMillis == 0L) env.now.timeInMillis else d.dateMillis
        val diff = -daysBetween(target, env.now)
        val (number, label) = when {
            diff > 1 -> diff.toString() to "días"
            diff == 1L -> "1" to "día"
            diff == 0L -> "¡Hoy!" to "es el día"
            else -> abs(diff).toString() to if (abs(diff) == 1L) "día desde" else "días desde"
        }
        val dateTxt = fmt("d 'de' MMMM yyyy", Date(target))
        when (d.size) {
            WidgetSize.SMALL -> {
                Draw.fitText(c, title, r.centerX(), r.top + r.height() * 0.16f, paint(d, r.height() * 0.12f, d.accentColor, true), r.width())
                Draw.fitText(c, number, r.centerX(), r.top + r.height() * 0.66f, paint(d, r.height() * 0.42f, d.textColor, true), r.width())
                Draw.fitText(c, label, r.centerX(), r.top + r.height() * 0.83f, paint(d, r.height() * 0.11f, soft(d)), r.width())
                Draw.fitText(c, dateTxt, r.centerX(), r.bottom, paint(d, r.height() * 0.08f, Draw.withAlpha(d.textColor, 150)), r.width())
            }
            WidgetSize.MEDIUM -> {
                val leftW = r.width() * 0.45f
                Draw.fitText(c, number, r.left + leftW / 2, r.top + r.height() * 0.66f, paint(d, r.height() * 0.6f, d.textColor, true), leftW)
                Draw.fitText(c, label, r.left + leftW / 2, r.bottom, paint(d, r.height() * 0.13f, soft(d)), leftW)
                val x = r.left + leftW + r.width() * 0.06f
                val colW = r.right - x
                Draw.fitText(c, "Faltan para", x, r.top + r.height() * 0.28f, paint(d, r.height() * 0.12f, soft(d)), colW, Paint.Align.LEFT)
                val box = RectF(x, r.top + r.height() * 0.34f, r.right, r.top + r.height() * 0.78f)
                Draw.drawMultiline(c, title, box, paint(d, r.height() * 0.2f, d.accentColor, true), r.height() * 0.2f, r.height() * 0.08f, Layout.Alignment.ALIGN_NORMAL)
                Draw.fitText(c, dateTxt, x, r.bottom, paint(d, r.height() * 0.1f, Draw.withAlpha(d.textColor, 150)), colW, Paint.Align.LEFT)
            }
            WidgetSize.LARGE -> {
                val box = RectF(r.left, r.top, r.right, r.top + r.height() * 0.24f)
                Draw.drawMultiline(c, title, box, paint(d, r.height() * 0.1f, d.accentColor, true), r.height() * 0.1f, r.height() * 0.05f)
                Draw.fitText(c, number, r.centerX(), r.top + r.height() * 0.62f, paint(d, r.height() * 0.32f, d.textColor, true), r.width())
                Draw.fitText(c, label, r.centerX(), r.top + r.height() * 0.74f, paint(d, r.height() * 0.07f, soft(d)), r.width())
                // Desglose en semanas y días
                if (diff > 7) {
                    val txt = "${diff / 7} semanas y ${diff % 7} días"
                    Draw.fitText(c, txt, r.centerX(), r.top + r.height() * 0.86f, paint(d, r.height() * 0.055f, soft(d)), r.width())
                }
                Draw.fitText(c, dateTxt, r.centerX(), r.bottom, paint(d, r.height() * 0.055f, Draw.withAlpha(d.textColor, 150)), r.width())
            }
        }
    }

    // ------------------------------------------------------------------- clima

    fun weatherDescription(code: Int): String = when (code) {
        0 -> "Despejado"
        1 -> "Mayormente despejado"
        2 -> "Parcialmente nublado"
        3 -> "Nublado"
        45, 48 -> "Niebla"
        51, 53, 55, 56, 57 -> "Llovizna"
        61, 63, 65, 66, 67, 80, 81, 82 -> "Lluvia"
        71, 73, 75, 77, 85, 86 -> "Nieve"
        95, 96, 99 -> "Tormenta"
        else -> "—"
    }

    private fun weather(c: Canvas, d: WidgetDesign, r: RectF) {
        val info = if (d.city.isBlank()) null else Repository.weatherFor(d.lat, d.lon)
        val city = d.city.ifBlank { "Elegí tu ciudad" }
        val temp = info?.let { "${it.temperature.roundToInt()}°" } ?: "--°"
        val desc = info?.let { weatherDescription(it.code) } ?: "Sin datos todavía"
        val range = info?.let { "Máx ${it.max.roundToInt()}° · Mín ${it.min.roundToInt()}°" } ?: ""
        val code = info?.code ?: 2
        val day = info?.isDay ?: true
        when (d.size) {
            WidgetSize.SMALL -> {
                Draw.fitText(c, city, r.left, r.top + r.height() * 0.13f, paint(d, r.height() * 0.11f, d.textColor, true), r.width() * 0.62f, Paint.Align.LEFT)
                weatherIcon(c, d, r.right - r.width() * 0.16f, r.top + r.height() * 0.16f, r.width() * 0.32f, code, day)
                Draw.fitText(c, temp, r.left, r.top + r.height() * 0.7f, paint(d, r.height() * 0.42f, d.textColor), r.width(), Paint.Align.LEFT)
                Draw.fitText(c, desc, r.left, r.top + r.height() * 0.86f, paint(d, r.height() * 0.1f, soft(d)), r.width(), Paint.Align.LEFT)
                Draw.fitText(c, range, r.left, r.bottom, paint(d, r.height() * 0.08f, Draw.withAlpha(d.textColor, 150)), r.width(), Paint.Align.LEFT)
            }
            WidgetSize.MEDIUM -> {
                weatherIcon(c, d, r.left + r.height() * 0.45f, r.centerY(), r.height() * 0.8f, code, day)
                val x = r.left + r.height() * 1.0f
                val colW = r.right - x
                Draw.fitText(c, city, x, r.top + r.height() * 0.2f, paint(d, r.height() * 0.14f, d.textColor, true), colW, Paint.Align.LEFT)
                Draw.fitText(c, temp, x, r.top + r.height() * 0.66f, paint(d, r.height() * 0.46f, d.textColor), colW * 0.5f, Paint.Align.LEFT)
                Draw.fitText(c, desc, r.right, r.top + r.height() * 0.52f, paint(d, r.height() * 0.12f, soft(d)), colW * 0.48f, Paint.Align.RIGHT)
                Draw.fitText(c, range, x, r.bottom, paint(d, r.height() * 0.1f, d.accentColor, true), colW, Paint.Align.LEFT)
            }
            WidgetSize.LARGE -> {
                Draw.fitText(c, city, r.centerX(), r.top + r.height() * 0.08f, paint(d, r.height() * 0.08f, d.textColor, true), r.width())
                weatherIcon(c, d, r.centerX(), r.top + r.height() * 0.33f, r.height() * 0.38f, code, day)
                Draw.fitText(c, temp, r.centerX(), r.top + r.height() * 0.78f, paint(d, r.height() * 0.24f, d.textColor), r.width())
                Draw.fitText(c, desc, r.centerX(), r.top + r.height() * 0.89f, paint(d, r.height() * 0.07f, soft(d)), r.width())
                Draw.fitText(c, range, r.centerX(), r.bottom, paint(d, r.height() * 0.06f, d.accentColor, true), r.width())
            }
        }
    }

    fun weatherIcon(c: Canvas, d: WidgetDesign, cx: Float, cy: Float, size: Float, code: Int, isDay: Boolean) {
        val sun = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (isDay) Color.parseColor("#FFD166") else Color.parseColor("#E8E6FF") }
        val cloud = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Draw.withAlpha(Color.WHITE, 235) }
        val dark = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#C7CBE0") }
        val r = size * 0.22f
        fun drawSun(x: Float, y: Float, rad: Float) {
            if (isDay) {
                val ray = Paint(sun).apply { strokeWidth = rad * 0.18f; strokeCap = Paint.Cap.ROUND }
                for (i in 0 until 8) {
                    val a = i * PI / 4
                    c.drawLine(x + (cos(a) * rad * 1.35f).toFloat(), y + (sin(a) * rad * 1.35f).toFloat(), x + (cos(a) * rad * 1.7f).toFloat(), y + (sin(a) * rad * 1.7f).toFloat(), ray)
                }
                c.drawCircle(x, y, rad, sun)
            } else {
                val moon = Path().apply {
                    addCircle(x, y, rad * 1.1f, Path.Direction.CW)
                    op(Path().apply { addCircle(x + rad * 0.55f, y - rad * 0.35f, rad * 0.95f, Path.Direction.CW) }, Path.Op.DIFFERENCE)
                }
                c.drawPath(moon, sun)
            }
        }
        when (code) {
            0, 1 -> drawSun(cx, cy, r * 1.2f)
            2 -> {
                drawSun(cx - size * 0.12f, cy - size * 0.12f, r)
                c.drawPath(Draw.cloudPath(cx + size * 0.06f, cy + size * 0.06f, size * 0.62f), cloud)
            }
            3, 45, 48 -> {
                c.drawPath(Draw.cloudPath(cx - size * 0.12f, cy - size * 0.1f, size * 0.5f), dark)
                c.drawPath(Draw.cloudPath(cx + size * 0.05f, cy + size * 0.04f, size * 0.66f), cloud)
                if (code != 3) {
                    val fog = Paint(cloud).apply { strokeWidth = size * 0.05f; strokeCap = Paint.Cap.ROUND; alpha = 180 }
                    c.drawLine(cx - size * 0.3f, cy + size * 0.36f, cx + size * 0.3f, cy + size * 0.36f, fog)
                }
            }
            in 71..86 -> {
                c.drawPath(Draw.cloudPath(cx, cy - size * 0.12f, size * 0.7f), cloud)
                for (i in -1..1) c.drawCircle(cx + i * size * 0.18f, cy + size * 0.3f + abs(i) * size * 0.04f, size * 0.045f, cloud)
            }
            95, 96, 99 -> {
                c.drawPath(Draw.cloudPath(cx, cy - size * 0.12f, size * 0.7f), dark)
                c.drawPath(Draw.boltPath(cx, cy + size * 0.25f, size * 0.32f), Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFD166") })
            }
            else -> {
                c.drawPath(Draw.cloudPath(cx, cy - size * 0.12f, size * 0.7f), cloud)
                val drop = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#8EC5FF"); strokeWidth = size * 0.045f; strokeCap = Paint.Cap.ROUND }
                for (i in -1..1) {
                    val x = cx + i * size * 0.18f
                    c.drawLine(x, cy + size * 0.2f, x - size * 0.05f, cy + size * 0.34f, drop)
                }
            }
        }
    }
}
