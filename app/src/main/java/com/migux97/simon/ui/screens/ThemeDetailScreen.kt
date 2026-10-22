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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.migux97.simon.data.Lookup
import com.migux97.simon.data.Repository
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
    var showLive by remember { mutableStateOf(theme.live != null) }
    var chooseTarget by remember { mutableStateOf(false) }
    val apps = rememberApps()

    fun saveWidgets() = theme.widgets.forEach { w -> if (Repository.design(w.id) == null) Repository.saveDesign(w) }

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
                    FilterChip(selected = !showLive, onClick = { showLive = false }, label = { Text("Fondo fijo") })
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = showLive, onClick = { showLive = true }, label = { Text("Fondo animado") })
                }
            }
            Text(theme.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))

            Panel {
                Text("1 · Fondo de pantalla", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { chooseTarget = true }) { Text("Poner fondo fijo") }
                    if (theme.live != null) FilledTonalButton(onClick = {
                        if (LiveWallpaper.apply(context, theme.live)) Toast.makeText(context, "¡Fondo animado aplicado!", Toast.LENGTH_SHORT).show()
                    }) { Text("Animado") }
                }
            }
            Panel {
                Text("2 · Widgets", style = MaterialTheme.typography.titleMedium)
                Text("Agregalos uno por uno; cada uno te pide confirmación.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            theme.widgets.forEach { w ->
                Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    WidgetOnBackdrop(w, Modifier.fillMaxWidth(if (w.size == WidgetSize.SMALL) 0.5f else 1f))
                    Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(w.name, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f), maxLines = 1)
                        OutlinedButton(onClick = { saveWidgets(); nav.editor(w.id) }) { Text("Editar") }
                        Button(onClick = { saveWidgets(); pinDesign(context, Repository.design(w.id) ?: w) }) { Text("Agregar") }
                    }
                }
            }
            Panel {
                Text("3 · Íconos", style = MaterialTheme.typography.titleMedium)
                Text("Elegí las apps a las que querés darles el estilo del tema.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FilledTonalButton(onClick = { nav.iconsWithTheme(theme.id) }) { Text("Personalizar íconos") }
            }
            Spacer(Modifier.height(8.dp))
        }
        Button(
            onClick = {
                saveWidgets()
                Repository.setAppliedTheme(theme.id)
                scope.launch {
                    if (theme.live != null && showLive) {
                        if (LiveWallpaper.apply(context, theme.live)) Toast.makeText(context, "¡Tema aplicado! Ahora agregá los widgets.", Toast.LENGTH_LONG).show()
                    } else {
                        val ok = applyStatic(context, theme.wallpaper, WallpaperTarget.BOTH)
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
                Toast.makeText(context, if (ok) "¡Fondo aplicado!" else "No se pudo aplicar el fondo", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
