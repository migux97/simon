package com.migux97.simon.canva

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AlignHorizontalCenter
import androidx.compose.material.icons.filled.AlignHorizontalLeft
import androidx.compose.material.icons.filled.AlignHorizontalRight
import androidx.compose.material.icons.filled.AlignVerticalBottom
import androidx.compose.material.icons.filled.AlignVerticalCenter
import androidx.compose.material.icons.filled.AlignVerticalTop
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.FlipToFront
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.HorizontalDistribute
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LibraryAddCheck
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.VerticalDistribute
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.runtime.rememberCoroutineScope
import com.migux97.simon.data.PhotoAdjust
import com.migux97.simon.ui.components.HelpTip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.migux97.simon.ui.components.LabeledSlider
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/** Botón de la barra de abajo. Con [content] abre un panel; con [onClick] hace algo directo. */
class CanvaTool(
    val key: String,
    val icon: ImageVector,
    val label: String,
    val onClick: (() -> Unit)? = null,
    val content: (@Composable ColumnScope.() -> Unit)? = null
)

private val BarColor = Color(0xF21C1530)
private val Accent = Color(0xFF8B6CFF)
private val GuideColor = Color(0xFFFF2EA6)

// Paneles propios del editor (los de cada herramienta usan la clave de la herramienta).
private const val P_EDIT = "_edit"
private const val P_ALIGN = "_align"
private const val P_ORDER = "_order"
private const val P_ALPHA = "_alpha"
private const val P_LAYERS = "_layers"
private const val P_FLIP = "_flip"
private const val P_ADJUST = "_adjust"

/**
 * Editor tipo Canva. Con [fullScreen] el lienzo ocupa toda la pantalla (para
 * diseñar la pantalla de inicio, el bloqueo o la carga a tamaño real) y las
 * barras flotan encima; si no, el lienzo se ve entero entre las barras.
 */
