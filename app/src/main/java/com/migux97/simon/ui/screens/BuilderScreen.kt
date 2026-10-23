package com.migux97.simon.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.migux97.simon.custom.CustomWidgetLive
import com.migux97.simon.custom.Tokens
import com.migux97.simon.data.Catalog
import com.migux97.simon.data.CustomWidget
import com.migux97.simon.data.ElementType
import com.migux97.simon.data.FontStyle
import com.migux97.simon.data.HomeItem
import com.migux97.simon.data.ImageStore
import com.migux97.simon.data.ItemType
import com.migux97.simon.data.MediaLibrary
import com.migux97.simon.data.Motion
import com.migux97.simon.data.ProgressSource
import com.migux97.simon.data.ProgressStyle
import com.migux97.simon.data.Repository
import com.migux97.simon.data.ShapeKind
import com.migux97.simon.data.StudioStore
import com.migux97.simon.data.TapAction
import com.migux97.simon.data.WElement
import com.migux97.simon.data.newId
import com.migux97.simon.launcher.AnimatedFrames
import com.migux97.simon.launcher.AppCatalog
import com.migux97.simon.launcher.AppPickerList
import com.migux97.simon.launcher.LauncherLayout
import com.migux97.simon.ui.Nav
import com.migux97.simon.ui.components.BackTopBar
import com.migux97.simon.ui.components.ChoiceChips
import com.migux97.simon.ui.components.ColorRow
import com.migux97.simon.ui.components.LabeledSlider
import com.migux97.simon.ui.components.Panel
import com.migux97.simon.ui.components.PreviewBackdrop
import com.migux97.simon.ui.components.SceneThumb
import com.migux97.simon.widget.Place
import com.migux97.simon.widget.RefreshWorker
import com.migux97.simon.widget.WeatherService
import com.migux97.simon.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** Elementos nuevos con valores lindos por defecto. */
private fun newElement(type: ElementType): WElement = when (type) {
    ElementType.TEXT -> WElement(type = type, text = "{hora}", x = 0.1f, y = 0.25f, w = 0.8f, h = 0.4f, font = FontStyle.LIGHT)
    ElementType.CLOCK -> WElement(type = type, x = 0.3f, y = 0.1f, w = 0.4f, h = 0.8f, fill1 = 0xFFFFFFFF.toInt(), fill2 = 0xFFFF7EB6.toInt())
    ElementType.SHAPE -> WElement(type = type, x = 0.3f, y = 0.3f, w = 0.4f, h = 0.4f, shape = ShapeKind.ROUNDED, alpha = 0.9f)
    ElementType.PROGRESS -> WElement(type = type, x = 0.1f, y = 0.78f, w = 0.8f, h = 0.1f, fill1 = 0xFFFFD166.toInt(), fill2 = 0xFFFFFFFF.toInt())
    ElementType.APP_ICON -> WElement(type = type, x = 0.4f, y = 0.3f, w = 0.2f, h = 0.4f, action = TapAction.OPEN_APP)
    ElementType.WEATHER_ICON -> WElement(type = type, x = 0.05f, y = 0.15f, w = 0.3f, h = 0.7f)
    else -> WElement(type = type, x = 0.25f, y = 0.15f, w = 0.5f, h = 0.7f, shape = ShapeKind.ROUNDED, corner = 0.2f)
}

