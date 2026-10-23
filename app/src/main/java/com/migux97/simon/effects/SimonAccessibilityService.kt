package com.migux97.simon.effects

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.migux97.simon.data.PanelZone
import com.migux97.simon.data.Repository
import com.migux97.simon.data.StudioStore
import com.migux97.simon.ui.theme.SimonTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Dueño de ciclo de vida para poder usar Compose en ventanas flotantes. */
class OverlayOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val controller = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry

    fun start() {
        controller.performAttach()
        controller.performRestore(null)
        registry.currentState = Lifecycle.State.RESUMED
    }

    fun stop() {
        registry.currentState = Lifecycle.State.DESTROYED
    }
}

/**
 * Servicio de accesibilidad de Simón: panel deslizable propio desde el borde
 * superior, bloqueo con gesto, capturas y efectos sobre la pantalla de bloqueo.
 */
class SimonAccessibilityService : AccessibilityService() {

    private val main = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var trigger: View? = null
    private var panel: ComposeView? = null
    private var panelOwner: OverlayOwner? = null
    private val wm get() = getSystemService(Context.WINDOW_SERVICE) as WindowManager

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Repository.init(this)
        EffectsHub.register(this)
        com.migux97.simon.launcher.AppCatalog.ensureLoaded(this)
        scope.launch {
            StudioStore.effectsFlow.map { it.panel }.distinctUntilChanged().collect { updateTrigger() }
        }
        scope.launch {
            StudioStore.modeFlow.map { it.overlayEnabled }.distinctUntilChanged().collect { on -> if (!on) hideOverlay() }
        }
        scope.launch {
            StudioStore.edgeFlow.map { Triple(it.enabled, it.rightSide, listOf(it.handleY, it.handleHeight, it.handleAlpha, it.handleColor.toFloat())) }
                .distinctUntilChanged().collect { updateEdgeHandle() }
        }
        scope.launch {
            StudioStore.bubbleFlow.map { listOf(it.enabled, it.sizeDp, it.color1, it.color2, it.stickerId, it.alpha) }
                .distinctUntilChanged().collect { updateBubble() }
        }
    }

    private fun fullScreenParams(focusable: Boolean = true) = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        (if (focusable) 0 else WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    // ------------------------------------------------------- barra lateral

    private var edgeHandle: View? = null
    private var edgePanel: ComposeView? = null
    private var edgeOwner: OverlayOwner? = null

    private fun removeEdgeHandle() {
        edgeHandle?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        edgeHandle = null
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun updateEdgeHandle() {
        removeEdgeHandle()
        val s = StudioStore.edgeSettings
        if (!s.enabled) { hideEdgePanel(); return }
        val dm = resources.displayMetrics
        val w = (14 * dm.density).toInt()
        val h = (dm.heightPixels * s.handleHeight.coerceIn(0.05f, 0.6f)).toInt()
        val params = WindowManager.LayoutParams(
            w, h,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or (if (s.rightSide) Gravity.END else Gravity.START)
            y = (dm.heightPixels * s.handleY.coerceIn(0f, 0.95f)).toInt()
        }
        val view = View(this)
        val bar = android.graphics.drawable.GradientDrawable().apply {
            val a = (s.handleAlpha.coerceIn(0f, 1f) * 255).toInt()
            setColor((s.handleColor and 0x00FFFFFF) or (a shl 24))
            cornerRadius = 6 * dm.density
        }
        // Una línea finita pegada al borde; el resto del área es tocable pero invisible.
        val inset = (9 * dm.density).toInt()
        view.background = android.graphics.drawable.InsetDrawable(bar, if (s.rightSide) inset else 0, 0, if (s.rightSide) 0 else inset, 0)
        var downX = 0f
        var fired = false
        view.setOnTouchListener { _, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> { downX = ev.rawX; fired = false }
                MotionEvent.ACTION_MOVE -> {
                    val dx = ev.rawX - downX
                    val inward = if (s.rightSide) -dx else dx
                    if (!fired && inward > 16 * dm.density) { fired = true; showEdgePanel() }
                }
                MotionEvent.ACTION_UP -> if (!fired) showEdgePanel()
            }
            true
        }
        try {
            wm.addView(view, params)
            edgeHandle = view
        } catch (_: Exception) {
        }
    }

    fun showEdgePanel() {
        main.post {
            if (edgePanel != null) return@post
            val owner = OverlayOwner().also { it.start() }
            val view = ComposeView(this).apply {
                setViewTreeLifecycleOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)
                setContent { SimonTheme { EdgePanelContent(service = this@SimonAccessibilityService, onClose = { hideEdgePanel() }) } }
            }
            try {
                wm.addView(view, fullScreenParams())
                edgePanel = view
                edgeOwner = owner
            } catch (_: Exception) {
                owner.stop()
            }
        }
    }

    fun hideEdgePanel() {
        main.post {
            edgePanel?.let { try { wm.removeView(it) } catch (_: Exception) {} }
            edgeOwner?.stop()
            edgePanel = null
            edgeOwner = null
        }
    }

    // ------------------------------------------------------------- burbuja

    private var bubble: ComposeView? = null
    private var bubbleOwner: OverlayOwner? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var bubbleMenu: ComposeView? = null
    private var bubbleMenuOwner: OverlayOwner? = null

    private fun removeBubble() {
        bubble?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        bubbleOwner?.stop()
        bubble = null
        bubbleOwner = null
        bubbleParams = null
    }

    private fun updateBubble() {
        removeBubble()
        val s = StudioStore.bubbleSettings
        if (!s.enabled) { hideBubbleMenu(); return }
        val dm = resources.displayMetrics
        val sizePx = (s.sizeDp.coerceIn(32, 96) * dm.density).toInt()
        val params = WindowManager.LayoutParams(
            sizePx, sizePx,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = if (s.rightSide) dm.widthPixels - sizePx else 0
            y = (dm.heightPixels * s.y.coerceIn(0.05f, 0.9f)).toInt()
        }
        val owner = OverlayOwner().also { it.start() }
        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                BubbleFace(
                    s = StudioStore.bubbleSettings,
                    onTap = { showBubbleMenu() },
                    onLongPress = { hideBubbleFor(30_000) },
                    onDrag = { d -> moveBubble(d.x, d.y) },
                    onDragEnd = { snapBubble() }
                )
            }
        }
        try {
            wm.addView(view, params)
            bubble = view
            bubbleOwner = owner
            bubbleParams = params
        } catch (_: Exception) {
            owner.stop()
        }
    }

    private fun moveBubble(dx: Float, dy: Float) {
        val v = bubble ?: return
        val p = bubbleParams ?: return
        val dm = resources.displayMetrics
        p.x = (p.x + dx.toInt()).coerceIn(0, dm.widthPixels - p.width)
        p.y = (p.y + dy.toInt()).coerceIn(0, dm.heightPixels - p.height)
        try { wm.updateViewLayout(v, p) } catch (_: Exception) {}
    }

    /** Al soltarla, se pega al borde más cercano y recuerda dónde quedó. */
    private fun snapBubble() {
        val v = bubble ?: return
        val p = bubbleParams ?: return
        val dm = resources.displayMetrics
        val right = p.x + p.width / 2 > dm.widthPixels / 2
        p.x = if (right) dm.widthPixels - p.width else 0
        try { wm.updateViewLayout(v, p) } catch (_: Exception) {}
        val y = p.y.toFloat() / dm.heightPixels.coerceAtLeast(1)
        StudioStore.updateBubble { it.copy(rightSide = right, y = y) }
    }

    private fun hideBubbleFor(ms: Long) {
        bubble?.visibility = View.GONE
        main.postDelayed({ bubble?.visibility = View.VISIBLE }, ms)
    }

    fun showBubbleMenu() {
        main.post {
            if (bubbleMenu != null) return@post
            val owner = OverlayOwner().also { it.start() }
            val view = ComposeView(this).apply {
                setViewTreeLifecycleOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)
                setContent { SimonTheme { BubbleMenuContent(service = this@SimonAccessibilityService, onClose = { hideBubbleMenu() }) } }
            }
            try {
                wm.addView(view, fullScreenParams())
                bubbleMenu = view
                bubbleMenuOwner = owner
            } catch (_: Exception) {
                owner.stop()
            }
        }
    }

    fun hideBubbleMenu() {
        main.post {
            bubbleMenu?.let { try { wm.removeView(it) } catch (_: Exception) {} }
            bubbleMenuOwner?.stop()
            bubbleMenu = null
            bubbleMenuOwner = null
        }
    }

    /** Cierra cualquier ventana flotante abierta (antes de una captura, por ejemplo). */
    fun closeFloating() {
        hidePanel()
        hideEdgePanel()
        hideBubbleMenu()
    }

    // ------------------------------------------------- capa sobre tu launcher

    private var overlay: ComposeView? = null
    private var overlayOwner: OverlayOwner? = null
    private var homePkg: String? = null
    private var homeCheckedAt = 0L

    private fun homePackage(): String? {
        val now = System.currentTimeMillis()
        if (homePkg == null || now - homeCheckedAt > 15_000) {
            homePkg = com.migux97.simon.launcher.LauncherMode.currentHomePackage(this)
            homeCheckedAt = now
        }
        return homePkg
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        // El panel de notificaciones y el teclado no cambian lo que hay debajo.
        if (pkg == "com.android.systemui") return
        // Las ventanas flotantes propias (panel, efectos) no cuentan; las pantallas de Simón sí.
        if (pkg == packageName && event.className?.toString()?.contains("Activity") != true) return
        val mode = StudioStore.appMode
        val home = homePackage()
        if (mode.overlayEnabled && pkg == home && home != packageName) showOverlay() else hideOverlay()
    }

    /** Capa decorativa (stickers, widgets animados) encima del launcher original. No toma toques. */
    private fun showOverlay() {
        if (overlay != null) return
        val owner = OverlayOwner().also { it.start() }
        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent { SimonTheme { com.migux97.simon.launcher.HomeOverlayContent() } }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        try {
            wm.addView(view, params)
            overlay = view
            overlayOwner = owner
        } catch (_: Exception) {
            owner.stop()
        }
    }

    private fun hideOverlay() {
        overlay?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        overlayOwner?.stop()
        overlay = null
        overlayOwner = null
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        hideOverlay()
        removeTrigger()
        removeEdgeHandle()
        removeBubble()
        panel?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        edgePanel?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        bubbleMenu?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        panelOwner?.stop(); edgeOwner?.stop(); bubbleMenuOwner?.stop()
        panel = null; edgePanel = null; bubbleMenu = null
        scope.cancel()
        if (instance === this) instance = null
        super.onDestroy()
    }

    // ---------------------------------------------------------- disparador

    private fun removeTrigger() {
        trigger?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        trigger = null
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun updateTrigger() {
        removeTrigger()
        val s = StudioStore.effectsSettings.panel
        if (!s.enabled) return
        val dm = resources.displayMetrics
        val width = when (s.zone) {
            PanelZone.FULL -> dm.widthPixels
            else -> (dm.widthPixels * s.zoneWidth.coerceIn(0.1f, 1f)).toInt()
        }
        val height = (28 * dm.density).toInt()
        val params = WindowManager.LayoutParams(
            width, height,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or when (s.zone) {
                PanelZone.LEFT -> Gravity.START
                PanelZone.RIGHT -> Gravity.END
                else -> Gravity.CENTER_HORIZONTAL
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        val view = View(this)
        var downY = 0f
        var fired = false
        view.setOnTouchListener { _, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> { downY = ev.rawY; fired = false }
                MotionEvent.ACTION_MOVE -> if (!fired && ev.rawY - downY > 18 * dm.density) { fired = true; showPanel() }
                MotionEvent.ACTION_UP -> if (!fired && ev.rawY - downY > 6 * dm.density) showPanel()
            }
            true
        }
        try {
            wm.addView(view, params)
            trigger = view
        } catch (_: Exception) {
        }
    }

    // --------------------------------------------------------------- panel

    fun showPanel() {
        main.post {
            if (panel != null) return@post
            val owner = OverlayOwner().also { it.start() }
            val view = ComposeView(this).apply {
                setViewTreeLifecycleOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)
                setContent { SimonTheme { PanelContent(onClose = { hidePanel() }, service = this@SimonAccessibilityService) } }
            }
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
            try {
                wm.addView(view, params)
                panel = view
                panelOwner = owner
            } catch (_: Exception) {
                owner.stop()
            }
        }
    }

    fun hidePanel() {
        main.post {
            panel?.let { try { wm.removeView(it) } catch (_: Exception) {} }
            panelOwner?.stop()
            panel = null
            panelOwner = null
        }
    }

    // ------------------------------------------------------- acciones globales

    fun lock(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) else false

    fun screenshot() {
        closeFloating()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) main.postDelayed({ performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT) }, 500)
    }

    fun openNotifications() {
        performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    }

    companion object {
        @Volatile
        var instance: SimonAccessibilityService? = null
            private set

        fun isEnabled(context: Context): Boolean {
            val flat = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
            val me = ComponentName(context, SimonAccessibilityService::class.java).flattenToString()
            return flat.split(":").any { it.equals(me, ignoreCase = true) }
        }
    }
}
