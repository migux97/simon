package com.migux97.simon.wallpaper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.migux97.simon.data.LiveConfig
import com.migux97.simon.data.SceneType
import com.migux97.simon.widget.Draw
import com.migux97.simon.widget.WidgetRenderer
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** Una escena animada. Se usa igual en el fondo real y en la vista previa. */
abstract class Scene(val config: LiveConfig) {
    protected var w = 1f
    protected var h = 1f
    protected var t = 0f
    protected val rnd = Random(config.id.hashCode())
    private val clockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val timeFmt = SimpleDateFormat("HH:mm", WidgetRenderer.locale)
    private val dateFmt = SimpleDateFormat("EEEE d 'de' MMMM", WidgetRenderer.locale)

    /** Cantidad de partículas ajustada por la densidad elegida. */
    protected fun count(base: Int) = max(1, (base * config.density).toInt())

    fun resize(width: Int, height: Int) {
        val changed = width.toFloat() != w || height.toFloat() != h
        w = width.toFloat().coerceAtLeast(1f)
        h = height.toFloat().coerceAtLeast(1f)
        if (changed) setup()
    }

    fun step(dt: Float) {
        val d = (dt * config.speed).coerceIn(0f, 0.1f)
        t += d
        update(d)
    }

    fun render(canvas: Canvas) {
        draw(canvas)
        if (config.showClock) drawClock(canvas)
    }

    protected abstract fun setup()
    protected abstract fun update(dt: Float)
    protected abstract fun draw(canvas: Canvas)

    private fun drawClock(canvas: Canvas) {
        val now = Date()
        val tf = Draw.typeface(config.clockFont)
        clockPaint.typeface = tf
        clockPaint.color = config.clockColor
        clockPaint.textSize = min(w, h) * 0.26f
        clockPaint.setShadowLayer(min(w, h) * 0.02f, 0f, 0f, Draw.withAlpha(Color.BLACK, 90))
        canvas.drawText(timeFmt.format(now), w / 2, h * 0.27f, clockPaint)
        datePaint.typeface = tf
        datePaint.color = Draw.withAlpha(config.clockColor, 220)
        datePaint.textSize = min(w, h) * 0.05f
        datePaint.setShadowLayer(min(w, h) * 0.015f, 0f, 0f, Draw.withAlpha(Color.BLACK, 90))
        canvas.drawText(WidgetRenderer.capitalize(dateFmt.format(now)), w / 2, h * 0.27f + min(w, h) * 0.09f, datePaint)
    }

    protected fun verticalGradient(top: Int, bottom: Int): Paint = Paint().apply {
        shader = LinearGradient(0f, 0f, 0f, h, top, bottom, Shader.TileMode.CLAMP)
    }

    companion object {
        fun create(config: LiveConfig): Scene = when (config.scene) {
            SceneType.AURORA -> AuroraScene(config)
            SceneType.GRADIENT_FLOW -> FlowScene(config)
            SceneType.STARFIELD -> StarfieldScene(config)
            SceneType.BUBBLES -> BubbleScene(config)
            SceneType.RAIN -> RainScene(config)
            SceneType.SNOW -> SnowScene(config)
            SceneType.FIREFLIES -> FireflyScene(config)
            SceneType.WAVES -> WaveScene(config)
            SceneType.CONFETTI -> ConfettiScene(config)
            SceneType.HEARTS -> HeartScene(config)
            SceneType.MATRIX -> MatrixScene(config)
            SceneType.NEON_GRID -> NeonGridScene(config)
            SceneType.VIDEO -> FlowScene(config)
        }
    }
}

// ---------------------------------------------------------------------- aurora

private class AuroraScene(cfg: LiveConfig) : Scene(cfg) {
    private lateinit var bg: Paint
    private val band = Paint(Paint.ANTI_ALIAS_FLAG)
    private val star = Paint(Paint.ANTI_ALIAS_FLAG)
    private var stars = FloatArray(0)
    private val path = Path()