@Composable
fun <S> CanvaEditor(
    state: CanvaState<S>,
    title: String,
    fullScreen: Boolean,
    canvasAspect: Float,
    onClose: () -> Unit,
    tools: List<CanvaTool>,
    hint: String,
    hintKey: String,
    topActions: @Composable RowScope.() -> Unit = {},
    editPanel: (@Composable ColumnScope.(CLayer) -> Unit)? = null,
    /** Si devuelve true, «Editar» lo resolvió el que llama (por ejemplo, abriendo otra hoja). */
    onEditOutside: ((CLayer) -> Boolean)? = null,
    background: @Composable BoxScope.() -> Unit = {},
    foreground: @Composable BoxScope.() -> Unit = {},
    /** Carga la imagen original de una foto o un dibujo (para recortar y ajustar). */
    loadPhoto: (suspend (String) -> Bitmap?)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cropping by remember { mutableStateOf<Pair<String, Bitmap>?>(null) }
    var photoBitmap by remember { mutableStateOf<Pair<String, Bitmap>?>(null) }
    val prefs = remember { context.getSharedPreferences("simon_hints", Context.MODE_PRIVATE) }
    var showHint by remember { mutableStateOf(!prefs.getBoolean(hintKey, false)) }
    var canvasPx by remember { mutableStateOf(1f to 1f) }

    // Si la selección se vacía, se cierran los paneles que dependen de ella.
    LaunchedEffect(state.selection.isEmpty()) {
        if (state.selection.isEmpty() && state.panel in listOf(P_EDIT, P_ALIGN, P_ORDER, P_ALPHA, P_FLIP, P_ADJUST)) state.panel = null
    }

    BackHandler {
        when {
            state.panel != null -> state.panel = null
            state.selection.isNotEmpty() -> state.clearSelection()
            else -> onClose()
        }
    }

    fun openEdit() {
        val l = state.selected.singleOrNull() ?: return
        if (onEditOutside?.invoke(l) == true) return
        if (editPanel != null) state.panel = P_EDIT
    }

    /** Trae la foto original de la capa elegida (para recortar o para las vistas previas de los filtros). */
    fun withPhoto(then: (String, Bitmap) -> Unit) {
        val l = state.selected.singleOrNull()?.takeIf { it.photo != null } ?: return
        val loader = loadPhoto ?: return
        photoBitmap?.takeIf { it.first == l.id }?.let { then(l.id, it.second); return }
        scope.launch {
            val bmp = withContext(Dispatchers.IO) { loader(l.id) }
            if (bmp == null) {
                Toast.makeText(context, "No se pudo abrir la imagen", Toast.LENGTH_SHORT).show()
            } else {
                photoBitmap = l.id to bmp
                then(l.id, bmp)
            }
        }
    }
    val photoTools = PhotoTools(
        canPhoto = loadPhoto != null && state.selected.singleOrNull()?.photo != null,
        onCrop = { withPhoto { id, bmp -> cropping = id to bmp } },
        onAdjust = { withPhoto { _, _ -> state.panel = P_ADJUST } }
    )

    val stage: @Composable (Modifier) -> Unit = { mod ->
        CanvasStage(state, mod, onSize = { w, h -> canvasPx = w to h }, onEdit = { openEdit() }, canEdit = editPanel != null || onEditOutside != null, background = background, foreground = foreground, content = content)
    }
    val topBar: @Composable () -> Unit = {
        TopBar(state, title, fullScreen, onClose, topActions)
        if (showHint) Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
            HelpTip(hint)
            TextButton(onClick = {
                showHint = false
                try { prefs.edit().putBoolean(hintKey, true).apply() } catch (_: Exception) {}
            }) { Text("Entendido", color = if (fullScreen) Color.White else Accent) }
        }
    }
    val bottom: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth().imePadding()) {
            state.panel?.let { key ->
                PanelHost(state, key, tools, editPanel, canvasPx, photoBitmap)
            }
            BottomBar(state, tools, canEdit = editPanel != null || onEditOutside != null, onEdit = { openEdit() }, photo = photoTools)
        }
    }

    cropping?.let { (id, bmp) ->
        val current = state.layers.firstOrNull { it.id == id }?.photo ?: PhotoAdjust()
        CropDialog(bmp, current, onDismiss = { cropping = null }) { adj, aspect ->
            cropping = null
            // Una capa bloqueada (por ejemplo el fondo) no cambia de forma: solo se recorta lo que muestra.
            val locked = state.layers.firstOrNull { it.id == id }?.locked == true
            state.setPhoto(id, adj, if (locked) null else aspect, canvasPx.first, canvasPx.second)
        }
    }

    if (fullScreen) {
        Box(Modifier.fillMaxSize()) {
            stage(Modifier.fillMaxSize())
            Column(Modifier.align(Alignment.TopCenter).fillMaxWidth().statusBarsPadding()) { topBar() }
            Box(Modifier.align(Alignment.BottomCenter)) { bottom() }
        }
    } else {
        Column(Modifier.fillMaxSize().background(Color(0xFF15111F))) {
            Column(Modifier.statusBarsPadding()) { topBar() }
            BoxWithConstraints(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .pointerInput(Unit) { detectTapGestures { state.clearSelection() } },
                contentAlignment = Alignment.Center
            ) {
                val fitsWidth = maxWidth / maxHeight < canvasAspect
                val mod = if (fitsWidth) Modifier.fillMaxWidth().aspectRatio(canvasAspect) else Modifier.fillMaxSize().aspectRatio(canvasAspect, matchHeightConstraintsFirst = true)
                stage(mod)
            }
            bottom()
        }
    }
}

// ------------------------------------------------------------------ lienzo

@Composable
private fun <S> CanvasStage(
    state: CanvaState<S>,
    modifier: Modifier,
    onSize: (Float, Float) -> Unit,
    onEdit: () -> Unit,
    canEdit: Boolean,
    background: @Composable BoxScope.() -> Unit,
    foreground: @Composable BoxScope.() -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    BoxWithConstraints(modifier) {
        val wPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val hPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        LaunchedEffect(wPx, hPx) { onSize(wPx, hPx) }
        background()
        // Tocar un lugar vacío quita la selección.
        Box(Modifier.matchParentSize().pointerInput(Unit) { detectTapGestures { state.clearSelection() } })
        content()
        foreground()
        state.layers.filterNot { it.hidden }.forEach { l ->
            key(l.id) { LayerHit(state, l, wPx, hPx, onEdit) }
        }
        SelectionOverlay(state, wPx, hPx)
        FloatingMenu(state, wPx, hPx, canEdit, onEdit)
    }
}

/**
 * Menú chico arriba de lo elegido (como en Canva): lo más usado a mano,
 * sin tener que buscarlo en la barra de abajo.
 */
