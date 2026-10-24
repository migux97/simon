package com.migux97.simon.canva

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.migux97.simon.custom.CustomWidgetLive
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.migux97.simon.data.Catalog
import com.migux97.simon.data.Categories
import com.migux97.simon.data.Category
import com.migux97.simon.ui.components.CategoryHeader
import com.migux97.simon.ui.components.CategoryTiles
import com.migux97.simon.data.CustomWidget
import com.migux97.simon.data.LiveConfig
import com.migux97.simon.data.MediaLibrary
import com.migux97.simon.data.StudioStore
import com.migux97.simon.launcher.AnimatedFrames
import com.migux97.simon.ui.components.PhoneWallpaperBackdrop
import com.migux97.simon.ui.components.SceneThumb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val ThumbBg = Brush.linearGradient(listOf(Color(0xFF2A1B5C), Color(0xFF6C4CF5)))

/** Miniatura en vivo de un bloque (texto, forma, gráfico). */
@Composable
fun BlockThumb(block: ElementBlock, modifier: Modifier = Modifier) {
    val preview = remember(block) { CustomWidget(id = "thumb_" + block.label, bgAlpha = 0f, corner = 0f, elements = block.elements) }
    Box(modifier.clip(RoundedCornerShape(14.dp)).background(ThumbBg), contentAlignment = Alignment.Center) {
        // [ElementBlock.hW] es relativo al ancho del lienzo: la forma del bloque es w / hW.
        Box(Modifier.fillMaxWidth(0.84f).aspectRatio((block.w / block.hW).coerceIn(0.3f, 8f))) {
            CustomWidgetLive(preview, Modifier.fillMaxSize(), interactive = false)
        }
    }
}

