package com.migux97.simon.ui.screens

import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.migux97.simon.data.Catalog
import com.migux97.simon.data.FontStyle
import com.migux97.simon.data.ImageStore
import com.migux97.simon.data.LiveConfig
import com.migux97.simon.data.Lookup
import com.migux97.simon.data.Repository
import com.migux97.simon.data.SceneType
import com.migux97.simon.data.StaticPattern
import com.migux97.simon.data.newId
import com.migux97.simon.ui.Nav
import com.migux97.simon.ui.components.BackTopBar
import com.migux97.simon.ui.components.ChoiceChips
import com.migux97.simon.ui.components.ColorRow
import com.migux97.simon.ui.components.GradientBadge
import com.migux97.simon.ui.components.LabeledSlider
import com.migux97.simon.ui.components.Panel
import com.migux97.simon.ui.components.SceneThumb
import com.migux97.simon.ui.components.ScenePreview
import com.migux97.simon.ui.components.StaticThumb
import com.migux97.simon.ui.theme.SimonGradient
import com.migux97.simon.wallpaper.LiveWallpaper
import com.migux97.simon.wallpaper.StaticWallpapers
import com.migux97.simon.wallpaper.WallpaperTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun WallpapersScreen(nav: Nav) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val saved by Repository.savedLive.collectAsStateWithLifecycle()
    var pendingPhoto by remember { mutableStateOf<android.net.Uri?>(null) }

    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            Toast.makeText(context, "Preparando tu video…", Toast.LENGTH_SHORT).show()
            val path = withContext(Dispatchers.IO) { ImageStore.importVideo(context, uri) }
            if (path == null) {
                Toast.makeText(context, "No se pudo copiar el video", Toast.LENGTH_SHORT).show()
            } else {
                val cfg = LiveConfig(name = "Mi video", scene = SceneType.VIDEO, color1 = 0xFF000000.toInt(), color2 = 0xFFFFFFFF.toInt(), color3 = 0xFF6C4CF5.toInt(), videoPath = path)
                Repository.saveLiveToCollection(cfg)
                nav.live(cfg.id)
            }
        }
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> pendingPhoto = uri }

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text("Fondos", style = MaterialTheme.typography.headlineLarge)
            Text("Animados, estáticos o con tus fotos y videos", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
            listOf("Animados", "Estáticos", "Míos").forEachIndexed { i, t -> Tab(tab == i, { tab = i }, text = { Text(t) }) }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            when (tab) {
                0 -> {
                    item {
                        ActionTile("Usar mi video", Icons.Filled.Movie) {
                            videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                        }
                    }
                    items(Catalog.liveWallpapers, key = { it.id }) { lv -> LiveTile(lv) { nav.live(lv.id) } }
                }
                1 -> {
                    item {
                        ActionTile("Usar mi foto", Icons.Filled.AddPhotoAlternate) {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    }
                    items(Catalog.staticWallpapers, key = { it.id }) { sw ->
                        Column {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(9f / 18f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { nav.static(sw.id) }
                            ) { StaticThumb(sw, Modifier.fillMaxSize()) }
                            Text(sw.name, style = MaterialTheme.typography.labelMedium, maxLines = 1, modifier = Modifier.padding(top = 4.dp, start = 2.dp))
                        }
                    }
                }
                else -> {
                    if (saved.isEmpty()) {
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                            Text("Acá aparecen los fondos animados que personalices o apliques.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp))
                        }
                    }
                    items(saved, key = { it.id }) { lv -> LiveTile(lv) { nav.live(lv.id) } }
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
                    val ok = withContext(Dispatchers.IO) {
                        val bmp = ImageStore.decodeUri(context, uri, 2600)
                        bmp != null && StaticWallpapers.apply(context, bmp, target)
                    }
                    Toast.makeText(context, if (ok) "¡Fondo aplicado!" else "No se pudo aplicar el fondo", Toast.LENGTH_SHORT).show()
                }
            }
        )
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

@Composable
private fun LiveTile(lv: LiveConfig, onClick: () -> Unit) {
    val active by Repository.live.collectAsStateWithLifecycle()
    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 18f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
                .clickable(onClick = onClick)
        ) {
            if (lv.scene == SceneType.VIDEO) {
                Icon(Icons.Filled.Movie, contentDescription = null, tint = Color.White, modifier = Modifier.align(Alignment.Center).size(40.dp))
            } else {
                SceneThumb(lv, Modifier.fillMaxSize())
            }
            if (active?.id == lv.id) GradientBadge("Activo", SimonGradient, Modifier.align(Alignment.TopStart).padding(6.dp))
        }
        Text(lv.name, style = MaterialTheme.typography.labelMedium, maxLines = 1, modifier = Modifier.padding(top = 4.dp, start = 2.dp))
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

// ------------------------------------------------------------ fondo animado

@Composable
fun LiveDetailScreen(nav: Nav, id: String) {
    val context = LocalContext.current
    val original = remember(id) { Lookup.live(id) }
    if (original == null) {
        Column { BackTopBar("Fondo", onBack = { nav.back() }); Text("Este fondo ya no existe.", Modifier.padding(24.dp)) }
        return
    }
    var cfg by remember(id) { mutableStateOf(original) }
    val isVideo = cfg.scene == SceneType.VIDEO

    Column(Modifier.fillMaxSize()) {
        BackTopBar(cfg.name, onBack = { nav.back() })
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
            if (!isVideo) {
                Panel {
                    ColorRow("Color de fondo", cfg.color1) { cfg = cfg.copy(color1 = it) }
                    ColorRow("Color principal", cfg.color2) { cfg = cfg.copy(color2 = it) }
                    ColorRow("Color secundario", cfg.color3) { cfg = cfg.copy(color3 = it) }
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
        Button(
            onClick = {
                // Los fondos del catálogo se guardan como una copia propia al personalizarlos.
                val toApply = if (cfg != original && Catalog.liveWallpapers.any { it.id == cfg.id }) cfg.copy(id = newId(), name = "${cfg.name} (mío)") else cfg
                cfg = toApply
                val done = LiveWallpaper.apply(context, toApply)
                if (done) Toast.makeText(context, "¡Fondo animado aplicado!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp)
        ) { Text("Aplicar fondo animado") }
    }
}

// ------------------------------------------------------------- fondo estático

@Composable
fun StaticDetailScreen(nav: Nav, id: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val original = remember(id) { Lookup.static(id) }
    if (original == null) {
        Column { BackTopBar("Fondo", onBack = { nav.back() }); Text("Este fondo ya no existe.", Modifier.padding(24.dp)) }
        return
    }
    var sw by remember(id) { mutableStateOf(original) }
    var chooseTarget by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        BackTopBar(sw.name, onBack = { nav.back() })
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
            Panel { Text("Diseño", style = MaterialTheme.typography.titleMedium) }
            ChoiceChips(StaticPattern.entries, sw.pattern, { it.label }, { sw = sw.copy(pattern = it) })
            Panel {
                ColorRow("Color 1", sw.color1) { sw = sw.copy(color1 = it) }
                ColorRow("Color 2", sw.color2) { sw = sw.copy(color2 = it) }
                ColorRow("Color 3", sw.color3) { sw = sw.copy(color3 = it) }
            }
        }
        Button(
            onClick = { chooseTarget = true },
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp)
        ) { Text(if (busy) "Aplicando…" else "Usar como fondo") }
    }

    if (chooseTarget) {
        TargetDialog("¿Dónde lo ponemos?", onDismiss = { chooseTarget = false }) { target ->
            chooseTarget = false
            busy = true
            scope.launch {
                val ok = applyStatic(context, sw, target)
                busy = false
                Toast.makeText(context, if (ok) "¡Fondo aplicado!" else "No se pudo aplicar el fondo", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

suspend fun applyStatic(context: android.content.Context, sw: com.migux97.simon.data.StaticWallpaper, target: WallpaperTarget): Boolean {
    val (w, h) = StaticWallpapers.screenSize(context)
    return withContext(Dispatchers.Default) {
        val bmp = StaticWallpapers.render(sw, w, h)
        withContext(Dispatchers.IO) { StaticWallpapers.apply(context, bmp, target) }
    }
}