    override fun setup() {
        bg = verticalGradient(config.color1, Draw.mix(config.color1, Color.BLACK, 0.5f))
        stars = FloatArray(count(70) * 3) { i -> when (i % 3) { 0 -> rnd.nextFloat() * w; 1 -> rnd.nextFloat() * h * 0.7f; else -> rnd.nextFloat() } }
    }

    override fun update(dt: Float) = Unit

    override fun draw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, w, h, bg)
        for (i in stars.indices step 3) {
            star.color = Draw.withAlpha(Color.WHITE, (80 + 150 * (0.5f + 0.5f * sin(t * 2 + stars[i + 2] * 20))).toInt())
            canvas.drawCircle(stars[i], stars[i + 1], 1.2f + stars[i + 2] * 1.8f, star)
        }
        for (layer in 0 until 3) {
            val color = if (layer % 2 == 0) config.color2 else config.color3
            val baseY = h * (0.28f + layer * 0.1f)
            val amp = h * (0.05f + layer * 0.015f)
            val height = h * (0.22f + layer * 0.04f)
            path.reset()
            path.moveTo(0f, baseY + height)
            var x = 0f
            val stepX = w / 40f
            while (x <= w + stepX) {
                val y = baseY + sin(x / w * 2 * PI * 1.2 + t * (0.5 + layer * 0.25) + layer).toFloat() * amp +
                    sin(x / w * 2 * PI * 3.1 - t * 0.7).toFloat() * amp * 0.35f
                path.lineTo(x, y)
                x += stepX
            }
            path.lineTo(w, baseY + height)
            path.close()
            val pulse = 0.55f + 0.45f * sin(t * 0.8f + layer * 1.7f)
            band.shader = LinearGradient(
                0f, baseY - amp, 0f, baseY + height,
                intArrayOf(Draw.withAlpha(color, (170 * pulse).toInt()), Draw.withAlpha(color, (60 * pulse).toInt()), Color.TRANSPARENT),
                floatArrayOf(0f, 0.4f, 1f), Shader.TileMode.CLAMP
            )
            canvas.drawPath(path, band)
        }
    }
}

// ------------------------------------------------------------- degradado vivo

private class FlowScene(cfg: LiveConfig) : Scene(cfg) {
    private val blob = Paint(Paint.ANTI_ALIAS_FLAG)
    private lateinit var bg: Paint
    override fun setup() {
        bg = verticalGradient(config.color1, Draw.mix(config.color1, config.color2, 0.4f))
    }

    override fun update(dt: Float) = Unit

    override fun draw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, w, h, bg)
        val colors = listOf(config.color2, config.color3, Draw.mix(config.color2, config.color3, 0.5f), config.color1)
        val r = max(w, h) * 0.7f
        for (i in 0 until 4) {
            val cx = w * (0.5f + 0.45f * sin(t * (0.21f + i * 0.07f) + i * 1.9f))
            val cy = h * (0.5f + 0.45f * cos(t * (0.17f + i * 0.05f) + i * 2.3f))
            blob.shader = RadialGradient(cx, cy, r, Draw.withAlpha(colors[i], 200), Draw.withAlpha(colors[i], 0), Shader.TileMode.CLAMP)
            canvas.drawCircle(cx, cy, r, blob)
        }
    }
}

// ------------------------------------------------------------------- estrellas

private class StarfieldScene(cfg: LiveConfig) : Scene(cfg) {
    private class Star(var x: Float, var y: Float, var z: Float, val tint: Boolean)
    private var stars = listOf<Star>()
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private lateinit var bg: Paint

    private fun newStar(randomZ: Boolean) = Star((rnd.nextFloat() - 0.5f) * 2f, (rnd.nextFloat() - 0.5f) * 2f, if (randomZ) rnd.nextFloat() else 1f, rnd.nextFloat() < 0.25f)

