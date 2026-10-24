package com.migux97.simon.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.migux97.simon.data.Catalog
import com.migux97.simon.data.ImageStore
import com.migux97.simon.data.LauncherBackground
import com.migux97.simon.data.LauncherBgType
import com.migux97.simon.data.Lookup
import com.migux97.simon.data.MediaLibrary
import com.migux97.simon.data.PanelTile
import com.migux97.simon.data.QuickShortcut
import com.migux97.simon.data.RotationEntry
import com.migux97.simon.data.RotationKind
import com.migux97.simon.data.StudioStore
import com.migux97.simon.data.TapAction
import com.migux97.simon.effects.SimonAccessibilityService
import com.migux97.simon.effects.SimonNotificationListener
import com.migux97.simon.effects.tileIcon
import com.migux97.simon.launcher.AnimatedFrames
import com.migux97.simon.launcher.AppCatalog
import com.migux97.simon.launcher.AppIconBitmap
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

private fun readToolPerms(context: Context): ToolPerms = try {
    val nm = context.getSystemService(android.app.NotificationManager::class.java)
    ToolPerms(
        access = SimonAccessibilityService.isEnabled(context),
        listener = SimonNotificationListener.isEnabled(context),
        overlay = Settings.canDrawOverlays(context),
        writeSettings = Settings.System.canWrite(context),
        dnd = nm?.isNotificationPolicyAccessGranted == true
    )
} catch (_: Exception) {
    ToolPerms()
}

