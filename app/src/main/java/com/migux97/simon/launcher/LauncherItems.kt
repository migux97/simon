package com.migux97.simon.launcher

import android.graphics.Bitmap
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.FlipToFront
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.migux97.simon.custom.CustomWidgetLive
import com.migux97.simon.data.HomeItem
import com.migux97.simon.data.ImageStore
import com.migux97.simon.data.ItemType
import com.migux97.simon.data.LauncherBackground
import com.migux97.simon.data.LauncherBgType
import com.migux97.simon.data.LauncherSettings
import com.migux97.simon.data.Lookup
import com.migux97.simon.data.MediaLibrary
import com.migux97.simon.data.Motion
import com.migux97.simon.data.Repository
import com.migux97.simon.data.StickerSource
import com.migux97.simon.data.StudioStore
import com.migux97.simon.ui.components.ScenePreview
import com.migux97.simon.ui.components.WidgetPreview
import com.migux97.simon.ui.components.motion
import com.migux97.simon.widget.Draw
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

// ------------------------------------------------------------------ fondo

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LauncherBackgroundLayer(bg: LauncherBackground, pager: PagerState) {
    if (bg.type == LauncherBgType.SYSTEM && bg.dim <= 0f) return
    val shift = if (bg.parallax) -(pager.currentPage + pager.currentPageOffsetFraction) * 24f else 0f
    Box(Modifier.fillMaxSize()) {
        val mod = Modifier.fillMaxSize().graphicsLayer { translationX = shift * density; scaleX = 1.08f; scaleY = 1.08f }
        when (bg.type) {
            LauncherBgType.SYSTEM -> Unit
            LauncherBgType.COLOR -> Box(Modifier.fillMaxSize().background(Color(bg.color1)))
            LauncherBgType.GRADIENT -> Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(bg.color1), Color(bg.color2)))))
            LauncherBgType.IMAGE -> BitmapFill(bg.path, null, mod)
            LauncherBgType.DRAWING -> BitmapFill(null, bg.mediaId, mod)
            LauncherBgType.VIDEO -> bg.path?.takeIf { File(it).exists() }?.let {
                // Con "videos desactivados" (rendimiento) se muestra el primer cuadro.
                if (StudioStore.perf.allowVideo) LoopingVideo(it, Modifier.fillMaxSize())
                else MediaLibrary.videoThumb(it)?.let { b -> Image(b.asImageBitmap(), null, mod, contentScale = ContentScale.Crop) }
            }
            LauncherBgType.SCENE -> bg.liveId?.let { Lookup.live(it) }?.let { ScenePreview(it, Modifier.fillMaxSize()) }
            LauncherBgType.ANIMATION -> AnimatedFrames(bg.mediaId, mod, crop = true)
        }
        if (bg.dim > 0f) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = bg.dim.coerceIn(0f, 0.9f))))
    }
}

@Composable
fun BitmapFill(path: String?, drawingId: String?, modifier: Modifier, crop: Boolean = true) {
    val bmp by produceState<Bitmap?>(null, path, drawingId) {
        value = withContext(Dispatchers.IO) { if (drawingId != null) MediaLibrary.loadDrawing(drawingId, 1600) else ImageStore.load(path, 1600) }
    }
    bmp?.let { Image(it.asImageBitmap(), null, modifier, contentScale = if (crop) ContentScale.Crop else ContentScale.Fit) }
}

@Composable
fun LoopingVideo(path: String, modifier: Modifier) {
    AndroidView(
        factory = { ctx ->
            VideoView(ctx).apply {
                setVideoPath(path)
                setOnPreparedListener { mp -> mp.isLooping = true; mp.setVolume(0f, 0f); start() }
            }
        },
        modifier = modifier
    )
}

/** Reproduce una animación hecha en el editor. */
@Composable
fun AnimatedFrames(animationId: String?, modifier: Modifier, crop: Boolean = false) {
    val frames by produceState<List<Bitmap>>(emptyList(), animationId) {
        value = withContext(Dispatchers.IO) { MediaLibrary.loadFrames(animationId, 900) }
    }
    var time by remember { mutableLongStateOf(0L) }
    val active = com.migux97.simon.ui.components.LocalAnimationsActive.current
    LaunchedEffect(frames.size, active) {
        if (frames.size > 1 && active) com.migux97.simon.ui.components.frameLoop { time = it }
    }
    val idx = MediaLibrary.frameIndex(StudioStore.animation(animationId), frames.size, time)
    frames.getOrNull(idx)?.let { Image(it.asImageBitmap(), null, modifier, contentScale = if (crop) ContentScale.Crop else ContentScale.Fit) }
}

// ---------------------------------------------------------------- página

