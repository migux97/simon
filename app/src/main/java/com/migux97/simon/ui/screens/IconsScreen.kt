package com.migux97.simon.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.migux97.simon.data.Catalog
import com.migux97.simon.data.CustomIcon
import com.migux97.simon.data.FontStyle
import com.migux97.simon.data.IconForeground
import com.migux97.simon.data.IconShape
import com.migux97.simon.data.IconStyle
import com.migux97.simon.data.ImageStore
import com.migux97.simon.data.Repository
import com.migux97.simon.data.newId
import com.migux97.simon.icons.AppEntry
import com.migux97.simon.icons.IconChanger
import com.migux97.simon.ui.Nav
import com.migux97.simon.ui.components.BackTopBar
import com.migux97.simon.ui.components.ChoiceChips
import com.migux97.simon.ui.components.ColorRow
import com.migux97.simon.ui.components.GradientBadge
import com.migux97.simon.ui.components.HelpTip
import com.migux97.simon.ui.components.Panel
import com.migux97.simon.ui.components.StartOverButton
import com.migux97.simon.ui.components.UnsavedChangesDialog
import com.migux97.simon.ui.theme.SimonGradient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Caché de la lista de apps para no recalcularla en cada pantalla. */
object AppsCache {
    var apps: List<AppEntry>? = null
}

@Composable
fun rememberApps(): List<AppEntry>? {
    val context = LocalContext.current
    val apps by produceState(AppsCache.apps) {
        if (value == null) {
            val list = IconChanger.installedApps(context)
            AppsCache.apps = list
            value = list
        }
    }
    return apps
}

val DefaultIconStyle = IconStyle(IconShape.SQUIRCLE, 0xFF8B6CFF.toInt(), 0xFFFF7EB6.toInt(), IconForeground.TINTED, 0xFFFFFFFF.toInt())

fun iconStyleFor(themeId: String?): IconStyle =
    themeId?.let { id -> Catalog.themes.firstOrNull { it.id == id }?.iconStyle } ?: DefaultIconStyle

/** Ícono renderizado con un estilo, para listas y vistas previas. */
@Composable
fun StyledIcon(app: AppEntry?, label: String, style: IconStyle, customImagePath: String?, size: Int, modifier: Modifier = Modifier) {
    val bmp by produceState<Bitmap?>(null, app?.packageName, style, customImagePath, label) {
        value = withContext(Dispatchers.Default) {
            IconChanger.render(style, app?.icon, label, ImageStore.load(customImagePath, 400), size)
        }
    }
    Box(modifier) { bmp?.let { Image(it.asImageBitmap(), contentDescription = label, modifier = Modifier.fillMaxSize()) } }
}

@Composable
fun AppIcon(app: AppEntry, modifier: Modifier = Modifier) {
    val bmp = remember(app.packageName) { runCatching { app.icon.toBitmap(144, 144) }.getOrNull() }
    Box(modifier) { bmp?.let { Image(it.asImageBitmap(), contentDescription = app.label, modifier = Modifier.fillMaxSize()) } }
}

/** Texto que explica cómo agrega Android los íconos (se repite en la lista y en el editor). */
const val IconShortcutHelp =
    "Android agrega cada ícono como un acceso directo nuevo en tu pantalla de inicio: el ícono original de la app sigue estando. " +
        "Si querés, escondé o quitá el original desde tu launcher (mantenelo presionado). Algunos launchers le ponen una marca chiquita en la esquina."

/** Guarda un ícono de una app con un estilo y lo publica (actualiza si ya está en inicio). */
suspend fun publishStyledIcon(
    context: android.content.Context,
    app: AppEntry,
    style: IconStyle,
    label: String,
    imagePath: String?,
    previous: CustomIcon?
): Pair<IconChanger.PublishResult, CustomIcon?> {
    val name = label.ifBlank { app.label }
    val bmp = withContext(Dispatchers.Default) {
        IconChanger.render(style, app.icon, name, ImageStore.load(imagePath, 512), 256)
    }
    val result = IconChanger.publishIcon(context, app.packageName, name, bmp)
    val saved = when (result) {
        IconChanger.PublishResult.UPDATED, IconChanger.PublishResult.REQUESTED -> {
            val icon = CustomIcon(id = previous?.id ?: newId(), packageName = app.packageName, label = name, style = style, customImagePath = imagePath)
            Repository.saveIcon(icon)
            icon
        }
        IconChanger.PublishResult.FAILED -> null
    }
    return result to saved
}

