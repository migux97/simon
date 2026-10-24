package com.migux97.simon.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.migux97.simon.data.AppliedWallpaper
import com.migux97.simon.data.Catalog
import com.migux97.simon.data.FontStyle
import com.migux97.simon.data.ImageStore
import com.migux97.simon.data.LiveConfig
import com.migux97.simon.data.Lookup
import com.migux97.simon.data.Repository
import com.migux97.simon.data.SceneType
import com.migux97.simon.data.StaticPattern
import com.migux97.simon.data.StaticWallpaper
import com.migux97.simon.data.newId
import com.migux97.simon.ui.Nav
import com.migux97.simon.ui.components.BackTopBar
import com.migux97.simon.ui.components.ChoiceChips
import com.migux97.simon.ui.components.ColorRow
import com.migux97.simon.ui.components.ConfirmDialog
import com.migux97.simon.ui.components.GradientBadge
import com.migux97.simon.ui.components.HelpTip
import com.migux97.simon.ui.components.LabeledSlider
import com.migux97.simon.ui.components.Panel
import com.migux97.simon.ui.components.PhoneWallpaper
import com.migux97.simon.ui.components.PhoneWallpaperBackdrop
import com.migux97.simon.ui.components.SceneThumb
import com.migux97.simon.ui.components.ScenePreview
import com.migux97.simon.ui.components.StartOverButton
import com.migux97.simon.ui.components.StaticThumb
import com.migux97.simon.ui.components.UnsavedChangesDialog
import com.migux97.simon.ui.theme.SimonGradient
import com.migux97.simon.wallpaper.LiveWallpaper
import com.migux97.simon.wallpaper.StaticWallpapers
import com.migux97.simon.wallpaper.WallpaperTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Fondos que se están armando y todavía no se guardaron (un video recién
 * elegido, un fondo a partir de los colores del teléfono). Viven solo en
 * memoria hasta que el usuario toca «Guardar» o «Aplicar».
 */
object WallpaperDrafts {
    val live = mutableMapOf<String, LiveConfig>()
    val static = mutableMapOf<String, StaticWallpaper>()
}

private const val PUT_NOW = "Puesto ahora"

private fun neutralStatic(base: StaticWallpaper) = base.copy(
    pattern = StaticPattern.LINEAR,
    color1 = 0xFF2A1B5C.toInt(),
    color2 = 0xFF0F2027.toInt(),
    color3 = 0xFF8B6CFF.toInt()
)

private fun neutralLive(base: LiveConfig) = base.copy(
    color1 = 0xFF101024.toInt(),
    color2 = 0xFF8B6CFF.toInt(),
    color3 = 0xFFFF7EB6.toInt(),
    speed = 1f,
    density = 1f,
    showClock = false,
    clockColor = 0xFFFFFFFF.toInt(),
    clockFont = FontStyle.LIGHT
)

/** Arma un fondo estático (sin guardar) con los colores del fondo actual del teléfono. */
fun draftFromPhoneColors(context: android.content.Context): StaticWallpaper {
    val colors = PhoneWallpaper.colors(context).map { it.toArgb() or 0xFF000000.toInt() }
    val c1 = colors.getOrElse(0) { 0xFF2A1B5C.toInt() }
    val c2 = colors.getOrElse(1) { c1 }
    val c3 = colors.getOrElse(2) { c2 }
    val sw = StaticWallpaper(name = "Con mis colores", pattern = StaticPattern.MESH, color1 = c1, color2 = c2, color3 = c3)
    WallpaperDrafts.static[sw.id] = sw
    return sw
}