@Composable
fun PageCanvas(page: Int, items: List<HomeItem>, settings: LauncherSettings, ui: LauncherUi) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wPx = constraints.maxWidth.toFloat()
        val hPx = constraints.maxHeight.toFloat()
        items.forEach { item ->
            androidx.compose.runtime.key(item.id) { FreeItem(item, wPx, hPx, settings, ui) }
        }
    }
}

@Composable
private fun FreeItem(item: HomeItem, wPx: Float, hPx: Float, settings: LauncherSettings, ui: LauncherUi) {
    val context = LocalContext.current
    val density = LocalDensity.current
    // Estado estable por elemento: los gestos lo modifican y se guarda al soltar.
    var live by remember(item.id) { mutableStateOf(item) }
    LaunchedEffect(item) { live = item }
    val selected = ui.editMode && ui.selectedId == item.id
    val wDp = with(density) { (live.w * wPx).toDp() }
    val hDp = with(density) { (live.h * hPx).toDp() }

    fun commit() = StudioStore.upsertItem(LauncherLayout.snap(live, settings).also { live = it })

    Box(
        Modifier
            .offset { IntOffset((live.x * wPx).roundToInt(), (live.y * hPx).roundToInt()) }
            .size(wDp, hDp)
    ) {
        val motion = if (item.type == ItemType.APP && item.motion == Motion.NONE)
            StudioStore.override(item.appKey ?: "")?.motion ?: settings.globalMotion else item.motion
        Box(
            Modifier
                .fillMaxSize()
                .motion(if (ui.editMode) Motion.NONE else motion, item.motionSpeed, live.rotation, live.alpha)
                .then(if (selected) Modifier.border(2.dp, Color.White, RoundedCornerShape(10.dp)) else Modifier)
        ) {
            ItemContent(item, settings, ui)
            // Stickers pegados al elemento (los de las apps ya vienen dentro del ícono)
            if (item.stickers.isNotEmpty()) com.migux97.simon.custom.StickerLayer(item.stickers, Modifier.fillMaxSize())
            // En modo edición una capa encima toma todos los toques (también sobre widgets del sistema).
            if (ui.editMode) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(item.id, item.locked) {
                            detectTapGestures(onTap = { ui.selectedId = item.id }, onDoubleTap = { ui.editItem = StudioStore.items.firstOrNull { it.id == item.id } })
                        }
                        .pointerInput(item.id, item.locked) {
                            if (!item.locked) detectDragGestures(
                                onDragStart = { ui.selectedId = item.id },
                                onDrag = { change, d ->
                                    change.consume()
                                    live = live.copy(
                                        x = (live.x + d.x / wPx).coerceIn(-live.w * 0.5f, 1f - live.w * 0.5f),
                                        y = (live.y + d.y / hPx).coerceIn(-0.02f, 1f - live.h * 0.5f)
                                    )
                                },
                                onDragEnd = { commit() }
                            )
                        }
                )
            } else if (item.type != ItemType.SYSTEM_WIDGET && item.type != ItemType.CUSTOM_WIDGET) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(item.id) {
                            detectTapGestures(
                                onTap = { onItemTap(context, item, ui) },
                                onLongPress = {
                                    if (item.type == ItemType.APP) {
                                        ui.appMenu = AppCatalog.find(item.appKey); ui.appMenuItem = item
                                    } else {
                                        ui.editMode = true; ui.selectedId = item.id
                                    }
                                }
                            )
                        }
                )
            }
        }
        if (selected && !item.locked) {
            // Manija para cambiar tamaño
            Handle(Icons.Filled.OpenInFull, Modifier.align(Alignment.BottomEnd).offset(10.dp, 10.dp), onEnd = { commit() }) { d ->
                live = live.copy(w = (live.w + d.x / wPx).coerceIn(0.05f, 1.2f), h = (live.h + d.y / hPx).coerceIn(0.03f, 1.2f))
            }
            // Manija para girar
            Handle(Icons.Filled.RotateRight, Modifier.align(Alignment.TopCenter).offset(0.dp, (-14).dp), onEnd = { commit() }) { d ->
                live = live.copy(rotation = (live.rotation + d.x * 0.6f) % 360f)
            }
        }
    }
}

@Composable
private fun Handle(icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onEnd: () -> Unit, onDrag: (androidx.compose.ui.geometry.Offset) -> Unit) {
    Box(
        modifier
            .size(28.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(Color.White)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, d -> change.consume(); onDrag(d) },
                    onDragEnd = { onEnd() }
                )
            },
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = Color(0xFF6C4CF5), modifier = Modifier.size(16.dp)) }
}

