package com.migux97.simon.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.migux97.simon.data.Lookup
import com.migux97.simon.data.Repository
import com.migux97.simon.data.WidgetDesign
import com.migux97.simon.data.WidgetSize
import com.migux97.simon.ui.Nav
import com.migux97.simon.ui.components.BackTopBar
import com.migux97.simon.ui.components.GradientBadge
import com.migux97.simon.ui.components.Panel
import com.migux97.simon.ui.components.ScenePreview
import com.migux97.simon.ui.components.StaticThumb
import com.migux97.simon.ui.components.WidgetOnBackdrop
import com.migux97.simon.ui.components.WidgetPreview
import com.migux97.simon.ui.theme.SimonGradient
import com.migux97.simon.wallpaper.LiveWallpaper
import com.migux97.simon.wallpaper.StaticWallpapers
import com.migux97.simon.wallpaper.WallpaperTarget
import kotlinx.coroutines.launch

@Composable
fun ThemeDetailScreen(nav: Nav, id: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val theme = remember(id) { Lookup.theme(id) }
    if (theme == null) {
        Column { BackTopBar("Tema", onBack = { nav.back() }); Text("Este tema no existe.", Modifier.padding(24.dp)) }
        return
    }
    val applied by Repository.appliedTheme.collectAsStateWithLifecycle()
    val bindings by Repository.bindings.collectAsStateWithLifecycle()
    val icons by Repository.icons.collectAsStateWithLifecycle()
    val designs by Repository.designs.collectAsStateWithLifecycle()
    val current by Repository.currentStatic.collectAsStateWithLifecycle()
    val live by Repository.live.collectAsStateWithLifecycle()
    var showLive by remember { mutableStateOf(theme.live != null) }
    var chooseTarget by remember { mutableStateOf(false) }
    // El tema se marca como aplicado recién cuando el fondo animado quedó puesto.
    var waitingLiveForTheme by remember { mutableStateOf(false) }
    val apps = rememberApps()

    var resumeTick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        resumeTick++
        if (waitingLiveForTheme) {
            waitingLiveForTheme = false
            val themeLive = theme.live
            if (themeLive != null && LiveWallpaper.isActive(context) && Repository.live.value?.id == themeLive.id) {
                Repository.setAppliedTheme(theme.id)
                Toast.makeText(context, "¡Tema aplicado! Ahora agregá los widgets.", Toast.LENGTH_LONG).show()
            }
        }
    }
    val liveActive = remember(resumeTick) { LiveWallpaper.isActive(context) }
    val staticOnHome = remember(resumeTick, current) { StaticWallpapers.isOnHomeNow(context, current) }
    val wallpaperDone = (liveActive && theme.live != null && live?.id == theme.live.id) ||
        (!liveActive && staticOnHome && current?.staticWallpaper?.id == theme.wallpaper.id)
    val widgetsOnHome = theme.widgets.count { w -> bindings.values.any { it == w.id } }
    val iconsWithStyle = icons.count { it.style == theme.iconStyle }

    /** Guarda solo este widget del tema (si todavía no estaba en tus diseños). */
    fun saveWidget(w: WidgetDesign) {
        if (Repository.design(w.id) == null) Repository.saveDesign(w)
    }

    fun saveWidgets() = theme.widgets.forEach { saveWidget(it) }

    Column(Modifier.fillMaxSize()) {
        BackTopBar(theme.name, onBack = { nav.back() })
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // Maqueta del teléfono con el tema aplicado
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 60.dp, vertical = 8.dp)
                    .aspectRatio(9f / 18.5f)
                    .clip(RoundedCornerShape(34.dp))
            ) {
                if (showLive && theme.live != null) ScenePreview(theme.live, Modifier.fillMaxSize())
                else StaticThumb(theme.wallpaper, Modifier.fillMaxSize())
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Spacer(Modifier.height(16.dp))
                    theme.widgets.filter { it.size != WidgetSize.SMALL }.forEach { WidgetPreview(it, Modifier.fillMaxWidth()) }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        theme.widgets.filter { it.size == WidgetSize.SMALL }.take(2).forEach { WidgetPreview(it, Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.weight(1f))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        apps?.take(4)?.forEach { app ->
                            StyledIcon(app, app.label, theme.iconStyle, null, 144, Modifier.size(42.dp))
                        }
                    }
                }
                if (applied == theme.id) GradientBadge("Aplicado", SimonGradient, Modifier.align(Alignment.TopEnd).padding(12.dp))
            }
            if (theme.live != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    FilterChip(selected = !showLive, onClick = { showLive = false }, label = { Text("Fondo estático") })
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = showLive, onClick = { showLive = true }, label = { Text("Fondo animado") })
                }
            }
            Text(theme.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))

            Panel {
                Text("Cómo va tu tema", style = MaterialTheme.typography.titleMedium)
                Text(if (wallpaperDone) "Fondo ✓" else "Fondo: todavía no está puesto", style = MaterialTheme.typography.bodyMedium)
                Text("Widgets: $widgetsOnHome de ${theme.widgets.size} en tu inicio", style = MaterialTheme.typography.bodyMedium)
                Text(
                    when (iconsWithStyle) {
                        0 -> "Íconos: ninguna app todavía"
                        1 -> "Íconos: 1 app"
                        else -> "Íconos: $iconsWithStyle apps"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Panel {
                Text("1 · Fondo de pantalla" + if (wallpaperDone) " ✓" else "", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { chooseTarget = true }) { Text("Poner fondo estático") }
                    if (theme.live != null) FilledTonalButton(onClick = {
                        if (LiveWallpaper.apply(context, theme.live)) Toast.makeText(context, "¡Fondo animado aplicado!", Toast.LENGTH_SHORT).show()
                    }) { Text("Poner fondo animado") }
                }
            }
            Panel {
                Text("2 · Widgets ($widgetsOnHome de ${theme.widgets.size} en tu inicio)", style = MaterialTheme.typography.titleMedium)
                Text("Agregalos uno por uno; cada uno te pide confirmación. «Editar» te deja cambiarlo antes.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            theme.widgets.forEach { w ->
                val onHome = bindings.values.count { it == w.id }
                Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Box {
                        WidgetOnBackdrop(designs.firstOrNull { it.id == w.id } ?: w, Modifier.fillMaxWidth(if (w.size == WidgetSize.SMALL) 0.5f else 1f))
                        if (onHome > 0) GradientBadge("En inicio", SimonGradient, Modifier.align(Alignment.TopStart).padding(8.dp))
                    }
                    Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(w.name, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f), maxLines = 1)
                        OutlinedButton(onClick = { saveWidget(w); nav.editor(w.id) }) { Text("Editar") }
                        Button(onClick = { saveWidget(w); pinDesign(context, Repository.design(w.id) ?: w) }) { Text(if (onHome > 0) "Agregar otro" else "Agregar") }
                    }
                }
            }
            Panel {
                Text("3 · Íconos" + if (iconsWithStyle > 0) " ($iconsWithStyle)" else "", style = MaterialTheme.typography.titleMedium)
                Text("Marcá las apps que quieras y aplicales el estilo del tema de una vez. Si una app ya tiene ícono de Simón, se actualiza.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FilledTonalButton(onClick = { nav.iconsWithTheme(theme.id) }) { Text("Elegir apps") }
            }
            Spacer(Modifier.height(8.dp))
        }
        Button(
            onClick = {
                saveWidgets()
                scope.launch {
                    if (theme.live != null && showLive) {
                        if (LiveWallpaper.apply(context, theme.live)) {
                            Repository.setAppliedTheme(theme.id)
                            resumeTick++
                            Toast.makeText(context, "¡Tema aplicado! Ahora agregá los widgets.", Toast.LENGTH_LONG).show()
                        } else {
                            // Android abre su pantalla para confirmar; al volver se revisa.
                            waitingLiveForTheme = true
                        }
                    } else {
                        val ok = applyStatic(context, theme.wallpaper, WallpaperTarget.BOTH)
                        if (ok) {
                            Repository.setAppliedTheme(theme.id)
                            resumeTick++
                        }
                        Toast.makeText(context, if (ok) "¡Tema aplicado! Ahora agregá los widgets." else "No se pudo poner el fondo", Toast.LENGTH_LONG).show()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp)
        ) { Text(if (applied == theme.id) "Volver a aplicar el tema" else "Aplicar tema") }
    }

    if (chooseTarget) {
        TargetDialog("¿Dónde lo ponemos?", onDismiss = { chooseTarget = false }) { target ->
            chooseTarget = false
            scope.launch {
                val ok = applyStatic(context, theme.wallpaper, target)
                if (ok) resumeTick++
                Toast.makeText(context, if (ok) "¡Fondo aplicado!" else "No se pudo aplicar el fondo", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
