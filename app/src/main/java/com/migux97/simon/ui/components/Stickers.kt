package com.migux97.simon.ui.components

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.migux97.simon.custom.StickerPainter
import com.migux97.simon.data.BuiltinSticker
import com.migux97.simon.data.ImageStore
import com.migux97.simon.data.Motion
import com.migux97.simon.data.StickerAttach
import com.migux97.simon.data.StickerCatalog
import com.migux97.simon.data.StickerInfo
import com.migux97.simon.data.StickerKind
import com.migux97.simon.data.StudioStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** Miniatura de un sticker (se dibuja fuera del hilo principal). */
@Composable
fun StickerThumb(info: StickerInfo, modifier: Modifier = Modifier) {
    val bmp by produceState<Bitmap?>(null, info) {
        value = withContext(Dispatchers.Default) { runCatching { StickerPainter.thumbnail(info, 160) }.getOrNull() }
    }
    Box(modifier) { bmp?.let { Image(it.asImageBitmap(), info.name, Modifier.fillMaxSize()) } }
}

/** Diálogo para elegir un sticker: los tuyos, prediseñados, o crear uno nuevo. */
@Composable
fun StickerPickerDialog(onDismiss: () -> Unit, onPick: (StickerInfo) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mine by StudioStore.stickersFlow.collectAsStateWithLifecycle()
    val drawings by StudioStore.drawingsFlow.collectAsStateWithLifecycle()
    val animations by StudioStore.animationsFlow.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(if (mine.isEmpty()) 1 else 0) }
    // Creador de stickers prediseñados con colores propios
    var shape by remember { mutableStateOf(BuiltinSticker.HEART) }
    var c1 by remember { mutableIntStateOf(0xFFFF4F81.toInt()) }
    var c2 by remember { mutableIntStateOf(0xFFFF9EC4.toInt()) }
    var outline by remember { mutableStateOf(true) }

    val photo = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val path = withContext(Dispatchers.IO) { ImageStore.importImage(context, uri, 700) }
            if (path != null) {
                val s = StickerInfo(name = "Mi foto", kind = StickerKind.IMAGE, path = path)
                StudioStore.saveSticker(s)
                onPick(s)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
        title = { Text("Elegí un sticker") },
        text = {
            Column(Modifier.height(460.dp)) {
                TabRow(selectedTabIndex = tab) {
                    listOf("Míos", "Listos", "Crear").forEachIndexed { i, t -> Tab(tab == i, { tab = i }, text = { Text(t) }) }
                }
                when (tab) {
                    0 -> StickerGrid(mine, onPick, emptyText = "Todavía no tenés stickers propios. Crealos en «Crear».")
                    1 -> StickerGrid(StickerCatalog.builtins, onPick)
                    else -> Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { photo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Desde una foto (PNG sin fondo queda genial)")
                        }
                        if (drawings.isNotEmpty() || animations.isNotEmpty()) Text("Desde tus dibujos y animaciones", style = MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(drawings, key = { "d" + it.id }) { d ->
                                val preview = remember(d.id) { StickerInfo(id = "tmp_" + d.id, name = d.name, kind = StickerKind.DRAWING, mediaId = d.id) }
                                StickerThumb(preview, Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x22888888)).clickable {
                                    val s = StickerInfo(name = d.name, kind = StickerKind.DRAWING, mediaId = d.id)
                                    StudioStore.saveSticker(s); onPick(s)
                                })
                            }
                            items(animations, key = { "a" + it.id }) { a ->
                                Box(Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x22888888)).clickable {
                                    val s = StickerInfo(name = a.name, kind = StickerKind.ANIMATION, mediaId = a.id)
                                    StudioStore.saveSticker(s); onPick(s)
                                }) { com.migux97.simon.launcher.AnimatedFrames(a.id, Modifier.fillMaxSize()) }
                            }
                        }
                        Text("O armá uno prediseñado con tus colores", style = MaterialTheme.typography.labelLarge)
                        val custom = StickerInfo(id = "preview", name = shape.label, kind = StickerKind.BUILTIN, builtin = shape, color1 = c1, color2 = c2, outline = outline)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StickerThumb(custom, Modifier.size(64.dp))
                            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                                ColorRow("Color 1", c1) { c1 = it }
                                ColorRow("Color 2", c2) { c2 = it }
                            }
                        }
                        ChoiceChips(BuiltinSticker.entries, shape, { it.label }, { shape = it })
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Borde blanco", Modifier.weight(1f)); Switch(outline, { outline = it })
                        }
                        OutlinedButton(onClick = {
                            val s = custom.copy(id = com.migux97.simon.data.newId())
                            StudioStore.saveSticker(s); onPick(s)
                        }) { Text("Crear y usar") }
                    }
                }
            }
        }
    )
}