private fun onItemTap(context: android.content.Context, item: HomeItem, ui: LauncherUi) {
    when (item.type) {
        ItemType.APP -> AppCatalog.find(item.appKey)?.let { AppCatalog.launch(context, it) }
        ItemType.FOLDER -> ui.openFolder = item
        ItemType.DESIGN_WIDGET -> context.startActivity(
            android.content.Intent(context, com.migux97.simon.MainActivity::class.java)
                .putExtra(com.migux97.simon.MainActivity.EXTRA_EDIT_DESIGN, item.designId)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        else -> Unit
    }
}

@Composable
private fun ItemContent(item: HomeItem, settings: LauncherSettings, ui: LauncherUi) {
    when (item.type) {
        ItemType.APP -> AppCatalog.find(item.appKey)?.let { AppIconView(it, settings, item.showLabel && settings.showLabels, Modifier.fillMaxSize()) }
            ?: Box(Modifier.fillMaxSize())
        ItemType.CUSTOM_WIDGET -> {
            val widgets by StudioStore.widgetsFlow.collectAsStateWithLifecycle()
            widgets.firstOrNull { it.id == item.customWidgetId }?.let {
                CustomWidgetLive(it, Modifier.fillMaxSize(), interactive = !ui.editMode, onOpenDrawer = { ui.drawerOpen = true })
            }
        }
        ItemType.DESIGN_WIDGET -> {
            val designs by Repository.designs.collectAsStateWithLifecycle()
            designs.firstOrNull { it.id == item.designId }?.let {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { WidgetPreview(it, Modifier.fillMaxSize()) }
            }
        }
        ItemType.SYSTEM_WIDGET -> AndroidView(
            factory = { ctx -> WidgetHost.createView(ctx, item.appWidgetId) ?: FrameLayout(ctx) },
            modifier = Modifier.fillMaxSize()
        )
        ItemType.STICKER -> when (item.stickerSource) {
            StickerSource.IMAGE -> BitmapFill(item.mediaPath, null, Modifier.fillMaxSize(), crop = false)
            StickerSource.DRAWING -> BitmapFill(null, item.mediaId, Modifier.fillMaxSize(), crop = false)
            StickerSource.ANIMATION -> AnimatedFrames(item.mediaId, Modifier.fillMaxSize())
            StickerSource.VIDEO -> item.mediaPath?.takeIf { File(it).exists() }?.let {
                if (StudioStore.perf.allowVideo && com.migux97.simon.ui.components.LocalAnimationsActive.current) LoopingVideo(it, Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)))
                else MediaLibrary.videoThumb(it)?.let { b -> Image(b.asImageBitmap(), null, Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop) }
            }
            StickerSource.LIBRARY -> StudioStore.sticker(item.mediaId)?.let { info ->
                androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                    androidx.compose.ui.graphics.drawscope.drawIntoCanvas {
                        com.migux97.simon.custom.StickerPainter.draw(it.nativeCanvas, info, android.graphics.RectF(0f, 0f, size.width, size.height))
                    }
                }
            }
            StickerSource.SCENE -> item.liveId?.let { Lookup.live(it) }?.let {
                ScenePreview(it, Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)))
            }
        }
        ItemType.FOLDER -> FolderIcon(item, settings)
    }
}

// --------------------------------------------------------------- íconos

@Composable
fun AppIconBitmap(app: LaunchApp, settings: LauncherSettings, sizeDp: Int, modifier: Modifier) {
    val context = LocalContext.current
    val overrides by StudioStore.overridesFlow.collectAsStateWithLifecycle()
    val override = overrides.firstOrNull { it.appKey == app.key }
    val px = with(LocalDensity.current) { sizeDp.dp.roundToPx() }.coerceIn(48, 320)
    val bmp by produceState<Bitmap?>(null, app.key, override, settings.globalStyle, settings.iconPack, px) {
        value = withContext(Dispatchers.Default) {
            try { IconResolver.icon(context, app, settings, override, px) } catch (e: Throwable) { null }
        }
    }
    Box(modifier) { bmp?.let { Image(it.asImageBitmap(), AppCatalog.label(app), Modifier.fillMaxSize()) } }
}

