package com.migux97.simon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import com.migux97.simon.ui.components.ChoiceChips
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.migux97.simon.data.Catalog
import com.migux97.simon.data.Lookup
import com.migux97.simon.data.SceneType
import com.migux97.simon.data.Repository
import com.migux97.simon.data.ThemePack
import com.migux97.simon.data.WidgetSize
import com.migux97.simon.ui.Nav
import com.migux97.simon.ui.components.GradientBadge
import com.migux97.simon.ui.components.Panel
import com.migux97.simon.ui.components.PhoneWallpaperBackdrop
import com.migux97.simon.ui.components.SceneThumb
import com.migux97.simon.ui.components.SectionHeader
import com.migux97.simon.ui.components.StaticThumb
import com.migux97.simon.ui.components.WidgetOnBackdrop
import com.migux97.simon.ui.components.WidgetPreview
import com.migux97.simon.ui.theme.SimonGradient
import com.migux97.simon.wallpaper.LiveWallpaper
import com.migux97.simon.wallpaper.StaticWallpapers

@Composable
fun HomeScreen(nav: Nav) {
    val applied by Repository.appliedTheme.collectAsStateWithLifecycle()
    val mode by com.migux97.simon.data.StudioStore.modeFlow.collectAsStateWithLifecycle()
    val popular = remember {
        Catalog.widgetTemplates.filter { it.size != WidgetSize.LARGE }.filterIndexed { i, _ -> i % 4 == 0 }.take(12)
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Column(
                Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    "Simón",
                    style = TextStyle(brush = SimonGradient, fontSize = 40.sp, fontWeight = FontWeight.Black)
                )
                Text("Hacé tu pantalla única", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { MyScreenNowCard(nav) }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickAction("Widgets", Icons.Filled.Widgets, Color(0xFF8B6CFF), Modifier.weight(1f)) { nav.tab("widgets") }
                QuickAction("Fondos", Icons.Filled.Wallpaper, Color(0xFF1FA2FF), Modifier.weight(1f)) { nav.tab("wallpapers") }
                QuickAction("Íconos", Icons.Filled.Apps, Color(0xFFFF7E5F), Modifier.weight(1f)) { nav.go("icons") }
                QuickAction("Efectos", Icons.Filled.Brush, Color(0xFFFF4F81), Modifier.weight(1f)) { nav.go("effects") }
            }
        }
        item {
            Text(
                "Todo esto funciona con el launcher que ya tenés.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }
        item {
            ModeCard(
                title = "Capa sobre tu launcher",
                text = "Stickers, widgets animados y dibujos flotando sobre tu pantalla de inicio de siempre, sin reemplazarla y sin bloquear los toques.",
                action = if (mode.overlayEnabled) "Activa · Editar →" else "Probar →",
                brush = Brush.linearGradient(listOf(Color(0xFFFF7E5F), Color(0xFFFF4F81))),
                onClick = { nav.go("overlay") }
            )
        }
        item {
            ModeCard(
                title = if (mode.launcherEnabled) "Simón Launcher · activado" else "Simón Launcher (opcional)",
                text = if (mode.launcherEnabled) "Tu pantalla de inicio sin límites: íconos y widgets donde quieras, superpuestos, con movimiento y gestos."
                else "Solo si querés reemplazar tu pantalla de inicio. Si no lo activás, Simón ni siquiera aparece como launcher y no consume nada.",
                action = if (mode.launcherEnabled) "Configurar →" else "Ver qué ofrece →",
                brush = SimonGradient,
                onClick = { nav.go("launcherSettings") }
            )
        }
        item {
            ModeCard(
                title = "Más funciones",
                text = "Rutinas automáticas, fondos que cambian solos, pantalla de carga y de bloqueo, barra lateral, burbuja flotante, sonidos por app y copias para compartir tu tema.",
                action = "Abrir →",
                brush = Brush.linearGradient(listOf(Color(0xFF1FA2FF), Color(0xFF12D8FA))),
                onClick = { nav.go("tools") }
            )
        }
        item { SectionHeader("Temas completos") }
        item {
            var themeCat by rememberSaveable { mutableStateOf<String?>(null) }
            val groups = remember { com.migux97.simon.data.Categories.group(Catalog.themes) { com.migux97.simon.data.Categories.of(it) } }
            val shown = themeCat?.let { c -> groups.firstOrNull { it.first.name == c }?.second } ?: Catalog.themes
            // Todas las categorías arriba; tocás una y la fila muestra solo esos temas.
            ChoiceChips(
                options = listOf<com.migux97.simon.data.Category?>(null) + groups.map { it.first },
                selected = themeCat?.let { com.migux97.simon.data.Category.valueOf(it) },
                label = { it?.let { c -> "${c.emoji} ${c.label}" } ?: "Todos" },
                onSelect = { themeCat = it?.name },
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(shown, key = { it.id }) { theme ->
                    ThemeCard(theme, applied == theme.id) { nav.theme(theme.id) }
                }
            }
        }
        item { SectionHeader("Widgets populares", "Ver todos", onAction = { nav.tab("widgets") }) }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(popular, key = { it.id }) { d ->
                    WidgetOnBackdrop(
                        d,
                        Modifier
                            .width(if (d.size == WidgetSize.SMALL) 150.dp else 290.dp)
                            .height(150.dp)
                            .clickable { nav.editor(d.id) }
                    )
                }
            }
        }
        item { SectionHeader("Fondos animados", "Ver todos", onAction = { nav.tab("wallpapers") }) }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(Catalog.liveWallpapers, key = { it.id }) { lv ->
                    Column(Modifier.width(120.dp)) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(9f / 18f)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { nav.live(lv.id) }
                        ) {
                            SceneThumb(lv, Modifier.fillMaxSize())
                            GradientBadge("En vivo", SimonGradient, Modifier.align(Alignment.TopStart).padding(8.dp))
                        }
                        Text(lv.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, modifier = Modifier.padding(top = 6.dp, start = 4.dp))
                    }
                }
            }
        }
        item { SectionHeader("Fondos de pantalla") }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(Catalog.staticWallpapers.take(16), key = { it.id }) { sw ->
                    Box(
                        Modifier
                            .width(110.dp)
                            .aspectRatio(9f / 18f)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { nav.static(sw.id) }
                    ) { StaticThumb(sw, Modifier.fillMaxSize()) }
                }
            }
        }
        item {
            Spacer(Modifier.height(12.dp))
            Panel {
                Text("¿Cómo agrego un widget?", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Elegí un diseño, personalizalo y tocá «Agregar a inicio». Si tu launcher no lo permite, mantené presionada la pantalla de inicio → Widgets → Simón, y elegí el diseño.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.14f))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(color),
            contentAlignment = Alignment.Center
        ) { Icon(icon, contentDescription = null, tint = Color.White) }
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
fun ThemeCard(theme: ThemePack, applied: Boolean, modifier: Modifier = Modifier.width(220.dp), onClick: () -> Unit) {
    Column(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 17f)
                .clip(RoundedCornerShape(24.dp))
                .clickable(onClick = onClick)
        ) {
            StaticThumb(theme.wallpaper, Modifier.fillMaxSize())
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Spacer(Modifier.height(18.dp))
                theme.widgets.firstOrNull()?.let { WidgetPreview(it, Modifier.fillMaxWidth()) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    theme.widgets.drop(1).filter { it.size == WidgetSize.SMALL }.take(2).forEach {
                        WidgetPreview(it, Modifier.weight(1f))
                    }
                }
            }
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))))
                    .padding(12.dp)
            ) {
                Column {
                    Text(theme.name, color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Text(theme.description, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
            }
            if (applied) GradientBadge("Aplicado", SimonGradient, Modifier.align(Alignment.TopEnd).padding(10.dp))
        }
    }
}