/** Plantillas de punto de partida para el constructor. */
private fun starter(kind: Int): CustomWidget = when (kind) {
    1 -> CustomWidget(
        name = "Saludo con hora", aspect = 2.1f, bg1 = 0xFF0F2027.toInt(), bg2 = 0xFF2C5364.toInt(),
        elements = listOf(
            WElement(type = ElementType.TEXT, text = "{saludo}", x = 0.06f, y = 0.1f, w = 0.6f, h = 0.2f, align = 0, font = FontStyle.CURSIVE, textColor = 0xFFFFD166.toInt()),
            WElement(type = ElementType.TEXT, text = "{hora}", x = 0.06f, y = 0.3f, w = 0.55f, h = 0.42f, align = 0, font = FontStyle.LIGHT),
            WElement(type = ElementType.TEXT, text = "{dia} {num} de {mes}", x = 0.06f, y = 0.74f, w = 0.6f, h = 0.15f, align = 0),
            WElement(type = ElementType.PROGRESS, progress = ProgressSource.BATTERY, progressStyle = ProgressStyle.RING, x = 0.68f, y = 0.18f, w = 0.27f, h = 0.64f, fill1 = 0xFF34C759.toInt(), fill2 = 0xFFFFFFFF.toInt()),
            WElement(type = ElementType.TEXT, text = "{bateria}", x = 0.7f, y = 0.4f, w = 0.23f, h = 0.2f)
        )
    )
    2 -> CustomWidget(
        name = "Corazones", aspect = 1f, bg1 = 0xFFFFDDE1.toInt(), bg2 = 0xFFEE9CA7.toInt(),
        elements = listOf(
            WElement(type = ElementType.SHAPE, shape = ShapeKind.HEART, x = 0.25f, y = 0.12f, w = 0.5f, h = 0.5f, fill1 = 0xFFFF4F81.toInt(), motion = Motion.PULSE),
            WElement(type = ElementType.TEXT, text = "{hora}", x = 0.1f, y = 0.62f, w = 0.8f, h = 0.22f, textColor = 0xFF5A2340.toInt(), font = FontStyle.CASUAL),
            WElement(type = ElementType.SHAPE, shape = ShapeKind.SPARKLE, x = 0.75f, y = 0.08f, w = 0.15f, h = 0.15f, fill1 = 0xFFFFFFFF.toInt(), motion = Motion.BLINK)
        )
    )
    3 -> CustomWidget(
        name = "Progreso del año", aspect = 3f, bg1 = 0xFF1C1C1E.toInt(), bg2 = 0xFF2C2C2E.toInt(), bgGradient = false,
        elements = listOf(
            WElement(type = ElementType.TEXT, text = "{año}", x = 0.05f, y = 0.12f, w = 0.3f, h = 0.35f, align = 0, font = FontStyle.MONO, textColor = 0xFFFF9F0A.toInt()),
            WElement(type = ElementType.PROGRESS, progress = ProgressSource.YEAR, progressStyle = ProgressStyle.DOTS, x = 0.05f, y = 0.58f, w = 0.9f, h = 0.25f, fill1 = 0xFFFF9F0A.toInt(), fill2 = 0xFFFFFFFF.toInt())
        )
    )
    else -> CustomWidget(name = "Mi widget", aspect = 2f, elements = listOf(newElement(ElementType.TEXT)))
}

