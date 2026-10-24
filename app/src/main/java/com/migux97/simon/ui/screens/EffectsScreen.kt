package com.migux97.simon.ui.screens

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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.migux97.simon.data.AppFilter
import com.migux97.simon.data.Catalog
import com.migux97.simon.data.EffectConfig
import com.migux97.simon.data.EffectStyle
import com.migux97.simon.data.EffectsSettings
import com.migux97.simon.data.FontStyle
import com.migux97.simon.data.IconShape
import com.migux97.simon.data.PanelSettings
import com.migux97.simon.data.PanelZone
import com.migux97.simon.data.StudioStore
import com.migux97.simon.effects.EffectPayload
import com.migux97.simon.effects.EffectsHub
import com.migux97.simon.effects.SimonAccessibilityService
import com.migux97.simon.effects.panelShape
import com.migux97.simon.effects.tileIcon
import com.migux97.simon.launcher.AnimatedFrames
import com.migux97.simon.launcher.AppCatalog
import com.migux97.simon.ui.Nav
import com.migux97.simon.ui.components.BackTopBar
import com.migux97.simon.ui.components.ChoiceChips
import com.migux97.simon.ui.components.ColorRow
import com.migux97.simon.ui.components.HelpTip
import com.migux97.simon.ui.components.LabeledSlider
import com.migux97.simon.ui.components.Panel
import com.migux97.simon.ui.components.PhoneWallpaperBackdrop
import com.migux97.simon.ui.components.SceneThumb
import com.migux97.simon.ui.components.StartOverButton
import com.migux97.simon.widget.Draw
import kotlin.math.roundToInt

@Composable
fun EffectsScreen(nav: Nav) {
    val context = LocalContext.current
    val fx by StudioStore.effectsFlow.collectAsStateWithLifecycle()
    val perms = rememberToolPerms()
    var tab by remember { mutableIntStateOf(0) }
    val defaults = remember { EffectsSettings() }

    // Lo que necesitan los efectos que aparecen por eventos: enterarse del evento y poder dibujar encima.
    val eventPerms = listOf(
        listenerPerm(perms, "Para enterarse de las notificaciones y los eventos con Simón cerrado."),
        overlayPerm(context, perms, "Para dibujar los efectos encima de todo. Alcanza con este o con el de abajo."),
        accessPerm(perms, "También sirve para dibujar encima de todo. Alcanza con este o con el de arriba.")
    )
    val eventReady = perms.listener && (perms.overlay || perms.access)

    Column(Modifier.fillMaxSize()) {
        BackTopBar("Efectos y panel", onBack = { nav.back() })
        TabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
            listOf("Notificación", "Desbloqueo", "Pantalla", "Carga", "Panel").forEachIndexed { i, t -> Tab(tab == i, { tab = i }, text = { Text(t, maxLines = 1) }) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
            when (tab) {
                0 -> {
                    PermissionsPanel(eventPerms, ready = eventReady)
                    EffectEditor(fx.notification, defaults.notification, isNotification = true, needs = eventReady) { c -> StudioStore.updateEffects { it.copy(notification = c) } }
                }
                1 -> {
                    PermissionsPanel(eventPerms, ready = eventReady)
                    EffectEditor(fx.unlock, defaults.unlock, needs = eventReady) { c -> StudioStore.updateEffects { it.copy(unlock = c) } }
                }
                2 -> {
                    PermissionsPanel(listOf(accessPerm(perms, "Para mostrar el efecto apenas se prende la pantalla.")))
                    HelpTip("Android no deja que ninguna app dibuje con la pantalla apagada: este efecto se ve apenas se prende, sobre la pantalla de bloqueo.")
                    EffectEditor(fx.screenOn, defaults.screenOn, needs = perms.access) { c -> StudioStore.updateEffects { it.copy(screenOn = c) } }
                }
                3 -> {
                    PermissionsPanel(eventPerms, ready = eventReady)
                    EffectEditor(fx.charging, defaults.charging, needs = eventReady) { c -> StudioStore.updateEffects { it.copy(charging = c) } }
                }
                else -> {
                    PermissionsPanel(listOf(accessPerm(perms, "Para abrir el panel deslizando desde arriba.")))
                    PanelEditor(fx, perms.access)
                }
            }
        }
    }
}

