package com.migux97.simon.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.migux97.simon.data.Catalog
import com.migux97.simon.data.LauncherBackground
import com.migux97.simon.data.LauncherBgType
import com.migux97.simon.data.Lookup
import com.migux97.simon.data.MediaLibrary
import com.migux97.simon.data.ImageStore
import com.migux97.simon.data.PanelTile
import com.migux97.simon.data.QuickShortcut
import com.migux97.simon.data.RotationEntry
import com.migux97.simon.data.RotationKind
import com.migux97.simon.data.StudioStore
import com.migux97.simon.data.TapAction
import com.migux97.simon.effects.SimonAccessibilityService
import com.migux97.simon.effects.SimonNotificationListener
import com.migux97.simon.launcher.AnimatedFrames
import com.migux97.simon.launcher.AppCatalog
import com.migux97.simon.launcher.AppPickerList
import com.migux97.simon.launcher.LaunchApp
import com.migux97.simon.ui.Nav
import com.migux97.simon.ui.components.BackTopBar
import com.migux97.simon.ui.components.ChoiceChips
import com.migux97.simon.ui.components.ColorRow
import com.migux97.simon.ui.components.LabeledSlider
import com.migux97.simon.ui.components.Panel
import com.migux97.simon.ui.components.SceneThumb
import com.migux97.simon.ui.components.StaticThumb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

// ======================================================================
//  Piezas compartidas por las pantallas de "Más funciones"
// ======================================================================

/** Estado de los permisos especiales, releído cada vez que la pantalla vuelve al frente. */
internal data class ToolPerms(
    val access: Boolean = false,
    val listener: Boolean = false,
    val overlay: Boolean = false,
    val writeSettings: Boolean = false,
    val dnd: Boolean = false
)

@Composable
internal fun rememberToolPerms(): ToolPerms {
    val context = LocalContext.current
    var perms by remember { mutableStateOf(ToolPerms()) }
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner) {
        owner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val nm = context.getSystemService(android.app.NotificationManager::class.java)
            perms = ToolPerms(
                access = SimonAccessibilityService.isEnabled(context),
                listener = SimonNotificationListener.isEnabled(context),
                overlay = Settings.canDrawOverlays(context),
                writeSettings = Settings.System.canWrite(context),
                dnd = nm?.isNotificationPolicyAccessGranted == true
            )
        }
    }
    return perms
}

internal fun openSystem(context: Context, intent: Intent) {
    try { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {}
}

internal fun overlayIntent(context: Context) =
    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))

internal fun writeSettingsIntent(context: Context) =
    Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))

@Composable
internal fun ToolScreen(title: String, nav: Nav, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize()) {
        BackTopBar(title, onBack = { nav.back() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 32.dp), content = content)
    }
}

@Composable
internal fun ToolTitle(text: String) =
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 6.dp))

@Composable
internal fun ToolHint(text: String) =
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp))

@Composable
internal fun ToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(value, onChange)
    }
}

/** Interruptor principal de una función, con su explicación. */
@Composable
internal fun MainSwitch(title: String, desc: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Switch(value, onChange)
        }
        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun PermissionLine(title: String, desc: String, ok: Boolean, onFix: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (ok) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline, null, tint = if (ok) Color(0xFF34C759) else MaterialTheme.colorScheme.error)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!ok) TextButton(onClick = onFix) { Text("Activar") }
    }
}

/** Diálogo con el buscador de apps. */
@Composable
internal fun AppPickDialog(onDismiss: () -> Unit, onPick: (LaunchApp) -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { AppCatalog.ensureLoaded(context) }
    val apps by AppCatalog.apps.collectAsState()
    Dialog(onDismissRequest = onDismiss) {
        Box(Modifier.clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surface)) {
            AppPickerList(apps) { onPick(it); onDismiss() }
        }
    }
}

