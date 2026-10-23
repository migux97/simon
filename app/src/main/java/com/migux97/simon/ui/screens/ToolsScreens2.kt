package com.migux97.simon.ui.screens

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.migux97.simon.data.SoundRule
import com.migux97.simon.data.StudioStore
import com.migux97.simon.data.VibePattern
import com.migux97.simon.effects.BubbleFace
import com.migux97.simon.tools.BackupManager
import com.migux97.simon.tools.SoundPlayer
import com.migux97.simon.ui.Nav
import com.migux97.simon.ui.components.ChoiceChips
import com.migux97.simon.ui.components.ColorRow
import com.migux97.simon.ui.components.LabeledSlider
import com.migux97.simon.ui.components.Panel
import com.migux97.simon.ui.components.StickerPickerDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

// ======================================================================
//  Barra lateral
// ======================================================================

@Composable
fun EdgeSettingsScreen(nav: Nav) {
    val context = LocalContext.current
    val perms = rememberToolPerms()
    val s by StudioStore.edgeFlow.collectAsStateWithLifecycle()
    fun set(t: (com.migux97.simon.data.EdgePanelSettings) -> com.migux97.simon.data.EdgePanelSettings) = StudioStore.updateEdge(t)

    ToolScreen("Barra lateral", nav) {
        MainSwitch(
            "Mostrar la pestaña",
            "Una línea finita en el borde de la pantalla. Deslizala hacia adentro (o tocala) desde cualquier app y aparece tu barra.",
            s.enabled
        ) { on -> set { it.copy(enabled = on) } }
        if (!perms.access) Panel {
            PermissionLine("Servicio de accesibilidad", "La barra flota sobre todo gracias a este servicio.", false) {
                openSystem(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
        ToolTitle("Pestaña")
        ChoiceChips(listOf(false, true), s.rightSide, { if (it) "Derecha" else "Izquierda" }, { v -> set { it.copy(rightSide = v) } })
        Panel {
            LabeledSlider("Altura en la pantalla", s.handleY, 0f..0.9f, "${(s.handleY * 100).roundToInt()}%") { v -> set { it.copy(handleY = v) } }
            LabeledSlider("Largo", s.handleHeight, 0.05f..0.5f, "${(s.handleHeight * 100).roundToInt()}%") { v -> set { it.copy(handleHeight = v) } }
            LabeledSlider("Visibilidad", s.handleAlpha, 0f..1f, "${(s.handleAlpha * 100).roundToInt()}%") { v -> set { it.copy(handleAlpha = v) } }
            ColorRow("Color", s.handleColor) { c -> set { it.copy(handleColor = c) } }
        }
        ToolTitle("Barra")
        Panel {
            ColorRow("Fondo arriba", s.bg1) { c -> set { it.copy(bg1 = c) } }
            ColorRow("Fondo abajo", s.bg2) { c -> set { it.copy(bg2 = c) } }
            ColorRow("Texto", s.textColor) { c -> set { it.copy(textColor = c) } }
            LabeledSlider("Columnas", s.columns.toFloat(), 1f..4f, "${s.columns}") { v -> set { it.copy(columns = v.roundToInt()) } }
        }
        QuickItemsEditor(
            s.apps, s.shortcuts, s.tools,
            onApps = { v -> set { it.copy(apps = v) } },
            onShortcuts = { v -> set { it.copy(shortcuts = v) } },
            onTools = { v -> set { it.copy(tools = v) } }
        )
        Row(Modifier.padding(16.dp)) {
            OutlinedButton(enabled = s.enabled && perms.access, onClick = {
                com.migux97.simon.effects.SimonAccessibilityService.instance?.showEdgePanel()
            }) { Text("Abrir para probar") }
        }
    }
}

// ======================================================================
//  Burbuja flotante
// ======================================================================

@Composable
fun BubbleSettingsScreen(nav: Nav) {
    val context = LocalContext.current
    val perms = rememberToolPerms()
    val s by StudioStore.bubbleFlow.collectAsStateWithLifecycle()
    var pickSticker by remember { mutableStateOf(false) }
    fun set(t: (com.migux97.simon.data.BubbleSettings) -> com.migux97.simon.data.BubbleSettings) = StudioStore.updateBubble(t)

    ToolScreen("Burbuja flotante", nav) {
        MainSwitch(
            "Mostrar la burbuja",
            "Flota sobre todas las apps. Arrastrala a donde quieras (se pega al borde), tocala para abrir tu menú y mantenela presionada para esconderla 30 segundos.",
            s.enabled
        ) { on -> set { it.copy(enabled = on) } }
        if (!perms.access) Panel {
            PermissionLine("Servicio de accesibilidad", "La burbuja flota sobre todo gracias a este servicio.", false) {
                openSystem(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
        Box(
            Modifier.fillMaxWidth().padding(16.dp).height(120.dp).clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF2A1B5C), Color(0xFF0F2027)))),
            contentAlignment = Alignment.Center
        ) {
            BubbleFace(s, onTap = {}, onLongPress = {}, onDrag = {}, onDragEnd = {})
        }
        ToolTitle("Aspecto")
        Panel {
            LabeledSlider("Tamaño", s.sizeDp.toFloat(), 36f..90f, "${s.sizeDp} dp") { v -> set { it.copy(sizeDp = v.roundToInt()) } }
            LabeledSlider("Opacidad", s.alpha, 0.2f..1f, "${(s.alpha * 100).roundToInt()}%") { v -> set { it.copy(alpha = v) } }
            ColorRow("Color 1", s.color1) { c -> set { it.copy(color1 = c) } }
            ColorRow("Color 2", s.color2) { c -> set { it.copy(color2 = c) } }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { pickSticker = true }) { Text(if (s.stickerId == null) "Poner un sticker" else "Cambiar sticker") }
                if (s.stickerId != null) TextButton(onClick = { set { it.copy(stickerId = null) } }) { Text("Quitar sticker") }
            }
        }
        QuickItemsEditor(
            s.apps, s.shortcuts, s.tools,
            onApps = { v -> set { it.copy(apps = v) } },
            onShortcuts = { v -> set { it.copy(shortcuts = v) } },
            onTools = { v -> set { it.copy(tools = v) } }
        )
    }

    if (pickSticker) StickerPickerDialog(onDismiss = { pickSticker = false }) { st ->
        set { it.copy(stickerId = st.id) }
        pickSticker = false
    }
}