@Composable
fun WallpapersScreen(nav: Nav) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val saved by Repository.savedLive.collectAsStateWithLifecycle()
    val savedStatic by Repository.savedStatic.collectAsStateWithLifecycle()
    val photos by Repository.appliedPhotos.collectAsStateWithLifecycle()
    val current by Repository.currentStatic.collectAsStateWithLifecycle()
    val live by Repository.live.collectAsStateWithLifecycle()
    var pendingPhoto by remember { mutableStateOf<android.net.Uri?>(null) }
    var reapplyPhoto by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }

    // Lo que está puesto puede cambiar fuera de la app: se revisa al volver.
    var resumeTick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumeTick++ }
    val liveActive = remember(resumeTick) { LiveWallpaper.isActive(context) }
    val staticStill = remember(resumeTick, current) { StaticWallpapers.isStillApplied(context, current) }
    val staticOnHome = remember(resumeTick, current) { StaticWallpapers.isOnHomeNow(context, current) }

    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            Toast.makeText(context, "Preparando tu video…", Toast.LENGTH_SHORT).show()
            val path = withContext(Dispatchers.IO) { ImageStore.importVideo(context, uri) }
            if (path == null) {
                Toast.makeText(context, "No se pudo copiar el video", Toast.LENGTH_SHORT).show()
            } else {
                // No se guarda hasta que toques «Guardar» o «Aplicar».
                val cfg = LiveConfig(name = "Mi video", scene = SceneType.VIDEO, color1 = 0xFF000000.toInt(), color2 = 0xFFFFFFFF.toInt(), color3 = 0xFF6C4CF5.toInt(), videoPath = path)
                WallpaperDrafts.live[cfg.id] = cfg
                nav.live(cfg.id)
            }
        }
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> pendingPhoto = uri }
    fun pickPhoto() = photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text("Fondos", style = MaterialTheme.typography.headlineLarge)
            Text("Animados, estáticos o con tus fotos y videos", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "current") {
                CurrentWallpaperCard(
                    live = if (liveActive) live else null,
                    applied = if (!liveActive && staticOnHome) current else null,
                    onEditLive = { cfg -> nav.live(cfg.id) },
                    onEditStatic = { sw ->
                        if (Lookup.static(sw.id) == null) WallpaperDrafts.static[sw.id] = sw
                        nav.static(sw.id)
                    },
                    onChangePhoto = { pickPhoto() },
                    onFromPhoneColors = { nav.static(draftFromPhoneColors(context).id) }
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }, key = "tabs") {
                TabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
                    listOf("Animados", "Estáticos", "Míos").forEachIndexed { i, t -> Tab(tab == i, { tab = i }, text = { Text(t) }) }
                }
            }
            when (tab) {
                0 -> {
                    item {
                        ActionTile("Usar mi video", Icons.Filled.Movie) {
                            videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                        }
                    }
                    items(Catalog.liveWallpapers, key = { "c_" + it.id }) { lv ->
                        LiveTile(lv, if (liveActive && live?.id == lv.id) PUT_NOW else null) { nav.live(lv.id) }
                    }
                }
                1 -> {
                    item {
                        ActionTile("Usar mi foto", Icons.Filled.AddPhotoAlternate) { pickPhoto() }
                    }
                    items(Catalog.staticWallpapers, key = { "s_" + it.id }) { sw ->
                        StaticTile(sw, if (staticStill && current?.staticWallpaper?.id == sw.id) PUT_NOW else null) { nav.static(sw.id) }
                    }
                }
                else -> {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "h_live") {
                        MineHeader("Fondos animados", if (saved.isEmpty()) "Los fondos animados que guardes o apliques aparecen acá." else null)
                    }
                    items(saved, key = { "l_" + it.id }) { lv ->
                        LiveTile(
                            lv,
                            if (liveActive && live?.id == lv.id) PUT_NOW else null,
                            onDelete = {
                                pendingDelete = lv.name to {
                                    Repository.deleteSavedLive(lv.id)
                                    if (Repository.live.value?.id != lv.id) ImageStore.delete(lv.videoPath)
                                }
                            }
                        ) { nav.live(lv.id) }
                    }
                    item(span = { GridItemSpan(maxLineSpan) }, key = "h_static") {
                        MineHeader("Fondos estáticos", if (savedStatic.isEmpty()) "Tocá «Guardar» en un fondo estático para tenerlo acá." else null)
                    }
                    items(savedStatic, key = { "ss_" + it.id }) { sw ->
                        StaticTile(
                            sw,
                            if (staticStill && current?.staticWallpaper?.id == sw.id) PUT_NOW else null,
                            onDelete = { pendingDelete = sw.name to { Repository.deleteSavedStatic(sw.id) } }
                        ) { nav.static(sw.id) }
                    }
                    item(span = { GridItemSpan(maxLineSpan) }, key = "h_photos") {
                        MineHeader("Tus fotos", if (photos.isEmpty()) "Las fotos que pongas como fondo con Simón quedan acá para volver a usarlas." else "Tocá una para volver a ponerla.")
                    }
                    items(photos, key = { "p_$it" }) { path ->
                        PhotoTile(
                            path,
                            if (staticStill && current?.photoPath == path) PUT_NOW else null,
                            onDelete = { pendingDelete = "esta foto" to { Repository.deleteAppliedPhoto(path) } }
                        ) { reapplyPhoto = path }
                    }
                }
            }
        }
    }

    pendingPhoto?.let { uri ->
        TargetDialog(
            title = "Usar esta foto como fondo",
            onDismiss = { pendingPhoto = null },
            onPick = { target ->
                pendingPhoto = null
                scope.launch {
                    val path = withContext(Dispatchers.IO) { ImageStore.importImage(context, uri, 2600) }
                    val ok = path != null && applyPhoto(context, path, target)
                    Toast.makeText(context, if (ok) "¡Fondo aplicado! Quedó guardado en Míos." else "No se pudo aplicar el fondo", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
    reapplyPhoto?.let { path ->
        TargetDialog(
            title = "Volver a poner esta foto",
            onDismiss = { reapplyPhoto = null },
            onPick = { target ->
                reapplyPhoto = null
                scope.launch {
                    val ok = applyPhoto(context, path, target)
                    Toast.makeText(context, if (ok) "¡Fondo aplicado!" else "No se pudo aplicar el fondo", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
    pendingDelete?.let { (name, action) ->
        ConfirmDialog(
            title = "¿Borrar $name?",
            message = "Se quita de Míos. Si está puesto como fondo, sigue puesto hasta que lo cambies.",
            confirm = "Borrar",
            onDismiss = { pendingDelete = null },
            onConfirm = {
                pendingDelete = null
                action()
            }
        )
    }
}

/** Pone una foto (ya copiada dentro de la app) como fondo y la anota como actual. */
suspend fun applyPhoto(context: android.content.Context, path: String, target: WallpaperTarget): Boolean = withContext(Dispatchers.IO) {
    val decoded: Bitmap? = try {
        BitmapFactory.decodeFile(path)
    } catch (e: Throwable) {
        null
    }
    val bmp = decoded ?: return@withContext false
    val ids = StaticWallpapers.applyTracked(context, bmp, target) ?: return@withContext false
    Repository.setCurrentStatic(
        AppliedWallpaper(photoPath = path, target = target.name, homeId = ids.first, lockId = ids.second, appliedAt = System.currentTimeMillis())
    )
    Repository.addAppliedPhoto(path)
    true
}

/** Tarjeta "Tu fondo actual": lo que hay puesto y cómo seguir desde ahí. */
@Composable
private fun CurrentWallpaperCard(
    live: LiveConfig?,
    applied: AppliedWallpaper?,
    onEditLive: (LiveConfig) -> Unit,
    onEditStatic: (StaticWallpaper) -> Unit,
    onChangePhoto: () -> Unit,
    onFromPhoneColors: () -> Unit
) {
    val staticWp = applied?.staticWallpaper
    val photo = applied?.photoPath
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(64.dp)
                .aspectRatio(9f / 18f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black)
        ) {
            when {
                live != null && live.scene == SceneType.VIDEO ->
                    Icon(Icons.Filled.Movie, contentDescription = null, tint = Color.White, modifier = Modifier.align(Alignment.Center).size(28.dp))
                live != null -> SceneThumb(live, Modifier.fillMaxSize())
                staticWp != null -> StaticThumb(staticWp, Modifier.fillMaxSize())
                photo != null -> PhotoThumb(photo, Modifier.fillMaxSize())
                else -> PhoneWallpaperBackdrop(Modifier.fillMaxSize())
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Tu fondo actual", style = MaterialTheme.typography.titleMedium)
            when {
                live != null -> {
                    Text("«${live.name}» · animado de Simón", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FilledTonalButton(onClick = { onEditLive(live) }) { Text("Editar") }
                }
                staticWp != null -> {
                    Text("«${staticWp.name}» · estático de Simón", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FilledTonalButton(onClick = { onEditStatic(staticWp) }) { Text("Editar") }
                }
                photo != null -> {
                    Text("Una foto tuya, puesta con Simón", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FilledTonalButton(onClick = onChangePhoto) { Text("Cambiar foto") }
                }
                else -> {
                    Text("Tu fondo actual no es de Simón. Podés partir de sus colores:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FilledTonalButton(onClick = onFromPhoneColors) { Text("Crear un fondo con estos colores") }
                }
            }
        }
    }
}

@Composable
private fun MineHeader(title: String, hint: String?) {
    Column(Modifier.padding(top = 6.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (hint != null) Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ActionTile(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 18f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(8.dp))
                Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

/** Celda de la grilla: miniatura, insignia opcional y nombre con botón de borrar opcional. */
@Composable
private fun WallpaperTile(name: String, badge: String?, onDelete: (() -> Unit)?, onClick: () -> Unit, thumb: @Composable () -> Unit) {
    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 18f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
                .clickable(onClick = onClick)
        ) {
            thumb()
            if (badge != null) GradientBadge(badge, SimonGradient, Modifier.align(Alignment.TopStart).padding(6.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, style = MaterialTheme.typography.labelMedium, maxLines = 1, modifier = Modifier.weight(1f).padding(top = 4.dp, start = 2.dp))
            if (onDelete != null) {
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "Borrar", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun LiveTile(lv: LiveConfig, badge: String?, onDelete: (() -> Unit)? = null, onClick: () -> Unit) {
    WallpaperTile(lv.name, badge, onDelete, onClick) {
        if (lv.scene == SceneType.VIDEO) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Movie, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
            }
        } else {
            SceneThumb(lv, Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun StaticTile(sw: StaticWallpaper, badge: String?, onDelete: (() -> Unit)? = null, onClick: () -> Unit) {
    WallpaperTile(sw.name, badge, onDelete, onClick) { StaticThumb(sw, Modifier.fillMaxSize()) }
}

@Composable
private fun PhotoTile(path: String, badge: String?, onDelete: (() -> Unit)?, onClick: () -> Unit) {
    WallpaperTile("Foto", badge, onDelete, onClick) { PhotoThumb(path, Modifier.fillMaxSize()) }
}

@Composable
private fun PhotoThumb(path: String, modifier: Modifier = Modifier) {
    val bmp by produceState<Bitmap?>(null, path) {
        value = withContext(Dispatchers.IO) { ImageStore.load(path, 400) }
    }
    Box(modifier) {
        bmp?.let { Image(it.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
    }
}

@Composable
fun TargetDialog(title: String, onDismiss: () -> Unit, onPick: (WallpaperTarget) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                WallpaperTarget.entries.forEach { t ->
                    TextButton(onClick = { onPick(t) }, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            when (t) {
                                WallpaperTarget.HOME -> "Pantalla de inicio"
                                WallpaperTarget.LOCK -> "Pantalla de bloqueo"
                                WallpaperTarget.BOTH -> "Inicio y bloqueo"
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

/** Fila inferior de los editores de fondos: «Guardar» (sin poner) y «Aplicar». */
@Composable
private fun SaveApplyBar(saveEnabled: Boolean, applyText: String, busy: Boolean, onSave: () -> Unit, onApply: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedButton(onClick = onSave, enabled = saveEnabled && !busy, modifier = Modifier.weight(1f)) { Text(if (saveEnabled) "Guardar" else "Guardado") }
        Button(onClick = onApply, enabled = !busy, modifier = Modifier.weight(1f)) { Text(if (busy) "Aplicando…" else applyText) }
    }
}

// ------------------------------------------------------------ fondo animado

@Composable
fun LiveDetailScreen(nav: Nav, id: String) {
    val context = LocalContext.current
    val draft = remember(id) { WallpaperDrafts.live[id] }
    val original = remember(id) { Lookup.live(id) ?: draft }
    if (original == null) {
        Column { BackTopBar("Fondo", onBack = { nav.back() }); Text("Este fondo ya no existe.", Modifier.padding(24.dp)) }
        return
    }
    val catalogItem = remember(id) { Catalog.liveWallpapers.firstOrNull { it.id == id } }
    // Si es tuyo se guarda encima; si es del catálogo se guarda una copia tuya.
    var ownId by remember(id) {
        mutableStateOf(if (catalogItem == null && Repository.savedLive.value.any { it.id == id }) id else null)
    }
    var cfg by remember(id) { mutableStateOf(original) }
    var baseline by remember(id) { mutableStateOf(original) }
    var confirmExit by remember { mutableStateOf(false) }
    val isDraft = draft != null && ownId == null
    val dirty = isDraft || cfg != baseline
    val isVideo = cfg.scene == SceneType.VIDEO

    fun persist(): LiveConfig {
        val item = catalogItem
        val toSave = if (ownId == null && item != null) {
            cfg.copy(id = newId(), name = if (cfg.name == item.name) "${cfg.name} (mío)" else cfg.name)
        } else cfg
        Repository.saveLiveToCollection(toSave)
        WallpaperDrafts.live.remove(id)
        ownId = toSave.id
        cfg = toSave
        baseline = toSave
        return toSave
    }

    fun applyNow() {
        val toApply = if (dirty) persist() else cfg
        val done = LiveWallpaper.apply(context, toApply)
        WallpaperDrafts.live.remove(id)
        baseline = toApply
        if (done) Toast.makeText(context, "¡Fondo animado aplicado!", Toast.LENGTH_SHORT).show()
        else Toast.makeText(context, "Elegí «Establecer fondo» para terminar", Toast.LENGTH_LONG).show()
    }

    fun discardAndLeave() {
        if (isDraft) {
            WallpaperDrafts.live.remove(id)
            ImageStore.delete(cfg.videoPath)
        }
        nav.back()
    }

    BackHandler(enabled = dirty) { confirmExit = true }

    Column(Modifier.fillMaxSize()) {
        BackTopBar(
            cfg.name,
            onBack = { if (dirty) confirmExit = true else nav.back() },
            actions = {
                if (!isVideo) StartOverButton(
                    message = if (catalogItem != null) "Volvés a los colores y ajustes originales de este fondo. ¿Seguís?"
                    else "Volvés a colores neutros y ajustes normales. ¿Seguís?"
                ) {
                    cfg = (catalogItem ?: neutralLive(cfg)).copy(id = cfg.id, name = cfg.name)
                }
            }
        )
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 70.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color.Black)
        ) {
            if (isVideo) {
                val path = cfg.videoPath
                if (path != null && File(path).exists()) {
                    AndroidView(factory = { ctx ->
                        VideoView(ctx).apply {
                            setVideoPath(path)
                            setOnPreparedListener { mp -> mp.isLooping = true; mp.setVolume(0f, 0f); start() }
                        }
                    }, modifier = Modifier.fillMaxSize())
                }
            } else {
                ScenePreview(cfg, Modifier.fillMaxSize())
            }
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            HelpTip("«Guardar» lo deja en Míos sin cambiar tu fondo. «Aplicar» lo pone en tu pantalla.")
            Panel {
                OutlinedTextField(cfg.name, { cfg = cfg.copy(name = it.take(30)) }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            if (!isVideo) {
                Panel {
                    ColorRow("Color de fondo", cfg.color1) { cfg = cfg.copy(color1 = it) }
                    ColorRow("Color principal", cfg.color2) { cfg = cfg.copy(color2 = it) }
                    ColorRow("Color de detalles", cfg.color3) { cfg = cfg.copy(color3 = it) }
                    LabeledSlider("Velocidad", cfg.speed, 0.2f..3f, "x%.1f".format(cfg.speed)) { cfg = cfg.copy(speed = it) }
                    LabeledSlider("Cantidad", cfg.density, 0.3f..2.5f, "x%.1f".format(cfg.density)) { cfg = cfg.copy(density = it) }
                }
                Panel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Mostrar reloj en el fondo", modifier = Modifier.weight(1f))
                        Switch(cfg.showClock, { cfg = cfg.copy(showClock = it) })
                    }
                    if (cfg.showClock) ColorRow("Color del reloj", cfg.clockColor) { cfg = cfg.copy(clockColor = it) }
                }
                if (cfg.showClock) ChoiceChips(FontStyle.entries, cfg.clockFont, { it.label }, { cfg = cfg.copy(clockFont = it) })
            } else {
                Panel {
                    Text("Tu video se reproduce en silencio y en bucle como fondo de pantalla.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Panel {
                Text(
                    "Android pide confirmar los fondos animados una vez. Después, cambiar entre fondos de Simón es instantáneo.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        SaveApplyBar(
            saveEnabled = dirty,
            applyText = "Aplicar",
            busy = false,
            onSave = {
                persist()
                Toast.makeText(context, "Guardado en Míos", Toast.LENGTH_SHORT).show()
            },
            onApply = { applyNow() }
        )
    }

    if (confirmExit) {
        UnsavedChangesDialog(
            onSave = {
                confirmExit = false
                persist()
                nav.back()
            },
            onDiscard = {
                confirmExit = false
                discardAndLeave()
            },
            onDismiss = { confirmExit = false }
        )
    }
}

// ------------------------------------------------------------- fondo estático

@Composable
fun StaticDetailScreen(nav: Nav, id: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val draft = remember(id) { WallpaperDrafts.static[id] }
    val original = remember(id) { Lookup.static(id) ?: draft }
    if (original == null) {
        Column { BackTopBar("Fondo", onBack = { nav.back() }); Text("Este fondo ya no existe.", Modifier.padding(24.dp)) }
        return
    }
    val catalogItem = remember(id) {
        Catalog.staticWallpapers.firstOrNull { it.id == id } ?: Catalog.themes.map { it.wallpaper }.firstOrNull { it.id == id }
    }
    var ownId by remember(id) { mutableStateOf(if (Repository.staticById(id) != null) id else null) }
    var sw by remember(id) { mutableStateOf(original) }
    var baseline by remember(id) { mutableStateOf(original) }
    var chooseTarget by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var confirmExit by remember { mutableStateOf(false) }
    val isDraft = draft != null && ownId == null
    val dirty = isDraft || sw != baseline

    fun persist(): StaticWallpaper {
        val item = catalogItem
        val toSave = if (ownId == null && item != null) {
            sw.copy(id = newId(), name = if (sw.name == item.name) "${sw.name} (mío)" else sw.name)
        } else sw
        Repository.saveStatic(toSave)
        WallpaperDrafts.static.remove(id)
        ownId = toSave.id
        sw = toSave
        baseline = toSave
        return toSave
    }

    BackHandler(enabled = dirty) { confirmExit = true }

    Column(Modifier.fillMaxSize()) {
        BackTopBar(
            sw.name,
            onBack = { if (dirty) confirmExit = true else nav.back() },
            actions = {
                StartOverButton(
                    message = if (catalogItem != null) "Volvés al diseño y los colores originales de este fondo. ¿Seguís?"
                    else "Volvés a un degradado con colores neutros. ¿Seguís?"
                ) {
                    sw = (catalogItem ?: neutralStatic(sw)).copy(id = sw.id, name = sw.name)
                }
            }
        )
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 70.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(28.dp))
        ) { StaticThumb(sw, Modifier.fillMaxSize()) }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            HelpTip("«Guardar» lo deja en Míos sin cambiar tu fondo. «Aplicar» lo pone en tu pantalla.")
            Panel {
                OutlinedTextField(sw.name, { sw = sw.copy(name = it.take(30)) }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            Panel { Text("Diseño", style = MaterialTheme.typography.titleMedium) }
            ChoiceChips(StaticPattern.entries, sw.pattern, { it.label }, { sw = sw.copy(pattern = it) })
            Panel {
                ColorRow("Color de fondo", sw.color1) { sw = sw.copy(color1 = it) }
                ColorRow("Color principal", sw.color2) { sw = sw.copy(color2 = it) }
                ColorRow("Color de detalles", sw.color3) { sw = sw.copy(color3 = it) }
            }
        }
        SaveApplyBar(
            saveEnabled = dirty,
            applyText = "Aplicar",
            busy = busy,
            onSave = {
                persist()
                Toast.makeText(context, "Guardado en Míos", Toast.LENGTH_SHORT).show()
            },
            onApply = { chooseTarget = true }
        )
    }

    if (chooseTarget) {
        TargetDialog("¿Dónde lo ponemos?", onDismiss = { chooseTarget = false }) { target ->
            chooseTarget = false
            busy = true
            val toApply = if (dirty) persist() else sw
            scope.launch {
                val ok = applyStatic(context, toApply, target)
                busy = false
                Toast.makeText(context, if (ok) "¡Fondo aplicado!" else "No se pudo aplicar el fondo", Toast.LENGTH_SHORT).show()
            }
        }
    }
    if (confirmExit) {
        UnsavedChangesDialog(
            onSave = {
                confirmExit = false
                persist()
                nav.back()
            },
            onDiscard = {
                confirmExit = false
                WallpaperDrafts.static.remove(id)
                nav.back()
            },
            onDismiss = { confirmExit = false }
        )
    }
}

/** Genera y pone un fondo estático, y lo anota como el fondo actual de Simón. */
suspend fun applyStatic(context: android.content.Context, sw: StaticWallpaper, target: WallpaperTarget): Boolean {
    val (w, h) = StaticWallpapers.screenSize(context)
    val ids = withContext(Dispatchers.Default) {
        val bmp = StaticWallpapers.render(sw, w, h)
        withContext(Dispatchers.IO) { StaticWallpapers.applyTracked(context, bmp, target) }
    } ?: return false
    Repository.setCurrentStatic(
        AppliedWallpaper(staticWallpaper = sw, target = target.name, homeId = ids.first, lockId = ids.second, appliedAt = System.currentTimeMillis())
    )
    return true
}
