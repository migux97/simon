package com.migux97.simon.ui.screens

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.migux97.simon.data.ActionType
import com.migux97.simon.data.Catalog
import com.migux97.simon.data.ChargingScreenSettings
import com.migux97.simon.data.EffectStyle
import com.migux97.simon.data.FontStyle
import com.migux97.simon.data.ImageStore
import com.migux97.simon.data.LauncherBgType
import com.migux97.simon.data.LockScreenSettings
import com.migux97.simon.data.Lookup
import com.migux97.simon.data.Repository
import com.migux97.simon.data.Routine
import com.migux97.simon.data.RoutineAction
import com.migux97.simon.data.RoutineTrigger
import com.migux97.simon.data.RotationEntry
import com.migux97.simon.data.RotationKind
import com.migux97.simon.data.RotationMode
import com.migux97.simon.data.StudioStore
import com.migux97.simon.data.TriggerType
import com.migux97.simon.data.newId
import com.migux97.simon.launcher.CHARGE_PAGE
import com.migux97.simon.launcher.LOCK_PAGE
import com.migux97.simon.tools.ChargingActivity
import com.migux97.simon.tools.LockActivity
import com.migux97.simon.tools.RoutineAlarms
import com.migux97.simon.tools.RoutineEngine
import com.migux97.simon.tools.WallpaperRotator
import com.migux97.simon.ui.Nav
import com.migux97.simon.ui.components.ChoiceChips
import com.migux97.simon.ui.components.ColorRow
import com.migux97.simon.ui.components.ConfirmDialog
import com.migux97.simon.ui.components.HelpTip
import com.migux97.simon.ui.components.LabeledSlider
import com.migux97.simon.ui.components.Panel
import com.migux97.simon.ui.components.SceneThumb
import com.migux97.simon.ui.components.ScenePreview
import com.migux97.simon.ui.components.StartOverButton
import com.migux97.simon.ui.components.StaticThumb
import com.migux97.simon.ui.components.UnsavedChangesDialog
import com.migux97.simon.wallpaper.WallpaperTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.roundToInt

// ======================================================================
//  Más funciones (índice)
// ======================================================================

@Composable
fun ToolsHubScreen(nav: Nav) {
    val context = LocalContext.current
    val perms = rememberToolPerms()
    val routines by StudioStore.routinesFlow.collectAsStateWithLifecycle()
    val rotation by StudioStore.rotationFlow.collectAsStateWithLifecycle()
    val charging by StudioStore.chargingFlow.collectAsStateWithLifecycle()
    val lock by StudioStore.lockFlow.collectAsStateWithLifecycle()
    val edge by StudioStore.edgeFlow.collectAsStateWithLifecycle()
    val bubble by StudioStore.bubbleFlow.collectAsStateWithLifecycle()
    val sounds by StudioStore.soundsFlow.collectAsStateWithLifecycle()

    ToolScreen("Más funciones", nav) {
        PermissionsPanel(
            listOf(
                accessPerm(perms, "Barra lateral, burbuja y para escuchar eventos (cargador, desbloqueo, Wi-Fi) con Simón cerrado."),
                listenerPerm(perms, "Sonidos por app o contacto y notificaciones en la pantalla de bloqueo."),
                overlayPerm(context, perms, "Para abrir solas la pantalla de carga y la de bloqueo.")
            ),
            note = "Las rutinas por evento y la pantalla de carga funcionan mientras esté activo el servicio de accesibilidad o el acceso a notificaciones."
        )
        val activeRoutines = routines.count { it.enabled }
        ToolCard("Rutinas automáticas", "Cuando pase algo (una hora, el cargador, auriculares, Wi-Fi…), Simón cambia el fondo, el tema, el sonido, el brillo y más.",
            if (activeRoutines > 0) "$activeRoutines activas" else null, listOf(Color(0xFF8B6CFF), Color(0xFF4A2FBD))) { nav.go("routines") }
        ToolCard("Fondos que cambian solos", "Una lista de fondos que rota cada cierto tiempo o al desbloquear, o un fondo de día y otro de noche.",
            if (rotation.mode != RotationMode.OFF) rotation.mode.label else null, listOf(Color(0xFF1FA2FF), Color(0xFF12D8FA))) { nav.go("rotation") }
        ToolCard("Pantalla al cargar", "Al enchufar el teléfono: reloj grande, batería, próxima alarma, una escena animada y tus stickers.",
            if (charging.enabled) "Activa" else null, listOf(Color(0xFF34C759), Color(0xFF0B8457))) { nav.go("charging") }
        ToolCard("Pantalla de bloqueo propia", "Tu diseño al prender la pantalla: fondo, reloj, notificaciones, widgets y stickers. Después sigue tu bloqueo de siempre.",
            if (lock.enabled) "Activa" else null, listOf(Color(0xFFFF7E5F), Color(0xFFFF4F81))) { nav.go("lockscreen") }
        ToolCard("Barra lateral", "Una pestañita en el borde: deslizá y aparecen tus apps, atajos y herramientas desde cualquier app.",
            if (edge.enabled) "Activa" else null, listOf(Color(0xFF6A3093), Color(0xFFA044FF))) { nav.go("edge") }
        ToolCard("Burbuja flotante", "Un botón que flota sobre todo; lo movés a donde quieras y al tocarlo abre tu menú rápido.",
            if (bubble.enabled) "Activa" else null, listOf(Color(0xFFFF9A44), Color(0xFFFC6076))) { nav.go("bubble") }
        ToolCard("Sonidos y vibración", "Un tono y una vibración distintos para cada app o para cada contacto que te escribe.",
            if (sounds.enabled) "${sounds.rules.count { it.enabled }} reglas" else null, listOf(Color(0xFF11998E), Color(0xFF38EF7D))) { nav.go("sounds") }
        ToolCard("Copias y compartir tema", "Guardá todo tu Simón en un archivo para pasarlo a otro teléfono o mandárselo a alguien.",
            null, listOf(Color(0xFF434343), Color(0xFF8E9EAB))) { nav.go("backup") }
    }
}