@Composable
private fun EffectEditor(c: EffectConfig, defaults: EffectConfig, isNotification: Boolean = false, needs: Boolean, onChange: (EffectConfig) -> Unit) {
    val context = LocalContext.current
    val animations by StudioStore.animationsFlow.collectAsStateWithLifecycle()
    var pickApps by remember { mutableStateOf(false) }
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Activado", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Switch(c.enabled, { onChange(c.copy(enabled = it)) })
        }
        if (!needs) Text("Falta activar los permisos de arriba para que funcione.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalButton(onClick = {
                val icon = EffectsHub.appIcon(context, context.packageName)
                EffectsHub.show(context, c, EffectPayload(icon = icon, title = if (isNotification) "Simón" else c.text, text = if (isNotification) "Así se ve tu efecto" else ""), force = true)
            }) { Text("Probar efecto") }
            Spacer(Modifier.weight(1f))
            StartOverButton(
                message = "Este efecto vuelve a su estilo, colores y opciones originales. ¿Seguís?",
                label = "Restablecer"
            ) { onChange(defaults.copy(enabled = c.enabled)) }
        }
    }
    Panel { Text("Estilo", style = MaterialTheme.typography.titleMedium) }
    ChoiceChips(EffectStyle.entries, c.style, { it.label }, { onChange(c.copy(style = it)) })
    when (c.style) {
        EffectStyle.USER_ANIMATION -> {
            Panel { Text(if (animations.isEmpty()) "Primero creá una animación en Crear." else "Elegí tu animación") }
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(animations, key = { it.id }) { a ->
                    Box(
                        Modifier.size(70.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .border(if (c.animationId == a.id) 3.dp else 0.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                            .clickable { onChange(c.copy(animationId = a.id)) }
                    ) { AnimatedFrames(a.id, Modifier.fillMaxSize()) }
                }
            }
        }
        EffectStyle.SCENE -> LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Catalog.liveWallpapers, key = { it.id }) { lv ->
                Box(
                    Modifier.width(54.dp).height(90.dp).clip(RoundedCornerShape(12.dp))
                        .border(if (c.liveId == lv.id) 3.dp else 0.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                        .clickable { onChange(c.copy(liveId = lv.id)) }
                ) { SceneThumb(lv, Modifier.fillMaxSize()) }
            }
        }
        else -> Unit
    }
    Panel {
        ColorRow("Color 1", c.color1) { onChange(c.copy(color1 = it)) }
        ColorRow("Color 2", c.color2) { onChange(c.copy(color2 = it)) }
        if (isNotification) Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Usar el color de cada app", Modifier.weight(1f)); Switch(c.useAppColor, { onChange(c.copy(useAppColor = it)) })
        }
        LabeledSlider("Duración", c.durationMs.toFloat(), 800f..8000f, "%.1f s".format(c.durationMs / 1000f)) { onChange(c.copy(durationMs = it.roundToInt())) }
        // "thickness" es el grosor de las líneas (bordes de luz y ondas).
        LabeledSlider("Grosor de la línea", c.thickness, 0.3f..3f, "x%.1f".format(c.thickness)) { onChange(c.copy(thickness = it)) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (isNotification) "Mostrar ícono de la app" else "Mostrar ícono", Modifier.weight(1f)); Switch(c.showIcon, { onChange(c.copy(showIcon = it)) })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Mostrar texto", Modifier.weight(1f)); Switch(c.showText, { onChange(c.copy(showText = it)) })
        }
        if (!isNotification && c.showText) OutlinedTextField(c.text, { onChange(c.copy(text = it.take(40))) }, label = { Text("Texto (ej: ¡Hola Franco!)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    }
    if (c.showText) ChoiceChips(FontStyle.entries, c.font, { it.label }, { onChange(c.copy(font = it)) })
    if (isNotification) {
        Panel { Text("¿Para qué apps?", style = MaterialTheme.typography.titleMedium) }
        ChoiceChips(AppFilter.entries, c.filter, { it.label }, { onChange(c.copy(filter = it)) })
        if (c.filter != AppFilter.ALL) Panel {
            Text("${c.apps.size} apps elegidas")
            OutlinedButton(onClick = { pickApps = !pickApps }) { Text(if (pickApps) "Listo" else "Elegir apps") }
            if (pickApps) {
                val apps by AppCatalog.apps.collectAsStateWithLifecycle()
                LaunchedEffect(Unit) { AppCatalog.ensureLoaded(context) }
                apps.forEach { app ->
                    val checked = c.apps.any { it.substringBefore('/') == app.packageName }
                    Row(Modifier.fillMaxWidth().clickable {
                        onChange(c.copy(apps = if (checked) c.apps.filterNot { it.substringBefore('/') == app.packageName } else c.apps + app.key))
                    }, verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked, null); Text(app.label)
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
}

/** Maqueta del panel con sus colores, forma de botones y primeros accesos. */
@Composable
private fun PanelPreview(s: PanelSettings) {
    val text = Color(s.textColor)
    val cols = s.columns.coerceIn(3, 6)
    Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth().height(200.dp).clip(RoundedCornerShape(22.dp))) {
        PhoneWallpaperBackdrop(Modifier.fillMaxSize())
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = (s.corner / 2).dp, bottomEnd = (s.corner / 2).dp))
                .background(Brush.verticalGradient(listOf(Color(s.bg1), Color(s.bg2))))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (s.showClock) Text("12:30", color = text, fontSize = 26.sp, fontFamily = FontFamily(Draw.typeface(s.clockFont)))
            s.tiles.take(cols * 2).chunked(cols).forEachIndexed { r, row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    row.forEachIndexed { i, t ->
                        // El primero se muestra "encendido" para ver los dos colores.
                        val on = r == 0 && i == 0
                        Box(
                            Modifier.size(34.dp).clip(panelShape(s.tileShape)).background(Color(if (on) s.tileActive else s.tileColor)),
                            contentAlignment = Alignment.Center
                        ) { Icon(tileIcon(t), null, Modifier.size(18.dp), tint = text) }
                    }
                    repeat(cols - row.size) { Spacer(Modifier.size(34.dp)) }
                }
            }
            if (s.showBrightness) Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(s.tileColor)))
            if (s.showNotifications) Box(Modifier.fillMaxWidth().height(22.dp).clip(RoundedCornerShape(10.dp)).background(Color(s.tileColor)))
        }
    }
}

