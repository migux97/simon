package com.migux97.simon.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.migux97.simon.data.Catalog
import com.migux97.simon.data.FontStyle
import com.migux97.simon.data.GestureAction
import com.migux97.simon.data.IconForeground
import com.migux97.simon.data.IconShape
import com.migux97.simon.data.ImageStore
import com.migux97.simon.data.LauncherBgType
import com.migux97.simon.data.LauncherSettings
import com.migux97.simon.data.Motion
import com.migux97.simon.data.PageTransition
import com.migux97.simon.data.StudioStore
import com.migux97.simon.launcher.AnimatedFrames
import com.migux97.simon.launcher.IconPackInfo
import com.migux97.simon.launcher.IconPacks
import com.migux97.simon.launcher.IconResolver
import com.migux97.simon.launcher.LauncherActivity
import com.migux97.simon.launcher.LauncherMode
import com.migux97.simon.launcher.SystemActions
import com.migux97.simon.ui.Nav
import com.migux97.simon.ui.components.BackTopBar
import com.migux97.simon.ui.components.ChoiceChips
import com.migux97.simon.ui.components.ColorRow
import com.migux97.simon.ui.components.LabeledSlider
import com.migux97.simon.ui.components.Panel
import com.migux97.simon.ui.components.SceneThumb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun LauncherSettingsScreen(nav: Nav) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s by StudioStore.launcherFlow.collectAsStateWithLifecycle()
    val drawings by StudioStore.drawingsFlow.collectAsStateWithLifecycle()
    val animations by StudioStore.animationsFlow.collectAsStateWithLifecycle()
    fun set(t: (LauncherSettings) -> LauncherSettings) { StudioStore.updateLauncher(t); IconResolver.clear() }

    var isDefault by remember { mutableStateOf(false) }
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner) { owner.repeatOnLifecycle(Lifecycle.State.RESUMED) { isDefault = SystemActions.isDefaultLauncher(context) } }
    var packs by remember { mutableStateOf<List<IconPackInfo>>(emptyList()) }
    LaunchedEffect(Unit) { packs = withContext(Dispatchers.IO) { IconPacks.installed(context) } }

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

    Column(Modifier.fillMaxSize()) {
        BackTopBar("Simón Launcher", onBack = { nav.back() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            val mode by StudioStore.modeFlow.collectAsStateWithLifecycle()
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Modo launcher", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    Switch(mode.launcherEnabled, { on ->
                        val intent = if (on) LauncherMode.enable(context) else LauncherMode.disable(context)
                        try { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {}
                    })
                }
                Text(
                    if (mode.launcherEnabled) "Activado: Simón aparece como pantalla de inicio. Apagalo cuando quieras para volver a tu launcher original."
                    else "Apagado: tu teléfono usa su launcher original y Simón no aparece como opción. Los widgets, fondos, íconos, efectos y la capa decorativa funcionan igual.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(onClick = { nav.go("performance") }) { Text("Rendimiento y batería") }
            }
            if (!mode.launcherEnabled) {
                Panel {
                    Text("Qué suma el modo launcher", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "• Íconos y widgets en cualquier lugar, superpuestos y con movimiento\n• Cambiar el ícono de cada app sin accesos directos\n• Widgets de otras apps, carpetas, dock y cajón propio\n• Fondo de video o animación solo en el inicio\n• Gestos y transiciones entre páginas",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                return@Column
            }
            Panel {
                Text(if (isDefault) "Simón es tu pantalla de inicio ✓" else "Simón todavía no es tu pantalla de inicio", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Como launcher, Simón controla toda la pantalla de inicio: íconos y widgets en cualquier lugar, superpuestos, hasta en la zona del notch.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isDefault) Button(onClick = {
                        try { context.startActivity(SystemActions.requestDefaultLauncher(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {}
                    }) { Text("Usar Simón") }
                    FilledTonalButton(onClick = {
                        context.startActivity(Intent(context, LauncherActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }) { Text("Abrir launcher") }
                }
                Text("Para mover cosas: mantené presionado un espacio vacío del launcher.", style = MaterialTheme.typography.bodySmall)
            }

            Title("Fondo del launcher")
            ChoiceChips(LauncherBgType.entries, s.background.type, { it.label }, { t ->
                when (t) {
                    LauncherBgType.IMAGE -> photo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    LauncherBgType.VIDEO -> video.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                    else -> set { it.copy(background = it.background.copy(type = t)) }
                }
            })
            when (s.background.type) {
                LauncherBgType.COLOR, LauncherBgType.GRADIENT -> Panel {
                    ColorRow("Color 1", s.background.color1) { c -> set { it.copy(background = it.background.copy(color1 = c)) } }
                    if (s.background.type == LauncherBgType.GRADIENT) ColorRow("Color 2", s.background.color2) { c -> set { it.copy(background = it.background.copy(color2 = c)) } }
                }
                LauncherBgType.SCENE -> LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(Catalog.liveWallpapers, key = { it.id }) { lv ->
                        Box(
                            Modifier.width(54.dp).height(90.dp).clip(RoundedCornerShape(12.dp))
                                .border(if (s.background.liveId == lv.id) 3.dp else 0.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                .clickable { set { it.copy(background = it.background.copy(liveId = lv.id)) } }
                        ) { SceneThumb(lv, Modifier.fillMaxSize()) }
                    }
                }
                LauncherBgType.ANIMATION -> LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(animations, key = { it.id }) { a ->
                        Box(
                            Modifier.size(70.dp).clip(RoundedCornerShape(12.dp))
                                .border(if (s.background.mediaId == a.id) 3.dp else 0.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                .clickable { set { it.copy(background = it.background.copy(mediaId = a.id)) } }
                        ) { AnimatedFrames(a.id, Modifier.fillMaxSize()) }
                    }
                }
                LauncherBgType.DRAWING -> LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(drawings, key = { it.id }) { d ->
                        OutlinedButton(onClick = { set { it.copy(background = it.background.copy(mediaId = d.id)) } }) {
                            Text(if (s.background.mediaId == d.id) "✓ ${d.name}" else d.name)
                        }
                    }
                }
                else -> Unit
            }
            Panel {
                LabeledSlider("Oscurecer fondo", s.background.dim, 0f..0.8f, "${(s.background.dim * 100).roundToInt()}%") { v -> set { it.copy(background = it.background.copy(dim = v)) } }
                Toggle("Efecto de profundidad al pasar páginas", s.background.parallax) { v -> set { it.copy(background = it.background.copy(parallax = v)) } }
                Toggle("Ocultar la barra de estado (hora, batería)", s.hideStatusBar) { v -> set { it.copy(hideStatusBar = v) } }
                Toggle("Ocultar la barra de navegación", s.hideNavBar) { v -> set { it.copy(hideNavBar = v) } }
            }

            Title("Íconos")
            Panel {
                LabeledSlider("Tamaño", s.iconSizeDp.toFloat(), 32f..96f, "${s.iconSizeDp} dp") { v -> set { it.copy(iconSizeDp = v.roundToInt()) } }
                Toggle("Mostrar nombres", s.showLabels) { v -> set { it.copy(showLabels = v) } }
                Toggle("Sombra en los nombres", s.labelShadow) { v -> set { it.copy(labelShadow = v) } }
                LabeledSlider("Tamaño del nombre", s.labelSizeSp.toFloat(), 8f..18f, "${s.labelSizeSp}") { v -> set { it.copy(labelSizeSp = v.roundToInt()) } }
                ColorRow("Color del nombre", s.labelColor) { c -> set { it.copy(labelColor = c) } }
            }
            ChoiceChips(FontStyle.entries, s.labelFont, { it.label }, { f -> set { it.copy(labelFont = f) } })
            Title("Estilo para todos los íconos")
            ChoiceChips(listOf<String?>(null) + Catalog.themes.map { it.id }, s.globalStyle?.let { g -> Catalog.themes.firstOrNull { it.iconStyle == g }?.id ?: "custom" }, {
                when (it) { null -> "Originales"; "custom" -> "Personalizado"; else -> Catalog.themes.first { t -> t.id == it }.name }
            }, { id -> set { it.copy(globalStyle = id?.let { i -> Catalog.themes.firstOrNull { t -> t.id == i }?.iconStyle }) } })
            s.globalStyle?.let { g ->
                ChoiceChips(IconShape.entries, g.shape, { it.label }, { v -> set { it.copy(globalStyle = g.copy(shape = v)) } })
                ChoiceChips(IconForeground.entries, g.foreground, { it.label }, { v -> set { it.copy(globalStyle = g.copy(foreground = v)) } })
                Panel {
                    ColorRow("Fondo 1", g.bg1) { c -> set { it.copy(globalStyle = g.copy(bg1 = c)) } }
                    ColorRow("Fondo 2", g.bg2) { c -> set { it.copy(globalStyle = g.copy(bg2 = c)) } }
                    ColorRow("Color del dibujo", g.fgColor) { c -> set { it.copy(globalStyle = g.copy(fgColor = c)) } }
                }
            }
            Title("Pack de íconos instalado")
            if (packs.isEmpty()) Hint("No hay packs de íconos instalados. Podés bajar cualquiera de la Play Store (los compatibles con Nova sirven).")
            ChoiceChips(listOf<String?>(null) + packs.map { it.packageName }, s.iconPack, { p -> p?.let { packs.firstOrNull { it.packageName == p }?.label } ?: "Ninguno" }, { p ->
                p?.let { IconPacks.forget(it) }
                set { it.copy(iconPack = p) }
            })
            Title("Movimiento de los íconos")
            ChoiceChips(Motion.entries, s.globalMotion, { it.label }, { m -> set { it.copy(globalMotion = m) } })

            Title("Dock")
            Panel {
                Toggle("Mostrar dock", s.dockEnabled) { v -> set { it.copy(dockEnabled = v) } }
                ColorRow("Color del dock", s.dockColor) { c -> set { it.copy(dockColor = c) } }
                LabeledSlider("Esquinas", s.dockRadius.toFloat(), 0f..40f, "${s.dockRadius}") { v -> set { it.copy(dockRadius = v.roundToInt()) } }
                Text("Para agregar apps al dock, mantené presionada una app → «Agregar al dock».", style = MaterialTheme.typography.bodySmall)
            }

            Title("Cajón de apps")
            Panel {
                ColorRow("Fondo", s.drawerColor) { c -> set { it.copy(drawerColor = c) } }
                ColorRow("Texto", s.drawerTextColor) { c -> set { it.copy(drawerTextColor = c) } }
                LabeledSlider("Columnas", s.drawerColumns.toFloat(), 3f..7f, "${s.drawerColumns}") { v -> set { it.copy(drawerColumns = v.roundToInt()) } }
                if (s.hiddenApps.isNotEmpty()) OutlinedButton(onClick = { set { it.copy(hiddenApps = emptyList()) } }) { Text("Mostrar las ${s.hiddenApps.size} apps escondidas") }
            }

            Title("Gestos")
            GestureRow("Deslizar hacia arriba", s.swipeUp) { g -> set { it.copy(swipeUp = g) } }
            GestureRow("Deslizar hacia abajo", s.swipeDown) { g -> set { it.copy(swipeDown = g) } }
            GestureRow("Doble toque", s.doubleTap) { g -> set { it.copy(doubleTap = g) } }
            GestureRow("Mantener presionado", s.longPress) { g -> set { it.copy(longPress = g) } }

            Title("Páginas")
            Panel {
                LabeledSlider("Cantidad de páginas", s.pages.toFloat(), 1f..8f, "${s.pages}") { v -> set { it.copy(pages = v.roundToInt()) } }
                Toggle("Indicador de página", s.pageIndicator) { v -> set { it.copy(pageIndicator = v) } }
                Toggle("Acomodar a la grilla al soltar", s.snapToGrid) { v -> set { it.copy(snapToGrid = v) } }
                LabeledSlider("Columnas de la grilla", s.gridColumns.toFloat(), 3f..7f, "${s.gridColumns}") { v -> set { it.copy(gridColumns = v.roundToInt()) } }
                LabeledSlider("Filas de la grilla", s.gridRows.toFloat(), 4f..9f, "${s.gridRows}") { v -> set { it.copy(gridRows = v.roundToInt()) } }
            }
            Title("Transición entre páginas")
            ChoiceChips(PageTransition.entries, s.transition, { it.label }, { t -> set { it.copy(transition = t) } })
            Spacer(Modifier.height(8.dp))
            Panel {
                OutlinedButton(onClick = {
                    StudioStore.setItems(emptyList())
                    set { it.copy(initialized = false) }
                }) { Text("Volver a la pantalla por defecto") }
            }
        }
    }
}

@Composable
private fun Title(text: String) = Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 6.dp))

@Composable
private fun Hint(text: String) = Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 20.dp))

@Composable
private fun Toggle(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f)); Switch(value, onChange)
    }
}

@Composable
private fun GestureRow(label: String, value: GestureAction, onChange: (GestureAction) -> Unit) {
    Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 20.dp, top = 6.dp))
    ChoiceChips(GestureAction.entries, value, { it.label }, onChange)
}