    override fun setup() {
        bg = Paint().apply { shader = RadialGradient(w / 2, h / 2, max(w, h) * 0.7f, Draw.mix(config.color1, config.color3, 0.25f), config.color1, Shader.TileMode.CLAMP) }
        stars = List(count(220)) { newStar(true) }
    }

    override fun update(dt: Float) {
        stars.forEach { s ->
            s.z -= dt * 0.25f
            if (s.z <= 0.02f) {
                s.x = (rnd.nextFloat() - 0.5f) * 2f; s.y = (rnd.nextFloat() - 0.5f) * 2f; s.z = 1f
            }
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, w, h, bg)
        val cx = w / 2; val cy = h / 2; val scale = max(w, h) * 0.5f
        stars.forEach { s ->
            val px = cx + s.x / s.z * scale * 0.5f
            val py = cy + s.y / s.z * scale * 0.5f
            if (px < 0 || px > w || py < 0 || py > h) return@forEach
            val tz = s.z + 0.03f
            val tx = cx + s.x / tz * scale * 0.5f
            val ty = cy + s.y / tz * scale * 0.5f
            val bright = (1f - s.z).coerceIn(0f, 1f)
            p.color = Draw.withAlpha(if (s.tint) config.color3 else config.color2, (bright * 255).toInt())
            p.strokeWidth = 1f + bright * 4f
            canvas.drawLine(tx, ty, px, py, p)
        }
    }
}

// -------------------------------------------------------------------- burbujas

private class BubbleScene(cfg: LiveConfig) : Scene(cfg) {
    private class Bubble(var x: Float, var y: Float, var r: Float, var speed: Float, var phase: Float)
    private var bubbles = listOf<Bubble>()
    private lateinit var bg: Paint
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shine = Paint(Paint.ANTI_ALIAS_FLAG)

    private fun reset(b: Bubble, anywhere: Boolean) {
        b.r = min(w, h) * (0.02f + rnd.nextFloat() * 0.07f)
        b.x = rnd.nextFloat() * w
        b.y = if (anywhere) rnd.nextFloat() * h else h + b.r
        b.speed = h * (0.05f + rnd.nextFloat() * 0.1f)
        b.phase = rnd.nextFloat() * 6.28f
    }

    override fun setup() {
        bg = verticalGradient(config.color1, Draw.mix(config.color1, Color.BLACK, 0.35f))
        bubbles = List(count(40)) { Bubble(0f, 0f, 0f, 0f, 0f).also { b -> reset(b, true) } }
    }

    override fun update(dt: Float) {
        bubbles.forEach { b ->
            b.y -= b.speed * dt
            if (b.y < -b.r) reset(b, false)
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, w, h, bg)
        bubbles.forEach { b ->
            val x = b.x + sin(t * 1.3f + b.phase) * b.r * 0.6f
            fill.color = Draw.withAlpha(config.color3, 45)
            canvas.drawCircle(x, b.y, b.r, fill)
            ring.color = Draw.withAlpha(config.color2, 150)
            ring.strokeWidth = max(1.5f, b.r * 0.07f)
            canvas.drawCircle(x, b.y, b.r, ring)
            shine.color = Draw.withAlpha(Color.WHITE, 170)
            canvas.drawCircle(x - b.r * 0.35f, b.y - b.r * 0.35f, b.r * 0.18f, shine)
        }
    }
}

// --------------------------------------------------------------------- lluvia

private class RainScene(cfg: LiveConfig) : Scene(cfg) {
    private class Drop(var x: Float, var y: Float, var len: Float, var speed: Float)
    private class Splash(var x: Float, var y: Float, var life: Float)
    private var drops = listOf<Drop>()
    private val splashes = ArrayList<Splash>()
    private lateinit var bg: Paint
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val oval = RectF()

    private fun reset(d: Drop, anywhere: Boolean) {
        d.x = rnd.nextFloat() * (w * 1.2f)
        d.y = if (anywhere) rnd.nextFloat() * h else -rnd.nextFloat() * h * 0.2f
        d.len = h * (0.02f + rnd.nextFloat() * 0.04f)
        d.speed = h * (0.9f + rnd.nextFloat() * 0.8f)
    }

