package com.migux97.simon.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.migux97.simon.data.StudioStore
import com.migux97.simon.effects.SimonAccessibilityService
import com.migux97.simon.launcher.AddSheet
import com.migux97.simon.launcher.ItemEditorSheet
import com.migux97.simon.launcher.LauncherLayout
import com.migux97.simon.launcher.LauncherUi
import com.migux97.simon.launcher.OVERLAY_PAGE
import com.migux97.simon.launcher.PageCanvas
import com.migux97.simon.ui.Nav
import com.migux97.simon.ui.components.BackTopBar
import com.migux97.simon.ui.components.Panel

/**
 * Editor de la capa decorativa: stickers, widgets y animaciones que se ven
 * encima de la pantalla de inicio del launcher que ya usás (One UI, MIUI…).
 */
@Composable
fun OverlayScreen(nav: Nav) {
    val context = LocalContext.current
    val mode by StudioStore.modeFlow.collectAsStateWithLifecycle()
    val items by StudioStore.homeItemsFlow.collectAsStateWithLifecycle()
    val settings by StudioStore.launcherFlow.collectAsStateWithLifecycle()
    val ui = remember { LauncherUi().apply { editMode = true } }
    var accessOk by remember { mutableStateOf(false) }
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner) { owner.repeatOnLifecycle(Lifecycle.State.RESUMED) { accessOk = SimonAccessibilityService.isEnabled(context) } }
    val config = LocalConfiguration.current
    val ratio = config.screenWidthDp.toFloat() / config.screenHeightDp.coerceAtLeast(1)
    val selected = items.firstOrNull { it.id == ui.selectedId }

    Column(Modifier.fillMaxSize()) {
        BackTopBar("Capa sobre tu launcher", onBack = { nav.back() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Mostrar la capa", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    Switch(mode.overlayEnabled, { on -> StudioStore.updateMode { it.copy(overlayEnabled = on) } })
                }
                Text(
                    "Decorá la pantalla de inicio de tu launcher de siempre, sin cambiarlo: stickers, widgets animados, dibujos y animaciones flotando encima. No bloquea los toques: tus íconos se siguen usando igual. Se ve solo en la pantalla de inicio y se pausa en el resto.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!accessOk) {
                    Text("Necesita el servicio de accesibilidad de Simón para saber cuándo estás en la pantalla de inicio.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = {
                        try { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {}
                    }) { Text("Activar accesibilidad") }
                }
            }
            Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { ui.addSheet = true }) { Text("Agregar") }
                if (selected != null) {
                    FilledTonalButton(onClick = { ui.editItem = selected }) { Text("Editar") }
                    OutlinedButton(onClick = { LauncherLayout.bringToFront(selected) }) { Text("Adelante") }
                    OutlinedButton(onClick = { StudioStore.removeItem(selected.id); ui.selectedId = null }) { Text("Quitar") }
                }
            }
            // Vista previa a escala de la pantalla: lo que pongas acá aparece igual sobre tu launcher.
            Box(
                Modifier
                    .padding(horizontal = 48.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .aspectRatio(ratio)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF2A1B5C), Color(0xFF0F2027))))
            ) {
                Text(
                    "Tu pantalla de inicio",
                    color = Color.White.copy(alpha = 0.35f),
                    modifier = Modifier.align(Alignment.Center)
                )
                PageCanvas(OVERLAY_PAGE, items.filter { it.page == OVERLAY_PAGE }.sortedBy { it.z }, settings, ui)
            }
            Text(
                "Arrastrá para mover · tocá para elegir · doble toque para propiedades",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }
    }

    if (ui.addSheet) AddSheet(ui, settings, OVERLAY_PAGE, onPickSystemWidget = { _, _ -> }, overlay = true)
    ui.editItem?.let { ItemEditorSheet(it, settings, ui) }
}

/**
 * Editor reutilizable de una capa libre (stickers, widgets, dibujos, animaciones)
 * para la pantalla de bloqueo y la de carga. [background] se dibuja debajo.
 */
@Composable
fun PageLayoutEditor(page: Int, hint: String, background: @Composable () -> Unit = {}) {
    val items by StudioStore.homeItemsFlow.collectAsStateWithLifecycle()
    val settings by StudioStore.launcherFlow.collectAsStateWithLifecycle()
    val ui = remember(page) { LauncherUi().apply { editMode = true } }
    val config = LocalConfiguration.current
    val ratio = config.screenWidthDp.toFloat() / config.screenHeightDp.coerceAtLeast(1)
    val selected = items.firstOrNull { it.id == ui.selectedId && it.page == page }

    Column {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { ui.addSheet = true }) { Text("Agregar") }
            if (selected != null) {
                FilledTonalButton(onClick = { ui.editItem = selected }) { Text("Editar") }
                OutlinedButton(onClick = { LauncherLayout.bringToFront(selected) }) { Text("Adelante") }
                OutlinedButton(onClick = { StudioStore.removeItem(selected.id); ui.selectedId = null }) { Text("Quitar") }
            }
        }
        Box(
            Modifier
                .padding(horizontal = 48.dp, vertical = 8.dp)
                .fillMaxWidth()
                .aspectRatio(ratio)
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF14102A), Color(0xFF050509))))
        ) {
            background()
            PageCanvas(page, items.filter { it.page == page }.sortedBy { it.z }, settings, ui)
        }
        Text(
            hint,
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
    }

    if (ui.addSheet) AddSheet(ui, settings, page, onPickSystemWidget = { _, _ -> }, overlay = true)
    ui.editItem?.let { ItemEditorSheet(it, settings, ui) }
}