@Composable
private fun Labeled(label: String, onClick: () -> Unit, thumb: @Composable () -> Unit) {
    Column(
        Modifier.width(84.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        thumb()
        Text(label, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BlockGrid(blocks: List<ElementBlock>, onAdd: (ElementBlock) -> Unit) {
    FlowRow(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        blocks.forEach { b -> Labeled(b.label, { onAdd(b) }) { BlockThumb(b, Modifier.size(76.dp)) } }
    }
}

/** Panel «Texto»: títulos, datos en vivo y combinaciones. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TextPanel(showLive: Boolean = true, onAdd: (ElementBlock) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CanvaPresets.textStyles.forEachIndexed { i, b ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().clickable { onAdd(b) }
            ) {
                Text(
                    b.label,
                    fontSize = listOf(24.sp, 18.sp, 14.sp)[i],
                    fontWeight = listOf(FontWeight.Black, FontWeight.Bold, FontWeight.Normal)[i],
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }
    if (showLive) {
        CanvaSection("Datos que se actualizan solos")
        FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CanvaPresets.liveTexts.forEach { b -> AssistChip(onClick = { onAdd(b) }, label = { Text(b.label) }) }
        }
    }
    CanvaSection("Combinaciones")
    BlockGrid(CanvaPresets.textCombos, onAdd)
}

/**
 * Primero las categorías; al tocar una se ve lo que tiene y una flecha para
 * volver. [key] separa el estado de cada panel.
 */
@Composable
fun <T> CategoryPanel(key: String, groups: List<Pair<Category, List<T>>>, content: @Composable (List<T>) -> Unit) {
    var openName by rememberSaveable(key) { mutableStateOf<String?>(null) }
    val open = openName?.let { n -> groups.firstOrNull { it.first.name == n } }
    if (open == null) {
        CategoryTiles(groups.map { it.first to it.second.size }, onPick = { openName = it.name })
    } else {
        CategoryHeader(open.first, open.second.size, onBack = { openName = null }, modifier = Modifier.padding(horizontal = 4.dp))
        content(open.second)
    }
}

/** Formas, gráficos (batería, progreso, reloj) y stickers, por categoría. */
@Composable
fun ShapesAndGraphics(onAdd: (ElementBlock) -> Unit) {
    val groups = remember { Categories.group(CanvaPresets.elements) { it.cats } }
    CategoryPanel("elements", groups) { list -> BlockGrid(list, onAdd) }
}

@Composable
fun ScenesRow(onPick: (LiveConfig) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(Catalog.liveWallpapers, key = { it.id }) { lv ->
            Column(Modifier.width(64.dp).clickable { onPick(lv) }, horizontalAlignment = Alignment.CenterHorizontally) {
                SceneThumb(lv, Modifier.width(56.dp).height(92.dp).clip(RoundedCornerShape(12.dp)))
                Text(lv.name, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Tus dibujos y animaciones, para agregarlos al diseño. */
@Composable
fun MyMediaRows(onDrawing: (String) -> Unit, onAnimation: (String) -> Unit) {
    val drawings by StudioStore.drawingsFlow.collectAsStateWithLifecycle()
    val animations by StudioStore.animationsFlow.collectAsStateWithLifecycle()
    CanvaSection("Mis dibujos")
    if (drawings.isEmpty()) Hint("Todavía no tenés dibujos. Creá uno en Crear → Dibujo.")
    else LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(drawings, key = { it.id }) { d ->
            val bmp by produceState<android.graphics.Bitmap?>(null, d.id, d.updatedAt) { value = withContext(Dispatchers.IO) { MediaLibrary.loadDrawing(d.id, 200) } }
            Box(Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x22888888)).clickable { onDrawing(d.id) }) {
                bmp?.let { Image(it.asImageBitmap(), d.name, Modifier.fillMaxSize()) }
            }
        }
    }
    CanvaSection("Mis animaciones")
    if (animations.isEmpty()) Hint("Todavía no tenés animaciones. Creá una en Crear → Animación.")
    else LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(animations, key = { it.id }) { a ->
            Box(Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x22888888)).clickable { onAnimation(a.id) }) {
                AnimatedFrames(a.id, Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun Hint(text: String) = Text(
    text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
)

@Composable
fun WidgetTemplateGrid(onPick: (CustomWidget) -> Unit) {
    val groups = remember { Categories.group(WidgetTemplates.all) { WidgetTemplates.categoriesOf(it) } }
    Hint("Elegí una categoría y tocá una plantilla. Después cambiás lo que quieras: textos, colores, tamaño.")
    CategoryPanel("widget_tpl", groups) { list -> WidgetTemplateList(list, onPick) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WidgetTemplateList(list: List<CustomWidget>, onPick: (CustomWidget) -> Unit) {
    FlowRow(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        list.forEach { t ->
            Column(Modifier.width(156.dp).clip(RoundedCornerShape(14.dp)).clickable { onPick(t) }.padding(4.dp)) {
                Box(Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(Color(0x22888888)))
                    Box(if (t.aspect >= 1.6f) Modifier.fillMaxWidth(0.94f).aspectRatio(t.aspect) else Modifier.height(86.dp).aspectRatio(t.aspect)) {
                        CustomWidgetLive(t, Modifier.fillMaxSize(), interactive = false)
                    }
                }
                Text(t.name, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
            }
        }
    }
}

@Composable
fun PageTemplateGrid(lock: Boolean, onPick: (PageTemplate) -> Unit) {
    val groups = remember { Categories.group(PageTemplates.all) { it.cats } }
    Hint("Diseños para la pantalla completa. Elegí una categoría y tocá uno para sumarlo.")
    CategoryPanel("page_tpl", groups) { list -> PageTemplateList(list, lock, onPick) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PageTemplateList(list: List<PageTemplate>, lock: Boolean, onPick: (PageTemplate) -> Unit) {
    FlowRow(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        list.forEach { t ->
            val preview = remember(t) { CustomWidget(id = "tpl_" + t.name, bgAlpha = 0f, corner = 0f, elements = t.elements) }
            Column(Modifier.width(96.dp).clip(RoundedCornerShape(14.dp)).clickable { onPick(t) }.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                PhoneWallpaperBackdrop(Modifier.fillMaxWidth().aspectRatio(0.46f).clip(RoundedCornerShape(12.dp)), lock = lock) {
                    CustomWidgetLive(preview, Modifier.fillMaxSize(), interactive = false)
                }
                Text(t.name, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
            }
        }
    }
}