@Composable
private fun ModeCard(title: String, text: String, action: String, brush: Brush, onClick: () -> Unit) {
    Column(
        Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(brush)
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge)
        Text(text, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium)
        Text(action, color = Color.White, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
    }
}

/** "Tu pantalla ahora": lo que ya tenés puesto, para seguir editando desde ahí. */
@Composable
private fun MyScreenNowCard(nav: Nav) {
    val context = LocalContext.current
    val applied by Repository.appliedTheme.collectAsStateWithLifecycle()
    val bindings by Repository.bindings.collectAsStateWithLifecycle()
    val icons by Repository.icons.collectAsStateWithLifecycle()
    val live by Repository.live.collectAsStateWithLifecycle()
    val current by Repository.currentStatic.collectAsStateWithLifecycle()
    var resumeTick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumeTick++ }
    val liveActive = remember(resumeTick) { LiveWallpaper.isActive(context) }
    val staticOnHome = remember(resumeTick, current) { StaticWallpapers.isOnHomeNow(context, current) }
    val themeName = applied?.let { id -> Lookup.theme(id)?.name }
    val widgetCount = bindings.size
    val iconCount = remember(icons) { icons.distinctBy { it.packageName }.size }
    val activeLive = if (liveActive) live else null
    val staticNow = if (!liveActive && staticOnHome) current else null

    val staticWp = staticNow?.staticWallpaper
    val photoNow = staticNow?.photoPath
    val wallpaperText = when {
        activeLive != null -> "Animado de Simón: «${activeLive.name}»"
        staticWp != null -> "Estático de Simón: «${staticWp.name}»"
        photoNow != null -> "Una foto tuya, puesta con Simón"
        else -> "Tu fondo de siempre (no es de Simón)"
    }
    val appliedId = applied
    val onEditTheme: (() -> Unit)? = appliedId?.let { tid -> { nav.theme(tid) } }

    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(70.dp)
                .aspectRatio(9f / 18f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black)
                .clickable { if (activeLive != null) nav.live(activeLive.id) else nav.go("wallEdit/home") }
        ) {
            if (activeLive != null) {
                if (activeLive.scene == SceneType.VIDEO) {
                    Icon(Icons.Filled.Movie, contentDescription = null, tint = Color.White, modifier = Modifier.align(Alignment.Center).size(28.dp))
                } else {
                    SceneThumb(activeLive, Modifier.fillMaxSize())
                }
            } else {
                // Tu fondo real (con permiso) o sus colores; se vuelve a leer al volver a la app.
                com.migux97.simon.ui.components.RealWallpaperThumb(false, Modifier.fillMaxSize(), resumeTick)
            }
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text("Tu pantalla ahora", style = MaterialTheme.typography.titleMedium)
            ScreenNowLine("Fondo", wallpaperText) { if (activeLive != null) nav.live(activeLive.id) else nav.go("wallEdit/home") }
            ScreenNowLine("Tema", themeName ?: "Ninguno aplicado", onEditTheme)
            ScreenNowLine(
                "Widgets",
                when (widgetCount) {
                    0 -> "Ninguno de Simón en tu inicio"
                    1 -> "1 de Simón en tu inicio"
                    else -> "$widgetCount de Simón en tu inicio"
                }
            ) { nav.tab("widgets") }
            ScreenNowLine(
                "Íconos",
                when (iconCount) {
                    0 -> "Ninguno personalizado"
                    1 -> "1 app personalizada"
                    else -> "$iconCount apps personalizadas"
                }
            ) { nav.go("icons") }
        }
    }
}

@Composable
private fun ScreenNowLine(title: String, value: String, onEdit: (() -> Unit)?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
        if (onEdit != null) TextButton(onClick = onEdit) { Text("Editar") }
    }
}