@Composable
internal fun rememberToolPerms(): ToolPerms {
    val context = LocalContext.current
    // Se lee de una vez para que no parpadee "falta activar" al abrir la pantalla.
    var perms by remember { mutableStateOf(readToolPerms(context)) }
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner) {
        owner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            perms = readToolPerms(context)
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

// ------------------------------------------------------------ permisos

/** Un permiso que necesita una pantalla, con el ajuste del sistema que lo activa. */
internal class PermItem(val title: String, val desc: String, val ok: Boolean, val intent: Intent)

internal fun accessPerm(perms: ToolPerms, desc: String) =
    PermItem("Servicio de accesibilidad", desc, perms.access, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

internal fun listenerPerm(perms: ToolPerms, desc: String) =
    PermItem("Acceso a notificaciones", desc, perms.listener, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))

internal fun overlayPerm(context: Context, perms: ToolPerms, desc: String) =
    PermItem("Mostrar sobre otras apps", desc, perms.overlay, overlayIntent(context))

/**
 * Panel único de permisos. Si ya está todo ([ready]) se achica a una línea
 * "Todo listo"; si falta algo, muestra la lista y un botón que abre el primer
 * ajuste pendiente.
 */
@Composable
internal fun PermissionsPanel(items: List<PermItem>, ready: Boolean = items.all { it.ok }, note: String? = null) {
    if (items.isEmpty()) return
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    Panel {
        if (ready && !expanded) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { expanded = true },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF34C759))
                Spacer(Modifier.width(10.dp))
                Text("Permisos ✓ Todo listo", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text("Ver", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Permisos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (ready) TextButton(onClick = { expanded = false }) { Text("Ocultar") }
            }
            if (!ready) Text(
                "Falta activar algo para que esto funcione. Se abre el ajuste de Android: buscá «Simón» y activalo.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            items.forEach { p -> PermissionLine(p.title, p.desc, p.ok) { openSystem(context, p.intent) } }
            if (note != null) Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!ready) {
                val first = items.firstOrNull { !it.ok }
                if (first != null) Button(onClick = { openSystem(context, first.intent) }) { Text("Activar lo que falta") }
            }
        }
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

// ------------------------------------------------------------ estructura

/**
 * Pantalla con barra superior y contenido desplazable. [onBack] reemplaza la
 * flecha de volver (por ejemplo para avisar de cambios sin guardar) y
 * [actions] va a la derecha de la barra (por ejemplo "Empezar de cero").
 */
@Composable
internal fun ToolScreen(
    title: String,
    nav: Nav,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        BackTopBar(title, onBack = { if (onBack != null) onBack() else nav.back() }, actions = actions)
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

// ------------------------------------------------------------ ordenar listas

/** Copia de la lista con el elemento [from] movido a la posición [to]. */
internal fun <T> List<T>.moved(from: Int, to: Int): List<T> {
    if (from !in indices || to !in indices || from == to) return this
    val m = toMutableList()
    val x = m.removeAt(from)
    m.add(to, x)
    return m
}

/** Botones "Subir", "Bajar" y "Quitar" para una fila de una lista ordenada. */
@Composable
internal fun OrderButtons(index: Int, count: Int, onMove: (Int) -> Unit, onRemove: (() -> Unit)?) {
    IconButton(onClick = { onMove(index - 1) }, enabled = index > 0) { Icon(Icons.Filled.KeyboardArrowUp, "Subir") }
    IconButton(onClick = { onMove(index + 1) }, enabled = index < count - 1) { Icon(Icons.Filled.KeyboardArrowDown, "Bajar") }
    if (onRemove != null) IconButton(onClick = onRemove) { Icon(Icons.Filled.Close, "Quitar") }
}

/** Lista ordenada de herramientas (accesos rápidos) con su selector para agregar más. */
@Composable
internal fun TileListEditor(tiles: List<PanelTile>, onChange: (List<PanelTile>) -> Unit, addLabel: String = "Agregar herramienta") {
    var adding by remember { mutableStateOf(false) }
    if (tiles.isEmpty()) Text("Ninguna todavía.", style = MaterialTheme.typography.bodySmall)
    tiles.forEachIndexed { i, t ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) { Icon(tileIcon(t), null, Modifier.size(20.dp)) }
            Spacer(Modifier.width(10.dp))
            Text(t.label, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            OrderButtons(i, tiles.size, { to -> onChange(tiles.moved(i, to)) }) { onChange(tiles.filterIndexed { j, _ -> j != i }) }
        }
    }
    val missing = PanelTile.entries.filter { it !in tiles }
    if (missing.isNotEmpty()) FilledTonalButton(onClick = { adding = true }) {
        Icon(Icons.Filled.Add, null)
        Spacer(Modifier.width(6.dp))
        Text(addLabel)
    }
    if (adding) AlertDialog(
        onDismissRequest = { adding = false },
        title = { Text(addLabel) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                missing.forEach { t ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable {
                            onChange(tiles + t)
                            adding = false
                        }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(tileIcon(t), null)
                        Spacer(Modifier.width(12.dp))
                        Text(t.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { adding = false }) { Text("Cerrar") } }
    )
}

// ------------------------------------------------------------ apps

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

/** Ícono de una app (clave "paquete/clase" o solo el paquete), como se ve en tu launcher. */
@Composable
internal fun AppIconFor(key: String?, sizeDp: Int) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { AppCatalog.ensureLoaded(context) }
    val apps by AppCatalog.apps.collectAsState()
    val launcher by StudioStore.launcherFlow.collectAsStateWithLifecycle()
    val app = key?.let { k -> apps.firstOrNull { it.key == k } ?: apps.firstOrNull { it.packageName == k.substringBefore('/') } }
    if (app != null) AppIconBitmap(app, launcher, sizeDp, Modifier.size(sizeDp.dp))
    else Box(Modifier.size(sizeDp.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant))
}

/** Apps para arrancar: las del dock, o teléfono, mensajes, cámara y navegador. */
internal fun defaultQuickApps(context: Context): List<String> {
    val dock = StudioStore.launcherSettings.dockApps
    if (dock.isNotEmpty()) return dock
    fun keyFor(intent: Intent): String? {
        val pkg = (try { context.packageManager.resolveActivity(intent, 0)?.activityInfo?.packageName } catch (_: Exception) { null })
            ?: return null
        return AppCatalog.apps.value.firstOrNull { it.packageName == pkg }?.key
    }
    return listOfNotNull(
        keyFor(Intent(Intent.ACTION_DIAL)),
        keyFor(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))),
        keyFor(Intent(MediaStore.ACTION_IMAGE_CAPTURE)),
        keyFor(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")))
    ).distinct()
}

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
        LauncherBgType.IMAGE, LauncherBgType.VIDEO -> Panel {
            val isImage = bg.type == LauncherBgType.IMAGE
            Row(verticalAlignment = Alignment.CenterVertically) {
                BgMediaThumb(bg, Modifier.width(54.dp).height(90.dp).clip(RoundedCornerShape(12.dp)))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (isImage) "Tu foto" else "Tu video", style = MaterialTheme.typography.titleSmall)
                    FilledTonalButton(onClick = { if (isImage) onPickPhoto() else onPickVideo() }) {
                        Text(if (isImage) "Cambiar foto" else "Cambiar video")
                    }
                }
            }
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

