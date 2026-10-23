package com.migux97.simon.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.migux97.simon.data.AppFilter
import com.migux97.simon.data.Catalog
import com.migux97.simon.data.EffectConfig
import com.migux97.simon.data.EffectStyle
import com.migux97.simon.data.EffectsSettings
import com.migux97.simon.data.FontStyle
import com.migux97.simon.data.IconShape
import com.migux97.simon.data.PanelSettings
import com.migux97.simon.data.PanelTile
import com.migux97.simon.data.PanelZone
import com.migux97.simon.data.StudioStore
import com.migux97.simon.effects.EffectPayload
import com.migux97.simon.effects.EffectsHub
import com.migux97.simon.effects.SimonAccessibilityService
import com.migux97.simon.effects.SimonNotificationListener
import com.migux97.simon.launcher.AnimatedFrames
import com.migux97.simon.launcher.AppCatalog
import com.migux97.simon.ui.Nav
import com.migux97.simon.ui.components.BackTopBar
import com.migux97.simon.ui.components.ChoiceChips
import com.migux97.simon.ui.components.ColorRow
import com.migux97.simon.ui.components.LabeledSlider
import com.migux97.simon.ui.components.Panel
import com.migux97.simon.ui.components.SceneThumb
import kotlin.math.roundToInt

private data class Perms(val listener: Boolean, val overlay: Boolean, val access: Boolean)

@Composable
private fun rememberPerms(): Perms {
    val context = LocalContext.current
    var perms by remember { mutableStateOf(Perms(false, false, false)) }
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner) {
        owner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            perms = Perms(
                SimonNotificationListener.isEnabled(context),
                EffectsHub.canDrawOverlays(context),
                SimonAccessibilityService.isEnabled(context)
            )
        }
    }
    return perms
}

@Composable
fun EffectsScreen(nav: Nav) {
    val context = LocalContext.current
    val fx by StudioStore.effectsFlow.collectAsStateWithLifecycle()
    val perms = rememberPerms()
    var tab by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        BackTopBar("Efectos y panel", onBack = { nav.back() })
        TabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
            listOf("Notificación", "Desbloqueo", "Pantalla", "Carga", "Panel").forEachIndexed { i, t -> Tab(tab == i, { tab = i }, text = { Text(t, maxLines = 1) }) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
            Panel {
                Text("Permisos", style = MaterialTheme.typography.titleMedium)
                PermRow("Acceso a notificaciones", "Para animar cuando llega una notificación y listarlas en el panel.", perms.listener) {
                    SystemIntent(context, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
                PermRow("Mostrar sobre otras apps", "Para dibujar los efectos encima de todo.", perms.overlay) {
                    SystemIntent(context, Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                }
                PermRow("Servicio de accesibilidad", "Para el panel deslizable, bloquear con gesto y efectos al prender la pantalla.", perms.access) {
                    SystemIntent(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
                Text(
                    "Android no deja que ninguna app dibuje con la pantalla apagada: el efecto de «Pantalla» se ve apenas se prende, sobre la pantalla de bloqueo.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            when (tab) {
                0 -> EffectEditor(fx.notification, isNotification = true, needs = perms.listener && (perms.overlay || perms.access)) { c -> StudioStore.updateEffects { it.copy(notification = c) } }
                1 -> EffectEditor(fx.unlock, needs = perms.listener && (perms.overlay || perms.access)) { c -> StudioStore.updateEffects { it.copy(unlock = c) } }
                2 -> EffectEditor(fx.screenOn, needs = perms.access) { c -> StudioStore.updateEffects { it.copy(screenOn = c) } }
                3 -> EffectEditor(fx.charging, needs = perms.listener && (perms.overlay || perms.access)) { c -> StudioStore.updateEffects { it.copy(charging = c) } }
                else -> PanelEditor(fx, perms.access)
            }
        }
    }
}

private fun SystemIntent(context: android.content.Context, intent: Intent) {
    try { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {}
}

@Composable
private fun PermRow(title: String, desc: String, ok: Boolean, onFix: () -> Unit) {
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

@Composable
private fun EffectEditor(c: EffectConfig, isNotification: Boolean = false, needs: Boolean, onChange: (EffectConfig) -> Unit) {
    val context = LocalContext.current
    val animations by StudioStore.animationsFlow.collectAsStateWithLifecycle()
    var pickApps by remember { mutableStateOf(false) }
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Activado", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Switch(c.enabled, { onChange(c.copy(enabled = it)) })
        }
        if (!needs) Text("Falta activar los permisos de arriba para que funcione.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        FilledTonalButton(onClick = {
            val icon = EffectsHub.appIcon(context, context.packageName)
            EffectsHub.show(context, c, EffectPayload(icon = icon, title = if (isNotification) "Simón" else c.text, text = if (isNotification) "Así se ve tu efecto" else ""), force = true)
        }) { Text("Probar efecto") }
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
        LabeledSlider("Intensidad", c.thickness, 0.3f..3f, "x%.1f".format(c.thickness)) { onChange(c.copy(thickness = it)) }
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
        if (!accessOk) Text("Necesita el servicio de accesibilidad activado.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        FilledTonalButton(onClick = { SimonAccessibilityService.instance?.showPanel() }, enabled = accessOk) { Text("Abrir el panel ahora") }
    }
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
        PanelTile.entries.forEach { t ->
            val on = t in s.tiles
            Row(Modifier.fillMaxWidth().clickable {
                set(s.copy(tiles = if (on) s.tiles - t else s.tiles + t))
            }, verticalAlignment = Alignment.CenterVertically) {
                Checkbox(on, null); Text(t.label)
            }
        }
    }
    Spacer(Modifier.height(16.dp))
}
