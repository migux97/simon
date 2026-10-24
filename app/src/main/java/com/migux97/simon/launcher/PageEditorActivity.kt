package com.migux97.simon.launcher

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.FlipToFront
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.migux97.simon.data.ItemType
import com.migux97.simon.data.LauncherBgType
import com.migux97.simon.data.Lookup
import com.migux97.simon.data.Repository
import com.migux97.simon.data.StudioStore
import com.migux97.simon.ui.components.ConfirmDialog
import com.migux97.simon.ui.components.HelpTip
import com.migux97.simon.ui.components.LocalAnimationsActive
import com.migux97.simon.ui.components.ScenePreview
import com.migux97.simon.ui.components.rememberAnimationsAllowed
import com.migux97.simon.ui.theme.SimonTheme
import com.migux97.simon.widget.Draw
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Editor a pantalla completa de una capa libre: la capa sobre tu launcher,
 * la pantalla de bloqueo o la de carga. Se edita a tamaño real y, detrás,
 * se ve tu fondo de pantalla de verdad (el tema muestra el wallpaper), así
 * trabajás a partir de lo que ya tiene tu teléfono.
 */
class PageEditorActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Repository.init(this)
        enableEdgeToEdge()
        val requested = intent?.getIntExtra(EXTRA_PAGE, OVERLAY_PAGE) ?: OVERLAY_PAGE
        val page = if (requested == OVERLAY_PAGE || requested == LOCK_PAGE || requested == CHARGE_PAGE) requested else OVERLAY_PAGE
        setContent { SimonTheme { PageEditorRoot(page, onDone = { finish() }) } }
        // La pantalla de bloqueo y la de carga se ven sin barra de estado: igual acá, para medir bien.
        if (page != OVERLAY_PAGE) {
            val ctrl = WindowCompat.getInsetsController(window, window.decorView)
            ctrl.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            ctrl.hide(WindowInsetsCompat.Type.statusBars())
        }
    }

    companion object {
        /** Extra Int con la capa a editar: [OVERLAY_PAGE], [LOCK_PAGE] o [CHARGE_PAGE]. */
        const val EXTRA_PAGE = "page"

        fun intent(context: Context, page: Int): Intent {
            val i = Intent(context, PageEditorActivity::class.java).putExtra(EXTRA_PAGE, page)
            if (context !is Activity) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return i
        }
    }
}

private fun pageTitle(page: Int) = when (page) {
    LOCK_PAGE -> "Pantalla de bloqueo"
    CHARGE_PAGE -> "Pantalla de carga"
    else -> "Capa sobre tu launcher"
}

@Composable
private fun PageEditorRoot(page: Int, onDone: () -> Unit) {
    val context = LocalContext.current
    val items by StudioStore.homeItemsFlow.collectAsStateWithLifecycle()
    val settings by StudioStore.launcherFlow.collectAsStateWithLifecycle()
    val ui = remember {
        LauncherUi().also { u ->
            u.editMode = true
            // «Deshacer» solo toca lo de esta capa.
            u.undoScope = { item -> item.page == page }
        }
    }
    val allowed = rememberAnimationsAllowed()
    val prefs = remember { context.getSharedPreferences("simon_hints", Context.MODE_PRIVATE) }
    var showTip by remember { mutableStateOf(!prefs.getBoolean("page_editor", false)) }
    var askClear by remember { mutableStateOf(false) }
    val pageItems = items.filter { it.page == page }.sortedBy { it.z }
    val selected = pageItems.firstOrNull { it.id == ui.selectedId }

    BackHandler {
        if (ui.selectedId != null) ui.selectedId = null else onDone()
    }

    CompositionLocalProvider(LocalAnimationsActive provides allowed) {
        Box(Modifier.fillMaxSize()) {
            PageBackground(page)
            // Tocar un lugar vacío quita la selección.
            Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onTap = { ui.selectedId = null }) })
            PageCanvas(page, pageItems, settings, ui)
            PageReference(page)

            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xCC1C1530))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    val n = pageItems.size
                    Text(
                        "Editando: ${pageTitle(page)} · " + if (n == 1) "1 elemento" else "$n elementos",
                        color = Color.White, fontSize = 13.sp
                    )
                }
                if (showTip) {
                    HelpTip("Arrastrá para mover · tocá para elegir · usá los círculos para agrandar o girar", Modifier.padding(top = 6.dp))
                    TextButton(onClick = {
                        showTip = false
                        try { prefs.edit().putBoolean("page_editor", true).apply() } catch (_: Exception) {}
                    }) { Text("Entendido", color = Color.White) }
                } else if (pageItems.isEmpty()) {
                    HelpTip("Todavía no pusiste nada. Tocá «Agregar» abajo para sumar stickers, widgets, dibujos o animaciones.", Modifier.padding(top = 6.dp))
                }
            }

            // Barra de herramientas abajo, por encima de la barra de navegación.
            Surface(
                color = Color(0xEE1C1530),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            ) {
                Row(
                    Modifier.navigationBarsPadding().padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        BarButton(Icons.Filled.Add, "Agregar") { ui.addSheet = true }
                        if (selected != null) {
                            BarButton(Icons.Filled.Tune, "Editar") { ui.editItem = selected }
                            BarButton(Icons.Filled.ContentCopy, "Duplicar") {
                                if (selected.type == ItemType.SYSTEM_WIDGET) {
                                    Toast.makeText(context, "Los widgets de otras apps no se pueden duplicar", Toast.LENGTH_SHORT).show()
                                } else {
                                    ui.snapshot(); LauncherLayout.duplicate(selected)
                                }
                            }
                            BarButton(Icons.Filled.FlipToFront, "Traer al frente") { ui.snapshot(); LauncherLayout.bringToFront(selected) }
                            BarButton(Icons.Filled.FlipToBack, "Mandar al fondo") { ui.snapshot(); LauncherLayout.sendToBack(selected) }
                            BarButton(Icons.Filled.Delete, "Quitar") { ui.remove(selected) }
                        }
                        BarButton(Icons.AutoMirrored.Filled.Undo, "Deshacer", enabled = ui.undo.canUndo) { ui.undoLast() }
                        BarButton(Icons.Filled.RestartAlt, "Empezar de cero", enabled = pageItems.isNotEmpty()) { askClear = true }
                    }
                    BarButton(Icons.Filled.Check, "Listo") { onDone() }
                }
            }
        }
    }

    if (ui.addSheet) AddSheet(ui, settings, page, onPickSystemWidget = { _, _ -> }, overlay = true)
    ui.editItem?.let { ItemEditorSheet(it, settings, ui) }
    if (askClear) ConfirmDialog(
        title = "¿Empezar de cero?",
        message = "Se quita todo lo que pusiste en la ${pageTitle(page).lowercase()}. Si te arrepentís, tocá «Deshacer».",
        confirm = "Sí, empezar de cero",
        onDismiss = { askClear = false },
        onConfirm = {
            askClear = false
            ui.snapshot()
            ui.selectedId = null
            LauncherLayout.clearPage(context, page)
        }
    )
}

