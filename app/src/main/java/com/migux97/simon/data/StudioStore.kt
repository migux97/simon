package com.migux97.simon.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Almacenamiento de todo lo nuevo: launcher, widgets creados, dibujos,
 * animaciones y efectos. Igual que [Repository], usa JSON en SharedPreferences
 * porque lo leen servicios y receptores además de la UI.
 */
object StudioStore {
    private lateinit var prefs: SharedPreferences
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Volatile
    private var initialized = false

    private class Slot<T>(val key: String, val serializer: KSerializer<T>, val default: T) {
        val flow = MutableStateFlow(default)
    }

    private val homeItems = Slot("home_items", ListSerializer(HomeItem.serializer()), emptyList())
    private val overrides = Slot("app_overrides", ListSerializer(AppOverride.serializer()), emptyList())
    private val launcher = Slot("launcher", LauncherSettings.serializer(), LauncherSettings())
    private val widgets = Slot("custom_widgets", ListSerializer(CustomWidget.serializer()), emptyList())
    private val drawings = Slot("drawings", ListSerializer(DrawingInfo.serializer()), emptyList())
    private val animations = Slot("animations", ListSerializer(AnimationInfo.serializer()), emptyList())
    private val effects = Slot("effects", EffectsSettings.serializer(), EffectsSettings())
    private val stickers = Slot("stickers", ListSerializer(StickerInfo.serializer()), emptyList())
    private val mode = Slot("mode", AppMode.serializer(), AppMode())
    private val performance = Slot("performance", PerformanceSettings.serializer(), PerformanceSettings())
    private val routines = Slot("routines", ListSerializer(Routine.serializer()), emptyList())
    private val rotation = Slot("rotation", WallpaperRotation.serializer(), WallpaperRotation())
    private val charging = Slot("charging", ChargingScreenSettings.serializer(), ChargingScreenSettings())
    private val lock = Slot("lock", LockScreenSettings.serializer(), LockScreenSettings())
    private val edge = Slot("edge", EdgePanelSettings.serializer(), EdgePanelSettings())
    private val bubble = Slot("bubble", BubbleSettings.serializer(), BubbleSettings())
    private val sounds = Slot("sounds", SoundSettings.serializer(), SoundSettings())

