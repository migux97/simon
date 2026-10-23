package com.migux97.simon.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.migux97.simon.data.LiveConfig
import com.migux97.simon.data.Repository
import com.migux97.simon.data.StaticWallpaper
import com.migux97.simon.data.WidgetDesign
import com.migux97.simon.data.WidgetKind
import com.migux97.simon.wallpaper.Scene
import com.migux97.simon.wallpaper.StaticWallpapers
import com.migux97.simon.widget.RenderEnv
import com.migux97.simon.widget.WidgetRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** Fondo de muestra detrás de las vistas previas (para que se vea el vidrio). */
val PreviewBackdrop = Brush.linearGradient(listOf(Color(0xFFFF9A9E), Color(0xFFA18CD1), Color(0xFF84FAB0)))

/** Marca de tiempo que avanza cada minuto, para refrescar relojes en vistas previas. */
@Composable
fun rememberMinuteTick(enabled: Boolean): Long {
    var tick by remember { mutableLongStateOf(System.currentTimeMillis() / 60000) }
    LaunchedEffect(enabled) {
        while (enabled && isActive) {
            val now = System.currentTimeMillis()
            delay(60000 - now % 60000 + 50)
            tick = System.currentTimeMillis() / 60000
        }
    }
    return tick
}

/** Vista previa real de un widget, dibujada con el mismo código que en la pantalla de inicio. */
@Composable
fun WidgetPreview(design: WidgetDesign, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val tick = rememberMinuteTick(design.kind.needsMinuteTick)
    val weatherKey = if (design.kind == WidgetKind.WEATHER) Repository.weatherFor(design.lat, design.lon)?.fetchedAt else null
    BoxWithConstraints(modifier.aspectRatio(design.size.aspect)) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }.roundToInt().coerceIn(32, 1000)
        val hPx = (wPx / design.size.aspect).roundToInt().coerceAtLeast(32)
        val bmp by produceState<Bitmap?>(initialValue = null, design, wPx, hPx, tick, weatherKey) {
            value = withContext(Dispatchers.Default) {
                try {
                    WidgetRenderer.render(design, wPx, hPx, RenderEnv.current(context))
                } catch (e: Throwable) {
                    null
                }
            }
        }
        bmp?.let {
            Image(it.asImageBitmap(), contentDescription = design.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        }
    }
}

/** Widget sobre un fondo de muestra, como se vería en la pantalla de inicio. */
@Composable
fun WidgetOnBackdrop(design: WidgetDesign, modifier: Modifier = Modifier, padding: Dp = 14.dp) {
    Box(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(PreviewBackdrop)
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        WidgetPreview(design, Modifier.fillMaxWidth())
    }
}

/** Escena animada en vivo (para detalle y portada). */
@Composable
fun ScenePreview(config: LiveConfig, modifier: Modifier = Modifier, animate: Boolean = true) {
    val scene = remember(config) { Scene.create(config) }
    var frame by remember { mutableLongStateOf(0L) }
    val active = LocalAnimationsActive.current
    LaunchedEffect(scene, animate, active) {
        if (!animate || !active) return@LaunchedEffect
        var last = 0L
        frameLoop { t ->
            val dt = if (last == 0L) 0f else (t - last) / 1000f
            last = t
            scene.step(dt)
            frame = t
        }
    }
    Canvas(modifier) {
        // Leer `frame` hace que el Canvas se redibuje en cada cuadro.
        if (frame < 0L) return@Canvas
        scene.resize(size.width.roundToInt(), size.height.roundToInt())
        drawIntoCanvas { scene.render(it.nativeCanvas) }
    }
}

/** Foto fija de una escena (para grillas, más liviano que animarlas todas). */
@Composable
fun SceneThumb(config: LiveConfig, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }.roundToInt().coerceIn(16, 600)
        val hPx = with(density) { maxHeight.toPx() }.roundToInt().coerceIn(16, 1200)
        val bmp by produceState<Bitmap?>(null, config, wPx, hPx) {
            value = withContext(Dispatchers.Default) {
                try {
                    val scene = Scene.create(config)
                    scene.resize(wPx, hPx)
                    repeat(40) { scene.step(0.06f) }
                    Bitmap.createBitmap(wPx, hPx, Bitmap.Config.ARGB_8888).also { scene.render(android.graphics.Canvas(it)) }
                } catch (e: Throwable) {
                    null
                }
            }
        }
        bmp?.let { Image(it.asImageBitmap(), contentDescription = config.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
    }
}

@Composable
fun StaticThumb(wallpaper: StaticWallpaper, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }.roundToInt().coerceIn(16, 700)
        val hPx = with(density) { maxHeight.toPx() }.roundToInt().coerceIn(16, 1400)
        val bmp by produceState<Bitmap?>(null, wallpaper, wPx, hPx) {
            value = withContext(Dispatchers.Default) { StaticWallpapers.render(wallpaper, wPx, hPx) }
        }
        bmp?.let { Image(it.asImageBitmap(), contentDescription = wallpaper.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
    }
}
