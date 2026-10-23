package com.migux97.simon.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.migux97.simon.data.ImageStore
import com.migux97.simon.data.Repository
import com.migux97.simon.data.SceneType
import com.migux97.simon.data.WidgetSize
import com.migux97.simon.icons.IconChanger
import com.migux97.simon.ui.Nav
import com.migux97.simon.ui.components.GradientBadge
import com.migux97.simon.ui.components.SceneThumb
import com.migux97.simon.ui.components.WidgetOnBackdrop
import com.migux97.simon.ui.theme.SimonGradient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface PendingDelete {
    data class Design(val id: String, val name: String) : PendingDelete
    data class Live(val id: String, val name: String, val video: String?) : PendingDelete
    data class IconItem(val id: String, val name: String) : PendingDelete
}

private fun LazyGridScope.header(text: String, sub: String? = null) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        Column(Modifier.padding(top = 14.dp, bottom = 2.dp)) {
            Text(text, style = MaterialTheme.typography.titleLarge)
            sub?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
fun MineScreen(nav: Nav) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val designs by Repository.designs.collectAsStateWithLifecycle()
    val bindings by Repository.bindings.collectAsStateWithLifecycle()
    val savedLive by Repository.savedLive.collectAsStateWithLifecycle()
    val active by Repository.live.collectAsStateWithLifecycle()
    val icons by Repository.icons.collectAsStateWithLifecycle()
    var pending by remember { mutableStateOf<PendingDelete?>(null) }
    val apps = rememberApps()

    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.statusBarsPadding()) {
                Text("Mío", style = MaterialTheme.typography.headlineLarge)
                Text("Tus diseños guardados", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        header("Mis widgets", if (designs.isEmpty()) "Los widgets que guardes o agregues aparecen acá." else "Tocá uno para editarlo.")
        items(designs, key = { "d_" + it.id }, span = { GridItemSpan(if (it.size == WidgetSize.SMALL) 3 else 6) }) { d ->
            val onScreen = bindings.values.count { it == d.id }
            Column {
                Box {
                    WidgetOnBackdrop(d, Modifier.fillMaxWidth().clickable { nav.editor(d.id) })
                    if (onScreen > 0) GradientBadge(if (onScreen == 1) "En inicio" else "En inicio ×$onScreen", SimonGradient, Modifier.align(Alignment.TopStart).padding(8.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(d.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, modifier = Modifier.weight(1f).padding(start = 4.dp))
                    TextButton(onClick = { pinDesign(context, d) }) { Text("Agregar") }
                    IconButton(onClick = { pending = PendingDelete.Design(d.id, d.name) }) { Icon(Icons.Filled.Delete, contentDescription = "Borrar") }
                }
            }
        }

        header("Mis fondos animados", if (savedLive.isEmpty()) "Personalizá un fondo animado para guardarlo acá." else null)
        items(savedLive, key = { "l_" + it.id }, span = { GridItemSpan(2) }) { lv ->
            Column {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(9f / 18f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                        .clickable { nav.live(lv.id) }
                ) {
                    if (lv.scene == SceneType.VIDEO) Icon(Icons.Filled.Movie, contentDescription = null, tint = Color.White, modifier = Modifier.align(Alignment.Center).size(36.dp))
                    else SceneThumb(lv, Modifier.fillMaxSize())
                    if (active?.id == lv.id) GradientBadge("Activo", SimonGradient, Modifier.align(Alignment.TopStart).padding(6.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(lv.name, style = MaterialTheme.typography.labelMedium, maxLines = 1, modifier = Modifier.weight(1f))
                    IconButton(onClick = { pending = PendingDelete.Live(lv.id, lv.name, lv.videoPath) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Borrar", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        header("Mis íconos", if (icons.isEmpty()) "Los accesos directos que crees quedan registrados acá." else "Tocá uno para volver a agregarlo.")
        items(icons, key = { "i_" + it.id }, span = { GridItemSpan(2) }) { ic ->
            val app = apps?.firstOrNull { it.packageName == ic.packageName }
            Column(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        if (app == null) return@clickable
                        scope.launch {
                            val bmp = withContext(Dispatchers.Default) {
                                IconChanger.render(ic.style, app.icon, ic.label, ImageStore.load(ic.customImagePath, 512), 256)
                            }
                            val ok = IconChanger.pinShortcut(context, ic.packageName, ic.label, bmp)
                            Toast.makeText(context, if (ok) "Confirmá para agregarlo" else "Tu launcher no permite agregar accesos directos", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StyledIcon(app, ic.label, ic.style, ic.customImagePath, 160, Modifier.size(56.dp))
                Text(ic.label, style = MaterialTheme.typography.labelMedium, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
                TextButton(onClick = { pending = PendingDelete.IconItem(ic.id, ic.label) }) { Text("Quitar") }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            androidx.compose.material3.OutlinedButton(onClick = { nav.go("performance") }, modifier = Modifier.padding(top = 16.dp)) { Text("Rendimiento y batería") }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                "Simón 1.0 · Clima por Open-Meteo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
            )
        }
    }

    pending?.let { p ->
        val name = when (p) {
            is PendingDelete.Design -> p.name
            is PendingDelete.Live -> p.name
            is PendingDelete.IconItem -> p.name
        }
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("¿Borrar «$name»?") },
            text = {
                Text(
                    when (p) {
                        is PendingDelete.Design -> "Los widgets en tu pantalla que usan este diseño van a quedar vacíos hasta que elijas otro."
                        is PendingDelete.Live -> "Se quita de tu colección."
                        is PendingDelete.IconItem -> "Solo se quita de esta lista. El acceso directo en tu pantalla lo borrás desde el launcher."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    when (p) {
                        is PendingDelete.Design -> {
                            Repository.deleteDesign(p.id)
                            com.migux97.simon.widget.WidgetUpdater.updateAll(context)
                        }
                        is PendingDelete.Live -> {
                            Repository.deleteSavedLive(p.id)
                            if (Repository.live.value?.id != p.id) ImageStore.delete(p.video)
                        }
                        is PendingDelete.IconItem -> Repository.deleteIcon(p.id)
                    }
                    pending = null
                }) { Text("Borrar") }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancelar") } }
        )
    }
}