/** Miniatura de la foto o del primer cuadro del video elegido como fondo. */
@Composable
private fun BgMediaThumb(bg: LauncherBackground, modifier: Modifier) {
    val bmp by produceState<Bitmap?>(null, bg.path, bg.type) {
        value = withContext(Dispatchers.IO) {
            if (bg.type == LauncherBgType.VIDEO) MediaLibrary.videoThumb(bg.path) else ImageStore.load(bg.path, 360)
        }
    }
    val b = bmp
    if (b != null) Image(b.asImageBitmap(), null, modifier, contentScale = ContentScale.Crop)
    else Box(modifier.background(Color.DarkGray))
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

internal fun shortcutIcon(a: TapAction): ImageVector = when (a) {
    TapAction.OPEN_URL -> Icons.Filled.Link
    TapAction.DIAL -> Icons.Filled.Call
    TapAction.FLASHLIGHT -> Icons.Filled.FlashlightOn
    TapAction.OPEN_PANEL -> Icons.Filled.Dashboard
    TapAction.OPEN_SIMON -> Icons.Filled.AutoAwesome
    TapAction.LOCK -> Icons.Filled.Lock
    TapAction.OPEN_DRAWER -> Icons.Filled.Apps
    else -> Icons.Filled.TouchApp
}

/** Ícono de un atajo: el de la app si abre una app, o uno según lo que hace. */
@Composable
internal fun ShortcutIcon(sc: QuickShortcut, sizeDp: Int) {
    if (sc.action == TapAction.OPEN_APP && sc.data.isNotBlank()) AppIconFor(sc.data, sizeDp)
    else Box(
        Modifier.size(sizeDp.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) { Icon(shortcutIcon(sc.action), null, Modifier.size((sizeDp * 0.55f).dp), tint = MaterialTheme.colorScheme.onSecondaryContainer) }
}

private fun shortcutDetail(sc: QuickShortcut): String = when {
    sc.action == TapAction.OPEN_APP -> "Abre ${appName(sc.data.ifBlank { null })}"
    sc.data.isNotBlank() -> "${sc.action.label} · ${sc.data}"
    else -> sc.action.label
}

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
    val context = LocalContext.current
    LaunchedEffect(Unit) { AppCatalog.ensureLoaded(context) }
    // Leer la lista hace que los nombres se actualicen cuando termina de cargar.
    val catalog by AppCatalog.apps.collectAsState()
    var pickApp by remember { mutableStateOf(false) }
    var newShortcut by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<QuickShortcut?>(null) }

    ToolTitle("Apps")
    Panel {
        if (apps.isNotEmpty()) Text("Aparecen en este orden. Usá las flechas para moverlas.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        apps.forEachIndexed { i, key ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIconFor(key, 36)
                Spacer(Modifier.width(10.dp))
                val name = catalog.firstOrNull { it.key == key }?.let { AppCatalog.label(it) } ?: appName(key)
                Text(name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                OrderButtons(i, apps.size, { to -> onApps(apps.moved(i, to)) }) { onApps(apps.filterIndexed { j, _ -> j != i }) }
            }
        }
        if (apps.isEmpty()) {
            Text("Sin apps todavía.", style = MaterialTheme.typography.bodySmall)
            val hasDock = StudioStore.launcherSettings.dockApps.isNotEmpty()
            OutlinedButton(onClick = {
                val d = defaultQuickApps(context)
                if (d.isNotEmpty()) onApps(d) else Toast.makeText(context, "No encontré apps para sugerir. Agregalas a mano.", Toast.LENGTH_SHORT).show()
            }) { Text(if (hasDock) "Usar las apps de mi dock" else "Usar teléfono, mensajes, cámara y navegador") }
        }
        FilledTonalButton(onClick = { pickApp = true }) {
            Icon(Icons.Filled.Add, null)
            Spacer(Modifier.width(6.dp))
            Text("Agregar app")
        }
    }
    ToolTitle("Atajos")
    Panel {
        if (shortcuts.isNotEmpty()) Text("Tocá un atajo para editarlo.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        shortcuts.forEachIndexed { i, sc ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { editing = sc }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShortcutIcon(sc, 36)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(sc.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(shortcutDetail(sc), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                OrderButtons(i, shortcuts.size, { to -> onShortcuts(shortcuts.moved(i, to)) }) { onShortcuts(shortcuts.filterNot { it.id == sc.id }) }
            }
        }
        if (shortcuts.isEmpty()) Text("Links, contactos para llamar, linterna, bloquear…", style = MaterialTheme.typography.bodySmall)
        FilledTonalButton(onClick = { newShortcut = true }) {
            Icon(Icons.Filled.Add, null)
            Spacer(Modifier.width(6.dp))
            Text("Agregar atajo")
        }
    }
    ToolTitle("Herramientas rápidas")
    Panel {
        TileListEditor(tools, onTools)
    }

    if (pickApp) AppPickDialog(onDismiss = { pickApp = false }) { app -> if (app.key !in apps) onApps(apps + app.key) }
    if (newShortcut) ShortcutDialog(null, onDismiss = { newShortcut = false }) { onShortcuts(shortcuts + it) }
    editing?.let { sc ->
        ShortcutDialog(sc, onDismiss = { editing = null }) { upd -> onShortcuts(shortcuts.map { if (it.id == upd.id) upd else it }) }
    }
}

