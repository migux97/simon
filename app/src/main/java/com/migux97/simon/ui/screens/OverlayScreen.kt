package com.migux97.simon.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.migux97.simon.data.StudioStore
import com.migux97.simon.effects.SimonAccessibilityService
import com.migux97.simon.launcher.CHARGE_PAGE
import com.migux97.simon.launcher.LOCK_PAGE
import com.migux97.simon.launcher.LauncherLayout
import com.migux97.simon.launcher.LauncherUi
import com.migux97.simon.launcher.OVERLAY_PAGE
import com.migux97.simon.launcher.PageCanvas
import com.migux97.simon.launcher.PageEditorActivity
import com.migux97.simon.ui.Nav
import com.migux97.simon.ui.components.BackTopBar
import com.migux97.simon.ui.components.Panel
import com.migux97.simon.ui.components.PhoneWallpaperBackdrop
import com.migux97.simon.ui.components.StartOverButton

/**
 * Editor de la capa decorativa: stickers, widgets y animaciones que se ven
 * encima de la pantalla de inicio del launcher que ya usás (One UI, MIUI…).
 * La edición se hace a pantalla completa, con tu fondo real detrás.
 */
@Composable
fun OverlayScreen(nav: Nav) {
    val context = LocalContext.current
    val mode by StudioStore.modeFlow.collectAsStateWithLifecycle()
    val items by StudioStore.homeItemsFlow.collectAsStateWithLifecycle()
    var accessOk by remember { mutableStateOf(false) }
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner) { owner.repeatOnLifecycle(Lifecycle.State.RESUMED) { accessOk = SimonAccessibilityService.isEnabled(context) } }
    val hasItems = items.any { it.page == OVERLAY_PAGE }

    Column(Modifier.fillMaxSize()) {
        BackTopBar("Capa sobre tu launcher", onBack = { nav.back() }) {
            if (hasItems) StartOverButton(
                message = "Se quitan todos los stickers, widgets y animaciones de la capa. Tu launcher y tus íconos no se tocan. ¿Seguís?"
            ) { LauncherLayout.clearPage(context, OVERLAY_PAGE) }
        }
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
            PageLayoutEditor(
                OVERLAY_PAGE,
                "Así se ve ahora sobre tu pantalla de inicio. Para cambiar algo, tocá «Editar en pantalla completa»: vas a ver tu fondo real detrás.",
                startOver = false
            )
        }
    }
}

/**
 * Vista previa de una capa libre (stickers, widgets, dibujos, animaciones)
 * con el botón para editarla a pantalla completa. Se usa para la capa, la
 * pantalla de bloqueo y la de carga. [background] se dibuja debajo de la vista previa.
 * La miniatura es solo para mirar: tocarla también abre el editor.
 */
@Composable
fun PageLayoutEditor(page: Int, hint: String, startOver: Boolean = true, background: @Composable () -> Unit = {}) {
    val context = LocalContext.current
    val items by StudioStore.homeItemsFlow.collectAsStateWithLifecycle()
    val settings by StudioStore.launcherFlow.collectAsStateWithLifecycle()
    val ui = remember(page) { LauncherUi() }
    val config = LocalConfiguration.current
    val ratio = config.screenWidthDp.toFloat() / config.screenHeightDp.coerceAtLeast(1)
    val pageItems = items.filter { it.page == page }.sortedBy { it.z }
    fun openEditor() {
        try { context.startActivity(PageEditorActivity.intent(context, page)) } catch (_: Exception) {}
    }

    Column {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val n = pageItems.size
            Text(
                if (n == 1) "1 elemento" else "$n elementos",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            if (startOver && n > 0) StartOverButton(
                message = "Se quita toda la decoración de esta pantalla (stickers, widgets, dibujos y animaciones). ¿Seguís?",
                label = "Quitar todo"
            ) { LauncherLayout.clearPage(context, page) }
        }
        Button(
            onClick = { openEditor() },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).height(52.dp)
        ) {
            Icon(Icons.Filled.Fullscreen, null)
            Spacer(Modifier.width(8.dp))
            Text("Editar en pantalla completa")
        }
        if (pageItems.isEmpty()) Text(
            "Todavía no pusiste nada. Tocá «Editar en pantalla completa» y después «Agregar».",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )
        // Miniatura a escala de la pantalla, solo para mirar.
        Box(
            Modifier
                .padding(horizontal = 48.dp, vertical = 8.dp)
                .fillMaxWidth()
                .aspectRatio(ratio)
                .clip(RoundedCornerShape(28.dp))
                .then(if (page == CHARGE_PAGE) Modifier.background(Color.Black) else Modifier)
        ) {
            if (page != CHARGE_PAGE) PhoneWallpaperBackdrop(Modifier.fillMaxSize(), lock = page == LOCK_PAGE)
            background()
            PageCanvas(page, pageItems, settings, ui)
            // Capa encima que toma los toques: la miniatura no se edita, abre el editor grande.
            Box(Modifier.fillMaxSize().pointerInput(page) { detectTapGestures(onTap = { openEditor() }) })
        }
        Text(
            hint,
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
    }
}