@Composable
fun AppIconView(app: LaunchApp, settings: LauncherSettings, showLabel: Boolean, modifier: Modifier) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val labelSpace = if (showLabel) (settings.labelSizeSp + 8).dp else 0.dp
        val iconDp = minOf(settings.iconSizeDp.dp, maxWidth * 0.9f, (maxHeight - labelSpace) * 0.95f).coerceAtLeast(16.dp)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            AppIconBitmap(app, settings, iconDp.value.roundToInt(), Modifier.size(iconDp))
            if (showLabel) {
                Text(
                    AppCatalog.label(app),
                    style = TextStyle(
                        color = Color(settings.labelColor),
                        fontSize = settings.labelSizeSp.sp,
                        fontFamily = FontFamily(Draw.typeface(settings.labelFont)),
                        textAlign = TextAlign.Center,
                        shadow = if (settings.labelShadow) Shadow(Color.Black.copy(alpha = 0.6f), blurRadius = 6f) else null
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp).fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun FolderIcon(item: HomeItem, settings: LauncherSettings) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val box = minOf(settings.iconSizeDp.dp, maxWidth * 0.9f, maxHeight * 0.7f)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(box)
                    .clip(RoundedCornerShape(box * 0.3f))
                    .background(Color(item.folderColor))
                    .padding(box * 0.12f)
            ) {
                val apps = item.folderApps.mapNotNull { AppCatalog.find(it) }.take(4)
                Column(verticalArrangement = Arrangement.spacedBy(box * 0.06f)) {
                    apps.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(box * 0.06f)) {
                            row.forEach { AppIconBitmap(it, settings, 40, Modifier.size(box * 0.35f)) }
                        }
                    }
                }
            }
            if (settings.showLabels) Text(
                item.title.ifBlank { "Carpeta" },
                color = Color(settings.labelColor), fontSize = settings.labelSizeSp.sp, maxLines = 1,
                style = TextStyle(shadow = if (settings.labelShadow) Shadow(Color.Black.copy(alpha = 0.6f), blurRadius = 6f) else null),
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}

// ----------------------------------------------------------------- dock

@Composable
fun Dock(settings: LauncherSettings, ui: LauncherUi) {
    val context = LocalContext.current
    val apps = settings.dockApps.mapNotNull { AppCatalog.find(it) }
    if (apps.isEmpty()) return
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(settings.dockRadius.dp))
            .background(Color(settings.dockColor))
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        apps.forEach { app ->
            Box(
                Modifier
                    .size(settings.iconSizeDp.dp.coerceAtMost(64.dp))
                    .motion(StudioStore.override(app.key)?.motion ?: settings.globalMotion)
                    .pointerInput(app.key) {
                        detectTapGestures(
                            onTap = { AppCatalog.launch(context, app) },
                            onLongPress = { ui.appMenu = app; ui.appMenuItem = null }
                        )
                    }
            ) { AppIconBitmap(app, settings, settings.iconSizeDp, Modifier.fillMaxSize()) }
        }
    }
}

// ---------------------------------------------------------- barra edición

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EditBar(ui: LauncherUi, settings: LauncherSettings, pager: PagerState, onAddPage: () -> Unit) {
    val context = LocalContext.current
    val selected = StudioStore.items.firstOrNull { it.id == ui.selectedId }
    Box(Modifier.fillMaxSize()) {
        Surface(
            color = Color(0xEE1C1530),
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 8.dp, start = 10.dp, end = 10.dp)
        ) {
            Row(Modifier.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (selected == null) {
                    BarButton(Icons.Filled.Add, "Agregar") { ui.addSheet = true }
                    BarButton(Icons.Filled.AddBox, "Página") { onAddPage() }
                    BarButton(Icons.Filled.Settings, "Ajustes") {
                        context.startActivity(
                            android.content.Intent(context, com.migux97.simon.MainActivity::class.java)
                                .putExtra(com.migux97.simon.MainActivity.EXTRA_ROUTE, "launcherSettings")
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                    Text("Página ${pager.currentPage + 1}", color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 8.dp))
                } else {
                    BarButton(Icons.Filled.Tune, "Editar") { ui.editItem = selected }
                    BarButton(Icons.Filled.FlipToFront, "Adelante") { LauncherLayout.bringToFront(selected) }
                    BarButton(Icons.Filled.FlipToBack, "Atrás") { LauncherLayout.sendToBack(selected) }
                    BarButton(Icons.Filled.ContentCopy, "Duplicar") { LauncherLayout.duplicate(selected) }
                    BarButton(if (selected.locked) Icons.Filled.Lock else Icons.Filled.LockOpen, "Fijar") {
                        StudioStore.upsertItem(selected.copy(locked = !selected.locked))
                    }
                    BarButton(Icons.Filled.Delete, "Quitar") {
                        if (selected.type == ItemType.SYSTEM_WIDGET) WidgetHost.delete(context, selected.appWidgetId)
                        StudioStore.removeItem(selected.id)
                        ui.selectedId = null
                    }
                }
                BarButton(Icons.Filled.Check, "Listo") { ui.editMode = false; ui.selectedId = null }
            }
        }
        Text(
            if (selected == null) "Arrastrá cualquier cosa · tocá para seleccionar · doble toque para propiedades"
            else "Arrastrá para mover · manijas para tamaño y giro",
            color = Color.White,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            style = TextStyle(shadow = Shadow(Color.Black, blurRadius = 8f)),
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 30.dp).graphicsLayer { alpha = 0.7f }
        )
    }
}

@Composable
private fun BarButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, label, tint = Color.White, modifier = Modifier.size(22.dp))
        Text(label, color = Color.White, fontSize = 10.sp)
    }
}