internal fun appName(key: String?): String =
    key?.let { k -> AppCatalog.find(k)?.let { AppCatalog.label(it) } ?: k.substringBefore('/') } ?: "Ninguna"

internal fun appNameForPackage(pkg: String): String =
    AppCatalog.apps.value.firstOrNull { it.packageName == pkg }?.label ?: pkg

// ------------------------------------------------------------ fondos

/** Editor de fondo (color, degradado, foto, video, escena, animación o dibujo). */
@Composable
internal fun BackgroundEditor(bg: LauncherBackground, onPickPhoto: () -> Unit, onPickVideo: () -> Unit, onChange: (LauncherBackground) -> Unit) {
    val drawings by StudioStore.drawingsFlow.collectAsStateWithLifecycle()
    val animations by StudioStore.animationsFlow.collectAsStateWithLifecycle()
    ChoiceChips(LauncherBgType.entries, bg.type, { it.label }, { t ->
        when (t) {
            LauncherBgType.IMAGE -> onPickPhoto()
            LauncherBgType.VIDEO -> onPickVideo()
            else -> onChange(bg.copy(type = t))
        }
    })
    when (bg.type) {
        LauncherBgType.COLOR, LauncherBgType.GRADIENT -> Panel {
            ColorRow("Color 1", bg.color1) { onChange(bg.copy(color1 = it)) }
            if (bg.type == LauncherBgType.GRADIENT) ColorRow("Color 2", bg.color2) { onChange(bg.copy(color2 = it)) }
        }
        LauncherBgType.SCENE -> LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Catalog.liveWallpapers, key = { it.id }) { lv ->
                Box(
                    Modifier.width(54.dp).height(90.dp).clip(RoundedCornerShape(12.dp))
                        .border(if (bg.liveId == lv.id) 3.dp else 0.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                        .clickable { onChange(bg.copy(liveId = lv.id)) }
                ) { SceneThumb(lv, Modifier.fillMaxSize()) }
            }
        }
        LauncherBgType.ANIMATION -> if (animations.isEmpty()) ToolHint("Todavía no creaste animaciones (Crear → Animación).")
        else LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(animations, key = { it.id }) { a ->
                Box(
                    Modifier.size(70.dp).clip(RoundedCornerShape(12.dp))
                        .border(if (bg.mediaId == a.id) 3.dp else 0.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                        .clickable { onChange(bg.copy(mediaId = a.id)) }
                ) { AnimatedFrames(a.id, Modifier.fillMaxSize()) }
            }
        }
        LauncherBgType.DRAWING -> if (drawings.isEmpty()) ToolHint("Todavía no hiciste dibujos (Crear → Dibujo).")
        else LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(drawings, key = { it.id }) { d ->
                OutlinedButton(onClick = { onChange(bg.copy(mediaId = d.id)) }) { Text(if (bg.mediaId == d.id) "✓ ${d.name}" else d.name) }
            }
        }
        else -> Unit
    }
    Panel {
        LabeledSlider("Oscurecer", bg.dim, 0f..0.8f, "${(bg.dim * 100).roundToInt()}%") { onChange(bg.copy(dim = it)) }
    }
}

/** Miniatura de un fondo de la rotación. */
@Composable
internal fun RotationThumb(e: RotationEntry, modifier: Modifier) {
    when (e.kind) {
        RotationKind.STATIC -> Lookup.static(e.ref)?.let { StaticThumb(it, modifier) } ?: Box(modifier.background(Color.DarkGray))
        else -> {
            val bmp by produceState<Bitmap?>(null, e) {
                value = withContext(Dispatchers.IO) {
                    if (e.kind == RotationKind.PHOTO) ImageStore.load(e.ref, 360) else MediaLibrary.loadDrawing(e.ref, 360)
                }
            }
            val b = bmp
            if (b != null) Image(b.asImageBitmap(), null, modifier, contentScale = ContentScale.Crop)
            else Box(modifier.background(Color.DarkGray))
        }
    }
}