    override fun setup() {
        bg = verticalGradient(config.color1, config.color3)
        drops = List(count(160)) { Drop(0f, 0f, 0f, 0f).also { reset(it, true) } }
        splashes.clear()
    }

    override fun update(dt: Float) {
        drops.forEach { d ->
            d.y += d.speed * dt
            d.x -= d.speed * dt * 0.15f
            if (d.y > h) {
                if (splashes.size < 60 && rnd.nextFloat() < 0.5f) splashes += Splash(d.x, h * (0.88f + rnd.nextFloat() * 0.12f), 1f)
                reset(d, false)
            }
        }
        val it = splashes.iterator()
        while (it.hasNext()) {
            val s = it.next()
            s.life -= dt * 2.5f
            if (s.life <= 0f) it.remove()
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, w, h, bg)
        p.color = Draw.withAlpha(config.color2, 150)
        p.strokeWidth = max(1.5f, min(w, h) * 0.003f)
        drops.forEach { d -> canvas.drawLine(d.x, d.y, d.x + d.len * 0.15f, d.y - d.len, p) }
        sp.strokeWidth = p.strokeWidth
        splashes.forEach { s ->
            val r = (1f - s.life) * min(w, h) * 0.03f
            sp.color = Draw.withAlpha(config.color2, (s.life * 160).toInt())
            oval.set(s.x - r, s.y - r * 0.3f, s.x + r, s.y + r * 0.3f)
            canvas.drawOval(oval, sp)
        }
    }
}

// ---------------------------------------------------------------------- nieve

private class SnowScene(cfg: LiveConfig) : Scene(cfg) {
    private class Flake(var x: Float, var y: Float, var r: Float, var speed: Float, var phase: Float)
    private var flakes = listOf<Flake>()
    private lateinit var bg: Paint
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    private fun reset(f: Flake, anywhere: Boolean) {
        f.r = min(w, h) * (0.003f + rnd.nextFloat() * 0.01f)
        f.x = rnd.nextFloat() * w
        f.y = if (anywhere) rnd.nextFloat() * h else -f.r
        f.speed = h * 0.04f + f.r * 6f
        f.phase = rnd.nextFloat() * 6.28f
    }

    override fun setup() {
        bg = verticalGradient(config.color1, config.color3)
        flakes = List(count(150)) { Flake(0f, 0f, 0f, 0f, 0f).also { reset(it, true) } }
    }

    override fun update(dt: Float) {
        flakes.forEach { f ->
            f.y += f.speed * dt
            f.x += sin(t + f.phase) * f.r * dt * 8f
            if (f.y > h + f.r) reset(f, false)
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, w, h, bg)
        flakes.forEach { f ->
            p.color = Draw.withAlpha(config.color2, (120 + f.r / (min(w, h) * 0.013f) * 135).toInt().coerceAtMost(255))
            canvas.drawCircle(f.x, f.y, f.r, p)
        }
    }
}

// ---------------------------------------------------------------- luciérnagas

private class FireflyScene(cfg: LiveConfig) : Scene(cfg) {
    private class Fly(var x: Float, var y: Float, var vx: Float, var vy: Float, val phase: Float, val size: Float)
    private var flies = listOf<Fly>()
    private lateinit var bg: Paint
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val core = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun setup() {
        bg = verticalGradient(config.color1, config.color3)
        val s = min(w, h)
        flies = List(count(45)) {
            Fly(rnd.nextFloat() * w, rnd.nextFloat() * h, (rnd.nextFloat() - 0.5f) * s * 0.05f, (rnd.nextFloat() - 0.5f) * s * 0.05f, rnd.nextFloat() * 6.28f, s * (0.004f + rnd.nextFloat() * 0.006f))
        }
    }