@Composable
private fun PanelEditor(fx: EffectsSettings, accessOk: Boolean) {
    val s = fx.panel
    fun set(p: PanelSettings) = StudioStore.updateEffects { it.copy(panel = p) }
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Panel deslizable de Simón", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Switch(s.enabled, { set(s.copy(enabled = it)) })
        }
        Text(
            "Deslizá hacia abajo desde la parte de arriba de la pantalla, en la zona elegida, para abrirlo. El resto de la barra sigue abriendo el panel del sistema, así no se interrumpe nada.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalButton(onClick = { SimonAccessibilityService.instance?.showPanel() }, enabled = accessOk) { Text("Probar panel") }
            Spacer(Modifier.weight(1f))
            StartOverButton(
                message = "El panel vuelve a sus colores, forma y accesos rápidos originales. ¿Seguís?",
                label = "Restablecer"
            ) { set(PanelSettings(enabled = s.enabled)) }
        }
    }
    Panel { Text("Así se ve", style = MaterialTheme.typography.titleMedium) }
    PanelPreview(s)
    Panel { Text("Zona para abrirlo", style = MaterialTheme.typography.titleMedium) }
    ChoiceChips(PanelZone.entries, s.zone, { it.label }, { set(s.copy(zone = it)) })
    Panel {
        if (s.zone != PanelZone.FULL) LabeledSlider("Ancho de la zona", s.zoneWidth, 0.15f..0.8f, "${(s.zoneWidth * 100).roundToInt()}%") { set(s.copy(zoneWidth = it)) }
        ColorRow("Fondo arriba", s.bg1) { set(s.copy(bg1 = it)) }
        ColorRow("Fondo abajo", s.bg2) { set(s.copy(bg2 = it)) }
        ColorRow("Botones", s.tileColor) { set(s.copy(tileColor = it)) }
        ColorRow("Botón encendido", s.tileActive) { set(s.copy(tileActive = it)) }
        ColorRow("Texto e íconos", s.textColor) { set(s.copy(textColor = it)) }
        LabeledSlider("Columnas", s.columns.toFloat(), 3f..6f, "${s.columns}") { set(s.copy(columns = it.roundToInt())) }
        LabeledSlider("Esquinas", s.corner.toFloat(), 0f..48f, "${s.corner}") { set(s.copy(corner = it.roundToInt())) }
        Row(verticalAlignment = Alignment.CenterVertically) { Text("Reloj", Modifier.weight(1f)); Switch(s.showClock, { set(s.copy(showClock = it)) }) }
        Row(verticalAlignment = Alignment.CenterVertically) { Text("Brillo", Modifier.weight(1f)); Switch(s.showBrightness, { set(s.copy(showBrightness = it)) }) }
        Row(verticalAlignment = Alignment.CenterVertically) { Text("Notificaciones", Modifier.weight(1f)); Switch(s.showNotifications, { set(s.copy(showNotifications = it)) }) }
    }
    Panel { Text("Forma de los botones", style = MaterialTheme.typography.titleMedium) }
    ChoiceChips(IconShape.entries, s.tileShape, { it.label }, { set(s.copy(tileShape = it)) })
    if (s.showClock) ChoiceChips(FontStyle.entries, s.clockFont, { it.label }, { set(s.copy(clockFont = it)) })
    Panel {
        Text("Accesos rápidos (en orden)", style = MaterialTheme.typography.titleMedium)
        Text("Aparecen en el panel en este orden. Usá las flechas para moverlos.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TileListEditor(s.tiles, { set(s.copy(tiles = it)) }, addLabel = "Agregar acceso")
    }
    Spacer(Modifier.height(16.dp))
}
