package com.migux97.simon.canva

import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesomeMosaic
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.migux97.simon.MainActivity
import com.migux97.simon.custom.CustomWidgetLive
import com.migux97.simon.custom.StickerLayer
import com.migux97.simon.data.Catalog
import com.migux97.simon.data.ElementType
import com.migux97.simon.data.HomeItem
import com.migux97.simon.data.ImageStore
import com.migux97.simon.data.ItemType
import com.migux97.simon.data.LauncherBackground
import com.migux97.simon.data.LauncherBgType
import com.migux97.simon.data.Repository
import com.migux97.simon.data.StickerSource
import com.migux97.simon.data.StudioStore
import com.migux97.simon.data.WElement
import com.migux97.simon.data.newId
import com.migux97.simon.launcher.AppCatalog
import com.migux97.simon.launcher.AppPickerList
import com.migux97.simon.launcher.CHARGE_PAGE
import com.migux97.simon.launcher.DockEditorSheet
import com.migux97.simon.launcher.ItemContent
import com.migux97.simon.launcher.ItemEditorSheet
import com.migux97.simon.launcher.LOCK_PAGE
import com.migux97.simon.launcher.LauncherLayout
import com.migux97.simon.launcher.LauncherUi
import com.migux97.simon.launcher.OVERLAY_PAGE
import com.migux97.simon.launcher.SystemWidgetList
import com.migux97.simon.launcher.WidgetHost
import com.migux97.simon.launcher.flip
import com.migux97.simon.ui.components.ChoiceChips
import com.migux97.simon.ui.components.ColorRow
import com.migux97.simon.ui.components.ConfirmDialog
import com.migux97.simon.ui.components.LabeledSlider
import com.migux97.simon.ui.components.Panel
import com.migux97.simon.ui.components.WidgetPreview
import com.migux97.simon.ui.screens.ElementProps
import com.migux97.simon.ui.screens.MotionProps
import com.migux97.simon.ui.screens.layerLabel
import com.migux97.simon.ui.screens.isPhoto
import com.migux97.simon.ui.screens.loadPhoto
import com.migux97.simon.data.PhotoAdjust
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** Lo que hay en una página (capa, bloqueo, carga o una página del launcher), ordenado de atrás hacia adelante. */
data class PageDoc(val page: Int, val items: List<HomeItem>)

private fun HomeItem.layerLabel(): String = when (type) {
    ItemType.APP -> AppCatalog.find(appKey)?.let { AppCatalog.label(it) } ?: "App"
    ItemType.FOLDER -> "Carpeta · " + title.ifBlank { "sin nombre" }
    ItemType.CUSTOM_WIDGET -> "Widget · " + (StudioStore.widget(customWidgetId)?.name ?: "creado")
    ItemType.DESIGN_WIDGET -> "Widget · " + (designId?.let { Repository.design(it)?.name } ?: "de Simón")
    ItemType.SYSTEM_WIDGET -> "Widget de otra app"
    ItemType.STICKER -> when (stickerSource) {
        StickerSource.IMAGE -> "Foto"
        StickerSource.VIDEO -> "Video"
        StickerSource.DRAWING -> "Dibujo"
        StickerSource.ANIMATION -> "Animación"
        StickerSource.SCENE -> "Escena animada"
        StickerSource.LIBRARY -> "Sticker"
    }
    ItemType.ELEMENT -> element?.layerLabel() ?: "Elemento"
}

/** Fotos y dibujos de la página: se pueden recortar y ajustar. */
private fun HomeItem.photoAdjust(): PhotoAdjust? = when {
    type == ItemType.STICKER && (stickerSource == StickerSource.IMAGE || stickerSource == StickerSource.DRAWING) -> photo
    type == ItemType.ELEMENT && element?.isPhoto() == true -> element.photo
    else -> null
}

