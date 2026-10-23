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
import androidx.compose.material3.Button
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
import com.migux97.simon.data.Catalog
import com.migux97.simon.data.CustomIcon
import com.migux97.simon.data.FontStyle
import com.migux97.simon.data.IconForeground
import com.migux97.simon.data.IconShape
import com.migux97.simon.data.IconStyle
import com.migux97.simon.data.ImageStore
import com.migux97.simon.data.Repository
import com.migux97.simon.icons.AppEntry
import com.migux97.simon.icons.IconChanger
import com.migux97.simon.ui.Nav
import com.migux97.simon.ui.components.BackTopBar
import com.migux97.simon.ui.components.ChoiceChips
import com.migux97.simon.ui.components.ColorRow
import com.migux97.simon.ui.components.Panel
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

@Composable
fun IconsScreen(nav: Nav, themeId: String?) {
    val apps = rememberApps()
    var query by rememberSaveable { mutableStateOf("") }
    var styleTheme by rememberSaveable { mutableStateOf(themeId) }
    val style = iconStyleFor(styleTheme)
    val filtered = remember(apps, query) { apps?.filter { query.isBlank() || it.label.contains(query, ignoreCase = true) } ?: emptyList() }

    Column(Modifier.fillMaxSize()) {
        BackTopBar(if (themeId != null) "Íconos del tema" else "Íconos", onBack = { nav.back() })
        Text(
            "Crea accesos directos con ícono nuevo para cualquier launcher. Con Simón Launcher podés cambiar el ícono de cada app directamente, sin accesos directos.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
        Text("Estilo", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 20.dp, bottom = 6.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                StylePick("Simón", DefaultIconStyle, styleTheme == null) { styleTheme = null }
            }
            items(Catalog.themes, key = { it.id }) { t ->
                StylePick(t.name, t.iconStyle, styleTheme == t.id) { styleTheme = t.id }
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
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(filtered, key = { it.packageName }) { app ->
                    ListItem(
                        leadingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AppIcon(app, Modifier.size(40.dp))
                                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                                StyledIcon(app, app.label, style, null, 144, Modifier.size(40.dp))
                            }
                        },
                        headlineContent = { Text(app.label, maxLines = 1) },
                        supportingContent = { Text("Tocá para personalizar", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.clickable { nav.iconEditor(app.packageName, styleTheme) }
                    )
                }
            }
        }
    }
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

@Composable
fun IconEditorScreen(nav: Nav, packageName: String, themeId: String?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val apps = rememberApps()
    val app = apps?.firstOrNull { it.packageName == packageName }
    var style by remember(packageName) { mutableStateOf(iconStyleFor(themeId)) }
    var label by remember(packageName) { mutableStateOf("") }
    var imagePath by remember(packageName) { mutableStateOf<String?>(null) }
    LaunchedEffect(app) { if (app != null && label.isEmpty()) label = app.label }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            imagePath = withContext(Dispatchers.IO) { ImageStore.importImage(context, uri, 512) }
        }
    }

    Column(Modifier.fillMaxSize()) {
        BackTopBar(app?.label ?: "Ícono", onBack = { nav.back() })
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
                Text("Antes", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 6.dp))
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
            Panel {
                OutlinedTextField(label, { label = it.take(24) }, label = { Text("Nombre del acceso") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            Panel { Text("Forma", style = MaterialTheme.typography.titleMedium) }
            ChoiceChips(IconShape.entries, style.shape, { it.label }, { style = style.copy(shape = it) })
            Panel { Text("Contenido", style = MaterialTheme.typography.titleMedium) }
            ChoiceChips(IconForeground.entries, style.foreground, { it.label }, { style = style.copy(foreground = it); imagePath = null })
            if (style.foreground == IconForeground.LETTER) {
                Spacer(Modifier.height(6.dp))
                ChoiceChips(FontStyle.entries, style.font, { it.label }, { style = style.copy(font = it) })
            }
            Panel {
                ColorRow("Fondo 1", style.bg1) { style = style.copy(bg1 = it) }
                ColorRow("Fondo 2", style.bg2) { style = style.copy(bg2 = it) }
                if (style.foreground != IconForeground.ORIGINAL) ColorRow("Color del dibujo", style.fgColor) { style = style.copy(fgColor = it) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (imagePath == null) "Usar una foto" else "Cambiar foto")
                    }
                    if (imagePath != null) FilledTonalButton(onClick = { imagePath = null }) { Text("Quitar") }
                }
            }
            com.migux97.simon.ui.components.StickerAttachEditor(style.stickers) { style = style.copy(stickers = it) }
            Panel {
                Text(
                    "Simón crea un acceso directo nuevo con este ícono que abre ${app.label}. Podés esconder el ícono original en tu launcher. Algunos launchers agregan una marca chiquita en la esquina.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Button(
            onClick = {
                scope.launch {
                    val bmp = withContext(Dispatchers.Default) {
                        IconChanger.render(style, app.icon, label, ImageStore.load(imagePath, 512), 256)
                    }
                    val ok = IconChanger.pinShortcut(context, app.packageName, label.ifBlank { app.label }, bmp)
                    if (ok) {
                        Repository.addIcon(CustomIcon(packageName = app.packageName, label = label.ifBlank { app.label }, style = style, customImagePath = imagePath))
                        Toast.makeText(context, "Confirmá para agregar el ícono a tu pantalla", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Tu launcher no permite agregar accesos directos", Toast.LENGTH_LONG).show()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp)
        ) { Text("Agregar a la pantalla de inicio") }
    }
}
