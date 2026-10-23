package com.migux97.simon.custom

import android.graphics.RectF
import android.widget.VideoView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.migux97.simon.data.CustomWidget
import com.migux97.simon.data.ElementType
import com.migux97.simon.data.ShapeKind
import com.migux97.simon.data.TapAction
import com.migux97.simon.data.WElement
import com.migux97.simon.launcher.SystemActions
import com.migux97.simon.ui.components.motion
import com.migux97.simon.wallpaper.Scene
import com.migux97.simon.widget.RenderEnv
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File

private fun needsFrames(e: WElement) =
    e.type == ElementType.ANIMATION || e.type == ElementType.SCENE || e.type == ElementType.CLOCK || Tokens.needsSeconds(e.text)

fun elementShape(e: WElement): Shape = when (e.shape) {
    ShapeKind.CIRCLE -> CircleShape
    ShapeKind.ROUNDED -> RoundedCornerShape(percent = (e.corner * 100).toInt().coerceIn(0, 50))
    else -> RectangleShape
}

/**
 * Widget creado, dibujado en vivo: animaciones, escenas, videos y movimiento.
 * Si [interactive] es true, los toques ejecutan las acciones configuradas.
 */
@Composable
fun CustomWidgetLive(
    widget: CustomWidget,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    onOpenDrawer: () -> Unit = {}
) {
    val context = LocalContext.current
    val animated = widget.elements.any { needsFrames(it) }
    var timeMs by remember { mutableLongStateOf(0L) }
    var env by remember { mutableStateOf(RenderEnv.current(context)) }
    val start = remember { System.currentTimeMillis() }
    val scenes = remember(widget.id) { HashMap<String, Scene>() }

    val active = com.migux97.simon.ui.components.LocalAnimationsActive.current
    LaunchedEffect(animated, active) {
        if (!active) return@LaunchedEffect
        if (animated) {
            var lastEnv = 0L
            com.migux97.simon.ui.components.frameLoop { t ->
                val now = System.currentTimeMillis()
                timeMs = t
                if (now - lastEnv > 1000) { env = RenderEnv.current(context); lastEnv = now }
            }
        } else {
            while (isActive) {
                // Sin segundos ni animaciones alcanza con refrescar una vez por minuto.
                delay(60_000L - System.currentTimeMillis() % 60_000L)
                env = RenderEnv.current(context)
            }
        }
    }

    BoxWithConstraints(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            drawIntoCanvas { c ->
                c.nativeCanvas.save()
                CustomWidgetRenderer.drawBackground(c.nativeCanvas, widget, size.width, size.height)
                c.nativeCanvas.restore()
            }
        }
        widget.elements.forEach { e ->
            val ex = maxWidth * e.x
            val ey = maxHeight * e.y
            val ew = maxWidth * e.w
            val eh = maxHeight * e.h
            var m = Modifier
                .offset(ex, ey)
                .size(ew.coerceAtLeast(1.dp), eh.coerceAtLeast(1.dp))
                .motion(e.motion, e.motionSpeed, e.rotation, e.alpha)
            if (interactive && e.action != TapAction.NONE) {
                m = m.clickable { SystemActions.run(context, e.action, e.actionData, onOpenDrawer) }
            }
            Box(m) {
                if (e.type == ElementType.VIDEO && e.mediaPath != null && File(e.mediaPath).exists()) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                setVideoPath(e.mediaPath)
                                setOnPreparedListener { mp -> mp.isLooping = true; mp.setVolume(0f, 0f); start() }
                            }
                        },
                        modifier = Modifier.fillMaxSize().clip(elementShape(e))
                    )
                } else {
                    Canvas(Modifier.fillMaxSize()) {
                        val t = timeMs
                        val currentEnv = env
                        drawIntoCanvas { c ->
                            ElementPainter.draw(context, c.nativeCanvas, e, RectF(0f, 0f, size.width, size.height), currentEnv, widget, t.coerceAtLeast(1L), scenes)
                        }
                        if (e.type == ElementType.SCENE) scenes[e.id]?.step(0.016f)
                    }
                }
            }
        }
    }
}
