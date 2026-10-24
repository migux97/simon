package com.migux97.simon.ui.components

import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ======================================================================
//  Piezas comunes para editar: empezar de cero, confirmar, salir sin
//  guardar, ayudas y el color del fondo real del teléfono.
// ======================================================================

/**
 * Botón "Empezar de cero" para la barra superior o una fila de acciones.
 * Siempre pide confirmación antes de llamar a [onConfirm].
 */
@Composable
fun StartOverButton(
    message: String = "Se borra lo que tenés en esta pantalla y arrancás en blanco. ¿Seguís?",
    label: String = "Empezar de cero",
    onConfirm: () -> Unit
) {
    var ask by remember { mutableStateOf(false) }
    TextButton(onClick = { ask = true }) {
        Icon(Icons.Filled.RestartAlt, null)
        Spacer(Modifier.width(4.dp))
        Text(label)
    }
    if (ask) ConfirmDialog(
        title = "¿Empezar de cero?",
        message = message,
        confirm = "Sí, empezar de cero",
        onDismiss = { ask = false },
        onConfirm = { ask = false; onConfirm() }
    )
}

/** Diálogo de confirmación simple con un botón destacado y "Cancelar". */
@Composable
fun ConfirmDialog(title: String, message: String, confirm: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

/** Al salir de un editor con cambios: guardar, salir igual o seguir editando. */
@Composable
fun UnsavedChangesDialog(onSave: () -> Unit, onDiscard: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Guardar los cambios?") },
        text = { Text("Hiciste cambios que todavía no guardaste.") },
        confirmButton = { TextButton(onClick = onSave) { Text("Guardar") } },
        dismissButton = {
            Row {
                TextButton(onClick = onDiscard) { Text("Salir sin guardar") }
                TextButton(onClick = onDismiss) { Text("Seguir editando") }
            }
        }
    )
}

/** Ayuda corta con ícono de información, para explicar gestos o qué hace una pantalla. */
@Composable
fun HelpTip(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

/** Botón "Deshacer" para barras de edición. */
@Composable
fun UndoButton(enabled: Boolean, onUndo: () -> Unit) {
    TextButton(onClick = onUndo, enabled = enabled) {
        Icon(Icons.AutoMirrored.Filled.Undo, null)
        Spacer(Modifier.width(4.dp))
        Text("Deshacer")
    }
}

/** Historial simple para deshacer: guarda copias de un estado inmutable. */
class UndoStack<T>(private val limit: Int = 30) {
    private val past = ArrayDeque<T>()
    var canUndo by mutableStateOf(false)
        private set

    fun push(state: T) {
        past.addLast(state)
        while (past.size > limit) past.removeFirst()
        canUndo = past.isNotEmpty()
    }

    fun pop(): T? {
        val s = past.removeLastOrNull()
        canUndo = past.isNotEmpty()
        return s
    }

    fun clear() {
        past.clear()
        canUndo = false
    }
}

/**
 * Colores del fondo de pantalla real del teléfono. Android no deja leer la
 * imagen sin permisos especiales, pero sí sus colores principales: alcanza
 * para que las vistas previas se parezcan a tu pantalla.
 */
object PhoneWallpaper {
    fun colors(context: Context, lock: Boolean = false): List<Color> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) return default
        return try {
            val wm = WallpaperManager.getInstance(context)
            val wc = wm.getWallpaperColors(if (lock) WallpaperManager.FLAG_LOCK else WallpaperManager.FLAG_SYSTEM)
                ?: wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                ?: return default
            listOfNotNull(wc.primaryColor, wc.secondaryColor, wc.tertiaryColor).map { Color(it.toArgb()) }
                .let { if (it.size == 1) listOf(it[0], it[0].copy(alpha = 0.7f)) else it }
        } catch (_: Exception) {
            default
        }
    }

    private val default = listOf(Color(0xFF2A1B5C), Color(0xFF0F2027))
}

/** Fondo que imita los colores de tu fondo de pantalla real. */
@Composable
fun PhoneWallpaperBackdrop(modifier: Modifier = Modifier, lock: Boolean = false, content: @Composable () -> Unit = {}) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val colors = remember(lock) { PhoneWallpaper.colors(context, lock) }
    Box(modifier.background(Brush.verticalGradient(colors))) { content() }
}

/** Fila de título con acciones a la derecha (por ejemplo "Empezar de cero"). */
@Composable
fun TitleWithActions(title: String, modifier: Modifier = Modifier, actions: @Composable () -> Unit) {
    Row(modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleMedium) }
        actions()
    }
}