/** Lo que se ve detrás de la capa en la pantalla real. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PageBackground(page: Int) {
    when (page) {
        LOCK_PAGE -> {
            val s by StudioStore.lockFlow.collectAsStateWithLifecycle()
            val pager = rememberPagerState { 1 }
            // Con «Fondo del sistema» se ve el fondo real detrás, apenas oscurecido como en el bloqueo.
            Box(Modifier.fillMaxSize().background(if (s.background.type == LauncherBgType.SYSTEM) Color.Black.copy(alpha = 0.15f) else Color.Black))
            LauncherBackgroundLayer(s.background.copy(parallax = false), pager)
        }
        CHARGE_PAGE -> {
            val s by StudioStore.chargingFlow.collectAsStateWithLifecycle()
            Box(Modifier.fillMaxSize().background(Color.Black))
            if (!s.blackBackground) s.sceneId?.let { Lookup.live(it) }?.let { ScenePreview(it, Modifier.fillMaxSize()) }
            if (s.dim > 0f) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = s.dim.coerceIn(0f, 0.95f))))
        }
        else -> Unit // La capa del launcher: se ve tu fondo de pantalla real.
    }
}

/**
 * Referencia semitransparente del reloj (y la batería en la de carga) para no
 * taparlos. No toma toques: se puede arrastrar lo que está debajo.
 */
@Composable
private fun PageReference(page: Int) {
    if (page != LOCK_PAGE && page != CHARGE_PAGE) return
    val context = LocalContext.current
    var now by remember { mutableStateOf(Date()) }
    var level by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        val bm = context.getSystemService(BatteryManager::class.java)
        while (true) {
            now = Date()
            level = try { bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 0 } catch (_: Exception) { 0 }
            delay(20_000)
        }
    }
    val locale = remember { Locale("es", "AR") }
    val time = SimpleDateFormat("HH:mm", locale).format(now)
    val date = SimpleDateFormat("EEEE d 'de' MMMM", locale).format(now).replaceFirstChar { it.titlecase(locale) }

    Box(Modifier.fillMaxSize().graphicsLayer { alpha = 0.45f }) {
        if (page == LOCK_PAGE) {
            val s by StudioStore.lockFlow.collectAsStateWithLifecycle()
            Column(
                Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(48.dp))
                Text(time, color = Color(s.clockColor), fontSize = s.clockSize.sp, fontFamily = FontFamily(Draw.typeface(s.clockFont)))
                if (s.showDate) Text(date, color = Color(s.clockColor), fontSize = 18.sp)
            }
        } else {
            val s by StudioStore.chargingFlow.collectAsStateWithLifecycle()
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(time, color = Color(s.clockColor), fontSize = 96.sp, fontFamily = FontFamily(Draw.typeface(s.clockFont)))
                Text(date, color = Color(s.clockColor).copy(alpha = 0.8f), fontSize = 18.sp)
                if (s.showBattery) {
                    Spacer(Modifier.height(36.dp))
                    Box(
                        Modifier.size(150.dp).border(12.dp, Color(s.accent), CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Text("$level%", color = Color.White, fontSize = 34.sp) }
                }
            }
        }
    }
}
