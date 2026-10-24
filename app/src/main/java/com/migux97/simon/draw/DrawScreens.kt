package com.migux97.simon.draw

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.migux97.simon.data.AnimationInfo
import com.migux97.simon.data.DrawingInfo
import com.migux97.simon.data.ImageStore
import com.migux97.simon.data.LiveConfig
import com.migux97.simon.data.MediaLibrary
import com.migux97.simon.data.SceneType
import com.migux97.simon.data.StudioStore
import com.migux97.simon.ui.Nav
import com.migux97.simon.ui.components.BackTopBar
import com.migux97.simon.ui.components.ChoiceChips
import com.migux97.simon.ui.components.ColorPickerDialog
import com.migux97.simon.ui.components.ConfirmDialog
import com.migux97.simon.ui.components.LabeledSlider
import com.migux97.simon.ui.components.QuickColors
import com.migux97.simon.ui.components.UnsavedChangesDialog
import com.migux97.simon.ui.screens.TargetDialog
import com.migux97.simon.wallpaper.LiveWallpaper
import com.migux97.simon.wallpaper.StaticWallpapers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

private data class CanvasFormat(val label: String, val w: Int, val h: Int)

private val drawingFormats = listOf(
    CanvasFormat("Cuadrado", 1080, 1080),
    CanvasFormat("Fondo de pantalla", 1080, 2160),
    CanvasFormat("Horizontal", 1920, 1080),
    CanvasFormat("Ícono", 512, 512)
)

/** Herramientas compartidas por el editor de dibujo y el de animación. */
@Composable
private fun ToolPanel(
    tool: Tool, onTool: (Tool) -> Unit,
    color: Int, onColor: (Int) -> Unit,
    width: Float, onWidth: (Float) -> Unit,
    alpha: Float, onAlpha: (Float) -> Unit,
    filled: Boolean, onFilled: (Boolean) -> Unit
) {
    var picker by remember { mutableStateOf(false) }
    ChoiceChips(Tool.entries, tool, { it.label }, onTool)
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Box(
                Modifier.size(34.dp).clip(CircleShape)
                    .background(androidx.compose.ui.graphics.Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                    .clickable { picker = true }
            )
        }
        itemsIndexed(QuickColors) { _, c ->
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(Color(c))
                    .border(if (c == color) 3.dp else 1.dp, if (c == color) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    .clickable { onColor(c) }
            )
        }
    }
    Column(Modifier.padding(horizontal = 16.dp)) {
        LabeledSlider("Grosor", width, 0.002f..0.12f, "${(width * 1000).roundToInt()}") { onWidth(it) }
        LabeledSlider("Opacidad", alpha, 0.05f..1f, "${(alpha * 100).roundToInt()}%") { onAlpha(it) }
        if (tool in listOf(Tool.RECT, Tool.CIRCLE, Tool.HEART, Tool.STAR)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Forma rellena", Modifier.weight(1f)); Switch(filled, onFilled)
            }
        }
    }
    if (picker) ColorPickerDialog(color, "Color del pincel", onDismiss = { picker = false }, onPick = onColor)
}

// ================================================================= dibujo

/** Capa de trazos sin fondo, guardada al lado del dibujo para poder seguir editándolo. */
private fun layerFile(id: String) = File(MediaLibrary.drawingFile(id).parentFile, "$id.layer.png")