/** La imagen original (sin recorte) de una foto o un dibujo de la página. */
internal fun HomeItem.loadPhoto(): android.graphics.Bitmap? = when {
    type == ItemType.STICKER && stickerSource == StickerSource.IMAGE -> ImageStore.load(mediaPath, 1600)
    type == ItemType.STICKER && stickerSource == StickerSource.DRAWING -> com.migux97.simon.data.MediaLibrary.loadDrawing(mediaId, 1600)
    type == ItemType.ELEMENT -> element?.loadPhoto()
    else -> null
}

private object PageSource : CanvaSource<PageDoc> {
    override fun layers(doc: PageDoc) = doc.items.map {
        CLayer(
            it.id, it.x, it.y, it.w, it.h, it.rotation, it.alpha, it.locked, it.hidden, it.groupId, it.layerLabel(),
            canDuplicate = it.type != ItemType.SYSTEM_WIDGET, flipX = it.flipX, flipY = it.flipY, photo = it.photoAdjust()
        )
    }

    override fun applyLayers(doc: PageDoc, changed: List<CLayer>): PageDoc {
        val byId = changed.associateBy { it.id }
        return doc.copy(items = doc.items.map { i ->
            byId[i.id]?.let { l ->
                val moved = i.copy(x = l.x, y = l.y, w = l.w, h = l.h, rotation = l.rotation, alpha = l.alpha, locked = l.locked, hidden = l.hidden, groupId = l.groupId, flipX = l.flipX, flipY = l.flipY)
                val p = l.photo
                when {
                    p == null -> moved
                    i.type == ItemType.ELEMENT -> moved.copy(element = i.element?.copy(photo = p))
                    else -> moved.copy(photo = p)
                }
            } ?: i
        })
    }

    override fun reorder(doc: PageDoc, idsBackToFront: List<String>): PageDoc {
        val byId = doc.items.associateBy { it.id }
        val ordered = idsBackToFront.mapNotNull { byId[it] } + doc.items.filter { it.id !in idsBackToFront }
        return doc.copy(items = ordered.mapIndexed { i, item -> item.copy(z = i + 1) })
    }

    override fun remove(doc: PageDoc, ids: Set<String>) = doc.copy(items = doc.items.filterNot { it.id in ids })

    override fun duplicate(doc: PageDoc, ids: List<String>): Pair<PageDoc, List<String>> {
        var z = doc.items.maxOfOrNull { it.z } ?: 0
        val copies = ids.mapNotNull { id -> doc.items.firstOrNull { it.id == id } }
            .filter { it.type != ItemType.SYSTEM_WIDGET }
            .map { it.copy(id = newId(), x = it.x + 0.05f, y = it.y + 0.03f, z = ++z, element = it.element?.copy(id = newId())) }
        return doc.copy(items = doc.items + copies) to copies.map { it.id }
    }
}

/** Para las páginas del launcher: cambiar de página, sumar o borrar páginas. */
class LauncherPageControls(
    val pages: Int,
    val onSwitch: (Int) -> Unit,
    val onAddPage: () -> Unit,
    val onDeletePage: (Int) -> Unit
)

/** Pasa un elemento suelto (texto, forma…) a algo que se puede poner en una página. */
private fun WElement.toItem(page: Int, z: Int) = HomeItem(
    type = ItemType.ELEMENT, page = page, x = x, y = y, w = w, h = h, rotation = rotation, alpha = alpha,
    groupId = groupId, z = z,
    element = copy(x = 0f, y = 0f, w = 1f, h = 1f, rotation = 0f, alpha = 1f, groupId = null)
)