@Composable
private fun ToolCard(title: String, text: String, status: String?, colors: List<Color>, onClick: () -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(Brush.linearGradient(colors)))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f, fill = false))
                if (status != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        status,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(colors.first()).padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ======================================================================
//  Rutinas
// ======================================================================

private val dayLetters = listOf(Calendar.SUNDAY to "D", Calendar.MONDAY to "L", Calendar.TUESDAY to "M", Calendar.WEDNESDAY to "M",
    Calendar.THURSDAY to "J", Calendar.FRIDAY to "V", Calendar.SATURDAY to "S")

private fun triggerText(t: RoutineTrigger): String = when (t.type) {
    TriggerType.TIME -> {
        val days = if (t.days.size == 7) "todos los días" else dayLetters.filter { it.first in t.days }.joinToString(" ") { it.second }
        "A las %02d:%02d · %s".format(t.hour, t.minute, days)
    }
    else -> t.type.label
}

/** Rutinas de ejemplo para arrancar rápido. */
private fun routineTemplates(): List<Routine> = listOf(
    Routine(
        name = "Modo noche", trigger = RoutineTrigger(TriggerType.TIME, 23, 0),
        actions = listOf(RoutineAction(ActionType.DND, number = 1), RoutineAction(ActionType.BRIGHTNESS, number = 15), RoutineAction(ActionType.RINGER, number = 1))
    ),
    Routine(
        name = "Buen día", trigger = RoutineTrigger(TriggerType.TIME, 7, 30, listOf(2, 3, 4, 5, 6)),
        actions = listOf(RoutineAction(ActionType.DND, number = 0), RoutineAction(ActionType.RINGER, number = 0), RoutineAction(ActionType.MESSAGE, "¡Buen día! ☀️"))
    ),
    Routine(
        name = "Música con auriculares", trigger = RoutineTrigger(TriggerType.HEADSET_ON),
        actions = listOf(RoutineAction(ActionType.MEDIA_VOLUME, number = 50), RoutineAction(ActionType.EFFECT, "🎧", EffectStyle.entries.first().ordinal))
    ),
    Routine(
        name = "A cargar", trigger = RoutineTrigger(TriggerType.CHARGER_ON),
        actions = listOf(RoutineAction(ActionType.CHARGING_SCREEN))
    )
)

/**
 * Rutina de ejemplo elegida en "Ideas": el editor la abre ya cargada para que
 * la revises antes de guardarla.
 */
internal var pendingRoutineTemplate: Routine? = null

@Composable
fun RoutinesScreen(nav: Nav) {
    val context = LocalContext.current
    val routines by StudioStore.routinesFlow.collectAsStateWithLifecycle()
    ToolScreen("Rutinas automáticas", nav) {
        HelpTip("Una rutina es «cuando pase esto» → «Simón hace esto». Tocá una rutina para editarla.")
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { pendingRoutineTemplate = null; nav.go("routine/new") }) {
                Icon(Icons.Filled.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("Nueva rutina")
            }
        }
        if (routines.isEmpty()) Panel {
            Text("Todavía no tenés rutinas", style = MaterialTheme.typography.titleSmall)
            Text(
                "Creá una desde cero o elegí una de las ideas de abajo: se abre lista para que la revises y la guardes.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        routines.forEach { r ->
            Panel(Modifier.clickable { nav.go("routine/${r.id}") }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(r.name, style = MaterialTheme.typography.titleMedium)
                        Text(triggerText(r.trigger), style = MaterialTheme.typography.bodySmall)
                        Text(r.actions.joinToString(" · ") { it.type.label }.ifBlank { "Sin acciones" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(r.enabled, { on ->
                        StudioStore.saveRoutine(r.copy(enabled = on))
                        RoutineAlarms.scheduleAll(context)
                    })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { nav.go("routine/${r.id}") }) { Text("Editar") }
                    TextButton(enabled = r.actions.isNotEmpty(), onClick = {
                        RoutineEngine.run(context, r)
                        Toast.makeText(context, "Rutina ejecutada", Toast.LENGTH_SHORT).show()
                    }) { Text("Probar ahora") }
                }
            }
        }
        ToolTitle("Ideas para empezar")
        ToolHint("Se abren en el editor: cambiá lo que quieras y tocá «Guardar».")
        routineTemplates().forEach { t ->
            Panel(Modifier.clickable {
                pendingRoutineTemplate = t
                nav.go("routine/new")
            }) {
                Text("+ ${t.name}", style = MaterialTheme.typography.titleSmall)
                Text("${triggerText(t.trigger)} → ${t.actions.joinToString(", ") { it.type.label.lowercase() }}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun RoutineEditorScreen(nav: Nav, id: String) {
    val context = LocalContext.current
    val perms = rememberToolPerms()
    val existing = remember(id) { StudioStore.routineList.firstOrNull { it.id == id } }
    val initial = remember(id) {
        existing ?: (if (id == "new") pendingRoutineTemplate?.copy(id = newId(), enabled = true) else null) ?: Routine()
    }
    var r by remember(id) { mutableStateOf(initial) }
    var addAction by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    var askLeave by remember { mutableStateOf(false) }
    var askDelete by remember { mutableStateOf(false) }
    // Una rutina nueva con acciones (por ejemplo una idea) todavía no está guardada.
    val dirty = r != initial || (existing == null && r.actions.isNotEmpty())

    fun save() {
        StudioStore.saveRoutine(r)
        RoutineAlarms.scheduleAll(context)
        pendingRoutineTemplate = null
    }

    fun leave() {
        if (dirty) {
            askLeave = true
        } else {
            pendingRoutineTemplate = null
            nav.back()
        }
    }

    BackHandler(enabled = dirty) { askLeave = true }

    ToolScreen(if (existing == null) "Nueva rutina" else "Editar rutina", nav, onBack = { leave() }) {
        Panel {
            OutlinedTextField(r.name, { r = r.copy(name = it) }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        ToolTitle("Cuándo")
        ToolHint("Elegido: ${r.trigger.type.label}. Deslizá la fila para ver más opciones.")
        ChoiceChips(TriggerType.entries, r.trigger.type, { it.label }, { r = r.copy(trigger = r.trigger.copy(type = it)) })
        if (r.trigger.type == TriggerType.TIME) Panel {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { pickTime = true }.padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("%02d:%02d".format(r.trigger.hour, r.trigger.minute), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Light)
                Text("Tocá para cambiar la hora", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            Text("Qué días", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                dayLetters.forEach { (day, letter) ->
                    val on = day in r.trigger.days
                    Box(
                        Modifier.size(38.dp).clip(CircleShape)
                            .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                val days = if (on) r.trigger.days - day else (r.trigger.days + day).sorted()
                                r = r.copy(trigger = r.trigger.copy(days = days))
                            },
                        contentAlignment = Alignment.Center
                    ) { Text(letter, color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { r = r.copy(trigger = r.trigger.copy(days = listOf(1, 2, 3, 4, 5, 6, 7))) }) { Text("Todos los días") }
                TextButton(onClick = { r = r.copy(trigger = r.trigger.copy(days = listOf(2, 3, 4, 5, 6))) }) { Text("Lunes a viernes") }
            }
            if (r.trigger.days.isEmpty()) Text("Elegí al menos un día o no se va a ejecutar nunca.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Text("Android puede demorarla unos minutos para cuidar la batería.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ToolTitle("Qué hace")
        if (r.actions.isEmpty()) ToolHint("Todavía no hace nada. Tocá «Agregar acción» y elegí qué tiene que pasar.")
        else ToolHint("Se hacen en este orden. Con las flechas las movés y con el ícono de copiar las duplicás.")
        r.actions.forEachIndexed { i, a ->
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 1}. ${a.type.label}", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, maxLines = 2)
                    IconButton(onClick = {
                        r = r.copy(actions = r.actions.toMutableList().also { it.add(i + 1, a) })
                    }) { Icon(Icons.Filled.ContentCopy, "Duplicar") }
                    OrderButtons(i, r.actions.size, { to -> r = r.copy(actions = r.actions.moved(i, to)) }) {
                        r = r.copy(actions = r.actions.filterIndexed { j, _ -> j != i })
                    }
                }
                ActionEditor(a, perms) { na -> r = r.copy(actions = r.actions.mapIndexed { j, old -> if (j == i) na else old }) }
            }
        }
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { addAction = true }) {
                Icon(Icons.Filled.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("Agregar acción")
            }
        }
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = {
                save()
                nav.back()
            }) { Text("Guardar") }
            OutlinedButton(onClick = {
                RoutineEngine.run(context, r, record = false)
                Toast.makeText(context, "Probando…", Toast.LENGTH_SHORT).show()
            }, enabled = r.actions.isNotEmpty()) { Text("Probar") }
            if (existing != null) TextButton(onClick = { askDelete = true }) { Text("Borrar rutina", color = MaterialTheme.colorScheme.error) }
        }
    }

    if (addAction) AlertDialog(
        onDismissRequest = { addAction = false },
        title = { Text("Agregar acción") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                ActionType.entries.forEach { t ->
                    Text(
                        t.label,
                        Modifier.fillMaxWidth().clickable {
                            r = r.copy(actions = r.actions + defaultAction(t))
                            addAction = false
                        }.padding(vertical = 12.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { addAction = false }) { Text("Cerrar") } }
    )
    if (pickTime) TimePickDialog(r.trigger.hour, r.trigger.minute, onDismiss = { pickTime = false }) { h, m ->
        r = r.copy(trigger = r.trigger.copy(hour = h, minute = m))
        pickTime = false
    }
    if (askDelete && existing != null) ConfirmDialog(
        title = "¿Borrar «${existing.name}»?",
        message = "La rutina se borra y deja de ejecutarse. No se puede deshacer.",
        confirm = "Borrar",
        onDismiss = { askDelete = false },
        onConfirm = {
            askDelete = false
            RoutineAlarms.cancel(context, existing)
            StudioStore.deleteRoutine(existing.id)
            nav.back()
        }
    )
    if (askLeave) UnsavedChangesDialog(
        onSave = {
            askLeave = false
            save()
            nav.back()
        },
        onDiscard = {
            askLeave = false
            pendingRoutineTemplate = null
            nav.back()
        },
        onDismiss = { askLeave = false }
    )
}

/** Reloj de Material para elegir la hora de una rutina. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickDialog(hour: Int, minute: Int, onDismiss: () -> Unit, onPick: (Int, Int) -> Unit) {
    val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿A qué hora?") },
        text = { TimePicker(state = state) },
        confirmButton = { TextButton(onClick = { onPick(state.hour, state.minute) }) { Text("Listo") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

private fun defaultAction(t: ActionType): RoutineAction = when (t) {
    ActionType.STATIC_WALLPAPER -> RoutineAction(t, Catalog.staticWallpapers.first().id, WallpaperTarget.BOTH.ordinal)
    ActionType.LIVE_WALLPAPER -> RoutineAction(t, Catalog.liveWallpapers.first().id)
    ActionType.THEME -> RoutineAction(t, Catalog.themes.first().id)
    ActionType.BRIGHTNESS -> RoutineAction(t, number = 50)
    ActionType.MEDIA_VOLUME -> RoutineAction(t, number = 50)
    ActionType.MESSAGE -> RoutineAction(t, "¡Hola!")
    ActionType.EFFECT -> RoutineAction(t, "", 0)
    else -> RoutineAction(t)
}

@Composable
private fun ActionEditor(a: RoutineAction, perms: ToolPerms, onChange: (RoutineAction) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pickApp by remember { mutableStateOf(false) }
    val photo = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val path = withContext(Dispatchers.IO) { ImageStore.importImage(context, uri, 2400) }
            if (path != null) onChange(a.copy(value = RoutineEngine.PHOTO_PREFIX + path))
            else Toast.makeText(context, "No se pudo abrir la foto", Toast.LENGTH_SHORT).show()
        }
    }
    when (a.type) {
        ActionType.STATIC_WALLPAPER -> {
            val drawings by StudioStore.drawingsFlow.collectAsStateWithLifecycle()
            val own: RotationEntry? = when {
                a.value.startsWith(RoutineEngine.PHOTO_PREFIX) -> RotationEntry(RotationKind.PHOTO, a.value.removePrefix(RoutineEngine.PHOTO_PREFIX))
                a.value.startsWith(RoutineEngine.DRAWING_PREFIX) -> RotationEntry(RotationKind.DRAWING, a.value.removePrefix(RoutineEngine.DRAWING_PREFIX))
                else -> null
            }
            if (own != null) Row(verticalAlignment = Alignment.CenterVertically) {
                RotationThumb(own, Modifier.width(48.dp).height(86.dp).clip(RoundedCornerShape(10.dp)))
                Spacer(Modifier.width(12.dp))
                Text(if (own.kind == RotationKind.PHOTO) "Tu foto" else "Tu dibujo", style = MaterialTheme.typography.bodyLarge)
            }
            FilledTonalButton(onClick = { photo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                Text(if (own?.kind == RotationKind.PHOTO) "Cambiar foto" else "Usar una foto mía")
            }
            if (drawings.isNotEmpty()) {
                Text("Mis dibujos", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(drawings, key = { it.id }) { d ->
                        val value = RoutineEngine.DRAWING_PREFIX + d.id
                        Box(
                            Modifier.width(48.dp).height(86.dp).clip(RoundedCornerShape(10.dp))
                                .border(if (a.value == value) 3.dp else 0.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                                .clickable { onChange(a.copy(value = value)) }
                        ) { RotationThumb(RotationEntry(RotationKind.DRAWING, d.id), Modifier.fillMaxSize()) }
                    }
                }
            }
            Text("Diseños de Simón", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Catalog.staticWallpapers, key = { it.id }) { sw ->
                    Box(
                        Modifier.width(48.dp).height(86.dp).clip(RoundedCornerShape(10.dp))
                            .border(if (a.value == sw.id) 3.dp else 0.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                            .clickable { onChange(a.copy(value = sw.id)) }
                    ) { StaticThumb(sw, Modifier.fillMaxSize()) }
                }
            }
            Text("Dónde se pone", style = MaterialTheme.typography.labelLarge)
            ChoiceChips(WallpaperTarget.entries, WallpaperTarget.entries.getOrElse(a.number) { WallpaperTarget.BOTH }, { it.label }, { onChange(a.copy(number = it.ordinal)) })
        }
        ActionType.LIVE_WALLPAPER -> {
            val saved by Repository.savedLive.collectAsStateWithLifecycle()
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(saved + Catalog.liveWallpapers, key = { it.id }) { lv ->
                    Box(
                        Modifier.width(48.dp).height(86.dp).clip(RoundedCornerShape(10.dp))
                            .border(if (a.value == lv.id) 3.dp else 0.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                            .clickable { onChange(a.copy(value = lv.id)) }
                    ) { SceneThumb(lv, Modifier.fillMaxSize()) }
                }
            }
            Text("Cambia al instante si el fondo animado de Simón está puesto.", style = MaterialTheme.typography.bodySmall)
        }
        ActionType.THEME -> ChoiceChips(Catalog.themes, Catalog.themes.firstOrNull { it.id == a.value }, { it.name }, { onChange(a.copy(value = it.id)) })
        ActionType.RINGER -> ChoiceChips(listOf(0, 1, 2), a.number, { listOf("Con sonido", "Vibrar", "Silencio")[it] }, { onChange(a.copy(number = it)) })
        ActionType.DND -> {
            ChoiceChips(listOf(0, 1), a.number, { if (it == 1) "Prender" else "Apagar" }, { onChange(a.copy(number = it)) })
            if (!perms.dnd) PermissionLine("Permiso de «No molestar»", "Sin esto Android no deja cambiarlo.", false) {
                openSystem(context, Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            }
        }
        ActionType.BRIGHTNESS -> {
            LabeledSlider("Brillo", a.number.toFloat(), 1f..100f, "${a.number}%") { onChange(a.copy(number = it.roundToInt())) }
            if (!perms.writeSettings) PermissionLine("Modificar ajustes del sistema", "Necesario para cambiar el brillo.", false) {
                openSystem(context, writeSettingsIntent(context))
            }
        }
        ActionType.MEDIA_VOLUME -> LabeledSlider("Volumen", a.number.toFloat(), 0f..100f, "${a.number}%") { onChange(a.copy(number = it.roundToInt())) }
        ActionType.OPEN_APP -> Row(verticalAlignment = Alignment.CenterVertically) {
            if (a.value.isNotBlank()) {
                AppIconFor(a.value, 36)
                Spacer(Modifier.width(10.dp))
            }
            OutlinedButton(onClick = { pickApp = true }) { Text(if (a.value.isBlank()) "Elegir app" else "Cambiar: ${appName(a.value)}") }
        }
        ActionType.EFFECT -> {
            ChoiceChips(EffectStyle.entries, EffectStyle.entries.getOrElse(a.number) { EffectStyle.entries.first() }, { it.label }, { onChange(a.copy(number = it.ordinal)) })
            OutlinedTextField(a.value, { onChange(a.copy(value = it)) }, label = { Text("Texto (opcional)") }, singleLine = true)
        }
        ActionType.MESSAGE -> OutlinedTextField(a.value, { onChange(a.copy(value = it)) }, label = { Text("Mensaje") }, singleLine = true)
        ActionType.NEXT_WALLPAPER, ActionType.FLASHLIGHT_OFF, ActionType.CHARGING_SCREEN -> Unit
    }
    if (a.type == ActionType.RINGER && a.number == 2 && !perms.dnd) PermissionLine("Permiso de «No molestar»", "Android lo pide para poner el teléfono en silencio.", false) {
        openSystem(context, Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
    }
    if (pickApp) AppPickDialog(onDismiss = { pickApp = false }) { onChange(a.copy(value = it.key)) }
}

// ======================================================================
//  Fondos que cambian solos
// ======================================================================

private val intervals = listOf(15, 30, 60, 180, 360, 720, 1440)
private fun intervalText(m: Int) = if (m < 60) "$m min" else if (m < 1440) "${m / 60} h" else "1 día"

@Composable
fun RotationScreen(nav: Nav) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val r by StudioStore.rotationFlow.collectAsStateWithLifecycle()
    val drawings by StudioStore.drawingsFlow.collectAsStateWithLifecycle()
    val saved by Repository.savedLive.collectAsStateWithLifecycle()
    var choosing by remember { mutableStateOf<String?>(null) } // "list", "day", "night"

    fun set(t: (com.migux97.simon.data.WallpaperRotation) -> com.migux97.simon.data.WallpaperRotation) {
        StudioStore.updateRotation(t)
        WallpaperRotator.schedule(context)
    }

    fun addEntry(e: RotationEntry) {
        when (choosing) {
            "day" -> set { it.copy(day = e, lastApplied = "") }
            "night" -> set { it.copy(night = e, lastApplied = "") }
            else -> set { if (it.entries.any { x -> x == e }) it else it.copy(entries = it.entries + e) }
        }
    }

    val photos = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) { uris ->
        if (uris.isNotEmpty()) scope.launch {
            val paths = withContext(Dispatchers.IO) { uris.mapNotNull { ImageStore.importImage(context, it, 2400) } }
            paths.forEach { addEntry(RotationEntry(RotationKind.PHOTO, it)) }
            choosing = null
        }
    }

    ToolScreen("Fondos que cambian solos", nav, actions = {
        StartOverButton(message = "Se vacía la lista de fondos y se quitan los fondos de día y de noche. El modo queda como está. ¿Seguís?") {
            set { it.copy(entries = emptyList(), index = 0, day = null, night = null, liveDay = null, liveNight = null, lastApplied = "") }
        }
    }) {
        ToolTitle("Modo")
        ChoiceChips(RotationMode.entries, r.mode, { it.label }, { m -> set { it.copy(mode = m, lastApplied = "") } })
        when (r.mode) {
            RotationMode.OFF -> ToolHint("Elegí «Cada cierto tiempo» para rotar una lista, o «Día y noche» para tener dos fondos.")
            RotationMode.INTERVAL -> {
                Panel {
                    Text("Cambiar cada", style = MaterialTheme.typography.bodyLarge)
                    ChoiceChips(intervals, r.intervalMin, ::intervalText, { m -> set { it.copy(intervalMin = m) } })
                    ToggleRow("También al desbloquear", r.onUnlock) { v -> set { it.copy(onUnlock = v) } }
                    ToggleRow("Orden al azar", r.shuffle) { v -> set { it.copy(shuffle = v) } }
                }
                ToolTitle("Fondos de la lista (${r.entries.size})")
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(r.entries, key = { WallpaperRotator.key(it) }) { e ->
                        Box(Modifier.width(64.dp).height(116.dp).clip(RoundedCornerShape(12.dp))) {
                            RotationThumb(e, Modifier.fillMaxSize())
                            Box(
                                Modifier.align(Alignment.TopEnd).padding(4.dp).size(22.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.6f))
                                    .clickable { set { it.copy(entries = it.entries - e, index = 0) } },
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Filled.Close, "Quitar", tint = Color.White, modifier = Modifier.size(14.dp)) }
                        }
                    }
                }
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { choosing = "list" }) { Text("Agregar fondos") }
                    OutlinedButton(enabled = r.entries.isNotEmpty(), onClick = {
                        scope.launch {
                            val ok = withContext(Dispatchers.IO) { WallpaperRotator.next(context) }
                            Toast.makeText(context, if (ok) "Fondo cambiado" else "No se pudo cambiar", Toast.LENGTH_SHORT).show()
                        }
                    }) { Text("Cambiar ahora") }
                }
            }
            RotationMode.DAY_NIGHT -> {
                Panel {
                    LabeledSlider("Empieza el día", r.dayStartHour.toFloat(), 0f..23f, "${r.dayStartHour}:00") { v -> set { it.copy(dayStartHour = v.roundToInt(), lastApplied = "") } }
                    LabeledSlider("Empieza la noche", r.nightStartHour.toFloat(), 0f..23f, "${r.nightStartHour}:00") { v -> set { it.copy(nightStartHour = v.roundToInt(), lastApplied = "") } }
                }
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DayNightSlot("Día ☀️", r.day, Modifier.weight(1f)) { choosing = "day" }
                    DayNightSlot("Noche 🌙", r.night, Modifier.weight(1f)) { choosing = "night" }
                }
                ToolTitle("Fondo animado de día y de noche")
                ToolHint("Si usás el fondo animado de Simón, también puede cambiar solo.")
                Text("Día", Modifier.padding(start = 20.dp, top = 6.dp))
                LiveRow(saved + Catalog.liveWallpapers, r.liveDay) { id -> set { it.copy(liveDay = if (it.liveDay == id) null else id) } }
                Text("Noche", Modifier.padding(start = 20.dp, top = 6.dp))
                LiveRow(saved + Catalog.liveWallpapers, r.liveNight) { id -> set { it.copy(liveNight = if (it.liveNight == id) null else id) } }
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    OutlinedButton(onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                StudioStore.updateRotation { it.copy(lastApplied = "") }
                                WallpaperRotator.tick(context)
                            }
                            Toast.makeText(context, "Listo", Toast.LENGTH_SHORT).show()
                        }
                    }) { Text("Aplicar ahora") }
                }
            }
        }
        if (r.mode != RotationMode.OFF) {
            ToolTitle("Dónde se pone")
            ChoiceChips(WallpaperTarget.entries, WallpaperTarget.entries.firstOrNull { it.name == r.target } ?: WallpaperTarget.BOTH, { it.label }, { t -> set { it.copy(target = t.name) } })
            ToolHint("Android revisa cada 15 minutos como mínimo, así que el cambio puede tardar un poco.")
        }
    }

    choosing?.let { which ->
        AlertDialog(
            onDismissRequest = { choosing = null },
            title = { Text(if (which == "list") "Agregar fondos" else "Elegir fondo") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    FilledTonalButton(onClick = {
                        photos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text("Mis fotos") }
                    if (drawings.isNotEmpty()) {
                        Text("Mis dibujos", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 10.dp))
                        drawings.forEach { d ->
                            Text(d.name, Modifier.fillMaxWidth().clickable {
                                addEntry(RotationEntry(RotationKind.DRAWING, d.id))
                                if (which != "list") choosing = null
                            }.padding(vertical = 8.dp))
                        }
                    }
                    Text("Diseños de Simón", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 10.dp, bottom = 6.dp))
                    Catalog.staticWallpapers.chunked(4).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                            row.forEach { sw ->
                                val e = RotationEntry(RotationKind.STATIC, sw.id)
                                val picked = e in r.entries
                                Box(
                                    Modifier.weight(1f).height(96.dp).clip(RoundedCornerShape(10.dp))
                                        .border(if (picked && which == "list") 3.dp else 0.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                                        .clickable {
                                            if (which == "list" && picked) set { it.copy(entries = it.entries - e, index = 0) }
                                            else addEntry(e)
                                            if (which != "list") choosing = null
                                        }
                                ) { StaticThumb(sw, Modifier.fillMaxSize()) }
                            }
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { choosing = null }) { Text("Listo") } }
        )
    }
}

@Composable
private fun DayNightSlot(label: String, e: RotationEntry?, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant).clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (e != null) RotationThumb(e, Modifier.fillMaxSize()) else Text("Elegir")
        }
        Text(label, Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun LiveRow(list: List<com.migux97.simon.data.LiveConfig>, selected: String?, onPick: (String) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(list, key = { it.id }) { lv ->
            Box(
                Modifier.width(48.dp).height(86.dp).clip(RoundedCornerShape(10.dp))
                    .border(if (selected == lv.id) 3.dp else 0.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                    .clickable { onPick(lv.id) }
            ) { SceneThumb(lv, Modifier.fillMaxSize()) }
        }
    }
}

// ======================================================================
//  Pantalla al cargar
// ======================================================================

@Composable
fun ChargingSettingsScreen(nav: Nav) {
    val context = LocalContext.current
    val perms = rememberToolPerms()
    val s by StudioStore.chargingFlow.collectAsStateWithLifecycle()
    fun set(t: (ChargingScreenSettings) -> ChargingScreenSettings) = StudioStore.updateCharging(t)

    ToolScreen("Pantalla al cargar", nav, actions = {
        StartOverButton(message = "Vuelve el diseño de la pantalla de carga al original y se quita tu decoración (stickers, widgets…). ¿Seguís?") {
            set { ChargingScreenSettings(enabled = it.enabled) }
            StudioStore.setItems(StudioStore.items.filterNot { it.page == CHARGE_PAGE })
        }
    }) {
        MainSwitch(
            "Mostrar al enchufar",
            "Como una mesita de luz: reloj grande, batería, próxima alarma y tu decoración. Se cierra sola al desenchufar. El contenido se mueve apenas cada tanto para no marcar la pantalla.",
            s.enabled
        ) { on -> set { it.copy(enabled = on) } }
        PermissionsPanel(
            listOf(
                overlayPerm(context, perms, "Android solo deja abrirla sola con este permiso."),
                accessPerm(perms, "Para enterarse de que enchufaste el teléfono con Simón cerrado. Alcanza con este o con el de abajo."),
                listenerPerm(perms, "También sirve para enterarse del cargador. Alcanza con este o con el de arriba.")
            ),
            ready = perms.overlay && (perms.access || perms.listener)
        )
        Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            OutlinedButton(onClick = {
                openSystem(context, Intent(context, ChargingActivity::class.java))
            }) { Text("Ver en pantalla real") }
        }
        Panel {
            ToggleRow("Solo con el teléfono bloqueado", s.onlyWhenLocked) { v -> set { it.copy(onlyWhenLocked = v) } }
            ToggleRow("Fondo negro (ahorra batería en OLED)", s.blackBackground) { v -> set { it.copy(blackBackground = v) } }
            ToggleRow("Mostrar batería", s.showBattery) { v -> set { it.copy(showBattery = v) } }
            ToggleRow("Mostrar próxima alarma", s.showAlarm) { v -> set { it.copy(showAlarm = v) } }
            LabeledSlider("Oscurecer", s.dim, 0f..0.9f, "${(s.dim * 100).roundToInt()}%") { v -> set { it.copy(dim = v) } }
        }
        if (!s.blackBackground) {
            ToolTitle("Escena de fondo")
            LiveRow(Catalog.liveWallpapers, s.sceneId) { id -> set { it.copy(sceneId = id) } }
        }
        ToolTitle("Reloj")
        ChoiceChips(FontStyle.entries, s.clockFont, { it.label }, { f -> set { it.copy(clockFont = f) } })
        Panel {
            ColorRow("Color del reloj", s.clockColor) { c -> set { it.copy(clockColor = c) } }
            ColorRow("Color de la batería", s.accent) { c -> set { it.copy(accent = c) } }
        }
        ToolTitle("Tu decoración")
        PageLayoutEditor(CHARGE_PAGE, "Stickers, widgets, dibujos y animaciones que aparecen en la pantalla de carga.") {
            if (!s.blackBackground) s.sceneId?.let { Lookup.live(it) }?.let { ScenePreview(it, Modifier.fillMaxSize()) }
        }
    }
}

// ======================================================================
//  Pantalla de bloqueo propia
// ======================================================================

@Composable
fun LockSettingsScreen(nav: Nav) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val perms = rememberToolPerms()
    val s by StudioStore.lockFlow.collectAsStateWithLifecycle()
    fun set(t: (LockScreenSettings) -> LockScreenSettings) = StudioStore.updateLock(t)

    val photo = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val path = withContext(Dispatchers.IO) { ImageStore.importImage(context, uri, 2400) }
            if (path != null) set { it.copy(background = it.background.copy(type = LauncherBgType.IMAGE, path = path)) }
        }
    }
    val video = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val path = withContext(Dispatchers.IO) { ImageStore.importVideo(context, uri) }
            if (path != null) set { it.copy(background = it.background.copy(type = LauncherBgType.VIDEO, path = path)) }
        }
    }

    ToolScreen("Pantalla de bloqueo", nav, actions = {
        StartOverButton(message = "Vuelve el diseño de tu pantalla de bloqueo al original y se quita tu decoración (stickers, widgets…). ¿Seguís?") {
            set { LockScreenSettings(enabled = it.enabled) }
            StudioStore.setItems(StudioStore.items.filterNot { it.page == LOCK_PAGE })
        }
    }) {
        MainSwitch(
            "Usar mi pantalla de bloqueo",
            "Al prender la pantalla aparece tu diseño. Deslizás hacia arriba y sigue el bloqueo de tu teléfono (PIN, huella o cara), así que tu seguridad no cambia.",
            s.enabled
        ) { on -> set { it.copy(enabled = on) } }
        PermissionsPanel(
            listOf(
                overlayPerm(context, perms, "Android solo deja abrirla sola con este permiso."),
                accessPerm(perms, "Para saber cuándo se prende la pantalla. Alcanza con este o con el de abajo."),
                listenerPerm(perms, "También sirve para saber cuándo se prende la pantalla, y además muestra tus notificaciones. Alcanza con este o con el de arriba.")
            ),
            ready = perms.overlay && (perms.access || perms.listener)
        )
        Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            OutlinedButton(onClick = { openSystem(context, Intent(context, LockActivity::class.java)) }) { Text("Ver en pantalla real") }
        }
        ToolTitle("Fondo")
        BackgroundEditor(
            s.background,
            onPickPhoto = { photo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onPickVideo = { video.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
            onChange = { bg -> set { it.copy(background = bg) } }
        )
        ToolTitle("Reloj")
        ChoiceChips(FontStyle.entries, s.clockFont, { it.label }, { f -> set { it.copy(clockFont = f) } })
        Panel {
            ColorRow("Color", s.clockColor) { c -> set { it.copy(clockColor = c) } }
            LabeledSlider("Tamaño", s.clockSize.toFloat(), 40f..140f, "${s.clockSize}") { v -> set { it.copy(clockSize = v.roundToInt()) } }
            ToggleRow("Mostrar la fecha", s.showDate) { v -> set { it.copy(showDate = v) } }
            ToggleRow("Mostrar notificaciones", s.showNotifications) { v -> set { it.copy(showNotifications = v) } }
            if (s.showNotifications && !perms.listener) Text("Necesita el acceso a notificaciones.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(s.hint, { v -> set { it.copy(hint = v) } }, label = { Text("Texto de abajo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        ToolTitle("Tu decoración")
        PageLayoutEditor(LOCK_PAGE, "Stickers, widgets creados, dibujos y animaciones sobre tu pantalla de bloqueo.") {
            val pager = androidx.compose.foundation.pager.rememberPagerState { 1 }
            com.migux97.simon.launcher.LauncherBackgroundLayer(s.background.copy(parallax = false), pager)
        }
        Spacer(Modifier.height(8.dp))
    }
}