@Composable
fun IconsScreen(nav: Nav, themeId: String?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val apps = rememberApps()
    val icons by Repository.icons.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var styleTheme by rememberSaveable { mutableStateOf(themeId) }
    val style = iconStyleFor(styleTheme)
    val customByPkg = remember(icons) { icons.distinctBy { it.packageName }.associateBy { it.packageName } }
    val filtered = remember(apps, query) { apps?.filter { query.isBlank() || it.label.contains(query, ignoreCase = true) } ?: emptyList() }
    val mine = remember(filtered, customByPkg) { filtered.filter { customByPkg.containsKey(it.packageName) } }
    val others = remember(filtered, customByPkg) { filtered.filterNot { customByPkg.containsKey(it.packageName) } }

    // Modo tema: elegir varias apps y aplicarles el estilo de una vez.
    val themeMode = themeId != null
    val theme = remember(themeId) { themeId?.let { id -> Catalog.themes.firstOrNull { it.id == id } } }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var queue by remember { mutableStateOf<List<String>>(emptyList()) }
    var waitingConfirm by remember { mutableStateOf(false) }
    var batchTotal by remember { mutableStateOf(0) }
    var batchDone by remember { mutableStateOf(0) }
    var batchUpdated by remember { mutableStateOf(0) }
    var batchBusy by remember { mutableStateOf(false) }

    fun finishBatch() {
        waitingConfirm = false
        batchBusy = false
        if (batchTotal > 0) {
            Toast.makeText(context, "Listo: estilo aplicado a $batchDone de $batchTotal apps", Toast.LENGTH_LONG).show()
        }
        batchTotal = 0
        batchDone = 0
        batchUpdated = 0
        selected = emptySet()
    }

    /** Pide agregar el siguiente ícono nuevo (Android los confirma de a uno). */
    fun pinNext() {
        val pkg = queue.firstOrNull()
        if (pkg == null) {
            finishBatch()
            return
        }
        queue = queue.drop(1)
        val app = apps?.firstOrNull { it.packageName == pkg }
        val st = theme?.iconStyle
        if (app == null || st == null) {
            pinNext()
            return
        }
        scope.launch {
            val previous = Repository.iconFor(pkg)
            val (result, _) = publishStyledIcon(context, app, st, previous?.label ?: app.label, null, previous)
            when (result) {
                IconChanger.PublishResult.UPDATED, IconChanger.PublishResult.REQUESTED -> batchDone++
                IconChanger.PublishResult.FAILED -> Unit
            }
            if (result == IconChanger.PublishResult.REQUESTED) {
                // Seguimos cuando el usuario vuelva del diálogo del launcher.
                waitingConfirm = true
            } else {
                pinNext()
            }
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (waitingConfirm) {
            waitingConfirm = false
            pinNext()
        }
    }

    fun applyToSelected() {
        val st = theme?.iconStyle ?: return
        val list = apps ?: return
        val chosen = list.filter { it.packageName in selected }
        if (chosen.isEmpty()) return
        batchBusy = true
        batchTotal = chosen.size
        batchDone = 0
        batchUpdated = 0
        scope.launch {
            // Los que ya están en tu pantalla se actualizan sin preguntar.
            val toPin = mutableListOf<String>()
            chosen.forEach { app ->
                if (IconChanger.isOnHomeScreen(context, app.packageName)) {
                    val previous = Repository.iconFor(app.packageName)
                    val (result, _) = publishStyledIcon(context, app, st, previous?.label ?: app.label, null, previous)
                    if (result == IconChanger.PublishResult.UPDATED) {
                        batchDone++
                        batchUpdated++
                    } else if (result == IconChanger.PublishResult.REQUESTED) {
                        batchDone++
                    }
                } else {
                    toPin += app.packageName
                }
            }
            queue = toPin
            if (toPin.isNotEmpty()) {
                Toast.makeText(context, "Android te va a pedir confirmar cada ícono nuevo, de a uno", Toast.LENGTH_LONG).show()
            }
            pinNext()
        }
    }

    Column(Modifier.fillMaxSize()) {
        BackTopBar(if (theme != null) "Íconos · ${theme.name}" else "Íconos", onBack = { nav.back() })
        Text(
            if (themeMode) "Marcá las apps a las que querés darles el estilo del tema y tocá «Aplicar». Para cambiar una sola a mano, tocá «Editar»."
            else "Creá accesos directos con ícono nuevo para cualquier launcher. Con Simón Launcher podés cambiar el ícono de cada app directamente, sin accesos directos.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
        HelpTip(IconShortcutHelp)
        if (!themeMode) {
            Text("Estilo para íconos nuevos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 20.dp, top = 6.dp, bottom = 6.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    StylePick("Simón", DefaultIconStyle, styleTheme == null) { styleTheme = null }
                }
                items(Catalog.themes, key = { it.id }) { t ->
                    StylePick(t.name, t.iconStyle, styleTheme == t.id) { styleTheme = t.id }
                }
            }
        }
        OutlinedTextField(
            query, { query = it },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, contentDescription = "Borrar") } },
            placeholder = { Text("Buscar app") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )
        if (apps == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (filtered.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    if (query.isBlank()) "No encontré apps para personalizar." else "No hay apps con ese nombre",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(bottom = 16.dp)) {
                if (mine.isNotEmpty()) {
                    item(key = "h_mine") { ListHeader("Tus íconos") }
                    items(mine, key = { "m_" + it.packageName }) { app ->
                        val custom = customByPkg[app.packageName]
                        AppIconRow(
                            app = app,
                            custom = custom,
                            previewStyle = if (themeMode) theme?.iconStyle ?: style else style,
                            selectable = themeMode,
                            checked = app.packageName in selected,
                            onToggle = { selected = if (app.packageName in selected) selected - app.packageName else selected + app.packageName },
                            onEdit = { nav.iconEditor(app.packageName, if (themeMode) themeId else styleTheme) }
                        )
                    }
                }
                if (others.isNotEmpty()) {
                    item(key = "h_all") { ListHeader(if (mine.isNotEmpty()) "Todas las apps" else "Apps") }
                    items(others, key = { "a_" + it.packageName }) { app ->
                        AppIconRow(
                            app = app,
                            custom = null,
                            previewStyle = if (themeMode) theme?.iconStyle ?: style else style,
                            selectable = themeMode,
                            checked = app.packageName in selected,
                            onToggle = { selected = if (app.packageName in selected) selected - app.packageName else selected + app.packageName },
                            onEdit = { nav.iconEditor(app.packageName, if (themeMode) themeId else styleTheme) }
                        )
                    }
                }
            }
        }
        if (themeMode && theme != null) {
            if (batchBusy) {
                Panel {
                    Text(
                        if (waitingConfirm) "Confirmá el ícono en tu pantalla… (van $batchDone de $batchTotal)" else "Aplicando… (van $batchDone de $batchTotal)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (waitingConfirm) TextButton(onClick = { waitingConfirm = false; pinNext() }) { Text("Siguiente") }
                        TextButton(onClick = { queue = emptyList(); finishBatch() }) { Text("Frenar") }
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val all = apps.orEmpty()
                TextButton(
                    onClick = { selected = if (selected.size == filtered.size && filtered.isNotEmpty()) emptySet() else filtered.map { it.packageName }.toSet() },
                    enabled = !batchBusy && all.isNotEmpty()
                ) { Text(if (selected.size == filtered.size && filtered.isNotEmpty()) "Ninguna" else "Todas") }
                Button(
                    onClick = { applyToSelected() },
                    enabled = selected.isNotEmpty() && !batchBusy,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        when (selected.size) {
                            0 -> "Elegí apps"
                            1 -> "Aplicar a 1 app"
                            else -> "Aplicar a ${selected.size} apps"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ListHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 20.dp, top = 10.dp, bottom = 4.dp)
    )
}

@Composable
private fun AppIconRow(
    app: AppEntry,
    custom: CustomIcon?,
    previewStyle: IconStyle,
    selectable: Boolean,
    checked: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit
) {
    ListItem(
        leadingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(app, Modifier.size(40.dp))
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                if (custom != null && !selectable) {
                    StyledIcon(app, custom.label, custom.style, custom.customImagePath, 144, Modifier.size(40.dp))
                } else if (custom != null && !checked) {
                    StyledIcon(app, custom.label, custom.style, custom.customImagePath, 144, Modifier.size(40.dp))
                } else {
                    StyledIcon(app, custom?.label ?: app.label, previewStyle, null, 144, Modifier.size(40.dp))
                }
            }
        },
        headlineContent = { Text(custom?.label ?: app.label, maxLines = 1) },
        supportingContent = {
            if (custom != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GradientBadge("Ya personalizado", SimonGradient)
                    if (!selectable) {
                        Spacer(Modifier.width(6.dp))
                        Text("Tocá para editarlo", style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                Text(if (selectable) "Marcala para darle el estilo" else "Tocá para personalizar", style = MaterialTheme.typography.bodySmall)
            }
        },
        trailingContent = if (selectable) {
            {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onEdit) { Text("Editar") }
                    Checkbox(checked = checked, onCheckedChange = { onToggle() })
                }
            }
        } else null,
        modifier = Modifier.clickable { if (selectable) onToggle() else onEdit() }
    )
}

@Composable
private fun StylePick(name: String, style: IconStyle, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(if (selected) 2.dp else 0.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StyledIcon(null, name, style.copy(foreground = IconForeground.LETTER), null, 144, Modifier.size(48.dp))
        Text(name, style = MaterialTheme.typography.labelSmall, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
    }
}

/** Lo que se edita de un ícono, para saber si hay cambios sin guardar. */
private data class IconDraft(val style: IconStyle, val label: String, val imagePath: String?)

@Composable
fun IconEditorScreen(nav: Nav, packageName: String, themeId: String?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val apps = rememberApps()
    val app = apps?.firstOrNull { it.packageName == packageName }
    // Si ya personalizaste esta app, se abre con tu ícono para seguir desde ahí.
    val existing = remember(packageName) { Repository.iconFor(packageName) }
    val themeStyle = remember(themeId) { themeId?.let { id -> Catalog.themes.firstOrNull { it.id == id } } }
    var savedIcon by remember(packageName) { mutableStateOf(existing) }
    var style by remember(packageName) { mutableStateOf(existing?.style ?: iconStyleFor(themeId)) }
    var label by remember(packageName) { mutableStateOf(existing?.label ?: "") }
    var imagePath by remember(packageName) { mutableStateOf(existing?.customImagePath) }
    var baseline by remember(packageName) {
        mutableStateOf(IconDraft(existing?.style ?: iconStyleFor(themeId), existing?.label ?: "", existing?.customImagePath))
    }
    var busy by remember { mutableStateOf(false) }
    var confirmExit by remember { mutableStateOf(false) }
    LaunchedEffect(app) { if (app != null && label.isEmpty()) label = app.label }

    val appLabel = app?.label ?: ""
    val dirty = IconDraft(style, label.ifBlank { appLabel }, imagePath) != baseline.copy(label = baseline.label.ifBlank { appLabel })

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            imagePath = withContext(Dispatchers.IO) { ImageStore.importImage(context, uri, 512) }
        }
    }

    fun removePhotoWithNotice() {
        if (imagePath != null) {
            imagePath = null
            Toast.makeText(context, "Se quitó la foto", Toast.LENGTH_SHORT).show()
        }
    }

    fun save(extraCopy: Boolean, thenBack: Boolean) {
        val a = app ?: return
        if (busy) return
        busy = true
        scope.launch {
            val name = label.ifBlank { a.label }
            var ok: Boolean
            if (extraCopy) {
                val bmp = withContext(Dispatchers.Default) {
                    IconChanger.render(style, a.icon, name, ImageStore.load(imagePath, 512), 256)
                }
                ok = IconChanger.pinShortcut(context, a.packageName, name, bmp)
                if (ok) {
                    val icon = CustomIcon(id = savedIcon?.id ?: newId(), packageName = a.packageName, label = name, style = style, customImagePath = imagePath)
                    Repository.saveIcon(icon)
                    savedIcon = icon
                }
                Toast.makeText(
                    context,
                    if (ok) "Confirmá para agregar la copia a tu pantalla" else "Tu launcher no permite agregar accesos directos",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                val (result, icon) = publishStyledIcon(context, a, style, name, imagePath, savedIcon)
                ok = icon != null
                if (icon != null) savedIcon = icon
                Toast.makeText(
                    context,
                    when (result) {
                        IconChanger.PublishResult.UPDATED -> "¡Listo! Se actualizó el ícono en tu pantalla"
                        IconChanger.PublishResult.REQUESTED -> "Confirmá para agregar el ícono a tu pantalla"
                        IconChanger.PublishResult.FAILED -> "Tu launcher no permite agregar accesos directos"
                    },
                    Toast.LENGTH_LONG
                ).show()
            }
            if (ok) {
                label = name
                baseline = IconDraft(style, name, imagePath)
            }
            busy = false
            if (ok && thenBack) nav.back()
        }
    }

    BackHandler(enabled = dirty && app != null) { confirmExit = true }

    Column(Modifier.fillMaxSize()) {
        BackTopBar(
            app?.label ?: "Ícono",
            onBack = { if (dirty && app != null) confirmExit = true else nav.back() },
            actions = {
                if (app != null) StartOverButton(
                    message = "Volvés al estilo base, sin foto y con el nombre original de la app. El ícono de tu pantalla no cambia hasta que guardes."
                ) {
                    style = iconStyleFor(themeId)
                    imagePath = null
                    label = app.label
                }
            }
        )
        if (app == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (apps == null) CircularProgressIndicator() else Text("No encontré esa app.")
            }
            return@Column
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(com.migux97.simon.ui.components.PreviewBackdrop)
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AppIcon(app, Modifier.size(64.dp))
                Text("Original", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 6.dp))
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, modifier = Modifier.size(32.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                StyledIcon(app, label, style, imagePath, 256, Modifier.size(84.dp))
                Text(label.ifBlank { app.label }, style = MaterialTheme.typography.labelMedium, maxLines = 1, modifier = Modifier.padding(top = 6.dp))
            }
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            if (savedIcon != null) {
                HelpTip("Estás editando el ícono que ya creaste. Al guardar se actualiza el que está en tu pantalla.")
            }
            val ts = themeStyle
            if (existing != null && ts != null && style != ts.iconStyle) {
                Panel {
                    Text("Elegiste el estilo «${ts.name}»", style = MaterialTheme.typography.titleSmall)
                    Text("Se abrió tu ícono actual para que no lo pierdas. Si querés, pasalo al estilo del tema.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FilledTonalButton(onClick = { style = ts.iconStyle; removePhotoWithNotice() }) { Text("Usar el estilo «${ts.name}»") }
                }
            }
            Panel {
                OutlinedTextField(label, { label = it.take(24) }, label = { Text("Nombre debajo del ícono") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            Panel { Text("Forma", style = MaterialTheme.typography.titleMedium) }
            ChoiceChips(IconShape.entries, style.shape, { it.label }, { style = style.copy(shape = it) })
            Panel {
                Text("Qué se ve en el ícono", style = MaterialTheme.typography.titleMedium)
                if (imagePath != null) Text("Ahora se ve tu foto. Si elegís otra opción, se quita la foto.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ChoiceChips(IconForeground.entries, if (imagePath != null) null else style.foreground, { it.label }, {
                style = style.copy(foreground = it)
                removePhotoWithNotice()
            })
            if (style.foreground == IconForeground.LETTER && imagePath == null) {
                Spacer(Modifier.height(6.dp))
                ChoiceChips(FontStyle.entries, style.font, { it.label }, { style = style.copy(font = it) })
            }
            Panel {
                ColorRow("Color de fondo", style.bg1) { style = style.copy(bg1 = it) }
                ColorRow("Segundo color del fondo", style.bg2) { style = style.copy(bg2 = it) }
                if (style.foreground != IconForeground.ORIGINAL) ColorRow("Color del dibujo", style.fgColor) { style = style.copy(fgColor = it) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (imagePath == null) "Usar una foto" else "Cambiar foto")
                    }
                    if (imagePath != null) FilledTonalButton(onClick = { imagePath = null }) { Text("Quitar foto") }
                }
            }
            com.migux97.simon.ui.components.StickerAttachEditor(style.stickers) { style = style.copy(stickers = it) }
            HelpTip(IconShortcutHelp)
            Spacer(Modifier.height(8.dp))
        }
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(onClick = { save(extraCopy = false, thenBack = false) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text(
                    when {
                        busy -> "Guardando…"
                        savedIcon != null -> "Guardar cambios"
                        else -> "Crear ícono"
                    }
                )
            }
            if (savedIcon != null) {
                OutlinedButton(onClick = { save(extraCopy = true, thenBack = false) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text("Agregar otra copia a inicio")
                }
            }
        }
    }

    if (confirmExit) {
        UnsavedChangesDialog(
            onSave = { confirmExit = false; save(extraCopy = false, thenBack = true) },
            onDiscard = { confirmExit = false; nav.back() },
            onDismiss = { confirmExit = false }
        )
    }
}