/**
 * Editor tipo Canva de una página entera, a tamaño real. Sirve para la capa
 * sobre tu launcher, la pantalla de bloqueo, la de carga y las páginas de Simón Launcher.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CanvaPageEditor(
    page: Int,
    title: String,
    ui: LauncherUi,
    onDone: () -> Unit,
    background: @Composable BoxScope.() -> Unit = {},
    foreground: @Composable BoxScope.() -> Unit = {},
    launcher: LauncherPageControls? = null,
    onPickSystemWidget: ((AppWidgetProviderInfo, Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val all by StudioStore.homeItemsFlow.collectAsStateWithLifecycle()
    val settings by StudioStore.launcherFlow.collectAsStateWithLifecycle()
    val storeDoc = PageDoc(page, all.filter { it.page == page }.sortedBy { it.z })
    val state = remember(page) {
        CanvaState(storeDoc, PageSource) { d -> StudioStore.setItems(StudioStore.items.filter { it.page != d.page } + d.items) }
    }
    LaunchedEffect(storeDoc) { state.syncExternal(storeDoc) }
    // Lo que se cambia en la hoja de un elemento (o con «Quitar») también se puede deshacer acá.
    DisposableEffect(state) {
        ui.onSnapshot = { state.checkpoint() }
        onDispose { ui.onSnapshot = null }
    }
    // Arranca con lo que estaba elegido (por ejemplo, al mantener presionado algo en el launcher).
    LaunchedEffect(page) { ui.selectedId?.let { id -> if (state.layers.any { it.id == id }) state.selectOnly(id) } }

    // Los widgets de otras apps quitados se liberan al terminar (si no volvieron con «Deshacer»).
    val startWidgets = remember(page) { storeDoc.items.filter { it.type == ItemType.SYSTEM_WIDGET }.map { it.appWidgetId }.toSet() }
    fun finish() {
        val inUse = StudioStore.items.map { it.appWidgetId }.toSet()
        startWidgets.filter { it !in inUse }.forEach { WidgetHost.delete(context, it) }
        ui.selectedId = null
        onDone()
    }

    val config = LocalConfiguration.current
    val aspect = config.screenWidthDp.toFloat() / config.screenHeightDp.coerceAtLeast(1)
    val isLauncher = page >= 0
    var askTemplate by remember { mutableStateOf<PageTemplate?>(null) }
    var pickSticker by remember { mutableStateOf(false) }
    var pickApp by remember { mutableStateOf(false) }
    var pickSystem by remember { mutableStateOf(false) }
    var askDeletePage by remember { mutableStateOf(false) }

    fun add(list: List<HomeItem>) {
        if (list.isEmpty()) return
        val base = state.doc.items.maxOfOrNull { it.z } ?: 0
        val placed = list.mapIndexed { i, it -> it.copy(page = page, z = base + 1 + i) }
        state.update { it.copy(items = it.items + placed) }
        state.select(placed.map { it.id })
        state.panel = null
    }
    fun addBlock(b: ElementBlock) = add(b.placeOn(aspect, state.doc.items.size).map { CanvaPresets.withMotion(it).toItem(page, 0) })
    fun box(w: Float, h: Float): Pair<Float, Float> {
        val off = (state.doc.items.size % 6) * 0.025f
        return ((1f - w) / 2f + off).coerceIn(0f, 1f - w) to ((1f - h) / 2f + off).coerceIn(0f, 1f - h)
    }
    fun addSticker(source: StickerSource, w: Float, h: Float, path: String? = null, mediaId: String? = null, liveId: String? = null) {
        val (x, y) = box(w, h)
        add(listOf(HomeItem(type = ItemType.STICKER, x = x, y = y, w = w, h = h, stickerSource = source, mediaPath = path, mediaId = mediaId, liveId = liveId)))
    }
    fun applyTemplate(t: PageTemplate, replace: Boolean) {
        val group = newId()
        val base = if (replace) 0 else state.doc.items.maxOfOrNull { it.z } ?: 0
        val items = t.elements.mapIndexed { i, e -> e.copy(id = newId(), groupId = group).toItem(page, base + 1 + i) }
        state.update { d -> d.copy(items = (if (replace) emptyList() else d.items) + items) }
        state.select(items.map { it.id })
        state.panel = null
    }

    var pickTarget by remember { mutableStateOf("") }
    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val video = pickTarget == "video"
            val path = withContext(Dispatchers.IO) { if (video) ImageStore.importVideo(context, uri) else ImageStore.importImage(context, uri, if (pickTarget.startsWith("bg")) 2400 else 1400) }
                ?: return@launch
            when (pickTarget) {
                "image" -> addSticker(StickerSource.IMAGE, 0.5f, 0.24f, path = path)
                "video" -> addSticker(StickerSource.VIDEO, 0.6f, 0.24f, path = path)
                "bg_lock" -> StudioStore.updateLock { it.copy(background = it.background.copy(type = LauncherBgType.IMAGE, path = path)) }
                "bg_launcher" -> StudioStore.updateLauncher { it.copy(background = it.background.copy(type = LauncherBgType.IMAGE, path = path)) }
            }
        }
    }
    fun pick(target: String, video: Boolean = false) {
        pickTarget = target
        mediaPicker.launch(PickVisualMediaRequest(if (video) ActivityResultContracts.PickVisualMedia.VideoOnly else ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    val tools = buildList {
        add(CanvaTool("tpl", Icons.Filled.AutoAwesomeMosaic, "Plantillas", content = {
            PageTemplateGrid(lock = page == LOCK_PAGE) { t -> if (state.doc.items.isEmpty()) applyTemplate(t, replace = false) else askTemplate = t }
        }))
        add(CanvaTool("text", Icons.Filled.TextFields, "Texto", content = { TextPanel { addBlock(it) } }))
        add(CanvaTool("el", Icons.Filled.Category, "Elementos", content = {
            ShapesAndGraphics { addBlock(it) }
            WidgetsRows(
                onCustom = { id, aspectW ->
                    val ww = 0.9f
                    val hh = (ww / aspectW * aspect).coerceIn(0.06f, 0.6f)
                    val (x, y) = box(ww, hh)
                    add(listOf(HomeItem(type = ItemType.CUSTOM_WIDGET, x = x, y = y, w = ww, h = hh, customWidgetId = id)))
                },
                onDesign = { id, wide, designAspect ->
                    val ww = if (wide) 0.92f else 0.46f
                    val hh = (ww / designAspect * aspect).coerceIn(0.06f, 0.6f)
                    val (x, y) = box(ww, hh)
                    add(listOf(HomeItem(type = ItemType.DESIGN_WIDGET, x = x, y = y, w = ww, h = hh, designId = id)))
                }
            )
            CanvaSection("Escenas animadas")
            ScenesRow { lv -> addSticker(StickerSource.SCENE, 0.5f, 0.25f, liveId = lv.id) }
            if (isLauncher) {
                CanvaSection("Del launcher")
                FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { pickApp = true }) { Text("App") }
                    FilledTonalButton(onClick = {
                        val w = LauncherLayout.cellW(settings)
                        val h = LauncherLayout.cellH(settings)
                        val (x, y) = box(w, h)
                        add(listOf(HomeItem(type = ItemType.FOLDER, x = x, y = y, w = w, h = h, title = "Carpeta")))
                        StudioStore.items.firstOrNull { it.id in state.selection }?.let { ui.editItem = it }
                    }) { Text("Carpeta") }
                    if (onPickSystemWidget != null) FilledTonalButton(onClick = { pickSystem = true }) { Text("Widget de otra app") }
                }
            }
        }))
        add(CanvaTool("photo", Icons.Filled.Image, "Fotos", content = {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { pick("image") }) { Text("Elegir foto") }
                FilledTonalButton(onClick = { pick("video", video = true) }) { Text("Elegir video") }
            }
            MyMediaRows(
                onDrawing = { id -> addSticker(StickerSource.DRAWING, 0.4f, 0.2f, mediaId = id) },
                onAnimation = { id -> addSticker(StickerSource.ANIMATION, 0.4f, 0.2f, mediaId = id) }
            )
        }))
        add(CanvaTool("st", Icons.Filled.EmojiEmotions, "Stickers", onClick = { pickSticker = true }))
        add(CanvaTool("bg", Icons.Filled.Wallpaper, "Fondo", content = { PageBackgroundPanel(page, pick = { pick(it) }) }))
        if (launcher != null) add(CanvaTool("page", Icons.Filled.ViewCarousel, "Páginas", content = {
            Hint("Estás editando la página ${page + 1}. Tocá otra para editarla.")
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(launcher.pages) { p ->
                    if (p == page) FilledTonalButton(onClick = {}) { Text("${p + 1}") }
                    else OutlinedButton(onClick = { state.panel = null; launcher.onSwitch(p) }) { Text("${p + 1}") }
                }
            }
            FlowRow(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { launcher.onAddPage() }, enabled = launcher.pages < 12) { Text("Nueva página") }
                OutlinedButton(onClick = { askDeletePage = true }) { Text(if (launcher.pages <= 1) "Vaciar página" else "Borrar esta página") }
                OutlinedButton(onClick = { ui.dockEditor = true }) { Text("Editar dock") }
                OutlinedButton(onClick = {
                    context.startActivity(Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_ROUTE, "launcherSettings").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }) { Text("Ajustes del launcher") }
            }
        }))
    }

    CanvaEditor(
        state = state,
        title = title,
        fullScreen = true,
        canvasAspect = aspect,
        onClose = { finish() },
        tools = tools,
        hint = "Tocá algo para elegirlo · arrastrá para moverlo · usá las manijas para agrandar y girar · mantené presionado para elegir varios",
        hintKey = "canva_page",
        loadPhoto = { id -> state.doc.items.firstOrNull { it.id == id }?.loadPhoto() },
        topActions = {
            IconButton(onClick = { finish() }) { Icon(Icons.Filled.Check, "Listo", tint = Color.White) }
        },
        editPanel = { l ->
            val item = state.doc.items.firstOrNull { it.id == l.id }
            val e = item?.element
            if (item != null && e != null) {
                // El giro y la transparencia son del cuadro en la página, no del elemento de adentro.
                val shown = e.copy(rotation = item.rotation, alpha = item.alpha)
                fun save(ne: WElement) = state.update(coalesce = true) { d ->
                    d.copy(items = d.items.map { if (it.id == item.id) it.copy(rotation = ne.rotation, alpha = ne.alpha, element = ne.copy(rotation = 0f, alpha = 1f)) else it })
                }
                ElementProps(shown, { save(it) }, pickMedia = {}, pickApp = { })
                CanvaSection("Movimiento y toque")
                MotionProps(shown, { save(it) }) { }
            }
        },
        onEditOutside = { l ->
            val item = state.doc.items.firstOrNull { it.id == l.id }
            if (item != null && item.type != ItemType.ELEMENT) {
                ui.editItem = StudioStore.items.firstOrNull { it.id == item.id } ?: item
                true
            } else false
        },
        background = background,
        foreground = foreground
    ) {
        PageItemsView(state.doc.items, ui)
    }

    ui.editItem?.let { ItemEditorSheet(it, settings, ui) }
    if (ui.dockEditor) DockEditorSheet(settings, ui)

    askTemplate?.let { t ->
        AlertDialog(
            onDismissRequest = { askTemplate = null },
            title = { Text("«${t.name}»") },
            text = { Text("¿Lo sumás a lo que ya tenés o reemplazás todo? Si te arrepentís, tocá «Deshacer».") },
            confirmButton = { TextButton(onClick = { askTemplate = null; applyTemplate(t, replace = false) }) { Text("Sumar") } },
            dismissButton = {
                Row {
                    TextButton(onClick = { askTemplate = null; applyTemplate(t, replace = true) }) { Text("Reemplazar todo") }
                    TextButton(onClick = { askTemplate = null }) { Text("Cancelar") }
                }
            }
        )
    }

    if (pickSticker) com.migux97.simon.ui.components.StickerPickerDialog(onDismiss = { pickSticker = false }) { info ->
        pickSticker = false
        addSticker(StickerSource.LIBRARY, 0.28f, 0.13f, mediaId = info.id)
    }

    if (pickApp) {
        val apps by AppCatalog.apps.collectAsStateWithLifecycle()
        AlertDialog(
            onDismissRequest = { pickApp = false },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { pickApp = false }) { Text("Cancelar") } },
            title = { Text("Elegí una app") },
            text = {
                AppPickerList(apps) { app ->
                    pickApp = false
                    val w = LauncherLayout.cellW(settings)
                    val h = LauncherLayout.cellH(settings)
                    val (x, y) = box(w, h)
                    add(listOf(HomeItem(type = ItemType.APP, x = x, y = y, w = w, h = h, appKey = app.key)))
                }
            }
        )
    }

    if (pickSystem && onPickSystemWidget != null) {
        AlertDialog(
            onDismissRequest = { pickSystem = false },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { pickSystem = false }) { Text("Cancelar") } },
            text = { SystemWidgetList { info -> pickSystem = false; onPickSystemWidget(info, page) } }
        )
    }

    if (askDeletePage && launcher != null) {
        val only = launcher.pages <= 1
        ConfirmDialog(
            title = if (only) "¿Vaciar la página?" else "¿Borrar la página ${page + 1}?",
            message = if (only) "Es la única página: se quita todo lo que tiene." else "Se borra la página ${page + 1} con todo lo que tiene y las siguientes se corren un lugar.",
            confirm = if (only) "Vaciar" else "Borrar página",
            onDismiss = { askDeletePage = false },
            onConfirm = {
                askDeletePage = false
                state.clearSelection()
                launcher.onDeletePage(page)
            }
        )
    }
}

/** Dibuja las cosas de la página quietas y sin tomar toques (para el editor). */
@Composable
private fun PageItemsView(items: List<HomeItem>, ui: LauncherUi) {
    val settings by StudioStore.launcherFlow.collectAsStateWithLifecycle()
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wPx = constraints.maxWidth.toFloat()
        val hPx = constraints.maxHeight.toFloat()
        items.filterNot { it.hidden }.forEach { item ->
            key(item.id) {
                Box(
                    Modifier
                        .offset { IntOffset((item.x * wPx).roundToInt(), (item.y * hPx).roundToInt()) }
                        .size(with(density) { (item.w * wPx).coerceAtLeast(1f).toDp() }, with(density) { (item.h * hPx).coerceAtLeast(1f).toDp() })
                        .graphicsLayer { rotationZ = item.rotation; alpha = item.alpha }
                        .flip(item.flipX, item.flipY)
                ) {
                    ItemContent(item, settings, ui)
                    if (item.stickers.isNotEmpty()) StickerLayer(item.stickers, Modifier.fillMaxSize())
                }
            }
        }
    }
}