@Composable
private fun <S> FloatingMenu(state: CanvaState<S>, wPx: Float, hPx: Float, canEdit: Boolean, onEdit: () -> Unit) {
    val sel = state.selected
    if (sel.isEmpty() || state.inGestureState) return
    val box = union(sel.map { it.bounds(wPx, hPx) }) ?: return
    val density = LocalDensity.current
    val menuH = with(density) { 46.dp.toPx() }
    val gap = with(density) { 14.dp.toPx() }
    var menuW by remember { mutableStateOf(0) }
    val locked = sel.any { it.locked }
    // Arriba de lo elegido; si no entra, abajo (dejando lugar para la manija de girar).
    val above = box.top * hPx - gap - menuH
    val below = box.bottom * hPx + with(density) { 56.dp.toPx() }
    // Si lo elegido ocupa toda la pantalla (por ejemplo el fondo), el menú queda adentro, arriba.
    val top = with(density) { 96.dp.toPx() }
    val y = when {
        above > top -> above
        below + menuH < hPx - with(density) { 140.dp.toPx() } -> below
        else -> (box.top * hPx + gap).coerceAtLeast(top)
    }
    val x = (box.cx * wPx - menuW / 2f).coerceIn(0f, (wPx - menuW).coerceAtLeast(0f))
    Row(
        Modifier
            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .onSizeChanged { menuW = it.width }
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (canEdit && sel.size == 1) MenuIcon(Icons.Filled.Edit, "Editar") { onEdit() }
        MenuIcon(Icons.Filled.ContentCopy, "Duplicar", enabled = sel.any { it.canDuplicate }) { state.duplicateSelected() }
        MenuIcon(if (locked) Icons.Filled.LockOpen else Icons.Filled.Lock, if (locked) "Desbloquear" else "Bloquear") { state.setLocked(!locked) }
        MenuIcon(Icons.Filled.Delete, "Borrar", tint = Color(0xFFD32F2F)) { state.deleteSelected() }
    }
}

@Composable
private fun MenuIcon(icon: ImageVector, label: String, enabled: Boolean = true, tint: Color = Color(0xFF2A1F45), onClick: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val c = if (enabled) tint else tint.copy(alpha = 0.3f)
        Icon(icon, label, tint = c, modifier = Modifier.size(20.dp))
        Text(label, color = c, fontSize = 10.sp, maxLines = 1)
    }
}

/** Zona invisible encima de cada capa: tocar, mantener presionado y arrastrar. */
@Composable
private fun <S> LayerHit(state: CanvaState<S>, l: CLayer, wPx: Float, hPx: Float, onEdit: () -> Unit) {
    val density = LocalDensity.current
    val snapPx = with(density) { 8.dp.toPx() }
    Box(
        Modifier
            .offset { IntOffset((l.x * wPx).roundToInt(), (l.y * hPx).roundToInt()) }
            .size(with(density) { (l.w * wPx).coerceAtLeast(1f).toDp() }, with(density) { (l.h * hPx).coerceAtLeast(1f).toDp() })
            .graphicsLayer { rotationZ = l.rotation }
            .pointerInput(l.id) {
                detectTapGestures(
                    onTap = { state.tap(l.id) },
                    onDoubleTap = { state.selectOnly(l.id); onEdit() },
                    onLongPress = { state.toggle(l.id) }
                )
            }
            .pointerInput(l.id, wPx, hPx) {
                var start = emptyList<CLayer>()
                var others = emptyList<Bounds>()
                var total = Offset.Zero
                detectDragGestures(
                    onDragStart = {
                        if (l.id !in state.selection) state.selectOnly(l.id)
                        start = state.selected.filterNot { it.locked }
                        others = state.layers.filter { it.id !in state.selection && !it.hidden }.map { it.bounds(wPx, hPx) }
                        total = Offset.Zero
                        state.beginGesture()
                    },
                    onDrag = { change, d ->
                        change.consume()
                        if (start.isEmpty()) return@detectDragGestures
                        total += d
                        val box = union(start.map { it.bounds(wPx, hPx) })!!
                        val fx = total.x / wPx
                        val fy = total.y / hPx
                        val moved = Bounds(box.left + fx, box.top + fy, box.right + fx, box.bottom + fy)
                        val (sx, sy, guides) = snapMove(moved, others, wPx, hPx, snapPx)
                        state.gesture(state.source.applyLayers(state.doc, start.map { it.copy(x = it.x + fx + sx, y = it.y + fy + sy) }))
                        state.guides = guides
                    },
                    onDragEnd = { state.endGesture() },
                    onDragCancel = { state.endGesture() }
                )
            }
    )
}

private fun rot(x: Float, y: Float, deg: Float): Offset {
    val a = Math.toRadians(deg.toDouble())
    val c = cos(a).toFloat()
    val s = sin(a).toFloat()
    return Offset(x * c - y * s, x * s + y * c)
}

