package com.migux97.simon.launcher

import android.app.Activity
import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.migux97.simon.ui.components.LocalAnimationsActive
import com.migux97.simon.ui.components.rememberAnimationsAllowed
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.migux97.simon.data.GestureAction
import com.migux97.simon.data.HomeItem
import com.migux97.simon.data.ItemType
import com.migux97.simon.data.PageTransition
import com.migux97.simon.data.Repository
import com.migux97.simon.data.StudioStore
import com.migux97.simon.ui.components.UndoStack
import com.migux97.simon.ui.theme.SimonTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

/** Estado de la interfaz del launcher compartido entre sus partes. */
class LauncherUi {
    var editMode by mutableStateOf(false)
    var selectedId by mutableStateOf<String?>(null)
    var drawerOpen by mutableStateOf(false)
    var drawerSearch by mutableStateOf(false)
    var openFolder by mutableStateOf<HomeItem?>(null)
    var addSheet by mutableStateOf(false)
    var editItem by mutableStateOf<HomeItem?>(null)
    var appMenu by mutableStateOf<LaunchApp?>(null)
    var appMenuItem by mutableStateOf<HomeItem?>(null)
    var iconEditor by mutableStateOf<LaunchApp?>(null)
    var dockEditor by mutableStateOf(false)
    var currentPage by mutableIntStateOf(0)

    /** Historial para «Deshacer»: copias de la lista de elementos antes de cada cambio. */
    val undo = UndoStack<List<HomeItem>>()

    /** Qué elementos toca «Deshacer» (por defecto, solo las páginas del launcher). */
    var undoScope: (HomeItem) -> Boolean = { it.page >= 0 }

    /** Widgets del sistema quitados: se liberan al salir de la edición, por si se deshace. */
    private val removedWidgets = mutableListOf<Int>()

    /** Guarda cómo está todo antes de un cambio, para poder deshacerlo. */
    fun snapshot() = undo.push(StudioStore.items)

    fun undoLast() {
        val prev = undo.pop() ?: return
        val scope = undoScope
        StudioStore.setItems(StudioStore.items.filterNot(scope) + prev.filter(scope))
    }

    /** Quita un elemento (se puede deshacer). */
    fun remove(item: HomeItem) {
        snapshot()
        if (item.type == ItemType.SYSTEM_WIDGET) removedWidgets += item.appWidgetId
        StudioStore.removeItem(item.id)
        if (selectedId == item.id) selectedId = null
    }

    /** Libera los widgets del sistema quitados que no volvieron con «Deshacer». */
    fun flushRemovedWidgets(context: Context) {
        if (removedWidgets.isEmpty()) return
        val inUse = StudioStore.items.map { it.appWidgetId }.toSet()
        removedWidgets.filter { it !in inUse }.forEach { WidgetHost.delete(context, it) }
        removedWidgets.clear()
    }

    fun closeAll(): Boolean {
        val anything = drawerOpen || openFolder != null || addSheet || editItem != null || appMenu != null || iconEditor != null || dockEditor || editMode
        drawerOpen = false; openFolder = null; addSheet = false; editItem = null; appMenu = null; iconEditor = null; dockEditor = false
        editMode = false; selectedId = null
        return anything
    }
}

class LauncherActivity : ComponentActivity() {

    private val homePressed = MutableStateFlow(0)
    private val editRequest = MutableStateFlow(0)
    private var pendingWidgetId = -1
    private var pendingPage = 0

