package com.migux97.simon.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class WeatherInfo(
    val temperature: Double,
    val code: Int,
    val max: Double,
    val min: Double,
    val isDay: Boolean,
    val fetchedAt: Long
)

/**
 * El último fondo estático o foto que puso Simón. Guarda los ids que devuelve
 * Android al ponerlo, para saber después si sigue puesto o si lo cambiaron.
 */
@Serializable
data class AppliedWallpaper(
    val staticWallpaper: StaticWallpaper? = null,
    val photoPath: String? = null,
    /** Nombre de [com.migux97.simon.wallpaper.WallpaperTarget]: HOME, LOCK o BOTH. */
    val target: String = "BOTH",
    val homeId: Int = 0,
    val lockId: Int = 0,
    val appliedAt: Long = 0L
)

/**
 * Almacenamiento local de Simón. Usa SharedPreferences con JSON porque se lee
 * desde receptores de widgets y desde el servicio de fondo animado, donde
 * conviene un acceso sincrónico. La UI observa los StateFlow.
 */
object Repository {
    private lateinit var prefs: SharedPreferences
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private const val KEY_DESIGNS = "designs"
    private const val KEY_BINDINGS = "bindings"
    private const val KEY_LIVE = "live"
    private const val KEY_SAVED_LIVE = "saved_live"
    private const val KEY_ICONS = "icons"
    private const val KEY_THEME = "applied_theme"
    private const val KEY_WEATHER = "weather"
    private const val KEY_FAVORITES = "favorites"
    private const val KEY_SAVED_STATIC = "saved_static"
    private const val KEY_CURRENT_STATIC = "current_static"
    private const val KEY_APPLIED_PHOTOS = "applied_photos"
    private const val MAX_PHOTOS = 24

    private val _designs = MutableStateFlow<List<WidgetDesign>>(emptyList())
    val designs: StateFlow<List<WidgetDesign>> = _designs.asStateFlow()

    private val _bindings = MutableStateFlow<Map<Int, String>>(emptyMap())
    val bindings: StateFlow<Map<Int, String>> = _bindings.asStateFlow()

    private val _live = MutableStateFlow<LiveConfig?>(null)
    val live: StateFlow<LiveConfig?> = _live.asStateFlow()

    private val _savedLive = MutableStateFlow<List<LiveConfig>>(emptyList())
    val savedLive: StateFlow<List<LiveConfig>> = _savedLive.asStateFlow()

    private val _icons = MutableStateFlow<List<CustomIcon>>(emptyList())
    val icons: StateFlow<List<CustomIcon>> = _icons.asStateFlow()

    private val _appliedTheme = MutableStateFlow<String?>(null)
    val appliedTheme: StateFlow<String?> = _appliedTheme.asStateFlow()

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val _savedStatic = MutableStateFlow<List<StaticWallpaper>>(emptyList())
    /** Fondos estáticos que el usuario guardó (personalizados o creados). */
    val savedStatic: StateFlow<List<StaticWallpaper>> = _savedStatic.asStateFlow()

    private val _currentStatic = MutableStateFlow<AppliedWallpaper?>(null)
    /** Último fondo estático o foto que puso Simón (puede que ya no esté puesto). */
    val currentStatic: StateFlow<AppliedWallpaper?> = _currentStatic.asStateFlow()

    private val _appliedPhotos = MutableStateFlow<List<String>>(emptyList())
    /** Copias de las fotos que el usuario puso como fondo con Simón (la más nueva primero). */
    val appliedPhotos: StateFlow<List<String>> = _appliedPhotos.asStateFlow()

    private var weather: Map<String, WeatherInfo> = emptyMap()

    @Volatile
    private var initialized = false