/** Marco de la selección, guías y manijas para agrandar y girar. */
@Composable
private fun <S> SelectionOverlay(state: CanvaState<S>, wPx: Float, hPx: Float) {
    val density = LocalDensity.current
    val sel = state.selected
    val guides = state.guides
    val single = sel.singleOrNull()
    val locked = sel.any { it.locked }

    Canvas(Modifier.fillMaxSize()) {
        guides.forEach { g ->
            if (g.vertical) drawLine(GuideColor, Offset(g.pos * wPx, 0f), Offset(g.pos * wPx, hPx), strokeWidth = 1.5.dp.toPx())
            else drawLine(GuideColor, Offset(0f, g.pos * hPx), Offset(wPx, g.pos * hPx), strokeWidth = 1.5.dp.toPx())
        }
        sel.forEach { l ->
            val c = Offset((l.x + l.w / 2) * wPx, (l.y + l.h / 2) * hPx)
            val hw = l.w * wPx / 2
            val hh = l.h * hPx / 2
            val pts = listOf(-hw to -hh, hw to -hh, hw to hh, -hw to hh).map { (x, y) -> c + rot(x, y, l.rotation) }
            val path = Path().apply { moveTo(pts[0].x, pts[0].y); pts.drop(1).forEach { lineTo(it.x, it.y) }; close() }
            drawPath(path, if (locked) Color.White.copy(alpha = 0.7f) else Accent, style = Stroke(width = (if (single != null) 2.dp else 1.dp).toPx()))
        }
        if (single == null && sel.size > 1) union(sel.map { it.bounds(wPx, hPx) })?.let { b ->
            drawRect(
                Accent,
                topLeft = Offset(b.left * wPx, b.top * hPx),
                size = androidx.compose.ui.geometry.Size(b.width * wPx, b.height * hPx),
                style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)))
            )
        }
    }
    if (sel.isEmpty() || locked) return

    val rotateGap = with(density) { 36.dp.toPx() }
    if (single != null) {
        val l = single
        val c = Offset((l.x + l.w / 2) * wPx, (l.y + l.h / 2) * hPx)
        for (sx in -1..1) for (sy in -1..1) {
            if (sx == 0 && sy == 0) continue
            // Las manijas de los lados solo si el lado es bastante largo.
            if (sx == 0 && l.w * wPx < with(density) { 64.dp.toPx() }) continue
            if (sy == 0 && l.h * hPx < with(density) { 64.dp.toPx() }) continue
            val p = c + rot(sx * l.w * wPx / 2, sy * l.h * hPx / 2, l.rotation)
            ResizeHandle(state, p, corner = sx != 0 && sy != 0, sx = sx, sy = sy, wPx = wPx, hPx = hPx)
        }
        val rp = c + rot(0f, l.h * hPx / 2 + rotateGap, l.rotation)
        RotateHandle(state, rp, c, wPx, hPx)
    } else {
        val b = union(sel.map { it.bounds(wPx, hPx) }) ?: return
        for (sx in listOf(-1, 1)) for (sy in listOf(-1, 1)) {
            val p = Offset((if (sx < 0) b.left else b.right) * wPx, (if (sy < 0) b.top else b.bottom) * hPx)
            ResizeHandle(state, p, corner = true, sx = sx, sy = sy, wPx = wPx, hPx = hPx)
        }
        RotateHandle(state, Offset(b.cx * wPx, b.bottom * hPx + rotateGap), Offset(b.cx * wPx, b.cy * hPx), wPx, hPx)
    }
}

