package com.migux97.simon.launcher

import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.migux97.simon.MainActivity
import com.migux97.simon.custom.CustomWidgetLive
import com.migux97.simon.data.AppOverride
import com.migux97.simon.data.Catalog
import com.migux97.simon.data.CustomWidget
import com.migux97.simon.data.ElementType
import com.migux97.simon.data.FontStyle
import com.migux97.simon.data.HomeItem
import com.migux97.simon.data.IconForeground
import com.migux97.simon.data.IconShape
import com.migux97.simon.data.ImageStore
import com.migux97.simon.data.ItemType
import com.migux97.simon.data.LauncherSettings
import com.migux97.simon.data.MediaLibrary
import com.migux97.simon.data.Motion
import com.migux97.simon.data.Repository
import com.migux97.simon.data.StickerSource
import com.migux97.simon.data.StudioStore
import com.migux97.simon.data.WElement
import com.migux97.simon.ui.components.ChoiceChips
import com.migux97.simon.ui.components.ColorRow
import com.migux97.simon.ui.components.LabeledSlider
import com.migux97.simon.ui.components.SceneThumb
import com.migux97.simon.ui.components.WidgetPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

// --------------------------------------------------------------- cajón

@Composable
fun AppDrawer(apps: List<LaunchApp>, settings: LauncherSettings, ui: LauncherUi) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var showHidden by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val list = apps.filter {
        (showHidden || it.key !in settings.hiddenApps) &&
            (query.isBlank() || AppCatalog.label(it).contains(query, ignoreCase = true))
    }
    LaunchedEffect(ui.drawerSearch) {
        if (ui.drawerSearch) { try { focus.requestFocus() } catch (_: Exception) {}; ui.drawerSearch = false }
    }
    val text = Color(settings.drawerTextColor)
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(settings.drawerColor))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                query, { query = it },
                placeholder = { Text("Buscar apps", color = text.copy(alpha = 0.6f)) },
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = text) },
                singleLine = true,
                shape = RoundedCornerShape(50),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = text, unfocusedTextColor = text),
                modifier = Modifier.weight(1f).focusRequester(focus)
            )
            IconButton(onClick = { ui.drawerOpen = false }) { Icon(Icons.Filled.Close, "Cerrar", tint = text) }
        }
        if (settings.hiddenApps.isNotEmpty()) {
            TextButton(onClick = { showHidden = !showHidden }, modifier = Modifier.padding(start = 8.dp)) {
                Text(if (showHidden) "Ocultar apps escondidas" else "Mostrar apps escondidas (${settings.hiddenApps.size})", color = text)
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(settings.drawerColumns.coerceIn(3, 7)),
            contentPadding = PaddingValues(10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(list, key = { it.key }) { app ->
                Column(
                    Modifier
                        .pointerInput(app.key) {
                            detectTapGestures(
                                onTap = { AppCatalog.launch(context, app); ui.drawerOpen = false },
                                onLongPress = { ui.appMenu = app; ui.appMenuItem = null }
                            )
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AppIconBitmap(app, settings, settings.iconSizeDp, Modifier.size(settings.iconSizeDp.dp.coerceAtMost(64.dp)))
                    Text(
                        AppCatalog.label(app), color = text, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp, start = 2.dp, end = 2.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------- carpeta

@Composable
fun FolderOverlay(folder: HomeItem, settings: LauncherSettings, ui: LauncherUi) {
    val context = LocalContext.current
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable { ui.openFolder = null },
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .padding(28.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(30.dp))
                .background(Color(folder.folderColor).copy(alpha = 0.92f))
                .clickable(enabled = false) { }
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    folder.title.ifBlank { "Carpeta" }, color = Color.White, style = MaterialTheme.typography.titleLarge,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                )
                TextButton(onClick = {
                    val current = StudioStore.items.firstOrNull { it.id == folder.id } ?: folder
                    ui.openFolder = null
                    ui.editItem = current
                }) {
                    Icon(Icons.Filled.Edit, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Editar carpeta", color = Color.White)
                }
            }
            Spacer(Modifier.height(12.dp))
            val apps = folder.folderApps.mapNotNull { AppCatalog.find(it) }
            if (apps.isEmpty()) Text("Carpeta vacía. Tocá «Editar carpeta» para agregar apps.", color = Color.White)
            apps.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                    row.forEach { app ->
                        Column(
                            Modifier
                                .width(68.dp)
                                .pointerInput(app.key) {
                                    detectTapGestures(
                                        onTap = { AppCatalog.launch(context, app); ui.openFolder = null },
                                        onLongPress = { ui.appMenu = app; ui.appMenuItem = null }
                                    )
                                },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AppIconBitmap(app, settings, 52, Modifier.size(52.dp))
                            Text(AppCatalog.label(app), color = Color.White, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------- agregar

private enum class AddMode { MAIN, APPS, SYSTEM }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSheet(ui: LauncherUi, settings: LauncherSettings, page: Int, onPickSystemWidget: (AppWidgetProviderInfo, Int) -> Unit, overlay: Boolean = false) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf(AddMode.MAIN) }
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val designs by Repository.designs.collectAsStateWithLifecycle()
    val widgets by StudioStore.widgetsFlow.collectAsStateWithLifecycle()
    val drawings by StudioStore.drawingsFlow.collectAsStateWithLifecycle()
    val animations by StudioStore.animationsFlow.collectAsStateWithLifecycle()
    val apps by AppCatalog.apps.collectAsStateWithLifecycle()

    fun add(item: HomeItem) {
        ui.snapshot()
        StudioStore.upsertItem(item.copy(page = page, z = StudioStore.maxZ(page) + 1))
        ui.addSheet = false
        ui.selectedId = item.id
    }
    fun box(w: Float, h: Float): Pair<Float, Float> = LauncherLayout.freeSpot(page, w, h, settings)

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val path = withContext(Dispatchers.IO) { ImageStore.importImage(context, uri, 1400) }
            if (path != null) { val (x, y) = box(0.4f, 0.22f); add(HomeItem(type = ItemType.STICKER, x = x, y = y, w = 0.4f, h = 0.22f, stickerSource = StickerSource.IMAGE, mediaPath = path)) }
        }
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val path = withContext(Dispatchers.IO) { ImageStore.importVideo(context, uri) }
            if (path != null) { val (x, y) = box(0.5f, 0.22f); add(HomeItem(type = ItemType.STICKER, x = x, y = y, w = 0.5f, h = 0.22f, stickerSource = StickerSource.VIDEO, mediaPath = path)) }
        }
    }

    var pickSticker by remember { mutableStateOf(false) }
    if (pickSticker) com.migux97.simon.ui.components.StickerPickerDialog(onDismiss = { pickSticker = false }) { info ->
        pickSticker = false
        val (x, y) = box(0.28f, 0.13f)
        add(HomeItem(type = ItemType.STICKER, x = x, y = y, w = 0.28f, h = 0.13f, stickerSource = StickerSource.LIBRARY, mediaId = info.id))
    }
    ModalBottomSheet(onDismissRequest = { ui.addSheet = false }, sheetState = sheet) {
        when (mode) {
            AddMode.APPS -> Column {
                TextButton(onClick = { mode = AddMode.MAIN }, modifier = Modifier.padding(start = 8.dp)) { Text("← Volver") }
                AppPickerList(apps) { app ->
                    add(LauncherLayout.appItem(app.key, page, settings))
                }
            }
            AddMode.SYSTEM -> Column {
                TextButton(onClick = { mode = AddMode.MAIN }, modifier = Modifier.padding(start = 8.dp)) { Text("← Volver") }
                SystemWidgetList { info ->
                    ui.addSheet = false
                    onPickSystemWidget(info, page)
                }
            }
            AddMode.MAIN -> Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 640.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp)
            ) {
                Text("Agregar a la pantalla", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp))
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // En la capa decorativa no hay apps ni carpetas: no se pueden tocar.
                    if (!overlay) FilledTonalButton(onClick = { mode = AddMode.APPS }) { Text("App") }
                    if (!overlay) FilledTonalButton(onClick = {
                        val (x, y) = box(LauncherLayout.cellW(settings), LauncherLayout.cellH(settings))
                        add(HomeItem(type = ItemType.FOLDER, x = x, y = y, w = LauncherLayout.cellW(settings), h = LauncherLayout.cellH(settings), title = "Carpeta"))
                        ui.editItem = StudioStore.items.lastOrNull()
                    }) { Text("Carpeta") }
                    FilledTonalButton(onClick = {
                        val text = CustomWidget(
                            name = "Texto", aspect = 3f, bgAlpha = 0f,
                            elements = listOf(WElement(type = ElementType.TEXT, text = "{saludo}", x = 0f, y = 0.1f, w = 1f, h = 0.8f, font = FontStyle.CURSIVE, shadow = true))
                        )
                        StudioStore.saveWidget(text)
                        val (x, y) = box(0.8f, 0.12f)
                        add(HomeItem(type = ItemType.CUSTOM_WIDGET, x = x, y = y, w = 0.8f, h = 0.12f, customWidgetId = text.id))
                        // Se abre el editor para escribir el texto enseguida.
                        ui.editItem = StudioStore.items.lastOrNull()
                    }) { Text("Texto") }
                }
                Section("Widgets que creaste")
                if (widgets.isEmpty()) Hint("Creá widgets propios en Simón → Crear.")
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(widgets, key = { it.id }) { w ->
                        Box(
                            Modifier
                                .height(90.dp)
                                .aspectRatio(w.aspect.coerceIn(0.5f, 3f))
                                .clickable {
                                    val ww = 0.9f
                                    val hh = (ww / w.aspect * 0.46f).coerceIn(0.08f, 0.6f)
                                    val (x, y) = box(ww, hh)
                                    add(HomeItem(type = ItemType.CUSTOM_WIDGET, x = x, y = y, w = ww, h = hh, customWidgetId = w.id))
                                }
                        ) { CustomWidgetLive(w, Modifier.fillMaxSize(), interactive = false) }
                    }
                }
                Section("Widgets de Simón")
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val list = designs + Catalog.widgetTemplates.filter { t -> designs.none { it.id == t.id } }.take(24)
                    items(list, key = { it.id }) { d ->
                        Box(
                            Modifier
                                .height(90.dp)
                                .aspectRatio(d.size.aspect)
                                .clickable {
                                    val saved = if (Repository.design(d.id) == null) d.copy(id = com.migux97.simon.data.newId()).also { Repository.saveDesign(it) } else d
                                    val ww = if (d.size.cellsW == 2) 0.46f else 0.92f
                                    val hh = ww / d.size.aspect * 0.46f
                                    val (x, y) = box(ww, hh)
                                    add(HomeItem(type = ItemType.DESIGN_WIDGET, x = x, y = y, w = ww, h = hh, designId = saved.id))
                                }
                        ) { WidgetPreview(d, Modifier.fillMaxSize()) }
                    }
                }
                if (!overlay) {
                    Section("Widgets de otras apps")
                    OutlinedButton(onClick = { mode = AddMode.SYSTEM }, modifier = Modifier.padding(horizontal = 16.dp)) { Text("Elegir widget del sistema") }
                }
                Section("Stickers y medios")
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text("Foto") }
                    FilledTonalButton(onClick = { videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) }) { Text("Video") }
                    FilledTonalButton(onClick = { pickSticker = true }) { Text("Sticker") }
                }
                Section("Mis dibujos")
                if (drawings.isEmpty()) Hint("Todavía no tenés dibujos. Creá uno en Crear → Dibujo.")
                else LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(drawings, key = { it.id }) { d ->
                        val bmp by produceState<android.graphics.Bitmap?>(null, d.id, d.updatedAt) { value = withContext(Dispatchers.IO) { MediaLibrary.loadDrawing(d.id, 300) } }
                        Box(
                            Modifier.size(80.dp).clip(RoundedCornerShape(14.dp)).background(Color(0x22FFFFFF)).clickable {
                                val (x, y) = box(0.4f, 0.2f)
                                add(HomeItem(type = ItemType.STICKER, x = x, y = y, w = 0.4f, h = 0.2f, stickerSource = StickerSource.DRAWING, mediaId = d.id))
                            }
                        ) { bmp?.let { Image(it.asImageBitmap(), d.name, Modifier.fillMaxSize()) } }
                    }
                }
                Section("Mis animaciones")
                if (animations.isEmpty()) Hint("Todavía no tenés animaciones. Creá una en Crear → Animación.")
                else LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(animations, key = { it.id }) { a ->
                        Box(
                            Modifier.size(80.dp).clip(RoundedCornerShape(14.dp)).background(Color(0x22FFFFFF)).clickable {
                                val (x, y) = box(0.4f, 0.2f)
                                add(HomeItem(type = ItemType.STICKER, x = x, y = y, w = 0.4f, h = 0.2f, stickerSource = StickerSource.ANIMATION, mediaId = a.id))
                            }
                        ) { AnimatedFrames(a.id, Modifier.fillMaxSize()) }
                    }
                }
                Section("Escenas animadas")
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(Catalog.liveWallpapers, key = { it.id }) { lv ->
                        Box(
                            Modifier.width(60.dp).height(100.dp).clip(RoundedCornerShape(14.dp)).clickable {
                                val (x, y) = box(0.5f, 0.25f)
                                add(HomeItem(type = ItemType.STICKER, x = x, y = y, w = 0.5f, h = 0.25f, stickerSource = StickerSource.SCENE, liveId = lv.id))
                            }
                        ) { SceneThumb(lv, Modifier.fillMaxSize()) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(text: String) =
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 8.dp))

