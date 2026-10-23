package com.migux97.simon.custom

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.migux97.simon.data.BuiltinSticker
import com.migux97.simon.data.ImageStore
import com.migux97.simon.data.MediaLibrary
import com.migux97.simon.data.StickerAttach
import com.migux97.simon.data.StickerInfo
import com.migux97.simon.data.StickerKind
import com.migux97.simon.data.StudioStore
import com.migux97.simon.ui.components.LocalAnimationsActive
import com.migux97.simon.ui.components.MotionMath
import com.migux97.simon.ui.components.frameLoop
import com.migux97.simon.ui.components.motion
import com.migux97.simon.widget.Draw
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Dibuja stickers en un Canvas común (widgets del sistema, íconos, bitmaps). */
object StickerPainter {

    private fun builtinPath(b: BuiltinSticker, r: RectF): Path {
        val cx = r.centerX(); val cy = r.centerY(); val s = min(r.width(), r.height())
        return when (b) {
            BuiltinSticker.HEART -> Draw.heartPath(cx, cy + s * 0.04f, s)
            BuiltinSticker.STAR -> Draw.starPath(cx, cy + s * 0.04f, s / 2, s * 0.22f)
            BuiltinSticker.SPARKLE -> Draw.sparklePath(cx, cy, s / 2)
            BuiltinSticker.CROWN -> Path().apply {
                moveTo(cx - s * 0.45f, cy + s * 0.3f)
                lineTo(cx - s * 0.45f, cy - s * 0.25f)
                lineTo(cx - s * 0.22f, cy + s * 0.02f)
                lineTo(cx, cy - s * 0.38f)
                lineTo(cx + s * 0.22f, cy + s * 0.02f)
                lineTo(cx + s * 0.45f, cy - s * 0.25f)
                lineTo(cx + s * 0.45f, cy + s * 0.3f)
                close()
            }
            BuiltinSticker.FLOWER -> Path().apply {
                for (i in 0 until 6) {
                    val a = i * PI / 3
                    addCircle(cx + (cos(a) * s * 0.24f).toFloat(), cy + (sin(a) * s * 0.24f).toFloat(), s * 0.2f, Path.Direction.CW)
                }
                op(Path(), Path.Op.UNION)
            }
            BuiltinSticker.MOON -> Path().apply {
                addCircle(cx, cy, s * 0.45f, Path.Direction.CW)
                op(Path().apply { addCircle(cx + s * 0.22f, cy - s * 0.15f, s * 0.38f, Path.Direction.CW) }, Path.Op.DIFFERENCE)
            }
            BuiltinSticker.SUN -> Path().apply {
                addCircle(cx, cy, s * 0.26f, Path.Direction.CW)
                for (i in 0 until 12) {
                    val a = i * PI / 6
                    val tip = Path()
                    tip.moveTo(cx + (cos(a) * s * 0.48f).toFloat(), cy + (sin(a) * s * 0.48f).toFloat())
                    tip.lineTo(cx + (cos(a + 0.18) * s * 0.3f).toFloat(), cy + (sin(a + 0.18) * s * 0.3f).toFloat())
                    tip.lineTo(cx + (cos(a - 0.18) * s * 0.3f).toFloat(), cy + (sin(a - 0.18) * s * 0.3f).toFloat())
                    tip.close()
                    op(tip, Path.Op.UNION)
                }
            }
            BuiltinSticker.CLOUD -> Draw.cloudPath(cx, cy - s * 0.08f, s * 0.95f)
            BuiltinSticker.MUSIC -> Path().apply {
                addOval(RectF(cx - s * 0.38f, cy + s * 0.12f, cx - s * 0.06f, cy + s * 0.38f), Path.Direction.CW)
                addRect(RectF(cx - s * 0.12f, cy - s * 0.4f, cx - s * 0.04f, cy + s * 0.25f), Path.Direction.CW)
                moveTo(cx - s * 0.12f, cy - s * 0.4f)
                lineTo(cx + s * 0.32f, cy - s * 0.25f)
                lineTo(cx + s * 0.32f, cy - s * 0.1f)
                lineTo(cx - s * 0.04f, cy - s * 0.22f)
                close()
                op(Path(), Path.Op.UNION)
            }
            BuiltinSticker.BOLT -> Draw.boltPath(cx, cy, s * 0.95f)
            BuiltinSticker.FIRE -> Path().apply {
                moveTo(cx, cy - s * 0.48f)
                cubicTo(cx + s * 0.1f, cy - s * 0.2f, cx + s * 0.42f, cy - s * 0.05f, cx + s * 0.34f, cy + s * 0.22f)
                cubicTo(cx + s * 0.28f, cy + s * 0.42f, cx + s * 0.1f, cy + s * 0.48f, cx, cy + s * 0.48f)
                cubicTo(cx - s * 0.1f, cy + s * 0.48f, cx - s * 0.32f, cy + s * 0.42f, cx - s * 0.36f, cy + s * 0.2f)
                cubicTo(cx - s * 0.42f, cy - s * 0.05f, cx - s * 0.15f, cy - s * 0.1f, cx, cy - s * 0.48f)
                close()
            }
            BuiltinSticker.BOW -> Path().apply {
                moveTo(cx, cy)
                lineTo(cx - s * 0.46f, cy - s * 0.26f)
                lineTo(cx - s * 0.46f, cy + s * 0.26f)
                close()
                moveTo(cx, cy)
                lineTo(cx + s * 0.46f, cy - s * 0.26f)
                lineTo(cx + s * 0.46f, cy + s * 0.26f)
                close()
                addCircle(cx, cy, s * 0.11f, Path.Direction.CW)
                op(Path(), Path.Op.UNION)
            }
            BuiltinSticker.CAT -> Path().apply {
                addCircle(cx, cy + s * 0.08f, s * 0.36f, Path.Direction.CW)
                val left = Path().apply { moveTo(cx - s * 0.36f, cy - s * 0.05f); lineTo(cx - s * 0.3f, cy - s * 0.46f); lineTo(cx - s * 0.06f, cy - s * 0.22f); close() }
                val right = Path().apply { moveTo(cx + s * 0.36f, cy - s * 0.05f); lineTo(cx + s * 0.3f, cy - s * 0.46f); lineTo(cx + s * 0.06f, cy - s * 0.22f); close() }
                op(left, Path.Op.UNION); op(right, Path.Op.UNION)
                // Ojitos recortados
                op(Path().apply {
                    addCircle(cx - s * 0.13f, cy + s * 0.04f, s * 0.05f, Path.Direction.CW)
                    addCircle(cx + s * 0.13f, cy + s * 0.04f, s * 0.05f, Path.Direction.CW)
                }, Path.Op.DIFFERENCE)
            }
            BuiltinSticker.PAW -> Path().apply {
                addOval(RectF(cx - s * 0.25f, cy - s * 0.02f, cx + s * 0.25f, cy + s * 0.4f), Path.Direction.CW)
                addCircle(cx - s * 0.33f, cy - s * 0.12f, s * 0.11f, Path.Direction.CW)
                addCircle(cx - s * 0.12f, cy - s * 0.32f, s * 0.11f, Path.Direction.CW)
                addCircle(cx + s * 0.12f, cy - s * 0.32f, s * 0.11f, Path.Direction.CW)
                addCircle(cx + s * 0.33f, cy - s * 0.12f, s * 0.11f, Path.Direction.CW)
                op(Path(), Path.Op.UNION)
            }
            BuiltinSticker.SMILE -> Path().apply {
                addCircle(cx, cy, s * 0.45f, Path.Direction.CW)
                val face = Path().apply {
                    addCircle(cx - s * 0.15f, cy - s * 0.1f, s * 0.06f, Path.Direction.CW)
                    addCircle(cx + s * 0.15f, cy - s * 0.1f, s * 0.06f, Path.Direction.CW)
                    addArc(RectF(cx - s * 0.24f, cy - s * 0.12f, cx + s * 0.24f, cy + s * 0.3f), 20f, 140f)
                    addArc(RectF(cx - s * 0.2f, cy - s * 0.02f, cx + s * 0.2f, cy + s * 0.24f), 160f, -140f)
                }
                op(face, Path.Op.DIFFERENCE)
            }
            BuiltinSticker.DROP -> Path().apply {
                moveTo(cx, cy - s * 0.48f)
                cubicTo(cx + s * 0.1f, cy - s * 0.25f, cx + s * 0.36f, cy - s * 0.02f, cx + s * 0.36f, cy + s * 0.18f)
                cubicTo(cx + s * 0.36f, cy + s * 0.38f, cx + s * 0.2f, cy + s * 0.48f, cx, cy + s * 0.48f)
                cubicTo(cx - s * 0.2f, cy + s * 0.48f, cx - s * 0.36f, cy + s * 0.38f, cx - s * 0.36f, cy + s * 0.18f)
                cubicTo(cx - s * 0.36f, cy - s * 0.02f, cx - s * 0.1f, cy - s * 0.25f, cx, cy - s * 0.48f)
                close()
            }
        }
    }