    override fun update(dt: Float) {
        val s = min(w, h)
        flies.forEach { f ->
            f.vx += (rnd.nextFloat() - 0.5f) * s * 0.08f * dt
            f.vy += (rnd.nextFloat() - 0.5f) * s * 0.08f * dt
            val maxV = s * 0.06f
            f.vx = f.vx.coerceIn(-maxV, maxV); f.vy = f.vy.coerceIn(-maxV, maxV)
            f.x += f.vx * dt; f.y += f.vy * dt
            if (f.x < 0) f.x += w; if (f.x > w) f.x -= w
            if (f.y < 0) f.y += h; if (f.y > h) f.y -= h
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, w, h, bg)
        flies.forEach { f ->
            val pulse = 0.35f + 0.65f * (0.5f + 0.5f * sin(t * 2.2f + f.phase))
            val gr = f.size * 7f
            glow.shader = RadialGradient(f.x, f.y, gr, Draw.withAlpha(config.color2, (150 * pulse).toInt()), Draw.withAlpha(config.color2, 0), Shader.TileMode.CLAMP)
            canvas.drawCircle(f.x, f.y, gr, glow)
            core.color = Draw.withAlpha(Draw.mix(config.color2, Color.WHITE, 0.6f), (255 * pulse).toInt())
            canvas.drawCircle(f.x, f.y, f.size, core)
        }
    }
}

// ----------------------------------------------------------------------- olas

private class WaveScene(cfg: LiveConfig) : Scene(cfg) {
    private lateinit var sky: Paint
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val sun = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun setup() {
        sky = verticalGradient(Draw.mix(config.color1, Color.WHITE, 0.25f), config.color1)
    }

    override fun update(dt: Float) = Unit

    override fun draw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, w, h, sky)
        sun.shader = RadialGradient(w * 0.7f, h * 0.3f, min(w, h) * 0.35f, Draw.withAlpha(Color.WHITE, 200), Draw.withAlpha(Color.WHITE, 0), Shader.TileMode.CLAMP)
        canvas.drawCircle(w * 0.7f, h * 0.3f, min(w, h) * 0.35f, sun)
        val layers = 5
        for (i in 0 until layers) {
            val k = i / (layers - 1f)
            val baseY = h * (0.5f + k * 0.38f)
            val amp = h * (0.012f + k * 0.018f)
            val speed = 0.4f + k * 0.6f
            path.reset()
            path.moveTo(0f, h)
            var x = 0f
            val stepX = w / 48f
            while (x <= w + stepX) {
                val y = baseY + sin(x / w * 2 * PI * (1.5 + i * 0.3) + t * speed * (if (i % 2 == 0) 1 else -1)).toFloat() * amp
                path.lineTo(x, y)
                x += stepX
            }
            path.lineTo(w, h)
            path.close()
            p.color = Draw.withAlpha(Draw.mix(config.color2, config.color3, k), 200 + (55 * k).toInt())
            canvas.drawPath(path, p)
        }
    }
}

// -------------------------------------------------------------------- confeti

private class ConfettiScene(cfg: LiveConfig) : Scene(cfg) {
    private class Piece(var x: Float, var y: Float, var vy: Float, var rot: Float, var vr: Float, val color: Int, val wd: Float, val ht: Float, val phase: Float)
    private var pieces = listOf<Piece>()
    private lateinit var bg: Paint
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    private fun palette() = intArrayOf(config.color2, config.color3, Color.parseColor("#4CC9F0"), Color.parseColor("#80ED99"), Color.WHITE)

    private fun make(anywhere: Boolean): Piece {
        val s = min(w, h)
        val cols = palette()
        return Piece(
            rnd.nextFloat() * w, if (anywhere) rnd.nextFloat() * h else -s * 0.05f,
            h * (0.08f + rnd.nextFloat() * 0.12f), rnd.nextFloat() * 360f, (rnd.nextFloat() - 0.5f) * 360f,
            cols[rnd.nextInt(cols.size)], s * (0.012f + rnd.nextFloat() * 0.012f), s * (0.02f + rnd.nextFloat() * 0.02f), rnd.nextFloat() * 6.28f
        )
    }

