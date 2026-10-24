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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.migux97.simon.data.CustomIcon
import com.migux97.simon.data.CustomWidget
import com.migux97.simon.data.ImageStore
import com.migux97.simon.data.Repository
import com.migux97.simon.data.SceneType
import com.migux97.simon.data.StudioStore
import com.migux97.simon.data.WidgetDesign
import com.migux97.simon.data.WidgetSize
import com.migux97.simon.data.newId
import com.migux97.simon.icons.IconChanger
import com.migux97.simon.ui.Nav
import com.migux97.simon.ui.components.GradientBadge
import com.migux97.simon.ui.components.SceneThumb
import com.migux97.simon.ui.components.WidgetOnBackdrop
import com.migux97.simon.ui.theme.SimonGradient
import com.migux97.simon.wallpaper.LiveWallpaper
import com.migux97.simon.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface PendingDelete {
    data class Design(val id: String, val name: String) : PendingDelete
    data class Custom(val id: String, val name: String) : PendingDelete
    data class Live(val id: String, val name: String, val video: String?) : PendingDelete
    data class IconItem(val id: String, val name: String) : PendingDelete
}

/** Una opción del menú ⋮ de cada elemento guardado. */
private data class MenuEntry(val label: String, val destructive: Boolean = false, val onClick: () -> Unit)

private fun LazyGridScope.header(text: String, sub: String? = null) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        Column(Modifier.padding(top = 14.dp, bottom = 2.dp)) {
            Text(text, style = MaterialTheme.typography.titleLarge)
            sub?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

/** Estado vacío con un botón que lleva a donde se crea eso. */
private fun LazyGridScope.emptyAction(label: String, onClick: () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onClick) { Text(label) }
        }
    }
}

private fun onHomeLabel(count: Int) = if (count <= 1) "En inicio" else "En inicio ×$count"