// ======================================================================
//  Sonidos y vibración
// ======================================================================

@Composable
fun SoundsScreen(nav: Nav) {
    val context = LocalContext.current
    val perms = rememberToolPerms()
    val s by StudioStore.soundsFlow.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<SoundRule?>(null) }

    ToolScreen("Sonidos y vibración", nav) {
        MainSwitch(
            "Sonidos propios",
            "Cuando llega una notificación que coincide con una regla, Simón suena y vibra a tu manera. Para que no se superponga con el sonido de siempre, silenciá esa app en los ajustes de notificaciones de Android.",
            s.enabled
        ) { on -> StudioStore.updateSounds { it.copy(enabled = on) } }
        if (!perms.listener) Panel {
            PermissionLine("Acceso a notificaciones", "Para saber qué app o qué contacto te escribe.", false) {
                openSystem(context, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }
        Panel {
            ToggleRow("Respetar el modo silencio", s.respectSilent) { v -> StudioStore.updateSounds { it.copy(respectSilent = v) } }
            Text("En modo vibrar solo vibra; en silencio no hace nada.", style = MaterialTheme.typography.bodySmall)
        }
        ToolTitle("Reglas")
        s.rules.forEach { r ->
            Panel(Modifier.clickable { editing = r }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(r.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            (if (r.packageName.isBlank()) "Todas las apps" else appNameForPackage(r.packageName)) +
                                (if (r.contains.isNotBlank()) " · menciona «${r.contains}»" else ""),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            (r.soundName.ifBlank { if (r.soundUri == null) "Sin sonido" else "Sonido elegido" }) + " · " + r.vibration.label,
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(r.enabled, { on -> saveRule(r.copy(enabled = on)) })
                }
            }
        }
        if (s.rules.isEmpty()) ToolHint("Ejemplo: WhatsApp + «Mamá» → un tono especial y vibración de latido.")
        Row(Modifier.padding(16.dp)) {
            Button(onClick = { editing = SoundRule() }) { Text("Nueva regla") }
        }
    }

    editing?.let { rule -> SoundRuleDialog(rule, onDismiss = { editing = null }) }
}

private fun saveRule(r: SoundRule) = StudioStore.updateSounds { s ->
    val list = s.rules.toMutableList()
    val i = list.indexOfFirst { it.id == r.id }
    if (i >= 0) list[i] = r else list += r
    s.copy(rules = list)
}

@Composable
private fun SoundRuleDialog(original: SoundRule, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var r by remember(original.id) { mutableStateOf(original) }
    var pickApp by remember { mutableStateOf(false) }

    val ringtone = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val uri: Uri? = res.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            r = if (uri == null) r.copy(soundUri = null, soundName = "Sin sonido")
            else r.copy(soundUri = uri.toString(), soundName = try { RingtoneManager.getRingtone(context, uri)?.getTitle(context) ?: "Tono" } catch (_: Exception) { "Tono" })
        }
    }
    val audioFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            r = r.copy(soundUri = uri.toString(), soundName = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "Audio")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Regla de sonido") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(r.name, { r = r.copy(name = it) }, label = { Text("Nombre") }, singleLine = true)
                Text("App", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pickApp = true }) { Text(if (r.packageName.isBlank()) "Todas" else appNameForPackage(r.packageName)) }
                    if (r.packageName.isNotBlank()) TextButton(onClick = { r = r.copy(packageName = "") }) { Text("Todas") }
                }
                OutlinedTextField(
                    r.contains, { r = r.copy(contains = it) },
                    label = { Text("Contacto o palabra (opcional)") },
                    supportingText = { Text("Suena solo si la notificación lo menciona, por ejemplo el nombre de quien escribe.") },
                    singleLine = true
                )
                Text("Sonido: ${r.soundName.ifBlank { if (r.soundUri == null) "ninguno" else "elegido" }}", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = {
                        val i = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION or RingtoneManager.TYPE_RINGTONE)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, r.soundUri?.let { Uri.parse(it) })
                        try { ringtone.launch(i) } catch (_: Exception) {}
                    }) { Text("Tono") }
                    FilledTonalButton(onClick = { audioFile.launch(arrayOf("audio/*")) }) { Text("Archivo") }
                }
                LabeledSlider("Volumen", r.volume, 0.1f..1f, "${(r.volume * 100).roundToInt()}%") { r = r.copy(volume = it) }
                Text("Vibración", style = MaterialTheme.typography.titleSmall)
                ChoiceChips(VibePattern.entries, r.vibration, { it.label }, { r = r.copy(vibration = it) })
                TextButton(onClick = {
                    r.soundUri?.let { SoundPlayer.play(context, it, r.volume) }
                    SoundPlayer.vibrate(context, r.vibration)
                }) { Text("Probar") }
            }
        },
        confirmButton = {
            TextButton(onClick = { saveRule(r); onDismiss() }) { Text("Guardar") }
        },
        dismissButton = {
            Row {
                if (StudioStore.soundSettings.rules.any { it.id == r.id }) TextButton(onClick = {
                    StudioStore.updateSounds { s -> s.copy(rules = s.rules.filterNot { it.id == r.id }) }
                    onDismiss()
                }) { Text("Borrar") }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    )
    if (pickApp) AppPickDialog(onDismiss = { pickApp = false }) { app ->
        r = r.copy(packageName = app.packageName, name = if (r.name == "Nueva regla") app.label else r.name)
    }
}