/** Widgets creados y de Simón para poner en la página. */
@Composable
private fun WidgetsRows(onCustom: (String, Float) -> Unit, onDesign: (String, Boolean, Float) -> Unit) {
    val widgets by StudioStore.widgetsFlow.collectAsStateWithLifecycle()
    val designs by Repository.designs.collectAsStateWithLifecycle()
    CanvaSection("Widgets que creaste")
    if (widgets.isEmpty()) Hint("Creá widgets propios en Simón → Crear → Widget.")
    else LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(widgets, key = { it.id }) { w ->
            Box(Modifier.height(72.dp).aspectRatio(w.aspect.coerceIn(0.5f, 3f)).clickable { onCustom(w.id, w.aspect) }) {
                CustomWidgetLive(w, Modifier.fillMaxSize(), interactive = false)
            }
        }
    }
    CanvaSection("Widgets de Simón")
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        val list = designs + Catalog.widgetTemplates.filter { t -> designs.none { it.id == t.id } }.take(24)
        items(list, key = { it.id }) { d ->
            Box(Modifier.height(72.dp).aspectRatio(d.size.aspect).clickable {
                val saved = if (Repository.design(d.id) == null) d.copy(id = newId()).also { Repository.saveDesign(it) } else d
                onDesign(saved.id, d.size.cellsW != 2, d.size.aspect)
            }) { WidgetPreview(d, Modifier.fillMaxSize()) }
        }
    }
}

