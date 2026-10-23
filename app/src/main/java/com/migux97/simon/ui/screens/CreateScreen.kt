package com.migux97.simon.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.migux97.simon.custom.CustomWidgetLive
import com.migux97.simon.data.MediaLibrary
import com.migux97.simon.data.StudioStore
import com.migux97.simon.launcher.AnimatedFrames
import com.migux97.simon.ui.Nav
import com.migux97.simon.ui.components.SectionHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class Delete(val kind: String, val id: String, val name: String)

@Composable
fun CreateScreen(nav: Nav) {
    val widgets by StudioStore.widgetsFlow.collectAsStateWithLifecycle()
    val drawings by StudioStore.drawingsFlow.collectAsStateWithLifecycle()
    val animations by StudioStore.animationsFlow.collectAsStateWithLifecycle()
    var pending by remember { mutableStateOf<Delete?>(null) }
    val stickers by StudioStore.stickersFlow.collectAsStateWithLifecycle()
    var pickSticker by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Column(Modifier.statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text("Crear", style = MaterialTheme.typography.headlineLarge)
                Text("Widgets, dibujos y animaciones hechos por vos", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BigAction("Widget", Icons.Filled.Widgets, listOf(Color(0xFF8B6CFF), Color(0xFFFF7EB6)), Modifier.weight(1f)) { nav.go("builder/new") }
                BigAction("Dibujo", Icons.Filled.Brush, listOf(Color(0xFFFF7E5F), Color(0xFFFEB47B)), Modifier.weight(1f)) { nav.go("drawing/new") }
                BigAction("Animación", Icons.Filled.Movie, listOf(Color(0xFF1FA2FF), Color(0xFF12D8FA)), Modifier.weight(1f)) { nav.go("animation/new") }
            }
        }
        item { SectionHeader("Empezá desde un modelo") }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(listOf("new1" to "Saludo con hora", "new2" to "Corazones", "new3" to "Progreso del año")) { (id, name) ->
                    Column(Modifier.width(150.dp).clickable { nav.go("builder/$id") }) {
                        Box(
                            Modifier.fillMaxWidth().height(90.dp).clip(RoundedCornerShape(18.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF2A1B5C), Color(0xFF6C4CF5)))),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Filled.Add, null, tint = Color.White) }
                        Text(name, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
        item { SectionHeader("Mis stickers", "Nuevo", onAction = { pickSticker = true }) }
        if (stickers.isEmpty()) item { Hint("Creá stickers desde fotos, dibujos, animaciones o formas con tus colores, y pegalos en íconos, widgets y el launcher.") }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(stickers, key = { "s" + it.id }) { st ->
                    Box(
                        Modifier.size(72.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { pending = Delete("s", st.id, st.name) }
                            .padding(8.dp)
                    ) { com.migux97.simon.ui.components.StickerThumb(st, Modifier.fillMaxSize()) }
                }
            }
        }
        item { SectionHeader("Mis widgets") }
        if (widgets.isEmpty()) item { Hint("Todavía no creaste widgets.") }
        items(widgets, key = { "w" + it.id }) { w ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.height(80.dp).aspectRatio(w.aspect.coerceIn(0.5f, 3.5f)).clickable { nav.go("builder/${w.id}") }
                ) { CustomWidgetLive(w, Modifier.fillMaxSize(), interactive = false) }
                Text(w.name, Modifier.weight(1f).padding(horizontal = 12.dp), style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = { pending = Delete("w", w.id, w.name) }) { Icon(Icons.Filled.Delete, "Borrar") }
            }
        }
        item { SectionHeader("Mis dibujos") }
        if (drawings.isEmpty()) item { Hint("Todavía no dibujaste nada.") }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(drawings, key = { it.id }) { d ->
                    val bmp by produceState<android.graphics.Bitmap?>(null, d.id, d.updatedAt) { value = withContext(Dispatchers.IO) { MediaLibrary.loadDrawing(d.id, 360) } }
                    Column(Modifier.width(110.dp)) {
                        Box(
                            Modifier.size(110.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable { nav.go("drawing/${d.id}") }
                        ) { bmp?.let { Image(it.asImageBitmap(), d.name, Modifier.fillMaxSize()) } }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(d.name, style = MaterialTheme.typography.labelMedium, maxLines = 1, modifier = Modifier.weight(1f))
                            IconButton(onClick = { pending = Delete("d", d.id, d.name) }, modifier = Modifier.size(30.dp)) { Icon(Icons.Filled.Delete, "Borrar", Modifier.size(16.dp)) }
                        }
                    }
                }
            }
        }
        item { SectionHeader("Mis animaciones") }
        if (animations.isEmpty()) item { Hint("Todavía no animaste nada.") }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(animations, key = { it.id }) { a ->
                    Column(Modifier.width(110.dp)) {
                        Box(
                            Modifier.size(110.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable { nav.go("animation/${a.id}") }
                        ) { AnimatedFrames(a.id, Modifier.fillMaxSize()) }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${a.name} · ${a.frames}", style = MaterialTheme.typography.labelMedium, maxLines = 1, modifier = Modifier.weight(1f))
                            IconButton(onClick = { pending = Delete("a", a.id, a.name) }, modifier = Modifier.size(30.dp)) { Icon(Icons.Filled.Delete, "Borrar", Modifier.size(16.dp)) }
                        }
                    }
                }
            }
        }
    }

    val context = LocalContext.current
    if (pickSticker) com.migux97.simon.ui.components.StickerPickerDialog(onDismiss = { pickSticker = false }) { pickSticker = false }
    pending?.let { p ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("¿Borrar «${p.name}»?") },
            text = { Text("Donde lo estés usando (launcher, widgets, efectos) va a dejar de verse.") },
            confirmButton = {
                TextButton(onClick = {
                    when (p.kind) {
                        "w" -> { StudioStore.deleteWidget(p.id); com.migux97.simon.widget.WidgetUpdater.updateAll(context) }
                        "d" -> MediaLibrary.deleteDrawing(p.id)
                        "s" -> StudioStore.deleteSticker(p.id)
                        else -> MediaLibrary.deleteAnimation(p.id)
                    }
                    pending = null
                }) { Text("Borrar") }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun Hint(text: String) = Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))

@Composable
private fun BigAction(label: String, icon: ImageVector, colors: List<Color>, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(colors))
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(30.dp))
        Text(label, color = Color.White, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 6.dp))
    }
}