@Composable
private fun Hint(text: String) =
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 20.dp))

@Composable
fun AppPickerList(apps: List<LaunchApp>, onPick: (LaunchApp) -> Unit) {
    var query by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth().heightIn(max = 640.dp)) {
        OutlinedTextField(query, { query = it }, placeholder = { Text("Buscar app") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(16.dp))
        LazyColumn {
            items(apps.filter { query.isBlank() || it.label.contains(query, true) }, key = { it.key }) { app ->
                val bmp = remember(app.key) { runCatching { app.icon.toBitmap(96, 96) }.getOrNull() }
                ListItem(
                    leadingContent = { bmp?.let { Image(it.asImageBitmap(), null, Modifier.size(40.dp)) } },
                    headlineContent = { Text(app.label) },
                    modifier = Modifier.clickable { onPick(app) }
                )
            }
        }
    }
}

@Composable
internal fun SystemWidgetList(onPick: (AppWidgetProviderInfo) -> Unit) {
    val context = LocalContext.current
    // null = todavía buscando; lista vacía = no hay widgets.
    val loaded by produceState<List<AppWidgetProviderInfo>?>(null) {
        value = withContext(Dispatchers.IO) { WidgetHost.providers(context) }
    }
    val providers = loaded.orEmpty()
    Column(Modifier.fillMaxWidth().heightIn(max = 640.dp)) {
        Text("Widgets de otras apps", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        if (loaded == null) Hint("Buscando widgets…")
        else if (providers.isEmpty()) Hint("No encontramos widgets de otras apps en tu teléfono.")
        LazyColumn {
            items(providers, key = { it.provider.flattenToString() }) { info ->
                val pm = context.packageManager
                val appLabel = remember(info) { runCatching { pm.getApplicationLabel(pm.getApplicationInfo(info.provider.packageName, 0)).toString() }.getOrDefault("") }
                val preview = remember(info) {
                    runCatching { (info.loadPreviewImage(context, 0) ?: info.loadIcon(context, 0))?.toBitmap(160, 100) }.getOrNull()
                }
                ListItem(
                    leadingContent = { preview?.let { Image(it.asImageBitmap(), null, Modifier.width(80.dp).height(50.dp)) } },
                    headlineContent = { Text(info.loadLabel(pm)) },
                    supportingContent = { Text(appLabel) },
                    modifier = Modifier.clickable { onPick(info) }
                )
            }
        }
    }
}

// -------------------------------------------------------- propiedades

private fun typeName(item: HomeItem): String = when (item.type) {
    ItemType.APP -> "Editar app"
    ItemType.FOLDER -> "Editar carpeta"
    ItemType.CUSTOM_WIDGET, ItemType.DESIGN_WIDGET, ItemType.SYSTEM_WIDGET -> "Editar widget"
    ItemType.ELEMENT -> "Editar elemento"
    ItemType.STICKER -> when (item.stickerSource) {
        StickerSource.IMAGE -> "Editar foto"
        StickerSource.VIDEO -> "Editar video"
        StickerSource.DRAWING -> "Editar dibujo"
        StickerSource.ANIMATION -> "Editar animación"
        StickerSource.SCENE -> "Editar escena"
        StickerSource.LIBRARY -> "Editar sticker"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditorSheet(original: HomeItem, settings: LauncherSettings, ui: LauncherUi) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Cómo estaba al abrir: «Cancelar cambios» vuelve a esto.
    val start = remember(original.id) { StudioStore.items.firstOrNull { it.id == original.id } ?: original }
    val startWidget = remember(original.id) { StudioStore.widget(start.customWidgetId) }
    var item by remember(original.id) { mutableStateOf(start) }
    var touched by remember(original.id) { mutableStateOf(false) }
    var keepRatio by remember(original.id) { mutableStateOf(true) }
    var folderQuery by remember(original.id) { mutableStateOf("") }
    var pickSticker by remember { mutableStateOf(false) }
    val apps by AppCatalog.apps.collectAsStateWithLifecycle()
    val widgets by StudioStore.widgetsFlow.collectAsStateWithLifecycle()
    val drawings by StudioStore.drawingsFlow.collectAsStateWithLifecycle()
    val animations by StudioStore.animationsFlow.collectAsStateWithLifecycle()

    fun markTouched() {
        if (!touched) { ui.snapshot(); touched = true }
    }
    fun save(i: HomeItem) { markTouched(); item = i; StudioStore.upsertItem(i) }
    fun cancelChanges() {
        StudioStore.upsertItem(start)
        startWidget?.let { StudioStore.saveWidget(it) }
        item = start
        touched = false
        ui.editItem = null
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val path = withContext(Dispatchers.IO) { ImageStore.importImage(context, uri, 1400) }
            if (path != null) save(item.copy(mediaPath = path))
        }
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val path = withContext(Dispatchers.IO) { ImageStore.importVideo(context, uri) }
            if (path != null) save(item.copy(mediaPath = path))
        }
    }
    if (pickSticker) com.migux97.simon.ui.components.StickerPickerDialog(onDismiss = { pickSticker = false }) { info ->
        pickSticker = false
        save(item.copy(mediaId = info.id))
    }

    // Un "Texto" agregado desde «Agregar» es un widget con un solo texto: se edita acá mismo.
    val textWidget = if (item.type == ItemType.CUSTOM_WIDGET)
        widgets.firstOrNull { it.id == item.customWidgetId }?.takeIf { it.elements.size == 1 && it.elements[0].type == ElementType.TEXT }
    else null

    ModalBottomSheet(onDismissRequest = { ui.editItem = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
      Column(Modifier.fillMaxWidth().heightIn(max = 720.dp).verticalScroll(rememberScrollState())) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(typeName(item), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Button(onClick = { ui.editItem = null }) { Text("Listo") }
            }
            if (touched) OutlinedButton(onClick = { cancelChanges() }) { Text("Cancelar cambios (volver a como estaba)") }

            // ---- Contenido propio de cada tipo, arriba de todo
            if (textWidget != null) {
                val el = textWidget.elements[0]
                fun saveText(e: WElement) { markTouched(); StudioStore.saveWidget(textWidget.copy(elements = listOf(e))) }
                OutlinedTextField(
                    el.text, { saveText(el.copy(text = it.take(80))) },
                    label = { Text("Texto") },
                    supportingText = { Text("Podés usar {saludo}, {hora}, {fecha}, {bateria}…") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Letra", style = MaterialTheme.typography.titleMedium)
                ChoiceChips(FontStyle.entries, el.font, { it.label }, { saveText(el.copy(font = it)) }, Modifier.padding(horizontal = 0.dp))
                ColorRow("Color del texto", el.textColor) { saveText(el.copy(textColor = it)) }
            }
            when (item.type) {
                ItemType.APP -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Mostrar nombre", Modifier.weight(1f))
                        Switch(item.showLabel, { save(item.copy(showLabel = it)) })
                    }
                    AppCatalog.find(item.appKey)?.let { app ->
                        FilledTonalButton(onClick = { ui.editItem = null; ui.iconEditor = app }) { Text("Personalizar ícono") }
                    }
                }
                ItemType.FOLDER -> {
                    OutlinedTextField(item.title, { save(item.copy(title = it.take(24))) }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    ColorRow("Color", item.folderColor) { save(item.copy(folderColor = it)) }
                    Text("Apps en la carpeta (${item.folderApps.size})", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        folderQuery, { folderQuery = it },
                        placeholder = { Text("Buscar app") },
                        leadingIcon = { Icon(Icons.Filled.Search, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Las que ya estaban en la carpeta van primero (el orden no salta al tildar).
                    val initial = remember(original.id) { start.folderApps.toSet() }
                    val shown = apps
                        .filter { folderQuery.isBlank() || it.label.contains(folderQuery, ignoreCase = true) || AppCatalog.label(it).contains(folderQuery, ignoreCase = true) }
                        .sortedByDescending { it.key in initial }
                    if (shown.isEmpty()) Text("No hay apps con ese nombre.", style = MaterialTheme.typography.bodySmall)
                    shown.forEach { app ->
                        val checked = app.key in item.folderApps
                        Row(Modifier.fillMaxWidth().clickable {
                            save(item.copy(folderApps = if (checked) item.folderApps - app.key else item.folderApps + app.key))
                        }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked, null)
                            Text(AppCatalog.label(app))
                        }
                    }
                }
                ItemType.CUSTOM_WIDGET -> FilledTonalButton(onClick = {
                    ui.editItem = null
                    context.startActivity(Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_ROUTE, "builder/${item.customWidgetId}").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }) { Text(if (textWidget != null) "Más opciones en el constructor" else "Editar en el constructor") }
                ItemType.DESIGN_WIDGET -> FilledTonalButton(onClick = {
                    ui.editItem = null
                    context.startActivity(Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_EDIT_DESIGN, item.designId).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }) { Text("Editar diseño") }
                ItemType.STICKER -> when (item.stickerSource) {
                    StickerSource.IMAGE -> FilledTonalButton(onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text("Cambiar imagen") }
                    StickerSource.VIDEO -> FilledTonalButton(onClick = { videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) }) { Text("Cambiar video") }
                    StickerSource.LIBRARY -> FilledTonalButton(onClick = { pickSticker = true }) { Text("Cambiar sticker") }
                    StickerSource.DRAWING -> {
                        Text("Cambiar dibujo", style = MaterialTheme.typography.titleMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(drawings, key = { it.id }) { d ->
                                val bmp by produceState<android.graphics.Bitmap?>(null, d.id, d.updatedAt) { value = withContext(Dispatchers.IO) { MediaLibrary.loadDrawing(d.id, 200) } }
                                Box(
                                    Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))
                                        .background(if (d.id == item.mediaId) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else Color(0x22888888))
                                        .clickable { save(item.copy(mediaId = d.id)) }
                                ) { bmp?.let { Image(it.asImageBitmap(), d.name, Modifier.fillMaxSize()) } }
                            }
                        }
                    }
                    StickerSource.ANIMATION -> {
                        Text("Cambiar animación", style = MaterialTheme.typography.titleMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(animations, key = { it.id }) { a ->
                                Box(
                                    Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))
                                        .background(if (a.id == item.mediaId) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else Color(0x22888888))
                                        .clickable { save(item.copy(mediaId = a.id)) }
                                ) { AnimatedFrames(a.id, Modifier.fillMaxSize()) }
                            }
                        }
                    }
                    StickerSource.SCENE -> {
                        Text("Cambiar escena", style = MaterialTheme.typography.titleMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(Catalog.liveWallpapers, key = { it.id }) { lv ->
                                Box(
                                    Modifier.width(48.dp).height(80.dp).clip(RoundedCornerShape(12.dp))
                                        .background(if (lv.id == item.liveId) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .padding(if (lv.id == item.liveId) 3.dp else 0.dp)
                                        .clickable { save(item.copy(liveId = lv.id)) }
                                ) { SceneThumb(lv, Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp))) }
                            }
                        }
                    }
                }
                ItemType.ELEMENT -> Text(
                    "El texto, la forma y los colores se cambian con «Editar» en el editor de la pantalla.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ItemType.SYSTEM_WIDGET -> Text(
                    "Es un widget de otra app: su contenido lo maneja esa app. Acá podés cambiar el tamaño, la posición y el giro.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ---- Posición y tamaño
            Text("Posición", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { save(item.copy(x = (1f - item.w) / 2f)) }) { Text("Centrar") }
                OutlinedButton(onClick = { save(item.copy(rotation = 0f)) }, enabled = item.rotation != 0f) { Text("Girar a 0°") }
            }
            LabeledSlider("Horizontal", item.x.coerceIn(-0.5f, 1f), -0.5f..1f, "${(item.x * 100).roundToInt()}%") { save(item.copy(x = it)) }
            LabeledSlider("Vertical", item.y.coerceIn(-0.05f, 1f), -0.05f..1f, "${(item.y * 100).roundToInt()}%") { save(item.copy(y = it)) }
            Text("Tamaño", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Mantener proporción", Modifier.weight(1f))
                Switch(keepRatio, { keepRatio = it })
            }
            LabeledSlider("Ancho", item.w.coerceIn(0.05f, 1.2f), 0.05f..1.2f, "${(item.w * 100).roundToInt()}%") { w ->
                save(if (keepRatio && item.w > 0f) item.copy(w = w, h = (item.h * w / item.w).coerceIn(0.03f, 1.2f)) else item.copy(w = w))
            }
            LabeledSlider("Alto", item.h.coerceIn(0.03f, 1.2f), 0.03f..1.2f, "${(item.h * 100).roundToInt()}%") { h ->
                save(if (keepRatio && item.h > 0f) item.copy(h = h, w = (item.w * h / item.h).coerceIn(0.05f, 1.2f)) else item.copy(h = h))
            }
            LabeledSlider("Giro", item.rotation.coerceIn(-180f, 180f), -180f..180f, "${item.rotation.roundToInt()}°") { save(item.copy(rotation = it)) }
            LabeledSlider("Transparencia", item.alpha, 0.1f..1f, "${(item.alpha * 100).roundToInt()}%") { save(item.copy(alpha = it)) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Bloquear posición", Modifier.weight(1f))
                Switch(item.locked, { save(item.copy(locked = it)) })
            }
            Text("Movimiento", style = MaterialTheme.typography.titleMedium)
        }
        ChoiceChips(Motion.entries, item.motion, { it.label }, { save(item.copy(motion = it)) })
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (item.motion != Motion.NONE) LabeledSlider("Velocidad", item.motionSpeed, 0.2f..3f, "x%.1f".format(item.motionSpeed)) { save(item.copy(motionSpeed = it)) }
            if (item.page >= 0) {
                Text("Mover a otra página", style = MaterialTheme.typography.titleMedium)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(settings.pages) { p ->
                        if (p == item.page) FilledTonalButton(onClick = {}) { Text("${p + 1}") }
                        else OutlinedButton(onClick = { save(item.copy(page = p)) }) { Text("${p + 1}") }
                    }
                }
            }
        }
        com.migux97.simon.ui.components.StickerAttachEditor(item.stickers) { save(item.copy(stickers = it)) }
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = {
                    ui.remove(item)
                    ui.editItem = null
                }) { Text("Quitar de la pantalla", color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { cancelChanges() }, enabled = touched) { Text("Cancelar cambios") }
            }
        }
      }
    }
}