@Composable
private fun HandleDot(center: Offset, size: androidx.compose.ui.unit.Dp, modifier: Modifier, content: @Composable BoxScope.() -> Unit = {}) {
    val density = LocalDensity.current
    val touch = 44.dp
    val half = with(density) { (touch / 2).toPx() }
    // Primero se ubica y después se agregan los toques: si no, la zona táctil queda en la esquina del lienzo.
    Box(
        Modifier
            .offset { IntOffset((center.x - half).roundToInt(), (center.y - half).roundToInt()) }
            .size(touch)
            .then(modifier),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(size)
                .shadow(3.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.5.dp, Accent, CircleShape),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

/** Manija de las esquinas (agranda manteniendo la forma) o de los lados (estira). */
@Composable
private fun <S> ResizeHandle(state: CanvaState<S>, pos: Offset, corner: Boolean, sx: Int, sy: Int, wPx: Float, hPx: Float) {
    val minPx = with(LocalDensity.current) { 14.dp.toPx() }
    HandleDot(pos, if (corner) 18.dp else 14.dp, Modifier.pointerInput(sx, sy, wPx, hPx) {
        var start = emptyList<CLayer>()
        var total = Offset.Zero
        detectDragGestures(
            onDragStart = {
                start = state.selected.filterNot { it.locked }
                total = Offset.Zero
                state.beginGesture()
            },
            onDrag = { change, d ->
                change.consume()
                total += d
                val changed = if (start.size == 1) resizeOne(start[0], total, sx, sy, corner, wPx, hPx, minPx)
                else resizeMany(start, total, sx, sy, wPx, hPx, minPx)
                state.gesture(state.source.applyLayers(state.doc, changed))
            },
            onDragEnd = { state.endGesture() },
            onDragCancel = { state.endGesture() }
        )
    })
}

private fun resizeOne(l: CLayer, total: Offset, sx: Int, sy: Int, corner: Boolean, wPx: Float, hPx: Float, minPx: Float): List<CLayer> {
    val w0 = l.w * wPx
    val h0 = l.h * hPx
    val c0 = Offset((l.x + l.w / 2) * wPx, (l.y + l.h / 2) * hPx)
    val local = rot(total.x, total.y, -l.rotation)
    var nw = w0
    var nh = h0
    if (corner) {
        val len = hypot(w0, h0).coerceAtLeast(1f)
        val proj = (local.x * sx * w0 + local.y * sy * h0) / len
        val s = ((len + proj) / len).coerceAtLeast(minPx / minOf(w0, h0).coerceAtLeast(1f))
        nw = w0 * s
        nh = h0 * s
    } else {
        if (sx != 0) nw = (w0 + sx * local.x).coerceAtLeast(minPx)
        if (sy != 0) nh = (h0 + sy * local.y).coerceAtLeast(minPx)
    }
    // El punto opuesto queda fijo.
    val anchor = c0 + rot(-sx * w0 / 2, -sy * h0 / 2, l.rotation)
    val nc = anchor + rot(sx * nw / 2, sy * nh / 2, l.rotation)
    return listOf(l.copy(x = (nc.x - nw / 2) / wPx, y = (nc.y - nh / 2) / hPx, w = nw / wPx, h = nh / hPx))
}

private fun resizeMany(list: List<CLayer>, total: Offset, sx: Int, sy: Int, wPx: Float, hPx: Float, minPx: Float): List<CLayer> {
    val b = union(list.map { it.bounds(wPx, hPx) }) ?: return list
    val bw = b.width * wPx
    val bh = b.height * hPx
    val len = hypot(bw, bh).coerceAtLeast(1f)
    val proj = (total.x * sx * bw + total.y * sy * bh) / len
    val s = ((len + proj) / len).coerceAtLeast(minPx / minOf(bw, bh).coerceAtLeast(1f))
    val anchor = Offset((if (sx < 0) b.right else b.left) * wPx, (if (sy < 0) b.bottom else b.top) * hPx)
    return list.map { l ->
        val c = Offset((l.x + l.w / 2) * wPx, (l.y + l.h / 2) * hPx)
        val nc = anchor + (c - anchor) * s
        val nw = l.w * wPx * s
        val nh = l.h * hPx * s
        l.copy(x = (nc.x - nw / 2) / wPx, y = (nc.y - nh / 2) / hPx, w = nw / wPx, h = nh / hPx)
    }
}

/** Manija para girar: se pega a 0°, 45°, 90°… cuando pasa cerca. */
@Composable
private fun <S> RotateHandle(state: CanvaState<S>, pos: Offset, center: Offset, wPx: Float, hPx: Float) {
    // El gesto vive más que esta composición: se leen siempre la posición y el centro actuales.
    val currentPos by rememberUpdatedState(pos)
    val currentCenter by rememberUpdatedState(center)
    HandleDot(pos, 26.dp, Modifier.pointerInput(wPx, hPx) {
        var start = emptyList<CLayer>()
        var startPos = Offset.Zero
        var pivot = Offset.Zero
        var total = Offset.Zero
        detectDragGestures(
            onDragStart = {
                start = state.selected.filterNot { it.locked }
                startPos = currentPos
                pivot = currentCenter
                total = Offset.Zero
                state.beginGesture()
            },
            onDrag = { change, d ->
                change.consume()
                total += d
                val p = startPos + total
                val a0 = Math.toDegrees(atan2((startPos.y - pivot.y).toDouble(), (startPos.x - pivot.x).toDouble()))
                val a1 = Math.toDegrees(atan2((p.y - pivot.y).toDouble(), (p.x - pivot.x).toDouble()))
                var delta = (a1 - a0).toFloat()
                if (start.size == 1) {
                    val target = start[0].rotation + delta
                    val snapped = (target / 45f).roundToInt() * 45f
                    if (kotlin.math.abs(target - snapped) < 4f) delta = snapped - start[0].rotation
                }
                val changed = start.map { l ->
                    val c = Offset((l.x + l.w / 2) * wPx, (l.y + l.h / 2) * hPx)
                    val nc = if (start.size == 1) c else pivot + rot(c.x - pivot.x, c.y - pivot.y, delta)
                    var r = (l.rotation + delta) % 360f
                    if (r > 180f) r -= 360f
                    if (r < -180f) r += 360f
                    l.copy(x = nc.x / wPx - l.w / 2, y = nc.y / hPx - l.h / 2, rotation = r)
                }
                state.gesture(state.source.applyLayers(state.doc, changed))
            },
            onDragEnd = { state.endGesture() },
            onDragCancel = { state.endGesture() }
        )
    }) { Icon(Icons.Filled.RotateRight, "Girar", tint = Accent, modifier = Modifier.size(16.dp)) }
}

// ------------------------------------------------------------------ barras

@Composable
private fun <S> TopBar(state: CanvaState<S>, title: String, fullScreen: Boolean, onClose: () -> Unit, actions: @Composable RowScope.() -> Unit) {
    Surface(
        color = BarColor,
        shape = if (fullScreen) RoundedCornerShape(26.dp) else RoundedCornerShape(0.dp),
        modifier = Modifier.fillMaxWidth().padding(if (fullScreen) 10.dp else 0.dp)
    ) {
        Row(Modifier.padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Cerrar", tint = Color.White) }
            Text(title, color = Color.White, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            IconButton(onClick = { state.undo() }, enabled = state.canUndo) {
                Icon(Icons.AutoMirrored.Filled.Undo, "Deshacer", tint = if (state.canUndo) Color.White else Color.White.copy(alpha = 0.3f))
            }
            IconButton(onClick = { state.redo() }, enabled = state.canRedo) {
                Icon(Icons.AutoMirrored.Filled.Redo, "Rehacer", tint = if (state.canRedo) Color.White else Color.White.copy(alpha = 0.3f))
            }
            actions()
        }
    }
}

@Composable
private fun BarItem(icon: ImageVector, label: String, active: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    val tint = when {
        !enabled -> Color.White.copy(alpha = 0.3f)
        active -> Color(0xFFC9B8FF)
        else -> Color.White
    }
    Column(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) Color.White.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .width(68.dp)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Text(label, color = tint, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Recortar y Ajustes, solo para fotos y dibujos. */
class PhotoTools(val canPhoto: Boolean, val onCrop: () -> Unit, val onAdjust: () -> Unit)

@Composable
private fun <S> BottomBar(state: CanvaState<S>, tools: List<CanvaTool>, canEdit: Boolean, onEdit: () -> Unit, photo: PhotoTools) {
    val sel = state.selected
    Surface(color = BarColor, shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.navigationBarsPadding().padding(horizontal = 6.dp, vertical = 4.dp).horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            fun toggle(key: String) { state.panel = if (state.panel == key) null else key }
            if (sel.isEmpty()) {
                tools.forEach { t ->
                    BarItem(t.icon, t.label, active = state.panel == t.key) {
                        if (t.content != null) toggle(t.key) else { state.panel = null; t.onClick?.invoke() }
                    }
                }
                BarItem(Icons.Filled.Layers, "Capas", active = state.panel == P_LAYERS) { toggle(P_LAYERS) }
                BarItem(Icons.Filled.LibraryAddCheck, "Elegir varios", active = state.multiSelect) { state.multiSelect = !state.multiSelect }
            } else {
                val locked = sel.any { it.locked }
                BarItem(Icons.Filled.Check, "Listo") { state.clearSelection(); state.panel = null }
                if (canEdit && sel.size == 1) BarItem(Icons.Filled.Edit, "Editar", active = state.panel == P_EDIT) { onEdit() }
                if (photo.canPhoto) {
                    BarItem(Icons.Filled.Crop, "Recortar") { state.panel = null; photo.onCrop() }
                    BarItem(Icons.Filled.Tune, "Ajustes", active = state.panel == P_ADJUST) { if (state.panel == P_ADJUST) state.panel = null else photo.onAdjust() }
                }
                BarItem(Icons.Filled.Flip, "Voltear", active = state.panel == P_FLIP, enabled = !locked) { toggle(P_FLIP) }
                BarItem(Icons.Filled.FormatAlignCenter, "Alinear", active = state.panel == P_ALIGN, enabled = !locked) { toggle(P_ALIGN) }
                BarItem(Icons.Filled.FlipToFront, "Orden", active = state.panel == P_ORDER) { toggle(P_ORDER) }
                BarItem(Icons.Filled.Opacity, "Opacidad", active = state.panel == P_ALPHA) { toggle(P_ALPHA) }
                BarItem(Icons.Filled.ContentCopy, "Duplicar", enabled = sel.any { it.canDuplicate }) { state.duplicateSelected() }
                BarItem(Icons.Filled.Delete, "Borrar") { state.deleteSelected() }
                BarItem(if (locked) Icons.Filled.LockOpen else Icons.Filled.Lock, if (locked) "Desbloquear" else "Bloquear") { state.setLocked(!locked) }
                if (state.canGroup) BarItem(Icons.Filled.Workspaces, "Agrupar") { state.group() }
                if (state.canUngroup) BarItem(Icons.Filled.Workspaces, "Desagrupar") { state.ungroup() }
                BarItem(Icons.Filled.LibraryAddCheck, "Elegir varios", active = state.multiSelect) { state.multiSelect = !state.multiSelect }
                BarItem(Icons.Filled.Layers, "Capas", active = state.panel == P_LAYERS) { toggle(P_LAYERS) }
            }
        }
    }
}

// ----------------------------------------------------------------- paneles

@Composable
private fun <S> PanelHost(
    state: CanvaState<S>,
    key: String,
    tools: List<CanvaTool>,
    editPanel: (@Composable ColumnScope.(CLayer) -> Unit)?,
    canvasPx: Pair<Float, Float>,
    photoBitmap: Pair<String, Bitmap>?
) {
    val maxH = (LocalConfiguration.current.screenHeightDp * 0.46f).dp
    val tool = tools.firstOrNull { it.key == key }
    val title = when (key) {
        P_EDIT -> "Editar"
        P_ALIGN -> "Alinear"
        P_ORDER -> "Orden y giro"
        P_ALPHA -> "Opacidad"
        P_LAYERS -> "Capas"
        P_FLIP -> "Voltear"
        P_ADJUST -> "Ajustes de foto"
        else -> tool?.label ?: ""
    }
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.heightIn(max = maxH)) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 4.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = { state.panel = null }) { Icon(Icons.Filled.KeyboardArrowDown, "Cerrar panel") }
            }
            Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 12.dp)) {
                val (wPx, hPx) = canvasPx
                when (key) {
                    P_EDIT -> state.selected.singleOrNull()?.let { l -> editPanel?.invoke(this, l) }
                    P_ALIGN -> AlignPanel(state, wPx, hPx)
                    P_ORDER -> OrderPanel(state)
                    P_ALPHA -> {
                        val a = state.selected.firstOrNull()?.alpha ?: 1f
                        Column(Modifier.padding(horizontal = 20.dp)) {
                            LabeledSlider("Opacidad", a, 0.05f..1f, "${(a * 100).roundToInt()}%") { state.setAlpha(it) }
                        }
                    }
                    P_LAYERS -> LayersPanel(state)
                    P_FLIP -> Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                        PanelButton(Icons.Filled.Flip, "Horizontal") { state.flip(true, wPx, hPx) }
                        PanelButton(Icons.Filled.Flip, "Vertical", rotate = 90f) { state.flip(false, wPx, hPx) }
                    }
                    P_ADJUST -> state.selected.singleOrNull()?.let { l ->
                        val adj = l.photo
                        if (adj != null) AdjustPanel(photoBitmap?.takeIf { it.first == l.id }?.second, adj) { next, slide ->
                            state.setPhoto(l.id, next, coalesce = slide)
                        }
                    }
                    else -> tool?.content?.invoke(this)
                }
            }
        }
    }
}