    private val bindLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) configureOrAdd(pendingWidgetId) else WidgetHost.delete(this, pendingWidgetId)
    }
    private val configureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) addSystemWidget(pendingWidgetId) else WidgetHost.delete(this, pendingWidgetId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Repository.init(this)
        AppCatalog.ensureLoaded(this)
        enableEdgeToEdge()
        if (intent?.getBooleanExtra(EXTRA_EDIT, false) == true) editRequest.value = 1
        setContent { SimonTheme { LauncherRoot(homePressed, onPickSystemWidget = ::pickSystemWidget, editRequest = editRequest) } }
    }

    override fun onStart() {
        super.onStart()
        try { WidgetHost.get(this).startListening() } catch (_: Exception) {}
    }

    override fun onStop() {
        super.onStop()
        try { WidgetHost.get(this).stopListening() } catch (_: Exception) {}
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_EDIT, false)) editRequest.value += 1
        else if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) homePressed.value += 1
    }

    // ---- Widgets de otras apps ----

    private fun pickSystemWidget(info: AppWidgetProviderInfo, page: Int) {
        pendingPage = page
        val id = WidgetHost.allocate(this)
        pendingWidgetId = id
        val mgr = AppWidgetManager.getInstance(this)
        if (mgr.bindAppWidgetIdIfAllowed(id, info.provider)) {
            configureOrAdd(id)
        } else {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
            try { bindLauncher.launch(intent) } catch (e: Exception) { WidgetHost.delete(this, id) }
        }
    }

    private fun configureOrAdd(id: Int) {
        val info = AppWidgetManager.getInstance(this).getAppWidgetInfo(id)
        if (info?.configure != null) {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                .setComponent(info.configure)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            try {
                configureLauncher.launch(intent)
            } catch (e: Exception) {
                // Algunos widgets protegen su configuración: el host del sistema la abre.
                try {
                    WidgetHost.get(this).startAppWidgetConfigureActivityForResult(this, id, 0, 77, null)
                    addSystemWidget(id)
                } catch (_: Exception) {
                    addSystemWidget(id)
                }
            }
        } else addSystemWidget(id)
    }

    private fun addSystemWidget(id: Int) {
        val info = AppWidgetManager.getInstance(this).getAppWidgetInfo(id) ?: return
        val dm = resources.displayMetrics
        val wDp = dm.widthPixels / dm.density
        val hDp = dm.heightPixels / dm.density
        val w = (info.minWidth / dm.density / wDp).coerceIn(0.3f, 1f).let { if (it < 0.5f) 0.5f else it }
        val h = (info.minHeight / dm.density / hDp).coerceIn(0.1f, 0.6f)
        val s = StudioStore.launcherSettings
        val (x, y) = LauncherLayout.freeSpot(pendingPage, w, h, s)
        StudioStore.upsertItem(HomeItem(type = ItemType.SYSTEM_WIDGET, page = pendingPage, x = x, y = y, w = w, h = h, appWidgetId = id, z = StudioStore.maxZ(pendingPage) + 1))
        Toast.makeText(this, "Widget agregado. Arrastralo para moverlo.", Toast.LENGTH_SHORT).show()
    }

    companion object {
        /** Extra booleano: abrir el launcher directamente en modo edición. */
        const val EXTRA_EDIT = "edit"

        fun component(context: android.content.Context) = ComponentName(context, LauncherActivity::class.java)

        /** Abre el launcher listo para mover y editar cosas. */
        fun editIntent(context: android.content.Context): Intent =
            Intent(context, LauncherActivity::class.java).putExtra(EXTRA_EDIT, true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LauncherRoot(
    homePressed: MutableStateFlow<Int>,
    onPickSystemWidget: (AppWidgetProviderInfo, Int) -> Unit,
    editRequest: MutableStateFlow<Int> = MutableStateFlow(0)
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val settings by StudioStore.launcherFlow.collectAsStateWithLifecycle()
    val items by StudioStore.homeItemsFlow.collectAsStateWithLifecycle()
    val apps by AppCatalog.apps.collectAsStateWithLifecycle()
    val ui = remember { LauncherUi() }
    val pages = settings.pages.coerceIn(1, 12)
    val pager = rememberPagerState { pages }

    // Primera vez: preguntar cómo arrancar (con tus apps o en blanco) cuando ya cargaron las apps.
    var seeding by remember { mutableStateOf(false) }
    fun seed(withApps: Boolean) {
        if (seeding) return
        seeding = true
        scope.launch {
            withContext(Dispatchers.IO) { LauncherLayout.createDefault(context, withApps) }
            seeding = false
        }
    }

    // Pedido de "abrir en modo edición" (desde los ajustes de Simón).
    LaunchedEffect(Unit) {
        editRequest.collect { v ->
            if (v > 0) {
                ui.closeAll()
                ui.editMode = true
                editRequest.value = 0
            }
        }
    }

    // Al entrar o salir de la edición: el historial de deshacer arranca de nuevo.
    LaunchedEffect(ui.editMode) {
        ui.flushRemovedWidgets(context)
        ui.undo.clear()
    }

    // Barra de estado y de navegación ocultables
    LaunchedEffect(settings.hideStatusBar, settings.hideNavBar) {
        val window = (context as Activity).window
        val ctrl = WindowCompat.getInsetsController(window, view)
        ctrl.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (settings.hideStatusBar) ctrl.hide(WindowInsetsCompat.Type.statusBars()) else ctrl.show(WindowInsetsCompat.Type.statusBars())
        if (settings.hideNavBar) ctrl.hide(WindowInsetsCompat.Type.navigationBars()) else ctrl.show(WindowInsetsCompat.Type.navigationBars())
    }

    // Botón de inicio: cerrar todo o volver a la primera página
    LaunchedEffect(Unit) {
        var last = homePressed.value
        homePressed.collect { v ->
            if (v != last) {
                last = v
                if (!ui.closeAll()) pager.animateScrollToPage(0)
            }
        }
    }

    // Paralaje del fondo del sistema
    LaunchedEffect(pager, pages) {
        snapshotFlow { pager.currentPage + pager.currentPageOffsetFraction }.collect { pos ->
            ui.currentPage = pager.currentPage
            if (settings.background.parallax && pages > 1) {
                try {
                    val wm = WallpaperManager.getInstance(context)
                    wm.setWallpaperOffsetSteps(1f / (pages - 1), 0f)
                    wm.setWallpaperOffsets(view.windowToken, (pos / (pages - 1)).coerceIn(0f, 1f), 0f)
                } catch (_: Exception) {
                }
            }
        }
    }

    BackHandler { ui.closeAll() }
    val perf by StudioStore.performanceFlow.collectAsStateWithLifecycle()
    val allowed = rememberAnimationsAllowed()

    fun runGesture(action: GestureAction) {
        when (action) {
            GestureAction.NONE -> Unit
            GestureAction.DRAWER -> ui.drawerOpen = true
            GestureAction.SEARCH -> { ui.drawerOpen = true; ui.drawerSearch = true }
            GestureAction.NOTIFICATIONS -> SystemActions.expandNotifications(context)
            GestureAction.SIMON_PANEL -> SystemActions.openSimonPanel(context)
            GestureAction.LOCK -> SystemActions.lockScreen(context)
            GestureAction.EDIT -> ui.editMode = true
            GestureAction.OPEN_SIMON -> SystemActions.openSimon(context)
            GestureAction.FLASHLIGHT -> SystemActions.toggleFlashlight(context)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(settings.swipeUp, settings.swipeDown, ui.editMode) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onVerticalDrag = { _, dy -> total += dy },
                    onDragEnd = {
                        if (!ui.editMode) {
                            if (total < -120f) runGesture(settings.swipeUp)
                            else if (total > 120f) runGesture(settings.swipeDown)
                        }
                    }
                )
            }
    ) {
      CompositionLocalProvider(LocalAnimationsActive provides allowed) {
        LauncherBackgroundLayer(settings.background, pager)

        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            userScrollEnabled = ui.selectedId == null
        ) { page ->
            Box(
                Modifier
                    .fillMaxSize()
                    .pageTransition(pager, page, settings.transition)
                    .pointerInput(ui.editMode, settings.doubleTap, settings.longPress) {
                        detectTapGestures(
                            onTap = { if (ui.editMode) ui.selectedId = null },
                            onDoubleTap = { if (!ui.editMode) runGesture(settings.doubleTap) },
                            onLongPress = { if (!ui.editMode) runGesture(settings.longPress) }
                        )
                    }
            ) {
                // Solo se animan las páginas a la vista: las de al lado quedan quietas.
                val pageActive = allowed && (!perf.onlyVisiblePage || page == pager.currentPage)
                CompositionLocalProvider(LocalAnimationsActive provides pageActive) {
                    PageCanvas(
                        page = page,
                        items = items.filter { it.page == page }.sortedBy { it.z },
                        settings = settings,
                        ui = ui
                    )
                }
            }
        }

        // Indicador de páginas + dock
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            if (settings.pageIndicator && pages > 1) {
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.Center) {
                    repeat(pages) { i ->
                        Box(
                            Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (i == pager.currentPage) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = if (i == pager.currentPage) 0.95f else 0.45f))
                        )
                    }
                }
            }
            if (settings.dockEnabled) Dock(settings, ui)
        }

        if (ui.editMode) EditBar(ui, settings, items, pager, onAddPage = {
            StudioStore.updateLauncher { it.copy(pages = (it.pages + 1).coerceAtMost(12)) }
            scope.launch { pager.animateScrollToPage(pages) }
        })

        AnimatedVisibility(ui.drawerOpen, enter = slideInVertically { it / 3 } + fadeIn(), exit = slideOutVertically { it / 3 } + fadeOut()) {
            AppDrawer(apps, settings, ui)
        }

        ui.openFolder?.let { FolderOverlay(it, settings, ui) }
      }
    }

    if (ui.addSheet) AddSheet(ui, settings, pager.currentPage, onPickSystemWidget)
    ui.editItem?.let { ItemEditorSheet(it, settings, ui) }
    ui.appMenu?.let { AppMenuSheet(it, ui.appMenuItem, settings, ui, pager.currentPage) }
    ui.iconEditor?.let { IconOverrideSheet(it, settings, ui) }
    if (ui.dockEditor) DockEditorSheet(settings, ui)

    if (apps.isNotEmpty() && !settings.initialized) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("¿Cómo querés arrancar?") },
            text = {
                Text(
                    "Podés empezar con tus apps de siempre ya acomodadas (después movés todo como quieras) " +
                        "o con las páginas vacías para armarlas a tu gusto. En los dos casos el dock trae " +
                        "teléfono, mensajes, cámara y navegador."
                )
            },
            confirmButton = { TextButton(onClick = { seed(true) }, enabled = !seeding) { Text("Con mis apps de siempre") } },
            dismissButton = { TextButton(onClick = { seed(false) }, enabled = !seeding) { Text("Empezar de cero") } }
        )
    }
}

/** Efecto de transición entre páginas. */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.pageTransition(pager: PagerState, page: Int, transition: PageTransition): Modifier = graphicsLayer {
    val offset = (pager.currentPage - page) + pager.currentPageOffsetFraction
    val o = offset.coerceIn(-1f, 1f)
    when (transition) {
        PageTransition.SLIDE -> Unit
        PageTransition.CUBE -> {
            cameraDistance = 12f * density
            transformOrigin = TransformOrigin(if (o < 0) 0f else 1f, 0.5f)
            rotationY = -90f * o
        }
        PageTransition.FADE -> {
            translationX = o * size.width
            alpha = 1f - abs(o)
        }
        PageTransition.ZOOM -> {
            val s = 1f - 0.25f * abs(o)
            scaleX = s; scaleY = s
            alpha = 1f - 0.5f * abs(o)
        }
        PageTransition.ROTATE -> {
            transformOrigin = TransformOrigin(0.5f, 1.2f)
            rotationZ = -25f * o
        }
        PageTransition.STACK -> {
            if (o > 0) {
                translationX = o * size.width
                val s = 1f - 0.15f * o
                scaleX = s; scaleY = s
                alpha = 1f - 0.4f * o
            }
        }
    }
}