// ------------------------------------------------------------- dock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DockEditorSheet(settings: LauncherSettings, ui: LauncherUi) {
    val apps by AppCatalog.apps.collectAsStateWithLifecycle()
    var picking by remember { mutableStateOf(false) }
    fun setDock(list: List<String>) = StudioStore.updateLauncher { it.copy(dockApps = list.distinct().take(MAX_DOCK)) }
    fun move(i: Int, d: Int) {
        val l = StudioStore.launcherSettings.dockApps.toMutableList()
        val j = i + d
        if (i !in l.indices || j !in l.indices) return
        val t = l[i]; l[i] = l[j]; l[j] = t
        setDock(l)
    }
    ModalBottomSheet(onDismissRequest = { ui.dockEditor = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        if (picking) Column {
            TextButton(onClick = { picking = false }, modifier = Modifier.padding(start = 8.dp)) { Text("← Volver") }
            Text("Elegí una app para el dock", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp))
            AppPickerList(apps.filter { it.key !in settings.dockApps }) { app ->
                setDock(StudioStore.launcherSettings.dockApps + app.key)
                picking = false
            }
        } else Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Editar dock", style = MaterialTheme.typography.titleLarge)
            Text("Las apps fijas de abajo de todo. Entran hasta $MAX_DOCK.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Mostrar el dock", Modifier.weight(1f))
                Switch(settings.dockEnabled, { on -> StudioStore.updateLauncher { it.copy(dockEnabled = on) } })
            }
            if (settings.dockApps.isEmpty()) Text("El dock está vacío. Tocá «Agregar app».", style = MaterialTheme.typography.bodyMedium)
            settings.dockApps.forEachIndexed { i, key ->
                val app = AppCatalog.find(key)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (app != null) AppIconBitmap(app, settings, 40, Modifier.size(36.dp)) else Spacer(Modifier.size(36.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(app?.let { AppCatalog.label(it) } ?: "App desinstalada", maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    TextButton(onClick = { move(i, -1) }, enabled = i > 0, contentPadding = PaddingValues(horizontal = 6.dp)) { Text("Subir") }
                    TextButton(onClick = { move(i, 1) }, enabled = i < settings.dockApps.size - 1, contentPadding = PaddingValues(horizontal = 6.dp)) { Text("Bajar") }
                    TextButton(onClick = { setDock(StudioStore.launcherSettings.dockApps - key) }, contentPadding = PaddingValues(horizontal = 6.dp)) {
                        Text("Quitar", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            val full = settings.dockApps.size >= MAX_DOCK
            Button(onClick = { picking = true }, enabled = !full) { Text("Agregar app") }
            if (full) Text("El dock ya tiene $MAX_DOCK apps; sacá una primero.", style = MaterialTheme.typography.bodySmall)
            Text("«Subir» la mueve a la izquierda y «Bajar» a la derecha.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Máximo de apps en el dock. */
const val MAX_DOCK = 6

// ------------------------------------------------------------ menú app

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMenuSheet(app: LaunchApp, homeItem: HomeItem?, settings: LauncherSettings, ui: LauncherUi, page: Int) {
    val context = LocalContext.current
    val inDock = app.key in settings.dockApps
    val hidden = app.key in settings.hiddenApps
    fun close() { ui.appMenu = null; ui.appMenuItem = null }
    ModalBottomSheet(onDismissRequest = { close() }) {
        Column(Modifier.padding(bottom = 28.dp)) {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                AppIconBitmap(app, settings, 48, Modifier.size(48.dp))
                Spacer(Modifier.width(12.dp))
                Text(AppCatalog.label(app), style = MaterialTheme.typography.titleLarge)
            }
            MenuRow("Personalizar ícono y nombre") { close(); ui.iconEditor = app }
            if (homeItem == null) MenuRow("Agregar a la pantalla de inicio") {
                StudioStore.upsertItem(LauncherLayout.appItem(app.key, page, settings)); close(); ui.drawerOpen = false
            } else MenuRow("Mover o editar en la pantalla") { close(); ui.editMode = true; ui.selectedId = homeItem.id }
            MenuRow(if (inDock) "Quitar del dock" else "Agregar al dock") {
                if (!inDock && settings.dockApps.size >= MAX_DOCK) {
                    Toast.makeText(context, "El dock ya tiene $MAX_DOCK apps; sacá una primero.", Toast.LENGTH_LONG).show()
                    return@MenuRow
                }
                StudioStore.updateLauncher { s -> s.copy(dockApps = if (inDock) s.dockApps - app.key else (s.dockApps + app.key).take(MAX_DOCK)) }
                close()
            }
            MenuRow(if (hidden) "Mostrar en el cajón" else "Esconder del cajón") {
                StudioStore.updateLauncher { s -> s.copy(hiddenApps = if (hidden) s.hiddenApps - app.key else s.hiddenApps + app.key) }
                close()
            }
            if (homeItem != null) MenuRow("Quitar de la pantalla") { ui.remove(homeItem); close() }
            MenuRow("Editar pantalla") { close(); ui.drawerOpen = false; ui.openFolder = null; ui.editMode = true }
            MenuRow("Información de la app") { AppCatalog.openInfo(context, app); close() }
            MenuRow("Desinstalar") { AppCatalog.uninstall(context, app); close() }
        }
    }
}

@Composable
private fun MenuRow(text: String, onClick: () -> Unit) {
    Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 14.dp))
}

// -------------------------------------------------------- ícono por app

private enum class IconSource(val label: String) { ORIGINAL("Original"), PACK("Pack"), STYLE("Estilo"), PHOTO("Foto"), DRAWING("Dibujo") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconOverrideSheet(app: LaunchApp, settings: LauncherSettings, ui: LauncherUi) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val existing = remember(app.key) { StudioStore.override(app.key) }
    var o by remember(app.key) { mutableStateOf(existing ?: AppOverride(appKey = app.key)) }
    val drawings by StudioStore.drawingsFlow.collectAsStateWithLifecycle()
    var source by remember(app.key) {
        mutableStateOf(
            when {
                existing?.imagePath != null -> IconSource.PHOTO
                existing?.drawingId != null -> IconSource.DRAWING
                existing?.style != null -> IconSource.STYLE
                existing?.useIconPack == false -> IconSource.ORIGINAL
                else -> if (settings.iconPack != null) IconSource.PACK else IconSource.ORIGINAL
            }
        )
    }
    val style = o.style ?: settings.globalStyle ?: IconResolver.defaultStyle.copy(foreground = IconForeground.TINTED)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val path = withContext(Dispatchers.IO) { ImageStore.importImage(context, uri, 512) }
            if (path != null) o = o.copy(imagePath = path, drawingId = null)
        }
    }
    fun apply() {
        val final = when (source) {
            IconSource.ORIGINAL -> o.copy(style = null, imagePath = null, drawingId = null, useIconPack = false)
            IconSource.PACK -> o.copy(style = null, imagePath = null, drawingId = null, useIconPack = true)
            IconSource.STYLE -> o.copy(style = style, imagePath = null, drawingId = null)
            IconSource.PHOTO -> o.copy(drawingId = null)
            IconSource.DRAWING -> o.copy(imagePath = null)
        }
        StudioStore.setOverride(final)
        IconResolver.clear()
        ui.iconEditor = null
    }
    val previewSettings = settings
    val previewOverride = when (source) {
        IconSource.ORIGINAL -> o.copy(style = null, imagePath = null, drawingId = null, useIconPack = false)
        IconSource.PACK -> o.copy(style = null, imagePath = null, drawingId = null, useIconPack = true)
        IconSource.STYLE -> o.copy(style = style, imagePath = null, drawingId = null)
        IconSource.PHOTO -> o.copy(drawingId = null)
        IconSource.DRAWING -> o.copy(imagePath = null)
    }
    val preview by produceState<android.graphics.Bitmap?>(null, previewOverride, app.key) {
        value = withContext(Dispatchers.Default) { runCatching { IconResolver.icon(context, app, previewSettings, previewOverride, 256) }.getOrNull() }
    }

    ModalBottomSheet(onDismissRequest = { ui.iconEditor = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 700.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(84.dp)) { preview?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize()) } }
                Spacer(Modifier.width(14.dp))
                OutlinedTextField(o.label ?: app.label, { o = o.copy(label = it.take(24)) }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            ChoiceChips(IconSource.entries.filter { it != IconSource.PACK || settings.iconPack != null }, source, { it.label }, { source = it })
            when (source) {
                IconSource.STYLE -> {
                    ChoiceChips(IconShape.entries, style.shape, { it.label }, { o = o.copy(style = style.copy(shape = it)) })
                    ChoiceChips(IconForeground.entries, style.foreground, { it.label }, { o = o.copy(style = style.copy(foreground = it)) })
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        ColorRow("Fondo 1", style.bg1) { o = o.copy(style = style.copy(bg1 = it)) }
                        ColorRow("Fondo 2", style.bg2) { o = o.copy(style = style.copy(bg2 = it)) }
                        ColorRow("Color del dibujo", style.fgColor) { o = o.copy(style = style.copy(fgColor = it)) }
                    }
                }
                IconSource.PHOTO -> FilledTonalButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(if (o.imagePath == null) "Elegir foto" else "Cambiar foto")
                }
                IconSource.DRAWING -> {
                    if (drawings.isEmpty()) Text("Todavía no hiciste dibujos (Simón → Crear → Dibujo).", modifier = Modifier.padding(horizontal = 20.dp))
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(drawings, key = { it.id }) { d ->
                            val bmp by produceState<android.graphics.Bitmap?>(null, d.id) { value = withContext(Dispatchers.IO) { MediaLibrary.loadDrawing(d.id, 200) } }
                            Box(Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x22888888)).clickable { o = o.copy(drawingId = d.id, imagePath = null) }) {
                                bmp?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize()) }
                            }
                        }
                    }
                }
                else -> Unit
            }
            com.migux97.simon.ui.components.StickerAttachEditor(o.stickers) { o = o.copy(stickers = it) }
            Text("Movimiento del ícono", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 20.dp))
            ChoiceChips(listOf<Motion?>(null) + Motion.entries, o.motion, { it?.label ?: "Como todos" }, { o = o.copy(motion = it) })
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { StudioStore.clearOverride(app.key); IconResolver.clear(); ui.iconEditor = null }) { Text("Restablecer") }
                FilledTonalButton(onClick = { apply() }) { Text("Guardar") }
            }
        }
    }
}
