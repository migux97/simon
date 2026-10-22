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

    private var weather: Map<String, WeatherInfo> = emptyMap()

    @Volatile
    private var initialized = false

    fun init(context: Context) {
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
            initialized = true
        }
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

    // ---- Íconos ----

    @Synchronized
    fun addIcon(icon: CustomIcon) {
        val list = listOf(icon) + _icons.value.filterNot { it.id == icon.id }
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