    /** Dibuja un sticker dentro de [r]. */
    fun draw(c: Canvas, info: StickerInfo, r: RectF, timeMs: Long = 0L) {
        when (info.kind) {
            StickerKind.BUILTIN -> {
                val b = info.builtin ?: BuiltinSticker.HEART
                val path = builtinPath(b, r)
                val s = min(r.width(), r.height())
                if (info.outline) {
                    val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = s * 0.09f
                        strokeJoin = Paint.Join.ROUND
                        this.color = Color.WHITE
                        setShadowLayer(s * 0.05f, 0f, s * 0.02f, Draw.withAlpha(Color.BLACK, 90))
                    }
                    c.drawPath(path, outline)
                }
                val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = Draw.angledGradient(r, 120, intArrayOf(info.color1, info.color2))
                }
                c.drawPath(path, fill)
                // Brillito
                val shine = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = Draw.withAlpha(Color.WHITE, 110) }
                c.drawCircle(r.centerX() - s * 0.16f, r.centerY() - s * 0.14f, s * 0.06f, shine)
            }
            StickerKind.IMAGE -> ImageStore.load(info.path, 600)?.let { ElementPainter.drawFit(c, it, r) }
            StickerKind.DRAWING -> MediaLibrary.loadDrawing(info.mediaId, 600)?.let { ElementPainter.drawFit(c, it, r) }
            StickerKind.ANIMATION -> {
                val frames = MediaLibrary.loadFrames(info.mediaId, 400)
                frames.getOrNull(MediaLibrary.frameIndex(StudioStore.animation(info.mediaId), frames.size, timeMs))?.let { ElementPainter.drawFit(c, it, r) }
            }
        }
    }

    /** Rectángulo que ocupa un sticker pegado dentro de un elemento de [w]×[h]. */
    fun rectFor(a: StickerAttach, w: Float, h: Float): RectF {
        val side = min(w, h) * a.size
        val cx = a.x * w; val cy = a.y * h
        return RectF(cx - side / 2, cy - side / 2, cx + side / 2, cy + side / 2)
    }

    /** Dibuja todos los stickers pegados a un elemento. */
    fun drawAttached(c: Canvas, list: List<StickerAttach>, w: Float, h: Float, timeMs: Long = 0L) {
        list.forEach { a ->
            val info = StudioStore.sticker(a.stickerId) ?: return@forEach
            val r = rectFor(a, w, h)
            val m = MotionMath.at(a.motion, a.motionSpeed, timeMs, r.width(), r.height())
            c.save()
            c.translate(m.dx, m.dy)
            c.rotate(a.rotation + m.rotation, r.centerX(), r.centerY())
            c.scale(m.scaleX, m.scaleY, r.centerX(), r.centerY())
            draw(c, info, r, timeMs)
            c.restore()
        }
    }

    /** Miniatura de un sticker como bitmap. */
    fun thumbnail(info: StickerInfo, size: Int = 160): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val pad = size * 0.1f
        draw(Canvas(bmp), info, RectF(pad, pad, size - pad, size - pad))
        return bmp
    }
}

/** Capa de stickers en vivo, con movimiento, sobre cualquier elemento en Compose. */
@Composable
fun StickerLayer(stickers: List<StickerAttach>, modifier: Modifier = Modifier) {
    if (stickers.isEmpty()) return
    val active = LocalAnimationsActive.current
    val animated = stickers.any { StudioStore.sticker(it.stickerId)?.kind == StickerKind.ANIMATION }
    var time by remember { mutableLongStateOf(0L) }
    LaunchedEffect(animated, active) {
        if (animated && active) frameLoop { time = it }
    }
    BoxWithConstraints(modifier) {
        val minSide = if (maxWidth < maxHeight) maxWidth else maxHeight
        stickers.forEach { a ->
            val info = StudioStore.sticker(a.stickerId) ?: return@forEach
            val side = minSide * a.size
            Box(
                Modifier
                    .offset(maxWidth * a.x - side / 2, maxHeight * a.y - side / 2)
                    .size(side.coerceAtLeast(1.dp))
                    .motion(a.motion, a.motionSpeed, a.rotation)
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val t = time
                    drawIntoCanvas { StickerPainter.draw(it.nativeCanvas, info, RectF(0f, 0f, size.width, size.height), t) }
                }
            }
        }
    }
}