    override fun setup() {
        bg = verticalGradient(config.color1, Draw.mix(config.color1, Color.BLACK, 0.4f))
        pieces = List(count(90)) { make(true) }
    }

    override fun update(dt: Float) {
        pieces = pieces.map { pc ->
            pc.y += pc.vy * dt
            pc.rot += pc.vr * dt
            if (pc.y > h + pc.ht) make(false) else pc
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, w, h, bg)
        pieces.forEach { pc ->
            val x = pc.x + sin(t * 1.5f + pc.phase) * pc.wd * 2
            val flip = cos(t * 4f + pc.phase)
            p.color = Draw.mix(pc.color, Color.BLACK, (1 - kotlin.math.abs(flip)) * 0.3f)
            canvas.save()
            canvas.rotate(pc.rot, x, pc.y)
            rect.set(x - pc.wd / 2, pc.y - pc.ht / 2 * kotlin.math.abs(flip), x + pc.wd / 2, pc.y + pc.ht / 2 * kotlin.math.abs(flip))
            canvas.drawRect(rect, p)
            canvas.restore()
        }
    }
}

// ------------------------------------------------------------------ corazones

private class HeartScene(cfg: LiveConfig) : Scene(cfg) {
    private class Heart(var x: Float, var y: Float, var size: Float, var speed: Float, val phase: Float, val useAlt: Boolean)
    private var hearts = listOf<Heart>()
    private lateinit var bg: Paint
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    private fun make(anywhere: Boolean): Heart {
        val s = min(w, h)
        val size = s * (0.04f + rnd.nextFloat() * 0.09f)
        return Heart(rnd.nextFloat() * w, if (anywhere) rnd.nextFloat() * h else h + size, size, h * (0.04f + rnd.nextFloat() * 0.07f), rnd.nextFloat() * 6.28f, rnd.nextBoolean())
    }

    override fun setup() {
        bg = verticalGradient(config.color1, Draw.mix(config.color1, config.color3, 0.6f))
        hearts = List(count(30)) { make(true) }
    }

    override fun update(dt: Float) {
        hearts = hearts.map { hh ->
            hh.y -= hh.speed * dt
            if (hh.y < -hh.size) make(false) else hh
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, w, h, bg)
        hearts.forEach { hh ->
            val x = hh.x + sin(t + hh.phase) * hh.size * 0.5f
            val beat = 1f + 0.06f * sin(t * 5f + hh.phase)
            p.color = Draw.withAlpha(if (hh.useAlt) config.color3 else config.color2, 140 + (hh.size / min(w, h) * 800).toInt().coerceAtMost(110))
            canvas.drawPath(Draw.heartPath(x, hh.y, hh.size * beat), p)
        }
    }
}

// --------------------------------------------------------------------- código

