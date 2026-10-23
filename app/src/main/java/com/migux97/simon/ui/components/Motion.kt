package com.migux97.simon.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.migux97.simon.data.Motion
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/** Aplica un movimiento continuo (flotar, latir, girar…) a cualquier elemento. */
@Composable
fun Modifier.motion(motion: Motion, speed: Float = 1f, baseRotation: Float = 0f, baseAlpha: Float = 1f): Modifier {
    // Sin movimiento si la pantalla no se ve, en ahorro de batería o con "reducir movimiento".
    val active = LocalAnimationsActive.current && !com.migux97.simon.data.StudioStore.perf.reduceMotion
    if (motion == Motion.NONE || !active) return this.graphicsLayer { rotationZ = baseRotation; alpha = baseAlpha }
    val period = (2400 / speed.coerceIn(0.2f, 4f)).toInt().coerceAtLeast(200)
    val transition = rememberInfiniteTransition(label = "motion")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(period, easing = LinearEasing), RepeatMode.Restart),
        label = "t"
    )
    return this.graphicsLayer {
        val wave = sin(t * 2 * PI).toFloat()
        rotationZ = baseRotation
        alpha = baseAlpha
        when (motion) {
            Motion.FLOAT -> translationY = wave * size.height * 0.06f
            Motion.PULSE -> { val s = 1f + 0.07f * wave; scaleX = s; scaleY = s }
            Motion.BOUNCE -> translationY = -abs(sin(t * PI).toFloat()) * size.height * 0.15f
            Motion.WIGGLE -> rotationZ = baseRotation + sin(t * 2 * PI * 4).toFloat() * 6f
            Motion.SWING -> { rotationZ = baseRotation + wave * 12f; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f) }
            Motion.SPIN -> rotationZ = baseRotation + t * 360f
            Motion.BLINK -> alpha = baseAlpha * (0.35f + 0.65f * (0.5f + 0.5f * wave))
            Motion.GLOW -> { val s = 1f + 0.03f * wave; scaleX = s; scaleY = s; alpha = baseAlpha * (0.75f + 0.25f * (0.5f + 0.5f * wave)) }
            Motion.JELLY -> { scaleX = 1f + 0.08f * wave; scaleY = 1f - 0.08f * wave }
            Motion.NONE -> Unit
        }
    }
}

/** Misma fórmula que [motion], para dibujar sobre un Canvas común (efectos y bitmaps). */
object MotionMath {
    data class Transform(val dx: Float = 0f, val dy: Float = 0f, val scaleX: Float = 1f, val scaleY: Float = 1f, val rotation: Float = 0f, val alpha: Float = 1f)

    fun at(motion: Motion, speed: Float, timeMs: Long, w: Float, h: Float): Transform {
        if (motion == Motion.NONE) return Transform()
        val period = (2400 / speed.coerceIn(0.2f, 4f)).coerceAtLeast(200f)
        val t = (timeMs % period.toLong()) / period
        val wave = sin(t * 2 * PI).toFloat()
        return when (motion) {
            Motion.FLOAT -> Transform(dy = wave * h * 0.06f)
            Motion.PULSE -> Transform(scaleX = 1f + 0.07f * wave, scaleY = 1f + 0.07f * wave)
            Motion.BOUNCE -> Transform(dy = -abs(sin(t * PI).toFloat()) * h * 0.15f)
            Motion.WIGGLE -> Transform(rotation = sin(t * 2 * PI * 4).toFloat() * 6f)
            Motion.SWING -> Transform(rotation = wave * 12f)
            Motion.SPIN -> Transform(rotation = t * 360f)
            Motion.BLINK -> Transform(alpha = 0.35f + 0.65f * (0.5f + 0.5f * wave))
            Motion.GLOW -> Transform(scaleX = 1f + 0.03f * wave, scaleY = 1f + 0.03f * wave, alpha = 0.75f + 0.25f * (0.5f + 0.5f * wave))
            Motion.JELLY -> Transform(scaleX = 1f + 0.08f * wave, scaleY = 1f - 0.08f * wave)
            Motion.NONE -> Transform()
        }
    }
}