/** «Fondo»: cada página tiene el suyo (la capa usa tu fondo de pantalla real). */
@Composable
private fun PageBackgroundPanel(page: Int, pick: (String) -> Unit) {
    when (page) {
        OVERLAY_PAGE -> Hint("La capa se ve encima de tu fondo de pantalla de siempre. Para cambiarlo andá a Simón → Fondos.")
        CHARGE_PAGE -> {
            val s by StudioStore.chargingFlow.collectAsStateWithLifecycle()
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Fondo negro (ahorra batería)", Modifier.weight(1f)); Switch(s.blackBackground, { v -> StudioStore.updateCharging { it.copy(blackBackground = v) } })
                }
                LabeledSlider("Oscurecer", s.dim, 0f..0.9f, "${(s.dim * 100).roundToInt()}%") { v -> StudioStore.updateCharging { it.copy(dim = v) } }
            }
            if (!s.blackBackground) {
                CanvaSection("Escena de fondo")
                ScenesRow { lv -> StudioStore.updateCharging { it.copy(sceneId = lv.id) } }
            }
        }
        LOCK_PAGE -> {
            val s by StudioStore.lockFlow.collectAsStateWithLifecycle()
            BackgroundEditor(s.background, { bg -> StudioStore.updateLock { it.copy(background = bg) } }, pickPhoto = { pick("bg_lock") })
        }
        else -> {
            val s by StudioStore.launcherFlow.collectAsStateWithLifecycle()
            BackgroundEditor(s.background, { bg -> StudioStore.updateLauncher { it.copy(background = bg) } }, pickPhoto = { pick("bg_launcher") }, scenes = true)
        }
    }
}