private class MatrixScene(cfg: LiveConfig) : Scene(cfg) {
    private val glyphs = "01ABCDEF23456789<>/=+*#%&SIMON".toCharArray()
    private var columns = 0
    private var cell = 0f
    private var heads = FloatArray(0)
    private var speeds = FloatArray(0)
    private var chars = CharArray(0)
    private var rows = 0
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = android.graphics.Typeface.MONOSPACE; textAlign = Paint.Align.CENTER }
    private val bgPaint = Paint()
    private var swapTimer = 0f

    override fun setup() {
        cell = min(w, h) / (22f * sqrt(config.density.coerceAtLeast(0.3f)))
        columns = (w / cell).toInt() + 1
        rows = (h / cell).toInt() + 2
        heads = FloatArray(columns) { rnd.nextFloat() * rows }
        speeds = FloatArray(columns) { 6f + rnd.nextFloat() * 12f }
        chars = CharArray(columns * rows) { glyphs[rnd.nextInt(glyphs.size)] }
        p.textSize = cell * 0.9f
        bgPaint.color = config.color1
    }

    override fun update(dt: Float) {
        for (i in 0 until columns) {
            heads[i] += speeds[i] * dt
            if (heads[i] - 18 > rows) { heads[i] = -rnd.nextFloat() * 10; speeds[i] = 6f + rnd.nextFloat() * 12f }
        }
        swapTimer += dt
        if (swapTimer > 0.08f) {
            swapTimer = 0f
            repeat(columns) { chars[rnd.nextInt(chars.size)] = glyphs[rnd.nextInt(glyphs.size)] }
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, w, h, bgPaint)
        for (c in 0 until columns) {
            val head = heads[c].toInt()
            for (k in 0 until 18) {
                val r = head - k
                if (r < 0 || r >= rows) continue
                p.color = if (k == 0) Draw.mix(config.color2, Color.WHITE, 0.7f)
                else Draw.withAlpha(Draw.mix(config.color2, config.color3, k / 18f), (255 * (1f - k / 18f)).toInt())
                canvas.drawText(chars[c * rows + r].toString(), c * cell + cell / 2, r * cell + cell, p)
            }
        }
    }
}

// ----------------------------------------------------------------- neón retro

private class NeonGridScene(cfg: LiveConfig) : Scene(cfg) {
    private lateinit var sky: Paint
    private val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val ground = Paint()
    private val clip = Path()

    override fun setup() {
        sky = verticalGradient(config.color1, Draw.mix(config.color1, config.color2, 0.35f))
        ground.color = Draw.mix(config.color1, Color.BLACK, 0.3f)
    }

    override fun update(dt: Float) = Unit

    override fun draw(canvas: Canvas) {
        val horizon = h * 0.58f
        canvas.drawRect(0f, 0f, w, horizon, sky)
        // Sol con franjas
        val r = min(w, h) * 0.3f
        val cx = w / 2; val cy = horizon - r * 0.25f
        sunPaint.shader = LinearGradient(0f, cy - r, 0f, cy + r, config.color2, config.color3, Shader.TileMode.CLAMP)
        canvas.save()
        clip.reset()
        clip.addCircle(cx, cy, r, Path.Direction.CW)
        canvas.clipPath(clip)
        canvas.clipRect(0f, 0f, w, horizon)
        var y = cy - r
        var band = 0
        while (y < cy + r) {
            val gap = if (y > cy) r * 0.02f * (band - 6).coerceAtLeast(0) else 0f
            canvas.drawRect(cx - r, y, cx + r, y + r * 0.1f - gap, sunPaint)
            y += r * 0.1f
            band++
        }
        canvas.restore()
        // Suelo y grilla en perspectiva
        canvas.drawRect(0f, horizon, w, h, ground)
        line.color = config.color3
        line.strokeWidth = max(1.5f, min(w, h) * 0.004f)
        val vanishX = w / 2
        for (i in -12..12) {
            val bx = vanishX + i * w * 0.18f
            canvas.drawLine(vanishX + i * w * 0.012f, horizon, bx, h, line)
        }
        val phase = (t * 0.6f) % 1f
        for (i in 0 until 14) {
            val k = (i + phase) / 14f
            val yy = horizon + (h - horizon) * k * k
            line.alpha = (60 + 195 * k).toInt()
            canvas.drawLine(0f, yy, w, yy, line)
        }
        line.alpha = 255
        // Brillo del horizonte
        val glow = Paint().apply { shader = LinearGradient(0f, horizon - h * 0.04f, 0f, horizon + h * 0.04f, Color.TRANSPARENT, Draw.withAlpha(config.color3, 0), Shader.TileMode.CLAMP) }
        glow.shader = LinearGradient(0f, horizon - h * 0.03f, 0f, horizon + h * 0.01f, Color.TRANSPARENT, Draw.withAlpha(config.color2, 140), Shader.TileMode.MIRROR)
        canvas.drawRect(0f, horizon - h * 0.03f, w, horizon + h * 0.01f, glow)
    }
}