// --------------------------------------------- apps, atajos y herramientas

private val shortcutActions = listOf(TapAction.OPEN_APP, TapAction.OPEN_URL, TapAction.DIAL, TapAction.FLASHLIGHT, TapAction.OPEN_PANEL, TapAction.OPEN_SIMON, TapAction.LOCK)

/** Edita las apps, los atajos y las herramientas de la barra lateral o la burbuja. */
@Composable
internal fun QuickItemsEditor(
    apps: List<String>,
    shortcuts: List<QuickShortcut>,
    tools: List<PanelTile>,
    onApps: (List<String>) -> Unit,
    onShortcuts: (List<QuickShortcut>) -> Unit,
    onTools: (List<PanelTile>) -> Unit
) {
    var pickApp by remember { mutableStateOf(false) }
    var newShortcut by remember { mutableStateOf(false) }

    ToolTitle("Apps")
    Panel {
        apps.forEach { key ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(appName(key), Modifier.weight(1f))
                IconButton(onClick = { onApps(apps - key) }) { Icon(Icons.Filled.Close, "Quitar") }
            }
        }
        if (apps.isEmpty()) Text("Sin apps todavía.", style = MaterialTheme.typography.bodySmall)
        FilledTonalButton(onClick = { pickApp = true }) { Text("Agregar app") }
    }
    ToolTitle("Atajos")
    Panel {
        shortcuts.forEach { sc ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(sc.label)
                    Text(sc.action.label + if (sc.data.isNotBlank() && sc.action != TapAction.OPEN_APP) " · ${sc.data}" else "", style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = { onShortcuts(shortcuts.filterNot { it.id == sc.id }) }) { Icon(Icons.Filled.Close, "Quitar") }
            }
        }
        if (shortcuts.isEmpty()) Text("Links, contactos para llamar, linterna, bloquear…", style = MaterialTheme.typography.bodySmall)
        FilledTonalButton(onClick = { newShortcut = true }) { Text("Agregar atajo") }
    }
    ToolTitle("Herramientas rápidas")
    Panel {
        PanelTile.entries.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { t ->
                    val on = t in tools
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onTools(if (on) tools - t else tools + t) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(t.label, color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(6.dp))
        }
    }

    if (pickApp) AppPickDialog(onDismiss = { pickApp = false }) { app -> if (app.key !in apps) onApps(apps + app.key) }
    if (newShortcut) ShortcutDialog(onDismiss = { newShortcut = false }) { onShortcuts(shortcuts + it) }
}

@Composable
private fun ShortcutDialog(onDismiss: () -> Unit, onSave: (QuickShortcut) -> Unit) {
    var label by remember { mutableStateOf("") }
    var action by remember { mutableStateOf(TapAction.OPEN_URL) }
    var data by remember { mutableStateOf("") }
    var pickApp by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo atajo") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(label, { label = it }, label = { Text("Nombre") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                ChoiceChips(shortcutActions, action, { it.label }, { action = it; data = "" })
                when (action) {
                    TapAction.OPEN_URL -> OutlinedTextField(data, { data = it }, label = { Text("Link (https://…)") }, singleLine = true)
                    TapAction.DIAL -> OutlinedTextField(data, { data = it }, label = { Text("Número") }, singleLine = true)
                    TapAction.OPEN_APP -> OutlinedButton(onClick = { pickApp = true }) { Text(if (data.isBlank()) "Elegir app" else appName(data)) }
                    else -> Unit
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = label.isNotBlank() && (action !in listOf(TapAction.OPEN_URL, TapAction.DIAL, TapAction.OPEN_APP) || data.isNotBlank()),
                onClick = { onSave(QuickShortcut(label = label.trim(), action = action, data = data.trim())); onDismiss() }
            ) { Text("Agregar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
    if (pickApp) AppPickDialog(onDismiss = { pickApp = false }) { app ->
        data = app.key
        if (label.isBlank()) label = app.label
    }
}