@Composable
private fun PanelButton(icon: ImageVector, label: String, enabled: Boolean = true, rotate: Float = 0f, onClick: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .width(84.dp)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        Icon(icon, null, tint = tint, modifier = Modifier.size(26.dp).graphicsLayer { rotationZ = rotate })
        Spacer(Modifier.size(4.dp))
        Text(label, fontSize = 12.sp, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun <S> AlignPanel(state: CanvaState<S>, wPx: Float, hPx: Float) {
    val many = state.selection.size > 1
    Text(
        if (many) "Se alinean entre ellas." else "Se alinea a la pantalla. Para alinear varias, elegilas con «Elegir varios» o manteniendo presionado.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp)
    )
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp)) {
        PanelButton(Icons.Filled.AlignHorizontalLeft, "Izquierda") { state.align(AlignMode.LEFT, wPx, hPx) }
        PanelButton(Icons.Filled.AlignHorizontalCenter, "Centro") { state.align(AlignMode.CENTER_H, wPx, hPx) }
        PanelButton(Icons.Filled.AlignHorizontalRight, "Derecha") { state.align(AlignMode.RIGHT, wPx, hPx) }
    }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp)) {
        PanelButton(Icons.Filled.AlignVerticalTop, "Arriba") { state.align(AlignMode.TOP, wPx, hPx) }
        PanelButton(Icons.Filled.AlignVerticalCenter, "Medio") { state.align(AlignMode.CENTER_V, wPx, hPx) }
        PanelButton(Icons.Filled.AlignVerticalBottom, "Abajo") { state.align(AlignMode.BOTTOM, wPx, hPx) }
    }
    val blocks = state.selected.map { it.groupId ?: it.id }.distinct().size
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        PanelButton(Icons.Filled.HorizontalDistribute, "Repartir ↔", enabled = blocks >= 3) { state.distribute(true, wPx, hPx) }
        PanelButton(Icons.Filled.VerticalDistribute, "Repartir ↕", enabled = blocks >= 3) { state.distribute(false, wPx, hPx) }
    }
    if (blocks < 3) Text(
        "Para repartir el espacio parejo elegí tres o más.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp)
    )
}

