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
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.migux97.simon.data.Catalog
import com.migux97.simon.data.Repository
import com.migux97.simon.data.ThemePack
import com.migux97.simon.data.WidgetSize
import com.migux97.simon.ui.Nav
import com.migux97.simon.ui.components.GradientBadge
import com.migux97.simon.ui.components.Panel
import com.migux97.simon.ui.components.SceneThumb
import com.migux97.simon.ui.components.SectionHeader
import com.migux97.simon.ui.components.StaticThumb
import com.migux97.simon.ui.components.WidgetOnBackdrop
import com.migux97.simon.ui.components.WidgetPreview
import com.migux97.simon.ui.theme.SimonGradient

@Composable
fun HomeScreen(nav: Nav) {
    val applied by Repository.appliedTheme.collectAsStateWithLifecycle()
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
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickAction("Widgets", Icons.Filled.Widgets, Color(0xFF8B6CFF), Modifier.weight(1f)) { nav.tab("widgets") }
                QuickAction("Fondos", Icons.Filled.Wallpaper, Color(0xFF1FA2FF), Modifier.weight(1f)) { nav.tab("wallpapers") }
                QuickAction("Íconos", Icons.Filled.Apps, Color(0xFFFF7E5F), Modifier.weight(1f)) { nav.tab("icons") }
                QuickAction("Mío", Icons.Filled.Brush, Color(0xFFFF4F81), Modifier.weight(1f)) { nav.tab("mine") }
            }
        }
        item { SectionHeader("Temas completos") }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(Catalog.themes, key = { it.id }) { theme ->
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
