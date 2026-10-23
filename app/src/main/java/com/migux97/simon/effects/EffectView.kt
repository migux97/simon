package com.migux97.simon.effects

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.os.SystemClock
import android.view.View
import com.migux97.simon.data.EffectConfig
import com.migux97.simon.data.EffectStyle
import com.migux97.simon.data.Lookup
import com.migux97.simon.data.MediaLibrary
import com.migux97.simon.data.StudioStore
import com.migux97.simon.wallpaper.Scene
import com.migux97.simon.widget.Draw
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** Lo que se muestra junto al efecto (ícono y textos de la notificación). */
data class EffectPayload(
    val icon: Bitmap? = null,
    val title: String = "",
    val text: String = "",
    val color: Int? = null
)

/** Vista que dibuja un efecto a pantalla completa y avisa cuando termina. */
@SuppressLint("ViewConstructor")
class EffectView(
    context: Context,
    private val config: EffectConfig,
    private val payload: EffectPayload,
    private val onFinished: () -> Unit
) : View(context) {

    private val start = SystemClock.uptimeMillis()
    private val duration = config.durationMs.coerceIn(600, 15000).toLong()
    private val rnd = Random(start)
    private val c1 = if (config.useAppColor && payload.color != null) payload.color else config.color1
    private val c2 = config.color2
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val frames: List<Bitmap> = if (config.style == EffectStyle.USER_ANIMATION) MediaLibrary.loadFrames(config.animationId, 900) else emptyList()
    private val scene: Scene? = if (config.style == EffectStyle.SCENE) config.liveId?.let { Lookup.live(it) }?.let { Scene.create(it) } else null
    private var lastFrame = start
    private var finished = false

    private class Particle(var x: Float, var y: Float, var vx: Float, var vy: Float, val color: Int, val size: Float, var rot: Float, val vr: Float, val delay: Float)
    private var particles: List<Particle> = emptyList()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        scene?.resize(w, h)
        val s = min(w, h).toFloat()
        val colors = intArrayOf(c1, c2, Color.WHITE, Draw.mix(c1, c2, 0.5f), Color.parseColor("#FFD166"))
        particles = when (config.style) {
            EffectStyle.CONFETTI -> List(120) {
                Particle(rnd.nextFloat() * w, -rnd.nextFloat() * h * 0.3f, (rnd.nextFloat() - 0.5f) * s * 0.2f, s * (0.5f + rnd.nextFloat() * 0.7f), colors[rnd.nextInt(colors.size)], s * (0.012f + rnd.nextFloat() * 0.014f), rnd.nextFloat() * 360f, (rnd.nextFloat() - 0.5f) * 720f, rnd.nextFloat() * 0.3f)
            }
            EffectStyle.HEARTS -> List(40) {
                Particle(rnd.nextFloat() * w, h + rnd.nextFloat() * h * 0.2f, (rnd.nextFloat() - 0.5f) * s * 0.05f, -s * (0.35f + rnd.nextFloat() * 0.5f), if (rnd.nextBoolean()) c1 else c2, s * (0.04f + rnd.nextFloat() * 0.06f), 0f, 0f, rnd.nextFloat() * 0.35f)
            }
            EffectStyle.SPARKLES -> List(50) {
                Particle(rnd.nextFloat() * w, rnd.nextFloat() * h, 0f, 0f, colors[rnd.nextInt(colors.size)], s * (0.015f + rnd.nextFloat() * 0.03f), 0f, 0f, rnd.nextFloat() * 0.7f)
            }
            EffectStyle.FIREWORK -> List(5) { b ->
                Particle(w * (0.2f + rnd.nextFloat() * 0.6f), h * (0.15f + rnd.nextFloat() * 0.45f), 0f, 0f, colors[b % colors.size], s * 0.3f, 0f, 0f, b * 0.14f)
            }
            else -> emptyList()
        }
    }

    override fun onDraw(canvas: Canvas) {
        val now = SystemClock.uptimeMillis()
        val elapsed = now - start
        val p = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
        val dt = ((now - lastFrame) / 1000f).coerceIn(0f, 0.05f)
        lastFrame = now
        // Entra rápido, se queda y sale suave.
        val env = when {
            p < 0.12f -> p / 0.12f
            p > 0.75f -> (1f - p) / 0.25f
            else -> 1f
        }.coerceIn(0f, 1f)
        val w = width.toFloat(); val h = height.toFloat(); val s = min(w, h)

        when (config.style) {
            EffectStyle.EDGE_LIGHT -> edgeLight(canvas, w, h, elapsed, env)
            EffectStyle.RIPPLE -> {
                paint.shader = null
                paint.style = Paint.Style.STROKE
                for (i in 0 until 4) {
                    val k = ((p * 1.6f) - i * 0.18f).coerceIn(0f, 1f)
                    if (k <= 0f) continue
                    paint.strokeWidth = s * 0.02f * config.thickness * (1f - k)
                    paint.color = Draw.withAlpha(if (i % 2 == 0) c1 else c2, (255 * (1f - k) * env).toInt())
                    canvas.drawCircle(w / 2, h * 0.45f, max(w, h) * 0.8f * k, paint)
                }
                paint.style = Paint.Style.FILL
            }
            EffectStyle.CONFETTI -> particles.forEach { pt ->
                val t = (p - pt.delay).coerceAtLeast(0f) * duration / 1000f
                val x = pt.x + pt.vx * t + sin(t * 3 + pt.rot) * s * 0.02f
                val y = pt.y + pt.vy * t
                paint.shader = null
                paint.color = Draw.withAlpha(pt.color, (255 * env).toInt())
                canvas.save()
                canvas.rotate(pt.rot + pt.vr * t, x, y)
                canvas.drawRect(x - pt.size / 2, y - pt.size, x + pt.size / 2, y + pt.size, paint)
                canvas.restore()
            }
            EffectStyle.HEARTS -> particles.forEach { pt ->
                val t = (p - pt.delay).coerceAtLeast(0f) * duration / 1000f
                val x = pt.x + sin(t * 2 + pt.size) * s * 0.04f
                val y = pt.y + pt.vy * t
                paint.shader = null
                paint.color = Draw.withAlpha(pt.color, (220 * env).toInt())
                canvas.drawPath(Draw.heartPath(x, y, pt.size * (1f + 0.1f * sin(t * 8))), paint)
            }
            EffectStyle.SPARKLES -> particles.forEach { pt ->
                val local = ((p - pt.delay) / 0.3f).coerceIn(0f, 1f)
                if (local <= 0f || local >= 1f) return@forEach
                val k = sin(local * PI).toFloat()
                paint.shader = null
                paint.color = Draw.withAlpha(pt.color, (255 * k * env).toInt())
                canvas.drawPath(Draw.sparklePath(pt.x, pt.y, pt.size * (0.5f + k)), paint)
            }
            EffectStyle.FIREWORK -> particles.forEach { b ->
                val local = ((p - b.delay) / 0.55f).coerceIn(0f, 1f)
                if (local <= 0f || local >= 1f) return@forEach
                val r = b.size * local
                val fade = 1f - local
                for (i in 0 until 24) {
                    val a = i / 24.0 * 2 * PI
                    val x = b.x + (cos(a) * r).toFloat()
                    val y = b.y + (sin(a) * r).toFloat() + local * local * s * 0.08f
                    paint.shader = null
                    paint.color = Draw.withAlpha(if (i % 2 == 0) b.color else Color.WHITE, (255 * fade * env).toInt())
                    canvas.drawCircle(x, y, s * 0.008f * (1f + fade), paint)
                }
            }
            EffectStyle.PULSE_ICON -> {
                val beat = 1f + 0.12f * sin(elapsed / 1000f * 2 * PI * 1.5).toFloat()
                val r = s * 0.2f * beat
                paint.shader = RadialGradient(w / 2, h * 0.42f, r * 2.2f, Draw.withAlpha(c1, (180 * env).toInt()), Draw.withAlpha(c1, 0), Shader.TileMode.CLAMP)
                canvas.drawCircle(w / 2, h * 0.42f, r * 2.2f, paint)
                paint.shader = null
                payload.icon?.let { icon ->
                    paint.alpha = (255 * env).toInt()
                    canvas.drawBitmap(icon, null, RectF(w / 2 - r, h * 0.42f - r, w / 2 + r, h * 0.42f + r), paint)
                    paint.alpha = 255
                }
            }
            EffectStyle.CURTAIN -> {
                val rise = (p / 0.35f).coerceIn(0f, 1f)
                val top = h * (1f - rise)
                paint.shader = LinearGradient(0f, top, 0f, h, Draw.withAlpha(c1, (230 * env).toInt()), Draw.withAlpha(c2, (230 * env).toInt()), Shader.TileMode.CLAMP)
                canvas.drawRect(0f, top, w, h, paint)
                paint.shader = null
                // Onda en el borde superior
                paint.color = Draw.withAlpha(Color.WHITE, (90 * env).toInt())
                var x = 0f
                while (x < w) {
                    val y = top + sin(x / w * 6 * PI + elapsed / 300.0).toFloat() * s * 0.015f
                    canvas.drawCircle(x, y, s * 0.006f, paint)
                    x += s * 0.02f
                }
            }
            EffectStyle.USER_ANIMATION -> {
                val info = StudioStore.animation(config.animationId)
                frames.getOrNull(MediaLibrary.frameIndex(info, frames.size, elapsed))?.let { f ->
                    val scale = min(w * 0.8f / f.width, h * 0.6f / f.height)
                    val dw = f.width * scale; val dh = f.height * scale
                    paint.shader = null
                    paint.alpha = (255 * env).toInt()
                    canvas.drawBitmap(f, null, RectF(w / 2 - dw / 2, h / 2 - dh / 2, w / 2 + dw / 2, h / 2 + dh / 2), paint)
                    paint.alpha = 255
                }
            }
            EffectStyle.SCENE -> scene?.let { sc ->
                sc.step(dt)
                canvas.saveLayerAlpha(0f, 0f, w, h, (255 * env).toInt())
                sc.render(canvas)
                canvas.restore()
            }
        }

        if (config.showIcon || config.showText) drawCard(canvas, w, h, env)

        if (p >= 1f) {
            if (!finished) { finished = true; post { onFinished() } }
        } else postInvalidateOnAnimation()
    }

    private fun edgeLight(canvas: Canvas, w: Float, h: Float, elapsed: Long, env: Float) {
        val stroke = min(w, h) * 0.018f * config.thickness
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = stroke
        val sweep = SweepGradient(w / 2, h / 2, intArrayOf(c1, c2, c1, c2, c1), null)
        val m = android.graphics.Matrix()
        m.setRotate(elapsed / 6f % 360f, w / 2, h / 2)
        sweep.setLocalMatrix(m)
        paint.shader = sweep
        paint.alpha = (255 * env).toInt()
        val inset = stroke / 2
        val radius = min(w, h) * 0.09f
        // Brillo difuso + línea nítida
        paint.maskFilter = android.graphics.BlurMaskFilter(stroke * 1.6f, android.graphics.BlurMaskFilter.Blur.NORMAL)
        canvas.drawRoundRect(RectF(inset, inset, w - inset, h - inset), radius, radius, paint)
        paint.maskFilter = null
        canvas.drawRoundRect(RectF(inset, inset, w - inset, h - inset), radius, radius, paint)
        paint.style = Paint.Style.FILL
        paint.shader = null
        paint.alpha = 255
    }

    /** Tarjetita con ícono y texto, debajo del notch. */
    private fun drawCard(canvas: Canvas, w: Float, h: Float, env: Float) {
        val title = if (config.showText) (config.text.ifBlank { payload.title }) else ""
        val text = if (config.showText) payload.text else ""
        val icon = if (config.showIcon) payload.icon else null
        if (title.isBlank() && text.isBlank() && icon == null) return
        val s = min(w, h)
        val cardH = s * 0.16f
        val top = h * 0.06f + (1f - env) * -cardH
        val rect = RectF(w * 0.06f, top, w * 0.94f, top + cardH)
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.color = Draw.withAlpha(Color.parseColor("#1C1530"), (225 * env).toInt())
        paint.setShadowLayer(s * 0.02f, 0f, s * 0.006f, Draw.withAlpha(Color.BLACK, (90 * env).toInt()))
        canvas.drawRoundRect(rect, cardH / 2, cardH / 2, paint)
        paint.clearShadowLayer()
        var x = rect.left + cardH * 0.25f
        icon?.let {
            paint.alpha = (255 * env).toInt()
            canvas.drawBitmap(it, null, RectF(x, rect.top + cardH * 0.18f, x + cardH * 0.64f, rect.bottom - cardH * 0.18f), paint)
            paint.alpha = 255
            x += cardH * 0.8f
        }
        val maxW = rect.right - x - cardH * 0.3f
        val tp = Draw.textPaint(config.font, Draw.withAlpha(Color.WHITE, (255 * env).toInt()), cardH * 0.26f, true)
        if (title.isNotBlank()) Draw.fitText(canvas, title, x, rect.top + cardH * (if (text.isBlank()) 0.58f else 0.42f), tp, maxW, Paint.Align.LEFT)
        if (text.isNotBlank()) {
            val sp = Draw.textPaint(config.font, Draw.withAlpha(Color.WHITE, (200 * env).toInt()), cardH * 0.2f)
            val clipped = if (sp.measureText(text) > maxW) text.take((text.length * maxW / sp.measureText(text)).toInt().coerceAtLeast(1) - 1) + "…" else text
            canvas.drawText(clipped, x, rect.top + cardH * 0.72f, sp)
        }
    }
}