    fun init(context: Context) {
        StudioStore.init(context)
        MediaLibrary.init(context)
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            prefs = context.applicationContext.getSharedPreferences("simon", Context.MODE_PRIVATE)
            _designs.value = read(KEY_DESIGNS) ?: emptyList()
            _bindings.value = (read<Map<String, String>>(KEY_BINDINGS) ?: emptyMap())
                .mapNotNull { (k, v) -> k.toIntOrNull()?.let { it to v } }.toMap()
            _live.value = read(KEY_LIVE)
            _savedLive.value = read(KEY_SAVED_LIVE) ?: emptyList()
            _icons.value = read(KEY_ICONS) ?: emptyList()
            _appliedTheme.value = prefs.getString(KEY_THEME, null)
            _favorites.value = prefs.getStringSet(KEY_FAVORITES, emptySet())?.toSet() ?: emptySet()
            weather = read(KEY_WEATHER) ?: emptyMap()
            _savedStatic.value = read(KEY_SAVED_STATIC) ?: emptyList()
            _currentStatic.value = read(KEY_CURRENT_STATIC)
            _appliedPhotos.value = read(KEY_APPLIED_PHOTOS) ?: emptyList()
            initialized = true
        }
    }

    /** Vuelve a leer todo del disco (después de importar una copia). */
    fun reload(context: Context) {
        synchronized(this) { initialized = false }
        init(context)
        StudioStore.reload(context)
    }

    private inline fun <reified T> read(key: String): T? {
        val raw = prefs.getString(key, null) ?: return null
        return try {
            json.decodeFromString<T>(raw)
        } catch (e: Exception) {
            null
        }
    }

    private inline fun <reified T> write(key: String, value: T) {
        prefs.edit().putString(key, json.encodeToString(value)).apply()
    }

    // ---- Diseños de widgets ----

    fun design(id: String?): WidgetDesign? = id?.let { i -> _designs.value.firstOrNull { it.id == i } }

    @Synchronized
    fun saveDesign(design: WidgetDesign) {
        val list = _designs.value.toMutableList()
        val idx = list.indexOfFirst { it.id == design.id }
        if (idx >= 0) list[idx] = design else list.add(0, design)
        _designs.value = list
        write(KEY_DESIGNS, list)
    }

    @Synchronized
    fun deleteDesign(id: String) {
        val list = _designs.value.filterNot { it.id == id }
        _designs.value = list
        write(KEY_DESIGNS, list)
    }

    // ---- Vinculación widget en pantalla -> diseño ----

    fun designForWidget(appWidgetId: Int): WidgetDesign? = design(_bindings.value[appWidgetId])

    /** Valor guardado para un widget en pantalla: id de diseño, o "cw:<id>" si es un widget creado. */
    fun bindingFor(appWidgetId: Int): String? = _bindings.value[appWidgetId]

    fun widgetsUsingDesign(designId: String): List<Int> =
        _bindings.value.filterValues { it == designId }.keys.toList()

    @Synchronized
    fun bindWidget(appWidgetId: Int, designId: String) {
        val map = _bindings.value.toMutableMap()
        map[appWidgetId] = designId
        _bindings.value = map
        write(KEY_BINDINGS, map.mapKeys { it.key.toString() })
    }

    @Synchronized
    fun unbindWidgets(ids: IntArray) {
        val map = _bindings.value.toMutableMap()
        ids.forEach { map.remove(it) }
        _bindings.value = map
        write(KEY_BINDINGS, map.mapKeys { it.key.toString() })
    }

    // ---- Fondo animado ----

    @Synchronized
    fun setLive(config: LiveConfig) {
        _live.value = config
        write(KEY_LIVE, config)
        saveLiveToCollection(config)
    }

    @Synchronized
    fun saveLiveToCollection(config: LiveConfig) {
        val list = _savedLive.value.toMutableList()
        val idx = list.indexOfFirst { it.id == config.id }
        if (idx >= 0) list[idx] = config else list.add(0, config)
        _savedLive.value = list
        write(KEY_SAVED_LIVE, list)
    }

    @Synchronized
    fun deleteSavedLive(id: String) {
        val list = _savedLive.value.filterNot { it.id == id }
        _savedLive.value = list
        write(KEY_SAVED_LIVE, list)
    }

    // ---- Fondos estáticos ----

    fun staticById(id: String): StaticWallpaper? = _savedStatic.value.firstOrNull { it.id == id }

    /** Agrega o actualiza un fondo estático guardado. */
    @Synchronized
    fun saveStatic(wallpaper: StaticWallpaper) {
        val list = _savedStatic.value.toMutableList()
        val idx = list.indexOfFirst { it.id == wallpaper.id }
        if (idx >= 0) list[idx] = wallpaper else list.add(0, wallpaper)
        _savedStatic.value = list
        write(KEY_SAVED_STATIC, list)
    }

    @Synchronized
    fun deleteSavedStatic(id: String) {
        val list = _savedStatic.value.filterNot { it.id == id }
        _savedStatic.value = list
        write(KEY_SAVED_STATIC, list)
    }

    /** Anota qué fondo estático o foto acaba de poner Simón. */
    @Synchronized
    fun setCurrentStatic(applied: AppliedWallpaper?) {
        _currentStatic.value = applied
        if (applied == null) prefs.edit().remove(KEY_CURRENT_STATIC).apply() else write(KEY_CURRENT_STATIC, applied)
    }

    /** Suma una foto (ya copiada dentro de la app) a la lista de fotos usadas como fondo. */
    @Synchronized
    fun addAppliedPhoto(path: String) {
        val all = listOf(path) + _appliedPhotos.value.filterNot { it == path }
        val keep = all.take(MAX_PHOTOS)
        val current = _currentStatic.value?.photoPath
        all.drop(MAX_PHOTOS).filterNot { it == current }.forEach { ImageStore.delete(it) }
        _appliedPhotos.value = keep
        write(KEY_APPLIED_PHOTOS, keep)
    }

    /** Quita una foto de la lista y borra su copia (salvo que sea la que está puesta). */
    @Synchronized
    fun deleteAppliedPhoto(path: String) {
        val list = _appliedPhotos.value.filterNot { it == path }
        _appliedPhotos.value = list
        write(KEY_APPLIED_PHOTOS, list)
        if (_currentStatic.value?.photoPath != path) ImageStore.delete(path)
    }

    // ---- Íconos ----

    @Synchronized
    fun addIcon(icon: CustomIcon) {
        val list = listOf(icon) + _icons.value.filterNot { it.id == icon.id }
        _icons.value = list
        write(KEY_ICONS, list)
    }

    /** El último ícono personalizado de una app, si existe. */
    fun iconFor(packageName: String): CustomIcon? = _icons.value.firstOrNull { it.packageName == packageName }

    /**
     * Guarda el ícono de una app reemplazando el anterior de esa misma app
     * (una app = un ícono personalizado; el acceso directo se actualiza).
     */
    @Synchronized
    fun saveIcon(icon: CustomIcon) {
        val list = listOf(icon) + _icons.value.filterNot { it.id == icon.id || it.packageName == icon.packageName }
        _icons.value = list
        write(KEY_ICONS, list)
    }

    @Synchronized
    fun deleteIcon(id: String) {
        val list = _icons.value.filterNot { it.id == id }
        _icons.value = list
        write(KEY_ICONS, list)
    }

    // ---- Temas ----

    fun setAppliedTheme(id: String?) {
        _appliedTheme.value = id
        prefs.edit().putString(KEY_THEME, id).apply()
    }

    // ---- Favoritos ----

    @Synchronized
    fun toggleFavorite(id: String) {
        val set = _favorites.value.toMutableSet()
        if (!set.add(id)) set.remove(id)
        _favorites.value = set
        prefs.edit().putStringSet(KEY_FAVORITES, set).apply()
    }

    // ---- Clima ----

    private fun weatherKey(lat: Double, lon: Double) = "%.2f,%.2f".format(java.util.Locale.US, lat, lon)

    fun weatherFor(lat: Double, lon: Double): WeatherInfo? = weather[weatherKey(lat, lon)]

    @Synchronized
    fun putWeather(lat: Double, lon: Double, info: WeatherInfo) {
        weather = weather + (weatherKey(lat, lon) to info)
        write(KEY_WEATHER, weather)
    }
}