/** Lee el nombre y el número del teléfono elegido en Contactos (el sistema da permiso solo para esa fila). */
private fun readPickedPhone(context: Context, uri: Uri): Pair<String, String>? = try {
    context.contentResolver.query(uri, arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, "display_name"), null, null, null)?.use { c ->
        if (c.moveToFirst()) {
            val number = c.getString(0) ?: ""
            val name = c.getString(1) ?: ""
            if (number.isBlank()) null else name to number
        } else null
    }
} catch (_: Exception) {
    null
}

/** Crea un atajo nuevo o edita [initial]. */
@Composable
private fun ShortcutDialog(initial: QuickShortcut?, onDismiss: () -> Unit, onSave: (QuickShortcut) -> Unit) {
    val context = LocalContext.current
    var label by remember { mutableStateOf(initial?.label ?: "") }
    var action by remember { mutableStateOf(initial?.action?.takeIf { it in shortcutActions } ?: TapAction.OPEN_URL) }
    var data by remember { mutableStateOf(initial?.data ?: "") }
    var pickApp by remember { mutableStateOf(false) }
    val contact = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val uri = res.data?.data
        if (res.resultCode == Activity.RESULT_OK && uri != null) {
            val picked = readPickedPhone(context, uri)
            if (picked != null) {
                data = picked.second
                if (label.isBlank()) label = picked.first.ifBlank { picked.second }
            } else Toast.makeText(context, "No se pudo leer el número", Toast.LENGTH_SHORT).show()
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nuevo atajo" else "Editar atajo") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Qué hace", style = MaterialTheme.typography.titleSmall)
                ChoiceChips(shortcutActions, action, { it.label }, { if (it != action) { action = it; data = "" } })
                when (action) {
                    TapAction.OPEN_URL -> OutlinedTextField(data, { data = it }, label = { Text("Link (https://…)") }, singleLine = true)
                    TapAction.DIAL -> {
                        OutlinedButton(onClick = {
                            try {
                                contact.launch(Intent(Intent.ACTION_PICK).setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE))
                            } catch (_: Exception) {
                                Toast.makeText(context, "No hay una app de contactos", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(Icons.Filled.Contacts, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Elegir contacto")
                        }
                        OutlinedTextField(data, { data = it }, label = { Text("Número") }, singleLine = true)
                    }
                    TapAction.OPEN_APP -> OutlinedButton(onClick = { pickApp = true }) { Text(if (data.isBlank()) "Elegir app" else appName(data)) }
                    else -> Unit
                }
                OutlinedTextField(label, { label = it }, label = { Text("Nombre que se ve") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(
                enabled = label.isNotBlank() && (action !in listOf(TapAction.OPEN_URL, TapAction.DIAL, TapAction.OPEN_APP) || data.isNotBlank()),
                onClick = {
                    val sc = initial?.copy(label = label.trim(), action = action, data = data.trim())
                        ?: QuickShortcut(label = label.trim(), action = action, data = data.trim())
                    onSave(sc)
                    onDismiss()
                }
            ) { Text(if (initial == null) "Agregar" else "Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
    if (pickApp) AppPickDialog(onDismiss = { pickApp = false }) { app ->
        data = app.key
        if (label.isBlank()) label = app.label
    }
}