@Composable
fun BuilderScreen(nav: Nav, widgetId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val initial = remember(widgetId) {
        StudioStore.widget(widgetId) ?: when (widgetId) {
            "new1" -> starter(1); "new2" -> starter(2); "new3" -> starter(3)
            else -> starter(0)
        }
    }
    var w by remember(widgetId) { mutableStateOf(initial) }
    var selectedId by remember { mutableStateOf<String?>(w.elements.firstOrNull()?.id) }
    var tab by remember { mutableIntStateOf(0) }
    var addMenu by remember { mutableStateOf(false) }
    var pickApp by remember { mutableStateOf<((String) -> Unit)?>(null) }
    val selected = w.elements.firstOrNull { it.id == selectedId }

    fun updateEl(e: WElement) { w = w.copy(elements = w.elements.map { if (it.id == e.id) e else it }) }
    fun save(): CustomWidget {
        StudioStore.saveWidget(w)
        WidgetUpdater.updateCustomWidget(context, w.id)
        if (w.city.isNotBlank()) RefreshWorker.runNow(context)
        return w
    }

    var pickTarget by remember { mutableStateOf("") }
    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val isVideo = pickTarget == "video"
            val path = withContext(Dispatchers.IO) { if (isVideo) ImageStore.importVideo(context, uri) else ImageStore.importImage(context, uri, 1400) }
            if (path == null) return@launch
            when (pickTarget) {
                "bg" -> w = w.copy(bgImage = path)
                else -> selected?.let { updateEl(it.copy(mediaPath = path)) }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        BackTopBar(w.name, onBack = { save(); nav.back() }) {
            IconButton(onClick = { save(); Toast.makeText(context, "Widget guardado", Toast.LENGTH_SHORT).show() }) { Icon(Icons.Filled.Check, "Guardar") }
        }
        // Lienzo
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(PreviewBackdrop)
                .padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            BoxWithConstraints(
                Modifier
                    .fillMaxWidth(if (w.aspect < 1.2f) 0.62f else 1f)
                    .aspectRatio(w.aspect.coerceIn(0.4f, 4f))
                    .heightIn(max = 260.dp)
            ) {
                val wPx = constraints.maxWidth.toFloat()
                val hPx = constraints.maxHeight.toFloat()
                CustomWidgetLive(w, Modifier.fillMaxSize(), interactive = false)
                // Capa de selección y arrastre
                w.elements.forEach { e ->
                    val density = LocalDensity.current
                    val isSel = e.id == selectedId
                    Box(
                        Modifier
                            .offset(with(density) { (e.x * wPx).toDp() }, with(density) { (e.y * hPx).toDp() })
                            .size(with(density) { (e.w * wPx).toDp() }, with(density) { (e.h * hPx).toDp() })
                            .graphicsLayer { rotationZ = e.rotation }
                            .then(if (isSel) Modifier.border(1.5.dp, Color.White, RoundedCornerShape(4.dp)) else Modifier)
                            .pointerInput(e.id) { detectTapGestures { selectedId = e.id } }
                            .pointerInput(e.id) {
                                detectDragGestures(onDragStart = { selectedId = e.id }) { change, d ->
                                    change.consume()
                                    val cur = w.elements.firstOrNull { it.id == e.id } ?: return@detectDragGestures
                                    updateEl(cur.copy(x = (cur.x + d.x / wPx).coerceIn(-cur.w * 0.5f, 1f - cur.w * 0.5f), y = (cur.y + d.y / hPx).coerceIn(-cur.h * 0.5f, 1f - cur.h * 0.5f)))
                                }
                            }
                    ) {
                        if (isSel) Box(
                            Modifier
                                .align(Alignment.BottomEnd)
                                .offset(8.dp, 8.dp)
                                .size(22.dp)
                                .shadow(3.dp, CircleShape)
                                .clip(CircleShape)
                                .background(Color.White)
                                .pointerInput(e.id + "r") {
                                    detectDragGestures { change, d ->
                                        change.consume()
                                        val cur = w.elements.firstOrNull { it.id == e.id } ?: return@detectDragGestures
                                        updateEl(cur.copy(w = (cur.w + d.x / wPx).coerceIn(0.03f, 1.5f), h = (cur.h + d.y / hPx).coerceIn(0.03f, 1.5f)))
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Filled.OpenInFull, null, tint = Color(0xFF6C4CF5), modifier = Modifier.size(12.dp)) }
                    }
                }
            }
        }
        Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                FilledTonalButton(onClick = { addMenu = true }) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Elemento") }
                DropdownMenu(expanded = addMenu, onDismissRequest = { addMenu = false }) {
                    ElementType.entries.forEach { t ->
                        DropdownMenuItem(text = { Text(t.label) }, onClick = {
                            addMenu = false
                            val e = newElement(t)
                            w = w.copy(elements = w.elements + e)
                            selectedId = e.id
                            tab = 0
                        })
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            if (selected != null) {
                IconButton(onClick = {
                    val copy = selected.copy(id = newId(), x = selected.x + 0.04f, y = selected.y + 0.04f)
                    w = w.copy(elements = w.elements + copy); selectedId = copy.id
                }) { Icon(Icons.Filled.ContentCopy, "Duplicar") }
                IconButton(onClick = {
                    val list = w.elements.toMutableList(); val i = list.indexOf(selected)
                    if (i < list.lastIndex) { list.removeAt(i); list.add(i + 1, selected); w = w.copy(elements = list) }
                }) { Icon(Icons.Filled.ArrowUpward, "Subir capa") }
                IconButton(onClick = {
                    val list = w.elements.toMutableList(); val i = list.indexOf(selected)
                    if (i > 0) { list.removeAt(i); list.add(i - 1, selected); w = w.copy(elements = list) }
                }) { Icon(Icons.Filled.ArrowDownward, "Bajar capa") }
                IconButton(onClick = { w = w.copy(elements = w.elements - selected); selectedId = w.elements.lastOrNull()?.id }) { Icon(Icons.Filled.Delete, "Borrar") }
            }
        }
        ScrollableTabRow(selectedTabIndex = tab, edgePadding = 12.dp, containerColor = MaterialTheme.colorScheme.background) {
            listOf("Elemento", "Movimiento y toque", "Widget", "Capas").forEachIndexed { i, t -> Tab(tab == i, { tab = i }, text = { Text(t) }) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 6.dp)) {
            when (tab) {
                0 -> if (selected == null) Panel { Text("Agregá o seleccioná un elemento.") } else ElementProps(selected, { updateEl(it) },
                    pickMedia = { target -> pickTarget = target; mediaPicker.launch(PickVisualMediaRequest(if (target == "video") ActivityResultContracts.PickVisualMedia.VideoOnly else ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    pickApp = { cb -> pickApp = cb })
                1 -> if (selected == null) Panel { Text("Seleccioná un elemento.") } else MotionProps(selected, { updateEl(it) }) { cb -> pickApp = cb }
                2 -> WidgetProps(w, onChange = { w = it }, pickBg = { pickTarget = "bg"; mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, pickApp = { cb -> pickApp = cb })
                3 -> Panel {
                    w.elements.asReversed().forEach { e ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(if (e.id == selectedId) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable { selectedId = e.id; tab = 0 }
                                .padding(10.dp)
                        ) {
                            Text(e.type.label, Modifier.weight(1f))
                            Text(if (e.type == ElementType.TEXT) e.text.take(18) else "", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        Surface(tonalElevation = 3.dp) {
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    val saved = save()
                    val ok = WidgetUpdater.requestPinCustom(context, saved)
                    Toast.makeText(context, if (ok) "Confirmá para agregarlo" else "Tu launcher no permite agregarlo desde acá", Toast.LENGTH_LONG).show()
                }, modifier = Modifier.weight(1f)) { Text("Widget del sistema") }
                if (StudioStore.appMode.launcherEnabled) Button(onClick = {
                    val saved = save()
                    val s = StudioStore.launcherSettings
                    val ww = 0.92f
                    val hh = (ww / saved.aspect * 0.46f).coerceIn(0.08f, 0.6f)
                    val (x, y) = LauncherLayout.freeSpot(0, ww, hh, s)
                    StudioStore.upsertItem(HomeItem(type = ItemType.CUSTOM_WIDGET, x = x, y = y, w = ww, h = hh, customWidgetId = saved.id, z = StudioStore.maxZ(0) + 1))
                    Toast.makeText(context, "Agregado a Simón Launcher (página 1)", Toast.LENGTH_SHORT).show()
                }, modifier = Modifier.weight(1f)) { Text("Al launcher") }
            }
        }
    }

    pickApp?.let { cb ->
        val apps by AppCatalog.apps.collectAsStateWithLifecycle()
        androidx.compose.runtime.LaunchedEffect(Unit) { AppCatalog.ensureLoaded(context) }
        AlertDialog(
            onDismissRequest = { pickApp = null },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { pickApp = null }) { Text("Cancelar") } },
            title = { Text("Elegí una app") },
            text = { AppPickerList(apps) { app -> cb(app.key); pickApp = null } }
        )
    }
}

// ---------------------------------------------------------- propiedades

@Composable
private fun ElementProps(e: WElement, onChange: (WElement) -> Unit, pickMedia: (String) -> Unit, pickApp: ((String) -> Unit) -> Unit) {
    val drawings by StudioStore.drawingsFlow.collectAsStateWithLifecycle()
    val animations by StudioStore.animationsFlow.collectAsStateWithLifecycle()
    when (e.type) {
        ElementType.TEXT -> {
            Panel {
                OutlinedTextField(e.text, { onChange(e.copy(text = it)) }, label = { Text("Texto") }, modifier = Modifier.fillMaxWidth())
                Text("Datos en vivo (tocá para agregar):", style = MaterialTheme.typography.bodySmall)
            }
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(Tokens.all) { (token, label) -> AssistChip(onClick = { onChange(e.copy(text = e.text + token)) }, label = { Text(label) }) }
            }
            ChoiceChips(FontStyle.entries, e.font, { it.label }, { onChange(e.copy(font = it)) })
            Panel {
                ColorRow("Color", e.textColor) { onChange(e.copy(textColor = it)) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Negrita", Modifier.weight(1f)); Switch(e.bold, { onChange(e.copy(bold = it)) })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Sombra", Modifier.weight(1f)); Switch(e.shadow, { onChange(e.copy(shadow = it)) })
                }
            }
            ChoiceChips(listOf(0, 1, 2), e.align, { listOf("Izquierda", "Centro", "Derecha")[it] }, { onChange(e.copy(align = it)) })
        }
        ElementType.CLOCK -> Panel {
            ColorRow("Esfera", e.fill1) { onChange(e.copy(fill1 = it)) }
            ColorRow("Agujas", e.textColor) { onChange(e.copy(textColor = it)) }
            ColorRow("Segundero", e.fill2) { onChange(e.copy(fill2 = it)) }
        }
        ElementType.SHAPE -> {
            ChoiceChips(ShapeKind.entries, e.shape, { it.label }, { onChange(e.copy(shape = it)) })
            FillPanel(e, onChange)
            Panel {
                ColorRow("Borde", e.strokeColor) { onChange(e.copy(strokeColor = it)) }
                LabeledSlider("Grosor del borde", e.strokeWidth, 0f..1f, "${(e.strokeWidth * 100).roundToInt()}") { onChange(e.copy(strokeWidth = it)) }
                if (e.shape == ShapeKind.ROUNDED) LabeledSlider("Esquinas", e.corner, 0f..0.5f, "${(e.corner * 100).roundToInt()}") { onChange(e.copy(corner = it)) }
            }
        }
        ElementType.IMAGE, ElementType.VIDEO -> {
            Panel {
                FilledTonalButton(onClick = { pickMedia(if (e.type == ElementType.VIDEO) "video" else "image") }) {
                    Text(if (e.mediaPath == null) "Elegir ${if (e.type == ElementType.VIDEO) "video" else "foto"}" else "Cambiar")
                }
                if (e.type == ElementType.VIDEO) Text("El video se mueve en Simón Launcher; como widget del sistema se ve su primer cuadro.", style = MaterialTheme.typography.bodySmall)
            }
            ChoiceChips(listOf(ShapeKind.RECT, ShapeKind.ROUNDED, ShapeKind.CIRCLE, ShapeKind.HEART, ShapeKind.STAR, ShapeKind.HEXAGON), e.shape, { it.label }, { onChange(e.copy(shape = it)) })
        }
        ElementType.DRAWING -> MediaChooser(drawings.map { it.id to it.name }, e.mediaId, isAnimation = false) { onChange(e.copy(mediaId = it)) }
        ElementType.ANIMATION -> MediaChooser(animations.map { it.id to it.name }, e.mediaId, isAnimation = true) { onChange(e.copy(mediaId = it)) }
        ElementType.SCENE -> {
            Panel { Text("Elegí la escena") }
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Catalog.liveWallpapers, key = { it.id }) { lv ->
                    Box(Modifier.width(54.dp).height(90.dp).clip(RoundedCornerShape(12.dp))
                        .border(if (e.liveId == lv.id) 3.dp else 0.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                        .clickable { onChange(e.copy(liveId = lv.id)) }) { SceneThumb(lv, Modifier.fillMaxSize()) }
                }
            }
            ChoiceChips(listOf(ShapeKind.RECT, ShapeKind.ROUNDED, ShapeKind.CIRCLE), e.shape, { it.label }, { onChange(e.copy(shape = it)) })
        }
        ElementType.PROGRESS -> {
            ChoiceChips(ProgressSource.entries, e.progress, { it.label }, { onChange(e.copy(progress = it)) })
            ChoiceChips(ProgressStyle.entries, e.progressStyle, { it.label }, { onChange(e.copy(progressStyle = it)) })
            FillPanel(e, onChange, secondLabel = "Color del fondo")
            if (e.progress == ProgressSource.CUSTOM) Panel {
                LabeledSlider("Valor", e.customValue, 0f..1f, "${(e.customValue * 100).roundToInt()}%") { onChange(e.copy(customValue = it)) }
            }
        }
        ElementType.APP_ICON -> Panel {
            val app = AppCatalog.find(e.appKey)
            Text(app?.label ?: "Ninguna app elegida")
            FilledTonalButton(onClick = { pickApp { key -> onChange(e.copy(appKey = key, action = TapAction.OPEN_APP, actionData = key)) } }) { Text("Elegir app") }
        }
        ElementType.WEATHER_ICON -> Panel { Text("Usa la ciudad elegida en la pestaña «Widget».") }
        ElementType.STICKER -> {
            var picking by remember { mutableStateOf(false) }
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StudioStore.sticker(e.mediaId)?.let { com.migux97.simon.ui.components.StickerThumb(it, Modifier.size(56.dp)) }
                    Spacer(Modifier.width(10.dp))
                    FilledTonalButton(onClick = { picking = true }) { Text(if (e.mediaId == null) "Elegir sticker" else "Cambiar sticker") }
                }
            }
            if (picking) com.migux97.simon.ui.components.StickerPickerDialog(onDismiss = { picking = false }) { info ->
                onChange(e.copy(mediaId = info.id)); picking = false
            }
        }
    }
}

@Composable
private fun FillPanel(e: WElement, onChange: (WElement) -> Unit, secondLabel: String = "Color 2") {
    Panel {
        ColorRow("Color", e.fill1) { onChange(e.copy(fill1 = it)) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Degradado", Modifier.weight(1f)); Switch(e.gradient, { onChange(e.copy(gradient = it)) })
        }
        if (e.gradient || e.type == ElementType.PROGRESS) ColorRow(secondLabel, e.fill2) { onChange(e.copy(fill2 = it)) }
        if (e.gradient) LabeledSlider("Dirección", e.gradientAngle.toFloat(), 0f..360f, "${e.gradientAngle}°") { onChange(e.copy(gradientAngle = it.roundToInt())) }
    }
}

@Composable
private fun MediaChooser(options: List<Pair<String, String>>, selected: String?, isAnimation: Boolean, onPick: (String) -> Unit) {
    Panel {
        Text(if (isAnimation) "Elegí una de tus animaciones" else "Elegí uno de tus dibujos")
        if (options.isEmpty()) Text("Todavía no tenés. Creá uno en la pestaña Crear.", style = MaterialTheme.typography.bodySmall)
    }
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options, key = { it.first }) { (id, name) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x22888888))
                        .border(if (selected == id) 3.dp else 0.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                        .clickable { onPick(id) }
                ) {
                    if (isAnimation) AnimatedFrames(id, Modifier.fillMaxSize()) else {
                        val bmp by produceState<android.graphics.Bitmap?>(null, id) { value = withContext(Dispatchers.IO) { MediaLibrary.loadDrawing(id, 200) } }
                        bmp?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize()) }
                    }
                }
                Text(name, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

@Composable
private fun MotionProps(e: WElement, onChange: (WElement) -> Unit, pickApp: ((String) -> Unit) -> Unit) {
    Panel {
        LabeledSlider("Giro", e.rotation, -180f..180f, "${e.rotation.roundToInt()}°") { onChange(e.copy(rotation = it)) }
        LabeledSlider("Transparencia", e.alpha, 0.05f..1f, "${(e.alpha * 100).roundToInt()}%") { onChange(e.copy(alpha = it)) }
        Text("Movimiento", style = MaterialTheme.typography.titleMedium)
    }
    ChoiceChips(Motion.entries, e.motion, { it.label }, { onChange(e.copy(motion = it)) })
    if (e.motion != Motion.NONE) Panel {
        LabeledSlider("Velocidad", e.motionSpeed, 0.2f..3f, "x%.1f".format(e.motionSpeed)) { onChange(e.copy(motionSpeed = it)) }
    }
    Panel { Text("Al tocarlo", style = MaterialTheme.typography.titleMedium) }
    // "Abrir cajón" solo tiene sentido dentro de Simón Launcher.
    val actions = if (StudioStore.appMode.launcherEnabled) TapAction.entries else TapAction.entries.filter { it != TapAction.OPEN_DRAWER }
    ChoiceChips(actions, e.action, { it.label }, { onChange(e.copy(action = it)) })
    ActionData(e.action, e.actionData, { onChange(e.copy(actionData = it)) }, pickApp)
}

@Composable
private fun ActionData(action: TapAction, data: String, onChange: (String) -> Unit, pickApp: ((String) -> Unit) -> Unit) {
    when (action) {
        TapAction.OPEN_APP -> Panel {
            Text(AppCatalog.find(data)?.label ?: "Ninguna app elegida")
            FilledTonalButton(onClick = { pickApp { onChange(it) } }) { Text("Elegir app") }
        }
        TapAction.OPEN_URL -> Panel { OutlinedTextField(data, onChange, label = { Text("Link") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        TapAction.DIAL -> Panel { OutlinedTextField(data, onChange, label = { Text("Número") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        else -> Unit
    }
}

@Composable
private fun WidgetProps(w: CustomWidget, onChange: (CustomWidget) -> Unit, pickBg: () -> Unit, pickApp: ((String) -> Unit) -> Unit) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Place>>(emptyList()) }
    Panel {
        OutlinedTextField(w.name, { onChange(w.copy(name = it.take(30))) }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Text("Forma del widget", style = MaterialTheme.typography.titleMedium)
    }
    ChoiceChips(listOf(1f, 2f, 2.15f, 3f, 4f, 0.75f, 0.5f), w.aspect, {
        when (it) { 1f -> "Cuadrado"; 2f -> "Ancho"; 2.15f -> "Mediano 4×2"; 3f -> "Tira"; 4f -> "Barra"; 0.75f -> "Alto"; else -> "Columna" }
    }, { onChange(w.copy(aspect = it)) })
    Panel {
        ColorRow("Fondo 1", w.bg1) { onChange(w.copy(bg1 = it)) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Degradado", Modifier.weight(1f)); Switch(w.bgGradient, { onChange(w.copy(bgGradient = it)) })
        }
        if (w.bgGradient) {
            ColorRow("Fondo 2", w.bg2) { onChange(w.copy(bg2 = it)) }
            LabeledSlider("Dirección", w.bgAngle.toFloat(), 0f..360f, "${w.bgAngle}°") { onChange(w.copy(bgAngle = it.roundToInt())) }
        }
        LabeledSlider("Opacidad del color", w.bgAlpha, 0f..1f, "${(w.bgAlpha * 100).roundToInt()}%") { onChange(w.copy(bgAlpha = it)) }
        LabeledSlider("Esquinas", w.corner, 0f..0.5f, "${(w.corner * 100).roundToInt()}") { onChange(w.copy(corner = it)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = pickBg) { Text(if (w.bgImage == null) "Foto de fondo" else "Cambiar foto") }
            if (w.bgImage != null) OutlinedButton(onClick = { onChange(w.copy(bgImage = null)) }) { Text("Quitar") }
        }
    }
    Panel {
        Text(if (w.city.isBlank()) "Ciudad para el clima" else "Ciudad: ${w.city}", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(query, { query = it }, label = { Text("Buscar ciudad") }, singleLine = true, modifier = Modifier.weight(1f))
            TextButton(onClick = { scope.launch { results = WeatherService.searchCity(query) } }) { Text("Buscar") }
        }
        results.forEach { p ->
            Text(p.label, modifier = Modifier.fillMaxWidth().clickable {
                onChange(w.copy(city = p.name, lat = p.lat, lon = p.lon)); results = emptyList()
                scope.launch { WeatherService.fetch(p.lat, p.lon)?.let { Repository.putWeather(p.lat, p.lon, it) } }
            }.padding(vertical = 8.dp))
        }
    }
    Panel { Text("Al tocar el widget del sistema", style = MaterialTheme.typography.titleMedium) }
    ChoiceChips(TapAction.entries.filter { it != TapAction.OPEN_DRAWER }, w.action, { it.label }, { onChange(w.copy(action = it)) })
    ActionData(w.action, w.actionData, { onChange(w.copy(actionData = it)) }, pickApp)
    Spacer(Modifier.height(16.dp))
}