@Composable
private fun StickerGrid(list: List<StickerInfo>, onPick: (StickerInfo) -> Unit, emptyText: String? = null) {
    if (list.isEmpty() && emptyText != null) {
        Text(emptyText, modifier = Modifier.padding(top = 16.dp))
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(64.dp),
        contentPadding = PaddingValues(top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(list, key = { it.id }) { s ->
            StickerThumb(s, Modifier.aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(Color(0x14888888)).clickable { onPick(s) })
        }
    }
}

/**
 * Lista de stickers pegados a algo, con controles de posición, tamaño, giro y
 * movimiento. Se usa en íconos, widgets y elementos del launcher.
 */
@Composable
fun StickerAttachEditor(list: List<StickerAttach>, onChange: (List<StickerAttach>) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    var selectedId by remember { mutableStateOf(list.lastOrNull()?.id) }
    val selected = list.firstOrNull { it.id == selectedId } ?: list.lastOrNull()
    fun update(a: StickerAttach) = onChange(list.map { if (it.id == a.id) a else it })

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Stickers", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            FilledTonalButton(onClick = { picking = true }) { Text("Agregar sticker") }
        }
        if (list.isEmpty()) Text("Pegale stickers: corazones, coronas, tus fotos, dibujos o animaciones.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(list, key = { it.id }) { a ->
                val info = StudioStore.sticker(a.stickerId)
                Box(
                    Modifier.size(52.dp).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(if (a.id == selected?.id) 2.dp else 0.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                        .clickable { selectedId = a.id }
                        .padding(6.dp)
                ) { info?.let { StickerThumb(it, Modifier.fillMaxSize()) } }
            }
        }
        selected?.let { a ->
            LabeledSlider("Horizontal", a.x, -0.2f..1.2f, "${(a.x * 100).roundToInt()}%") { update(a.copy(x = it)) }
            LabeledSlider("Vertical", a.y, -0.2f..1.2f, "${(a.y * 100).roundToInt()}%") { update(a.copy(y = it)) }
            LabeledSlider("Tamaño", a.size, 0.1f..1.2f, "${(a.size * 100).roundToInt()}%") { update(a.copy(size = it)) }
            LabeledSlider("Giro", a.rotation, -180f..180f, "${a.rotation.roundToInt()}°") { update(a.copy(rotation = it)) }
        }
    }
    selected?.let { a ->
        ChoiceChips(Motion.entries, a.motion, { it.label }, { update(a.copy(motion = it)) })
        TextButton(onClick = {
            val rest = list.filterNot { it.id == a.id }
            onChange(rest); selectedId = rest.lastOrNull()?.id
        }, modifier = Modifier.padding(horizontal = 8.dp)) { Text("Quitar este sticker", color = MaterialTheme.colorScheme.error) }
    }
    if (picking) StickerPickerDialog(onDismiss = { picking = false }) { info ->
        val a = StickerAttach(stickerId = info.id)
        onChange(list + a)
        selectedId = a.id
        picking = false
    }
}