@Composable
fun DrawingScreen(nav: Nav, drawingId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val existing = remember(drawingId) { StudioStore.drawing(drawingId) }
    var format by remember { mutableStateOf(existing?.let { CanvasFormat("", it.width, it.height) } ?: drawingFormats[0]) }
    var name by remember { mutableStateOf(existing?.name ?: "Mi dibujo") }
    var base by remember { mutableStateOf<Bitmap?>(null) }
    val strokes = remember { mutableStateListOf<Stroke>() }
    val redo = remember { mutableStateListOf<Stroke>() }
    var current by remember { mutableStateOf<Stroke?>(null) }
    var tool by remember { mutableStateOf(Tool.PEN) }
    var color by remember { mutableIntStateOf(0xFF6C4CF5.toInt()) }
    var width by remember { mutableFloatStateOf(0.012f) }
    var alpha by remember { mutableFloatStateOf(1f) }
    var filled by remember { mutableStateOf(false) }
    var bgColor by remember { mutableStateOf(existing?.takeIf { it.hasLayer }?.bgColor) }
    var bgImagePath by remember { mutableStateOf(existing?.takeIf { it.hasLayer }?.bgImagePath) }
    var bgImage by remember { mutableStateOf<Bitmap?>(null) }
    var savedId by remember { mutableStateOf(existing?.id) }
    var wallpaperDialog by remember { mutableStateOf(false) }
    var clearDialog by remember { mutableStateOf(false) }
    var askLeave by remember { mutableStateOf(false) }
    var dirty by remember { mutableStateOf(false) }

    // Al reabrir: la capa de trazos sola + el fondo que tenía. Los dibujos viejos
    // (sin capa guardada) se abren como imagen única, con fondo transparente.
    LaunchedEffect(drawingId) {
        if (existing != null) {
            val loaded: Pair<Bitmap?, Boolean> = withContext(Dispatchers.IO) {
                val lf = layerFile(existing.id)
                val layer = if (existing.hasLayer && lf.exists()) ImageStore.load(lf.absolutePath, 2200) else null
                if (layer != null) layer to true else MediaLibrary.loadDrawing(existing.id, 2200) to false
            }
            base = loaded.first
            if (!loaded.second) {
                bgColor = null
                bgImagePath = null
            }
            val path = bgImagePath
            bgImage = if (path != null) withContext(Dispatchers.IO) { ImageStore.load(path, 2200) } else null
        }
    }

    val photo = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val path = withContext(Dispatchers.IO) { ImageStore.importImage(context, uri, 2200) }
            val bmp = if (path != null) withContext(Dispatchers.IO) { ImageStore.load(path, 2200) } else null
            if (path != null && bmp != null) {
                bgImagePath = path
                bgImage = bmp
                bgColor = null
                dirty = true
            } else Toast.makeText(context, "No se pudo abrir la foto", Toast.LENGTH_SHORT).show()
        }
    }

    suspend fun save(): DrawingInfo {
        val id = savedId ?: existing?.id ?: com.migux97.simon.data.newId()
        val info = (existing ?: DrawingInfo(name = name, width = format.w, height = format.h)).copy(
            id = id, name = name, width = format.w, height = format.h,
            bgColor = bgColor, bgImagePath = if (bgImage != null) bgImagePath else null, hasLayer = true
        )
        val w = format.w
        val h = format.h
        val bc = bgColor
        val bi = bgImage
        val b = base
        val list = strokes.toList()
        val flat = withContext(Dispatchers.Default) { DrawCore.toBitmap(w, h, bc, bi, b, list) }
        val layer = withContext(Dispatchers.Default) { DrawCore.toBitmap(w, h, null, null, b, list) }
        withContext(Dispatchers.IO) {
            val lf = layerFile(id)
            FileOutputStream(lf).use { layer.compress(Bitmap.CompressFormat.PNG, 100, it) }
            ImageStore.evict(lf.absolutePath)
            MediaLibrary.saveDrawing(info, flat)
        }
        savedId = id
        dirty = false
        return info
    }

    fun leave() {
        if (dirty) askLeave = true else nav.back()
    }

    BackHandler(enabled = dirty) { askLeave = true }

    Column(Modifier.fillMaxSize()) {
        BackTopBar(name, onBack = { leave() }) {
            IconButton(onClick = { strokes.removeLastOrNull()?.let { redo.add(it); dirty = true } }, enabled = strokes.isNotEmpty()) { Icon(Icons.AutoMirrored.Filled.Undo, "Deshacer") }
            IconButton(onClick = { redo.removeLastOrNull()?.let { strokes.add(it); dirty = true } }, enabled = redo.isNotEmpty()) { Icon(Icons.AutoMirrored.Filled.Redo, "Rehacer") }
        }
        Box(
            Modifier.fillMaxWidth().weight(1f).padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            DrawingSurface(
                strokes = strokes, base = base, bgColor = bgColor, bgImage = bgImage, onion = null,
                tool = tool, color = color, width = width, alpha = alpha, filled = filled,
                current = current, onCurrent = { current = it }, onStroke = { strokes.add(it); redo.clear(); dirty = true },
                modifier = Modifier
                    .aspectRatio(format.w.toFloat() / format.h)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            )
        }
        Column(Modifier.weight(0.9f).verticalScroll(rememberScrollState())) {
            ToolPanel(tool, { tool = it }, color, { color = it }, width, { width = it }, alpha, { alpha = it }, filled, { filled = it })
            if (existing == null && strokes.isEmpty() && base == null) {
                Text("Formato", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 16.dp, top = 8.dp))
                ChoiceChips(drawingFormats, format, { it.label }, { format = it })
            }
            Text("Fondo", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 16.dp, top = 8.dp))
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(bgColor == null && bgImage == null, {
                    bgColor = null; bgImage = null; bgImagePath = null; dirty = true
                }, label = { Text("Transparente") })
                FilterChip(bgColor != null, {
                    bgColor = color; bgImage = null; bgImagePath = null; dirty = true
                }, label = { Text("Color del pincel") })
                FilterChip(bgImage != null, { photo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, label = { Text(if (bgImage != null) "Cambiar foto" else "Foto") })
            }
            OutlinedTextField(name, { name = it.take(30); dirty = true }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(16.dp))
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { clearDialog = true }) {
                    Icon(Icons.Filled.Delete, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Borrar…")
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        Surface(tonalElevation = 3.dp) {
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { wallpaperDialog = true }, modifier = Modifier.weight(1f)) { Text("Usar de fondo") }
                Button(onClick = {
                    scope.launch {
                        save()
                        Toast.makeText(context, "Guardado. Usalo en widgets, íconos y el launcher.", Toast.LENGTH_LONG).show()
                        nav.back()
                    }
                }, modifier = Modifier.weight(1f)) { Text("Guardar") }
            }
        }
    }

    if (clearDialog) AlertDialog(
        onDismissRequest = { clearDialog = false },
        title = { Text("¿Qué querés borrar?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(enabled = strokes.isNotEmpty(), onClick = {
                    redo.clear()
                    strokes.clear()
                    dirty = true
                    clearDialog = false
                }, modifier = Modifier.fillMaxWidth()) { Text("Solo los trazos de ahora") }
                Text(
                    "Deja lo que ya estaba guardado y borra lo que dibujaste desde que abriste.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = {
                    redo.clear()
                    strokes.clear()
                    base = null
                    bgColor = null
                    bgImage = null
                    bgImagePath = null
                    dirty = true
                    clearDialog = false
                }, modifier = Modifier.fillMaxWidth()) { Text("Empezar de cero (lienzo vacío)") }
                Text(
                    "Borra todo el dibujo y el fondo. Hasta que toques «Guardar» no se pierde nada.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { TextButton(onClick = { clearDialog = false }) { Text("Cancelar") } }
    )

    if (askLeave) UnsavedChangesDialog(
        onSave = {
            askLeave = false
            scope.launch {
                save()
                nav.back()
            }
        },
        onDiscard = {
            askLeave = false
            dirty = false
            nav.back()
        },
        onDismiss = { askLeave = false }
    )

    if (wallpaperDialog) TargetDialog("Usar como fondo de pantalla", onDismiss = { wallpaperDialog = false }) { target ->
        wallpaperDialog = false
        scope.launch {
            save()
            val bmp = withContext(Dispatchers.Default) { DrawCore.toBitmap(format.w, format.h, bgColor ?: 0xFF000000.toInt(), bgImage, base, strokes.toList()) }
            val ok = withContext(Dispatchers.IO) { StaticWallpapers.apply(context, bmp, target) }
            Toast.makeText(context, if (ok) "¡Fondo aplicado!" else "No se pudo aplicar", Toast.LENGTH_SHORT).show()
        }
    }
}