// ======================================================================
//  Copias y compartir
// ======================================================================

@Composable
fun BackupScreen(nav: Nav) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var includeVideos by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) {
            busy = true
            scope.launch {
                val r = withContext(Dispatchers.IO) { BackupManager.exportTo(context, uri, includeVideos) }
                busy = false
                toast(r.message)
            }
        }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) pendingRestore = uri }

    ToolScreen("Copias y compartir tema", nav) {
        Panel {
            Text("Qué se guarda", style = MaterialTheme.typography.titleMedium)
            Text(
                "Widgets y tus diseños, fondos, temas e íconos, el launcher y la capa, widgets creados, stickers, dibujos, animaciones, efectos, rutinas, pantalla de carga y de bloqueo, barra lateral, burbuja y sonidos.",
                style = MaterialTheme.typography.bodySmall
            )
            ToggleRow("Incluir videos (el archivo pesa más)", includeVideos) { includeVideos = it }
        }
        Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = !busy, onClick = { save.launch(BackupManager.suggestedName()) }) { Text("Guardar copia") }
            FilledTonalButton(enabled = !busy, onClick = {
                busy = true
                scope.launch {
                    val intent = withContext(Dispatchers.IO) { BackupManager.shareIntent(context, includeVideos) }
                    busy = false
                    if (intent != null) openSystem(context, intent) else toast("No se pudo preparar el archivo")
                }
            }) { Text("Compartir") }
        }
        ToolHint("Mandale el archivo .simon a quien quieras: lo abre con Simón → Más funciones → Copias → Restaurar.")
        ToolTitle("Restaurar")
        Panel {
            Text("Reemplaza todo lo que tenés ahora en Simón por lo que trae la copia.", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(enabled = !busy, onClick = { open.launch(arrayOf("*/*")) }) { Text("Elegir copia…") }
        }
        if (busy) ToolHint("Trabajando…")
        Spacer(Modifier.size(8.dp))
    }

    pendingRestore?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("¿Restaurar esta copia?") },
            text = { Text("Se reemplaza tu configuración actual de Simón. Los widgets que ya están en tu pantalla se actualizan solos.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingRestore = null
                    busy = true
                    scope.launch {
                        val r = withContext(Dispatchers.IO) { BackupManager.restore(context, uri) }
                        busy = false
                        toast(r.message)
                    }
                }) { Text("Restaurar") }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("Cancelar") } }
        )
    }
}
