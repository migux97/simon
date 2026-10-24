package com.migux97.simon.canva

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.migux97.simon.custom.PhotoFx
import com.migux97.simon.data.PhotoAdjust
import com.migux97.simon.data.PhotoFilter
import com.migux97.simon.ui.components.LabeledSlider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private val CropAccent = Color(0xFF8B6CFF)

/** Proporciones fijas para recortar. Null = libre. */
private val cropRatios: List<Pair<String, Float?>> = listOf(
    "Libre" to null, "Cuadrado" to 1f, "4:5" to 0.8f, "9:16" to 9f / 16f, "16:9" to 16f / 9f, "3:2" to 1.5f
)

/**
 * Pantalla de recorte: la foto entera con un recuadro que se mueve y se
 * agranda desde las esquinas. Devuelve el recorte y la forma (ancho/alto) que quedó.
 */
@Composable
fun CropDialog(bitmap: Bitmap, initial: PhotoAdjust, onDismiss: () -> Unit, onDone: (PhotoAdjust, Float) -> Unit) {
    var l by remember { mutableStateOf(initial.cropL) }
    var t by remember { mutableStateOf(initial.cropT) }
    var r by remember { mutableStateOf(initial.cropR) }
    var b by remember { mutableStateOf(initial.cropB) }
    var ratio by remember { mutableStateOf<Float?>(null) }
    val imgAspect = bitmap.width.toFloat() / bitmap.height.coerceAtLeast(1)

    /** Aplica una proporción fija, centrada en el recorte actual. */
    fun applyRatio(rt: Float?) {
        ratio = rt
        if (rt == null) return
        val cx = (l + r) / 2f
        val cy = (t + b) / 2f
        // En fracciones de la imagen: ancho/alto del recorte = rt / imgAspect.
        val want = rt / imgAspect
        var w = (r - l)
        var h = w / want
        if (h > 1f) { h = 1f; w = h * want }
        if (w > 1f) { w = 1f; h = w / want }
        l = (cx - w / 2f).coerceIn(0f, 1f - w); r = l + w
        t = (cy - h / 2f).coerceIn(0f, 1f - h); b = t + h
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(Color(0xFF0E0B16))) {
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.White) }
                Text("Recortar", color = Color.White, fontSize = 17.sp, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                TextButton(onClick = { l = 0f; t = 0f; r = 1f; b = 1f; ratio = null }) { Text("Restablecer", color = Color(0xFFC9B8FF)) }
            }
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                val boxAspect = maxWidth / maxHeight
                val mod = if (boxAspect > imgAspect) Modifier.fillMaxSize().aspectRatio(imgAspect, matchHeightConstraintsFirst = true) else Modifier.fillMaxWidth().aspectRatio(imgAspect)
                BoxWithConstraints(mod) {
                    val wPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                    val hPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
                    val minF = 0.06f
                    Image(bitmap.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                    // Afuera del recorte, oscuro; adentro, la grilla de tercios.
                    Canvas(Modifier.fillMaxSize().pointerInput(Unit) {
                        detectDragGestures { ch, d ->
                            ch.consume()
                            val dx = d.x / wPx
                            val dy = d.y / hPx
                            val w = r - l
                            val h = b - t
                            l = (l + dx).coerceIn(0f, 1f - w); r = l + w
                            t = (t + dy).coerceIn(0f, 1f - h); b = t + h
                        }
                    }) {
                        val dim = Color.Black.copy(alpha = 0.6f)
                        drawRect(dim, Offset.Zero, Size(wPx, t * hPx))
                        drawRect(dim, Offset(0f, b * hPx), Size(wPx, (1f - b) * hPx))
                        drawRect(dim, Offset(0f, t * hPx), Size(l * wPx, (b - t) * hPx))
                        drawRect(dim, Offset(r * wPx, t * hPx), Size((1f - r) * wPx, (b - t) * hPx))
                        drawRect(Color.White, Offset(l * wPx, t * hPx), Size((r - l) * wPx, (b - t) * hPx), style = Stroke(2.dp.toPx()))
                        for (i in 1..2) {
                            val x = (l + (r - l) * i / 3f) * wPx
                            val y = (t + (b - t) * i / 3f) * hPx
                            drawLine(Color.White.copy(alpha = 0.5f), Offset(x, t * hPx), Offset(x, b * hPx), 1.dp.toPx())
                            drawLine(Color.White.copy(alpha = 0.5f), Offset(l * wPx, y), Offset(r * wPx, y), 1.dp.toPx())
                        }
                    }
                    // Esquinas para agrandar el recorte.
                    for (sx in listOf(-1, 1)) for (sy in listOf(-1, 1)) {
                        val px = (if (sx < 0) l else r) * wPx
                        val py = (if (sy < 0) t else b) * hPx
                        CropCorner(px, py) { d ->
                            val rt = ratio
                            var dx = d.x / wPx
                            var dy = d.y / hPx
                            if (rt != null) {
                                // Con proporción fija se mueve en diagonal.
                                val want = rt / imgAspect
                                val m = if (kotlin.math.abs(dx) > kotlin.math.abs(dy * want)) dx else dy * want * sx * sy
                                dx = m; dy = m / want * sx * sy
                            }
                            if (sx < 0) l = (l + dx).coerceIn(0f, r - minF) else r = (r + dx).coerceIn(l + minF, 1f)
                            if (sy < 0) t = (t + dy).coerceIn(0f, b - minF) else b = (b + dy).coerceIn(t + minF, 1f)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                cropRatios.forEach { (label, rt) ->
                    FilterChip(selected = ratio == rt, onClick = { applyRatio(rt) }, label = { Text(label) })
                }
            }
            Button(
                onClick = {
                    val adj = initial.copy(cropL = l, cropT = t, cropR = r, cropB = b)
                    onDone(adj, ((r - l) * bitmap.width) / ((b - t) * bitmap.height).coerceAtLeast(1f))
                },
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 56.dp)
            ) { Text("Listo") }
        }
    }
}