    val homeItemsFlow: StateFlow<List<HomeItem>> = homeItems.flow.asStateFlow()
    val overridesFlow: StateFlow<List<AppOverride>> = overrides.flow.asStateFlow()
    val launcherFlow: StateFlow<LauncherSettings> = launcher.flow.asStateFlow()
    val widgetsFlow: StateFlow<List<CustomWidget>> = widgets.flow.asStateFlow()
    val drawingsFlow: StateFlow<List<DrawingInfo>> = drawings.flow.asStateFlow()
    val animationsFlow: StateFlow<List<AnimationInfo>> = animations.flow.asStateFlow()
    val effectsFlow: StateFlow<EffectsSettings> = effects.flow.asStateFlow()
    val stickersFlow: StateFlow<List<StickerInfo>> = stickers.flow.asStateFlow()
    val modeFlow: StateFlow<AppMode> = mode.flow.asStateFlow()
    val performanceFlow: StateFlow<PerformanceSettings> = performance.flow.asStateFlow()
    val routinesFlow: StateFlow<List<Routine>> = routines.flow.asStateFlow()
    val rotationFlow: StateFlow<WallpaperRotation> = rotation.flow.asStateFlow()
    val chargingFlow: StateFlow<ChargingScreenSettings> = charging.flow.asStateFlow()
    val lockFlow: StateFlow<LockScreenSettings> = lock.flow.asStateFlow()
    val edgeFlow: StateFlow<EdgePanelSettings> = edge.flow.asStateFlow()
    val bubbleFlow: StateFlow<BubbleSettings> = bubble.flow.asStateFlow()
    val soundsFlow: StateFlow<SoundSettings> = sounds.flow.asStateFlow()

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            prefs = context.applicationContext.getSharedPreferences("simon_studio", Context.MODE_PRIVATE)
            allSlots().forEach { load(it) }
            initialized = true
        }
    }

    private fun allSlots() = listOf(
        homeItems, overrides, launcher, widgets, drawings, animations, effects, stickers, mode, performance,
        routines, rotation, charging, lock, edge, bubble, sounds
    )

    private fun <T> load(slot: Slot<T>) {
        // Sin dato guardado vuelve al valor por defecto (importa al restaurar una copia).
        val raw = prefs.getString(slot.key, null) ?: run { slot.flow.value = slot.default; return }
        try {
            slot.flow.value = json.decodeFromString(slot.serializer, raw)
        } catch (_: Exception) {
        }
    }

    private fun <T> save(slot: Slot<T>, value: T) {
        slot.flow.value = value
        prefs.edit().putString(slot.key, json.encodeToString(slot.serializer, value)).apply()
    }

    // ---------------------------------------------------------------- inicio

    val items: List<HomeItem> get() = homeItems.flow.value

    @Synchronized
    fun setItems(list: List<HomeItem>) = save(homeItems, list)

    @Synchronized
    fun upsertItem(item: HomeItem) {
        val list = items.toMutableList()
        val i = list.indexOfFirst { it.id == item.id }
        if (i >= 0) list[i] = item else list += item
        save(homeItems, list)
    }

    @Synchronized
    fun removeItem(id: String) = save(homeItems, items.filterNot { it.id == id })

    fun maxZ(page: Int): Int = items.filter { it.page == page }.maxOfOrNull { it.z } ?: 0

    // -------------------------------------------------------------- overrides

    fun override(appKey: String): AppOverride? = overrides.flow.value.firstOrNull { it.appKey == appKey }

    @Synchronized
    fun setOverride(o: AppOverride) {
        val list = overrides.flow.value.filterNot { it.appKey == o.appKey } + o
        save(overrides, list)
    }

    @Synchronized
    fun clearOverride(appKey: String) = save(overrides, overrides.flow.value.filterNot { it.appKey == appKey })

    // --------------------------------------------------------------- launcher

    val launcherSettings: LauncherSettings get() = launcher.flow.value

    @Synchronized
    fun updateLauncher(transform: (LauncherSettings) -> LauncherSettings) = save(launcher, transform(launcher.flow.value))

    // ---------------------------------------------------------------- widgets

    fun widget(id: String?): CustomWidget? = id?.let { i -> widgets.flow.value.firstOrNull { it.id == i } }

    @Synchronized
    fun saveWidget(w: CustomWidget) {
        val list = widgets.flow.value.toMutableList()
        val i = list.indexOfFirst { it.id == w.id }
        if (i >= 0) list[i] = w else list.add(0, w)
        save(widgets, list)
    }

    @Synchronized
    fun deleteWidget(id: String) = save(widgets, widgets.flow.value.filterNot { it.id == id })

    // ------------------------------------------------------ dibujos y animación

    fun drawing(id: String?): DrawingInfo? = id?.let { i -> drawings.flow.value.firstOrNull { it.id == i } }

    @Synchronized
    fun saveDrawing(d: DrawingInfo) {
        val list = listOf(d) + drawings.flow.value.filterNot { it.id == d.id }
        save(drawings, list)
    }

    @Synchronized
    fun deleteDrawing(id: String) = save(drawings, drawings.flow.value.filterNot { it.id == id })

    fun animation(id: String?): AnimationInfo? = id?.let { i -> animations.flow.value.firstOrNull { it.id == i } }

    @Synchronized
    fun saveAnimation(a: AnimationInfo) {
        val list = listOf(a) + animations.flow.value.filterNot { it.id == a.id }
        save(animations, list)
    }

    @Synchronized
    fun deleteAnimation(id: String) = save(animations, animations.flow.value.filterNot { it.id == id })

    // ---------------------------------------------------------------- efectos

    val effectsSettings: EffectsSettings get() = effects.flow.value

    @Synchronized
    fun updateEffects(transform: (EffectsSettings) -> EffectsSettings) = save(effects, transform(effects.flow.value))

    // ---------------------------------------------------------------- stickers

    /** Sticker propio o prediseñado por id. */
    fun sticker(id: String?): StickerInfo? = id?.let { i ->
        stickers.flow.value.firstOrNull { it.id == i } ?: StickerCatalog.builtins.firstOrNull { it.id == i }
    }

    @Synchronized
    fun saveSticker(s: StickerInfo) = save(stickers, listOf(s) + stickers.flow.value.filterNot { it.id == s.id })

    @Synchronized
    fun deleteSticker(id: String) = save(stickers, stickers.flow.value.filterNot { it.id == id })

    // ------------------------------------------------------- modo y rendimiento

    val appMode: AppMode get() = mode.flow.value

    @Synchronized
    fun updateMode(transform: (AppMode) -> AppMode) = save(mode, transform(mode.flow.value))

    val perf: PerformanceSettings get() = performance.flow.value

    @Synchronized
    fun updatePerformance(transform: (PerformanceSettings) -> PerformanceSettings) = save(performance, transform(performance.flow.value))

    // ------------------------------------------------- funciones extra

    val routineList: List<Routine> get() = routines.flow.value

    @Synchronized
    fun saveRoutine(r: Routine) {
        val list = routines.flow.value.toMutableList()
        val i = list.indexOfFirst { it.id == r.id }
        if (i >= 0) list[i] = r else list += r
        save(routines, list)
    }

    @Synchronized
    fun deleteRoutine(id: String) = save(routines, routines.flow.value.filterNot { it.id == id })

    val rotationSettings: WallpaperRotation get() = rotation.flow.value
    @Synchronized fun updateRotation(t: (WallpaperRotation) -> WallpaperRotation) = save(rotation, t(rotation.flow.value))

    val chargingSettings: ChargingScreenSettings get() = charging.flow.value
    @Synchronized fun updateCharging(t: (ChargingScreenSettings) -> ChargingScreenSettings) = save(charging, t(charging.flow.value))

    val lockSettings: LockScreenSettings get() = lock.flow.value
    @Synchronized fun updateLock(t: (LockScreenSettings) -> LockScreenSettings) = save(lock, t(lock.flow.value))

    val edgeSettings: EdgePanelSettings get() = edge.flow.value
    @Synchronized fun updateEdge(t: (EdgePanelSettings) -> EdgePanelSettings) = save(edge, t(edge.flow.value))

    val bubbleSettings: BubbleSettings get() = bubble.flow.value
    @Synchronized fun updateBubble(t: (BubbleSettings) -> BubbleSettings) = save(bubble, t(bubble.flow.value))

    val soundSettings: SoundSettings get() = sounds.flow.value
    @Synchronized fun updateSounds(t: (SoundSettings) -> SoundSettings) = save(sounds, t(sounds.flow.value))

    /** Vuelve a leer todo del disco (después de importar una copia). */
    fun reload(context: Context) {
        synchronized(this) { initialized = false }
        init(context)
    }
}