@Composable
private fun <S> OrderPanel(state: CanvaState<S>) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp)) {
        PanelButton(Icons.Filled.FlipToFront, "Al frente") { state.bringToFront() }
        PanelButton(Icons.Filled.KeyboardArrowUp, "Adelante") { state.bringForward() }
        PanelButton(Icons.Filled.KeyboardArrowDown, "Atrás") { state.sendBackward() }
        PanelButton(Icons.Filled.FlipToBack, "Al fondo") { state.sendToBack() }
    }
    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { state.resetRotation() }, enabled = state.selected.any { it.rotation != 0f && !it.locked }) { Text("Enderezar (0°)") }
    }
    state.selected.singleOrNull()?.let { l ->
        Text(
            "Giro: ${l.rotation.roundToInt()}° · tamaño ${(l.w * 100).roundToInt()}% × ${(l.h * 100).roundToInt()}%",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun <S> LayersPanel(state: CanvaState<S>) {
    val list = state.layers
    if (list.isEmpty()) {
        Text("Todavía no hay nada. Agregá texto, elementos o fotos con la barra de abajo.", modifier = Modifier.padding(horizontal = 20.dp))
        return
    }
    Text(
        "Lo de arriba de la lista se ve adelante. Tocá una capa para elegirla.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp)
    )
    list.asReversed().forEachIndexed { i, l ->
        val selected = l.id in state.selection
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                .clickable { if (!l.hidden) state.tap(l.id) }
                .padding(start = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(l.label.ifBlank { "Capa" }, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (l.hidden) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                if (l.groupId != null) Text("En un grupo", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { state.moveLayer(l.id, 1) }, enabled = i > 0) { Icon(Icons.Filled.KeyboardArrowUp, "Subir") }
            IconButton(onClick = { state.moveLayer(l.id, -1) }, enabled = i < list.lastIndex) { Icon(Icons.Filled.KeyboardArrowDown, "Bajar") }
            IconButton(onClick = { state.toggleHidden(l.id) }) {
                Icon(if (l.hidden) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, if (l.hidden) "Mostrar" else "Ocultar")
            }
            IconButton(onClick = { state.toggleLockOf(l.id) }) {
                Icon(if (l.locked) Icons.Filled.Lock else Icons.Filled.LockOpen, if (l.locked) "Desbloquear" else "Bloquear")
            }
        }
    }
}

/** Grilla de opciones para los paneles de herramientas: miniatura arriba y nombre abajo. */
@Composable
fun CanvaSection(title: String, modifier: Modifier = Modifier) {
    Text(title, style = MaterialTheme.typography.titleSmall, modifier = modifier.padding(start = 20.dp, top = 12.dp, bottom = 6.dp))
}