@Composable
fun MineScreen(nav: Nav) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val designs by Repository.designs.collectAsStateWithLifecycle()
    val created by StudioStore.widgetsFlow.collectAsStateWithLifecycle()
    val bindings by Repository.bindings.collectAsStateWithLifecycle()
    val savedLive by Repository.savedLive.collectAsStateWithLifecycle()
    val lastLive by Repository.live.collectAsStateWithLifecycle()
    // El último animado aplicado solo está "Activo" si sigue puesto (pudiste cambiar a un fondo estático).
    var resumeTick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumeTick++ }
    val liveActive = remember(resumeTick) { LiveWallpaper.isActive(context) }
    val active = if (liveActive) lastLive else null
    val icons by Repository.icons.collectAsStateWithLifecycle()
    var pending by remember { mutableStateOf<PendingDelete?>(null) }
    var renameDesign by remember { mutableStateOf<WidgetDesign?>(null) }
    var renameCustom by remember { mutableStateOf<CustomWidget?>(null) }
    val apps = rememberApps()
    // Cuántos widgets de la pantalla de inicio muestran cada cosa (solo los que siguen puestos).
    val placedCounts = remember(bindings) { WidgetUpdater.placedBindings(context).values.groupingBy { it }.eachCount() }

    fun repinIcon(ic: CustomIcon) {
        val app = apps?.firstOrNull { it.packageName == ic.packageName }
        if (app == null) {
            Toast.makeText(context, "No encontré esa app en el teléfono", Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch {
            val bmp = withContext(Dispatchers.Default) {
                IconChanger.render(ic.style, app.icon, ic.label, ImageStore.load(ic.customImagePath, 512), 256)
            }
            val ok = IconChanger.pinShortcut(context, ic.packageName, ic.label, bmp)
            Toast.makeText(context, if (ok) "Confirmá en el cartel para agregarlo" else "Tu launcher no permite agregar accesos directos", Toast.LENGTH_SHORT).show()
        }
    }

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
                Text("Todo lo que guardaste. Tocá algo para editarlo o usá ⋮ para más opciones.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // ---- Widgets hechos a partir de plantillas
        header("Widgets de plantilla", if (designs.isEmpty()) "Los diseños que guardes o agregues desde Widgets aparecen acá." else null)
        if (designs.isEmpty()) emptyAction("Ir a Widgets") { nav.tab("widgets") }
        items(designs, key = { "d_" + it.id }, span = { GridItemSpan(if (it.size == WidgetSize.SMALL) 3 else 6) }) { d ->
            val onScreen = placedCounts[d.id] ?: 0
            Column {
                Box {
                    WidgetOnBackdrop(d, Modifier.fillMaxWidth().clickable { nav.editor(d.id) })
                    if (onScreen > 0) GradientBadge(onHomeLabel(onScreen), SimonGradient, Modifier.align(Alignment.TopStart).padding(8.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(d.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, modifier = Modifier.weight(1f).padding(start = 4.dp))
                    MoreMenu(
                        listOf(
                            MenuEntry("Editar") { nav.editor(d.id) },
                            MenuEntry("Duplicar") {
                                saveAndRefresh(context, d.copy(id = newId(), name = copyName(d.name)))
                                Toast.makeText(context, "Copia creada", Toast.LENGTH_SHORT).show()
                            },
                            MenuEntry("Renombrar") { renameDesign = d },
                            MenuEntry("Agregar a inicio") { pinDesign(context, d) },
                            MenuEntry("Borrar", destructive = true) { pending = PendingDelete.Design(d.id, d.name) }
                        )
                    )
                }
            }
        }

        // ---- Widgets del constructor
        header("Widgets creados por vos", if (created.isEmpty()) "Armá tu propio widget con textos, formas, fotos y datos en vivo." else null)
        if (created.isEmpty()) emptyAction("Crear un widget") { nav.go("builder/new") }
        items(created, key = { "c_" + it.id }, span = { GridItemSpan(if (it.aspect < 1.2f) 3 else 6) }) { cw ->
            val onScreen = placedCounts[WidgetUpdater.CUSTOM_PREFIX + cw.id] ?: 0
            Column {
                Box {
                    CustomWidgetTile(cw, Modifier.fillMaxWidth().clickable { nav.go("builder/${cw.id}") })
                    if (onScreen > 0) GradientBadge(onHomeLabel(onScreen), SimonGradient, Modifier.align(Alignment.TopStart).padding(8.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(cw.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, modifier = Modifier.weight(1f).padding(start = 4.dp))
                    MoreMenu(
                        listOf(
                            MenuEntry("Editar") { nav.go("builder/${cw.id}") },
                            MenuEntry("Duplicar") {
                                StudioStore.saveWidget(cw.copy(id = newId(), name = "${cw.name} (copia)".take(30)))
                                Toast.makeText(context, "Copia creada", Toast.LENGTH_SHORT).show()
                            },
                            MenuEntry("Renombrar") { renameCustom = cw },
                            MenuEntry("Agregar a inicio") {
                                val ok = WidgetUpdater.requestPinCustom(context, cw)
                                Toast.makeText(
                                    context,
                                    if (ok) "Confirmá en el cartel para agregarlo a tu inicio"
                                    else "Tu launcher no permite agregarlo desde acá: mantené presionada la pantalla de inicio → Widgets → Simón.",
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            MenuEntry("Borrar", destructive = true) { pending = PendingDelete.Custom(cw.id, cw.name) }
                        )
                    )
                }
            }
        }

        // ---- Fondos animados
        header("Mis fondos animados", if (savedLive.isEmpty()) "Personalizá un fondo animado para guardarlo acá." else null)
        if (savedLive.isEmpty()) emptyAction("Ver fondos") { nav.tab("wallpapers") }
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

        // ---- Íconos
        header("Mis íconos", if (icons.isEmpty()) "Los accesos directos que crees quedan registrados acá." else "Tocá uno para editarlo.")
        if (icons.isEmpty()) emptyAction("Ver íconos") { nav.go("icons") }
        items(icons, key = { "i_" + it.id }, span = { GridItemSpan(2) }) { ic ->
            val app = apps?.firstOrNull { it.packageName == ic.packageName }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Column(
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { nav.iconEditor(ic.packageName) }
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    StyledIcon(app, ic.label, ic.style, ic.customImagePath, 160, Modifier.size(56.dp))
                    Text(ic.label, style = MaterialTheme.typography.labelMedium, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
                }
                MoreMenu(
                    listOf(
                        MenuEntry("Editar") { nav.iconEditor(ic.packageName) },
                        MenuEntry("Volver a agregar a inicio") { repinIcon(ic) },
                        MenuEntry("Quitar de la lista", destructive = true) { pending = PendingDelete.IconItem(ic.id, ic.label) }
                    )
                )
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            OutlinedButton(onClick = { nav.go("performance") }, modifier = Modifier.padding(top = 16.dp)) { Text("Rendimiento y batería") }
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

    renameDesign?.let { d ->
        RenameDialog(d.name, maxLength = 40, onDismiss = { renameDesign = null }) { name ->
            renameDesign = null
            Repository.design(d.id)?.let { current -> saveAndRefresh(context, current.copy(name = name)) }
        }
    }
    renameCustom?.let { cw ->
        RenameDialog(cw.name, maxLength = 30, onDismiss = { renameCustom = null }) { name ->
            renameCustom = null
            StudioStore.widget(cw.id)?.let { current ->
                StudioStore.saveWidget(current.copy(name = name))
                WidgetUpdater.updateCustomWidget(context, current.id)
            }
        }
    }

    pending?.let { p ->
        val isIcon = p is PendingDelete.IconItem
        val name = when (p) {
            is PendingDelete.Design -> p.name
            is PendingDelete.Custom -> p.name
            is PendingDelete.Live -> p.name
            is PendingDelete.IconItem -> p.name
        }
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(if (isIcon) "¿Quitar «$name» de la lista?" else "¿Borrar «$name»?") },
            text = {
                Text(
                    when (p) {
                        is PendingDelete.Design -> "Los widgets en tu pantalla que usan este diseño van a quedar vacíos hasta que elijas otro."
                        is PendingDelete.Custom -> "Los widgets de tu inicio que lo muestran van a quedar vacíos hasta que elijas otro."
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
                            WidgetUpdater.updateAll(context)
                        }
                        is PendingDelete.Custom -> {
                            StudioStore.deleteWidget(p.id)
                            WidgetUpdater.updateAll(context)
                        }
                        is PendingDelete.Live -> {
                            Repository.deleteSavedLive(p.id)
                            if (Repository.live.value?.id != p.id) ImageStore.delete(p.video)
                        }
                        is PendingDelete.IconItem -> Repository.deleteIcon(p.id)
                    }
                    pending = null
                }) { Text(if (isIcon) "Quitar" else "Borrar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancelar") } }
        )
    }
}

/** Botón ⋮ con un menú de opciones. */
@Composable
private fun MoreMenu(entries: List<MenuEntry>) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Más opciones") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            entries.forEach { entry ->
                DropdownMenuItem(
                    text = {
                        if (entry.destructive) Text(entry.label, color = MaterialTheme.colorScheme.error) else Text(entry.label)
                    },
                    onClick = { open = false; entry.onClick() }
                )
            }
        }
    }
}

@Composable
private fun RenameDialog(initial: String, maxLength: Int, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Renombrar") },
        text = {
            OutlinedTextField(text, { text = it.take(maxLength) }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = { TextButton(onClick = { onConfirm(text.trim()) }, enabled = text.isNotBlank()) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