// ============================================================== animación

@Composable
fun AnimationScreen(nav: Nav, animationId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val existing = remember(animationId) { StudioStore.animation(animationId) }
    val size = existing?.width ?: 720
    val sizeH = existing?.height ?: 720
    var name by remember { mutableStateOf(existing?.name ?: "Mi animación") }
    var fps by remember { mutableIntStateOf(existing?.fps ?: 8) }
    var loop by remember { mutableStateOf(existing?.loop ?: true) }
    var bounce by remember { mutableStateOf(existing?.bounce ?: false) }

    // Cada cuadro: imagen base (si venía guardado) + trazos nuevos.
    val bases = remember { mutableStateListOf<Bitmap?>() }
    val frames = remember { mutableStateListOf<List<Stroke>>() }
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(animationId) {
        val old = if (existing != null) withContext(Dispatchers.IO) { MediaLibrary.loadFrames(existing.id, 2000) } else emptyList()
        if (old.isEmpty()) { bases.add(null); frames.add(emptyList()) } else old.forEach { bases.add(it); frames.add(emptyList()) }
        loaded = true
    }
    var index by remember { mutableIntStateOf(0) }
    var current by remember { mutableStateOf<Stroke?>(null) }
    val redo = remember { mutableStateListOf<Stroke>() }
    var tool by remember { mutableStateOf(Tool.PEN) }
    var color by remember { mutableIntStateOf(0xFFFF4F81.toInt()) }
    var width by remember { mutableFloatStateOf(0.015f) }
    var alpha by remember { mutableFloatStateOf(1f) }
    var filled by remember { mutableStateOf(false) }
    var onionOn by remember { mutableStateOf(true) }
    var playing by remember { mutableStateOf(false) }
    var playFrame by remember { mutableIntStateOf(0) }
    var bgColor by remember { mutableStateOf<Int?>(null) }
    var savedId by remember { mutableStateOf(existing?.id) }
    var applyDialog by remember { mutableStateOf(false) }
    var askLeave by remember { mutableStateOf(false) }
    var askDeleteFrame by remember { mutableStateOf(false) }
    var dirty by remember { mutableStateOf(false) }

    fun frameBitmap(i: Int, w: Int = size, h: Int = sizeH, withBg: Boolean = true): Bitmap =
        DrawCore.toBitmap(w, h, if (withBg) bgColor else null, null, bases.getOrNull(i), frames.getOrNull(i) ?: emptyList())

    fun select(i: Int) {
        if (i != index) redo.clear()
        index = i
        playing = false
    }

    /** Mueve el cuadro actual una posición (−1 antes, +1 después). */
    fun moveFrame(delta: Int) {
        val to = index + delta
        if (to !in frames.indices) return
        val f = frames.removeAt(index)
        frames.add(to, f)
        val b = bases.removeAt(index)
        bases.add(to, b)
        index = to
        redo.clear()
        dirty = true
    }

    val onion: Bitmap? = if (onionOn && index > 0 && loaded && !playing) remember(index, frames.getOrNull(index - 1), bases.getOrNull(index - 1)) { frameBitmap(index - 1, 360, (360f * sizeH / size).toInt()) } else null
    var preview by remember { mutableStateOf<List<Bitmap>>(emptyList()) }

    LaunchedEffect(playing) {
        if (playing) {
            preview = withContext(Dispatchers.Default) { frames.indices.map { frameBitmap(it, 480, (480f * sizeH / size).toInt()) } }
            val start = System.currentTimeMillis()
            val info = AnimationInfo(name = name, width = size, height = sizeH, frames = frames.size, fps = fps, loop = true, bounce = bounce)
            while (isActive && playing) {
                withFrameMillis { }
                playFrame = MediaLibrary.frameIndex(info, preview.size, System.currentTimeMillis() - start)
            }
        }
    }

    suspend fun save(): AnimationInfo {
        val id = savedId ?: existing?.id ?: com.migux97.simon.data.newId()
        val info = AnimationInfo(id = id, name = name, width = size, height = sizeH, frames = frames.size, fps = fps, loop = loop, bounce = bounce)
        val bitmaps = withContext(Dispatchers.Default) { frames.indices.map { frameBitmap(it) } }
        withContext(Dispatchers.IO) { MediaLibrary.saveAnimation(info, bitmaps) }
        savedId = id
        dirty = false
        return info
    }

    fun leave() {
        if (dirty) askLeave = true else nav.back()
    }

    BackHandler(enabled = dirty) { askLeave = true }

    Column(Modifier.fillMaxSize()) {
        BackTopBar(name, onBack = { leave() }) {
            IconButton(onClick = {
                if (index < frames.size) {
                    val list = frames[index]
                    if (list.isNotEmpty()) {
                        redo.add(list.last())
                        frames[index] = list.dropLast(1)
                        dirty = true
                    }
                }
            }, enabled = !playing && (frames.getOrNull(index)?.isNotEmpty() == true)) { Icon(Icons.AutoMirrored.Filled.Undo, "Deshacer") }
            IconButton(onClick = {
                if (index < frames.size) redo.removeLastOrNull()?.let { s ->
                    frames[index] = frames[index] + s
                    dirty = true
                }
            }, enabled = !playing && redo.isNotEmpty()) { Icon(Icons.AutoMirrored.Filled.Redo, "Rehacer") }
            IconButton(onClick = { playing = !playing }) { Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (playing) "Pausar" else "Reproducir") }
        }
        Box(Modifier.fillMaxWidth().weight(1f).padding(12.dp), contentAlignment = Alignment.Center) {
            if (playing) {
                preview.getOrNull(playFrame)?.let {
                    Image(it.asImageBitmap(), null, Modifier.aspectRatio(size.toFloat() / sizeH).clip(RoundedCornerShape(12.dp)))
                }
            } else if (loaded && index < frames.size) {
                DrawingSurface(
                    strokes = frames[index], base = bases.getOrNull(index), bgColor = bgColor, bgImage = null, onion = onion,
                    tool = tool, color = color, width = width, alpha = alpha, filled = filled,
                    current = current, onCurrent = { current = it },
                    onStroke = { s -> frames[index] = frames[index] + s; redo.clear(); dirty = true },
                    modifier = Modifier
                        .aspectRatio(size.toFloat() / sizeH)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                )
            }
        }
        // Tira de cuadros
        LazyRow(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            itemsIndexed(frames.toList()) { i, _ ->
                Box(
                    Modifier.size(46.dp).clip(RoundedCornerShape(10.dp))
                        .background(if (i == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { select(i) },
                    contentAlignment = Alignment.Center
                ) { Text("${i + 1}", color = if (i == index) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface) }
            }
        }
        // Acciones sobre el cuadro elegido
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(enabled = loaded && frames.size < 60, onClick = {
                frames.add(index + 1, emptyList()); bases.add(index + 1, null); select(index + 1); dirty = true
            }) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(4.dp)); Text("Nuevo") }
            TextButton(enabled = loaded && frames.size < 60 && index < frames.size, onClick = {
                frames.add(index + 1, frames[index]); bases.add(index + 1, bases[index]); select(index + 1); dirty = true
            }) { Icon(Icons.Filled.ContentCopy, null); Spacer(Modifier.width(4.dp)); Text("Duplicar") }
            TextButton(enabled = index > 0, onClick = { moveFrame(-1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null); Text("Mover antes")
            }
            TextButton(enabled = index < frames.lastIndex, onClick = { moveFrame(1) }) {
                Text("Mover después"); Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
            }
            TextButton(enabled = frames.size > 1, onClick = { askDeleteFrame = true }) {
                Icon(Icons.Filled.Delete, null); Spacer(Modifier.width(4.dp)); Text("Borrar")
            }
        }
        Column(Modifier.weight(0.85f).verticalScroll(rememberScrollState())) {
            ToolPanel(tool, { tool = it }, color, { color = it }, width, { width = it }, alpha, { alpha = it }, filled, { filled = it })
            Column(Modifier.padding(horizontal = 16.dp)) {
                LabeledSlider("Velocidad", fps.toFloat(), 1f..30f, "$fps cuadros/seg") { fps = it.roundToInt(); dirty = true }
                Row(verticalAlignment = Alignment.CenterVertically) { Text("Ver cuadro anterior (papel cebolla)", Modifier.weight(1f)); Switch(onionOn, { onionOn = it }) }
                Row(verticalAlignment = Alignment.CenterVertically) { Text("Repetir en bucle", Modifier.weight(1f)); Switch(loop, { loop = it; dirty = true }) }
                Row(verticalAlignment = Alignment.CenterVertically) { Text("Ida y vuelta", Modifier.weight(1f)); Switch(bounce, { bounce = it; dirty = true }) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(bgColor == null, { bgColor = null; dirty = true }, label = { Text("Fondo transparente") })
                    FilterChip(bgColor != null, { bgColor = color; dirty = true }, label = { Text("Fondo de color") })
                }
                OutlinedTextField(name, { name = it.take(30); dirty = true }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
            }
        }
        Surface(tonalElevation = 3.dp) {
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { applyDialog = true }, modifier = Modifier.weight(1f)) { Text("Usar de fondo") }
                Button(onClick = {
                    scope.launch {
                        save()
                        Toast.makeText(context, "Guardada. Usala en widgets, el launcher y los efectos.", Toast.LENGTH_LONG).show()
                        nav.back()
                    }
                }, modifier = Modifier.weight(1f)) { Text("Guardar") }
            }
        }
    }

    if (askDeleteFrame) ConfirmDialog(
        title = "¿Borrar el cuadro ${index + 1}?",
        message = "Se quita este cuadro de la animación.",
        confirm = "Borrar",
        onDismiss = { askDeleteFrame = false },
        onConfirm = {
            askDeleteFrame = false
            if (frames.size > 1 && index < frames.size) {
                frames.removeAt(index)
                bases.removeAt(index)
                redo.clear()
                index = index.coerceAtMost(frames.lastIndex)
                dirty = true
            }
        }
    )

    if (askLeave) UnsavedChangesDialog(
        onSave = {
            askLeave = false
            scope.launch {
                save()
                nav.back()
            }
        },
        onDiscard = {
            askLeave = false
            dirty = false
            nav.back()
        },
        onDismiss = { askLeave = false }
    )

    if (applyDialog) AlertDialog(
        onDismissRequest = { applyDialog = false },
        title = { Text("Fondo animado") },
        text = { Text("Tu animación va a quedar como fondo de pantalla animado, ocupando toda la pantalla.") },
        confirmButton = {
            TextButton(onClick = {
                applyDialog = false
                scope.launch {
                    val info = save()
                    val cfg = LiveConfig(
                        name = info.name, scene = SceneType.USER_ANIMATION,
                        color1 = bgColor ?: 0xFF000000.toInt(), color2 = 0xFFFFFFFF.toInt(), color3 = 0xFF6C4CF5.toInt(),
                        animationId = info.id
                    )
                    if (LiveWallpaper.apply(context, cfg)) Toast.makeText(context, "¡Fondo animado aplicado!", Toast.LENGTH_SHORT).show()
                }
            }) { Text("Aplicar") }
        },
        dismissButton = { TextButton(onClick = { applyDialog = false }) { Text("Cancelar") } }
    )
}