@Composable
private fun BackgroundEditor(bg: LauncherBackground, onChange: (LauncherBackground) -> Unit, pickPhoto: () -> Unit, scenes: Boolean = false) {
    val types = listOf(LauncherBgType.SYSTEM, LauncherBgType.COLOR, LauncherBgType.GRADIENT, LauncherBgType.IMAGE) + if (scenes) listOf(LauncherBgType.SCENE) else emptyList()
    ChoiceChips(types, bg.type, { it.label }, { t -> if (t == LauncherBgType.IMAGE && bg.path == null) pickPhoto() else onChange(bg.copy(type = t)) })
    Panel {
        when (bg.type) {
            LauncherBgType.COLOR -> ColorRow("Color", bg.color1) { onChange(bg.copy(color1 = it)) }
            LauncherBgType.GRADIENT -> {
                ColorRow("Color de arriba", bg.color1) { onChange(bg.copy(color1 = it)) }
                ColorRow("Color de abajo", bg.color2) { onChange(bg.copy(color2 = it)) }
            }
            LauncherBgType.IMAGE -> FilledTonalButton(onClick = pickPhoto) { Text(if (bg.path == null) "Elegir foto" else "Cambiar foto") }
            LauncherBgType.SYSTEM -> Text("Se ve tu fondo de pantalla de siempre.")
            else -> Unit
        }
        LabeledSlider("Oscurecer", bg.dim, 0f..0.9f, "${(bg.dim * 100).roundToInt()}%") { onChange(bg.copy(dim = it)) }
    }
    if (bg.type == LauncherBgType.SCENE) ScenesRow { lv -> onChange(bg.copy(liveId = lv.id)) }
}