@Composable
private fun CropCorner(x: Float, y: Float, onDrag: (Offset) -> Unit) {
    val half = with(LocalDensity.current) { 22.dp.toPx() }
    val drag by rememberUpdatedState(onDrag)
    Box(
        Modifier
            .offset { IntOffset((x - half).roundToInt(), (y - half).roundToInt()) }
            .size(44.dp)
            .pointerInput(Unit) { detectDragGestures { ch, d -> ch.consume(); drag(d) } },
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.size(20.dp).clip(CircleShape).background(Color.White).border(2.dp, CropAccent, CircleShape))
    }
}

/** Panel «Ajustes»: filtros con vista previa y deslizadores de brillo, contraste y saturación. */
@Composable
fun AdjustPanel(bitmap: Bitmap?, adjust: PhotoAdjust, onChange: (PhotoAdjust, Boolean) -> Unit) {
    val thumb by produceState<Bitmap?>(null, bitmap) {
        value = bitmap?.let { b ->
            withContext(Dispatchers.Default) {
                val s = 160f / maxOf(b.width, b.height)
                Bitmap.createScaledBitmap(b, (b.width * s).roundToInt().coerceAtLeast(1), (b.height * s).roundToInt().coerceAtLeast(1), true)
            }
        }
    }
    CanvaSection("Filtros")
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PhotoFilter.entries.forEach { f ->
            val selected = adjust.filter == f
            Column(
                Modifier.width(72.dp).clip(RoundedCornerShape(12.dp)).clickable { onChange(adjust.copy(filter = f), false) }.padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.size(62.dp).clip(RoundedCornerShape(10.dp)).background(Color(0x22888888))
                        .border(if (selected) 3.dp else 0.dp, CropAccent, RoundedCornerShape(10.dp))
                ) {
                    thumb?.let { tb ->
                        val fx = remember(tb, f) { PhotoFx.process(tb, PhotoAdjust(filter = f), "thumb_${tb.hashCode()}") }
                        Image(fx.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                }
                Text(f.label, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
    Column(Modifier.padding(horizontal = 20.dp)) {
        Spacer(Modifier.size(8.dp))
        LabeledSlider("Brillo", adjust.brightness, -1f..1f, "${(adjust.brightness * 100).roundToInt()}") { onChange(adjust.copy(brightness = it), true) }
        LabeledSlider("Contraste", adjust.contrast, 0.3f..1.7f, "${((adjust.contrast - 1f) * 100).roundToInt()}") { onChange(adjust.copy(contrast = it), true) }
        LabeledSlider("Saturación", adjust.saturation, 0f..2f, "${((adjust.saturation - 1f) * 100).roundToInt()}") { onChange(adjust.copy(saturation = it), true) }
        if (adjust.hasColor) TextButton(onClick = { onChange(adjust.copy(brightness = 0f, contrast = 1f, saturation = 1f, filter = PhotoFilter.NONE), false) }) {
            Text("Quitar ajustes", color = MaterialTheme.colorScheme.primary)
        }
    }
}
