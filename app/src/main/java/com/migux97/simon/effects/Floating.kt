package com.migux97.simon.effects

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.migux97.simon.custom.StickerLayer
import com.migux97.simon.data.BubbleSettings
import com.migux97.simon.data.LauncherSettings
import com.migux97.simon.data.PanelTile
import com.migux97.simon.data.QuickShortcut
import com.migux97.simon.data.StickerAttach
import com.migux97.simon.data.StudioStore
import com.migux97.simon.launcher.AppCatalog
import com.migux97.simon.launcher.AppIconBitmap
import com.migux97.simon.launcher.SystemActions

private sealed interface GridEntry {
    val label: String
    data class App(val app: com.migux97.simon.launcher.LaunchApp, override val label: String) : GridEntry
    data class Link(val sc: QuickShortcut, override val label: String) : GridEntry
    data class Tool(val tile: PanelTile, override val label: String) : GridEntry
}

@Composable
private fun GridCell(entry: GridEntry, launcher: LauncherSettings, textColor: Color, service: SimonAccessibilityService, onDone: () -> Unit) {
    val context = LocalContext.current
    Column(
        Modifier
            .width(66.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable {
                when (entry) {
                    is GridEntry.App -> { onDone(); AppCatalog.launch(context, entry.app) }
                    is GridEntry.Link -> { onDone(); SystemActions.run(context, entry.sc.action, entry.sc.data) }
                    is GridEntry.Tool -> {
                        runTile(context, entry.tile, service)
                        if (entry.tile != PanelTile.FLASHLIGHT && entry.tile != PanelTile.SOUND) onDone()
                    }
                }
            }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
            when (entry) {
                is GridEntry.App -> AppIconBitmap(entry.app, launcher, 46, Modifier.size(46.dp))
                is GridEntry.Link -> Box(Modifier.size(42.dp).clip(CircleShape).background(textColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Text(entry.label.take(1).uppercase(), color = textColor, fontSize = 18.sp)
                }
                is GridEntry.Tool -> Box(
                    Modifier.size(42.dp).clip(CircleShape)
                        .background(if (tileActive(context, entry.tile)) Color(0xFF8B6CFF) else textColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) { Icon(tileIcon(entry.tile), entry.label, tint = textColor) }
            }
        }
        Text(entry.label, color = textColor, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun GridSection(title: String, entries: List<GridEntry>, cols: Int, launcher: LauncherSettings, textColor: Color, service: SimonAccessibilityService, onDone: () -> Unit) {
    if (entries.isEmpty()) return
    Text(title, color = textColor.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
    entries.chunked(cols).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            row.forEach { GridCell(it, launcher, textColor, service, onDone) }
        }
    }
}

/** Grilla de apps, atajos y herramientas compartida por la barra lateral y la burbuja. */
@Composable
fun ActionGrid(
    apps: List<String>,
    shortcuts: List<QuickShortcut>,
    tools: List<PanelTile>,
    columns: Int,
    textColor: Color,
    service: SimonAccessibilityService,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val launcher by StudioStore.launcherFlow.collectAsStateWithLifecycle()
    val cols = columns.coerceIn(1, 5)
    val appEntries = apps.mapNotNull { AppCatalog.find(it) }.map { GridEntry.App(it, AppCatalog.label(it)) }
    val shortEntries = shortcuts.map { GridEntry.Link(it, it.label) }
    val toolEntries = tools.map { GridEntry.Tool(it, tileLabel(context, it)) }
    Column {
        GridSection("Apps", appEntries, cols, launcher, textColor, service, onDone)
        GridSection("Atajos", shortEntries, cols, launcher, textColor, service, onDone)
        GridSection("Herramientas", toolEntries, cols, launcher, textColor, service, onDone)
        if (appEntries.isEmpty() && shortEntries.isEmpty() && toolEntries.isEmpty()) {
            Text("Agregá apps y atajos desde Simón → Más funciones.", color = textColor, fontSize = 13.sp)
        }
    }
}

/** Barra lateral que se abre desde el borde. */
@Composable
fun EdgePanelContent(service: SimonAccessibilityService, onClose: () -> Unit) {
    val s by StudioStore.edgeFlow.collectAsStateWithLifecycle()
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable(onClick = onClose),
        contentAlignment = if (s.rightSide) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        AnimatedVisibility(visibleState = visible, enter = slideInHorizontally { if (s.rightSide) it else -it } + fadeIn()) {
            Column(
                Modifier
                    .fillMaxHeight(0.8f)
                    .padding(horizontal = 8.dp)
                    .shadow(12.dp, RoundedCornerShape(28.dp))
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.verticalGradient(listOf(Color(s.bg1), Color(s.bg2))))
                    .clickable(enabled = false) { }
                    .pointerInput(s.rightSide) {
                        var total = 0f
                        detectDragGestures(onDragStart = { total = 0f }, onDrag = { _, d -> total += d.x }, onDragEnd = {
                            if ((s.rightSide && total > 80f) || (!s.rightSide && total < -80f)) onClose()
                        })
                    }
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp)
            ) {
                Spacer(Modifier.statusBarsPadding())
                ActionGrid(s.apps, s.shortcuts, s.tools, s.columns, Color(s.textColor), service, onClose)
            }
        }
    }
}

/** El círculo flotante de la burbuja. */
@Composable
fun BubbleFace(s: BubbleSettings, onTap: () -> Unit, onLongPress: () -> Unit, onDrag: (Offset) -> Unit, onDragEnd: () -> Unit) {
    Box(
        Modifier
            .size(s.sizeDp.dp)
            .pointerInput(Unit) { detectTapGestures(onTap = { onTap() }, onLongPress = { onLongPress() }) }
            .pointerInput(Unit) { detectDragGestures(onDrag = { c, d -> c.consume(); onDrag(d) }, onDragEnd = { onDragEnd() }) }
            .padding(4.dp)
            .shadow(8.dp, CircleShape)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color(s.color1).copy(alpha = s.alpha), Color(s.color2).copy(alpha = s.alpha)))),
        contentAlignment = Alignment.Center
    ) {
        val sticker = s.stickerId
        if (sticker != null) StickerLayer(listOf(StickerAttach(stickerId = sticker, x = 0.5f, y = 0.5f, size = 0.8f)), Modifier.fillMaxSize())
        else Text("S", color = Color.White, fontSize = (s.sizeDp / 2.4f).sp)
    }
}

/** Menú que se abre al tocar la burbuja. */
@Composable
fun BubbleMenuContent(service: SimonAccessibilityService, onClose: () -> Unit) {
    val s by StudioStore.bubbleFlow.collectAsStateWithLifecycle()
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)).clickable(onClick = onClose),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(visibleState = visible, enter = scaleIn() + fadeIn()) {
            Column(
                Modifier
                    .padding(24.dp)
                    .shadow(16.dp, RoundedCornerShape(30.dp))
                    .clip(RoundedCornerShape(30.dp))
                    .background(Brush.linearGradient(listOf(Color(0xF2140F24), Color(0xF22A1B5C))))
                    .clickable(enabled = false) { }
                    .padding(18.dp)
            ) {
                ActionGrid(s.apps, s.shortcuts, s.tools, 4, Color.White, service, onClose)
            }
        }
    }
}
