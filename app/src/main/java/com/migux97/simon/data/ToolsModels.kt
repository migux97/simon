package com.migux97.simon.data

import kotlinx.serialization.Serializable

// ======================================================================
//  Rutinas automáticas
// ======================================================================

@Serializable
enum class TriggerType(val label: String) {
    TIME("A una hora"),
    CHARGER_ON("Al conectar el cargador"),
    CHARGER_OFF("Al desconectar el cargador"),
    HEADSET_ON("Al conectar auriculares"),
    HEADSET_OFF("Al desconectar auriculares"),
    WIFI_ON("Al conectarse a un Wi-Fi"),
    WIFI_OFF("Al desconectarse del Wi-Fi"),
    BATTERY_LOW("Con batería baja"),
    UNLOCK("Al desbloquear"),
    SCREEN_OFF("Al apagar la pantalla")
}

@Serializable
data class RoutineTrigger(
    val type: TriggerType = TriggerType.TIME,
    val hour: Int = 8,
    val minute: Int = 0,
    /** Días con los valores de Calendar.DAY_OF_WEEK (1 = domingo … 7 = sábado). */
    val days: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7)
)

@Serializable
enum class ActionType(val label: String) {
    STATIC_WALLPAPER("Poner un fondo fijo"),
    LIVE_WALLPAPER("Cambiar el fondo animado"),
    THEME("Aplicar un tema"),
    NEXT_WALLPAPER("Siguiente fondo de la rotación"),
    RINGER("Modo de sonido"),
    DND("No molestar"),
    BRIGHTNESS("Brillo"),
    MEDIA_VOLUME("Volumen multimedia"),
    OPEN_APP("Abrir una app"),
    EFFECT("Mostrar un efecto"),
    MESSAGE("Mostrar un mensaje"),
    FLASHLIGHT_OFF("Apagar la linterna"),
    CHARGING_SCREEN("Abrir la pantalla de carga")
}

/** [value] guarda un id, app o texto; [number] un nivel o modo según el tipo. */
@Serializable
data class RoutineAction(
    val type: ActionType,
    val value: String = "",
    val number: Int = 0
)

@Serializable
data class Routine(
    val id: String = newId(),
    val name: String = "Mi rutina",
    val enabled: Boolean = true,
    val trigger: RoutineTrigger = RoutineTrigger(),
    val actions: List<RoutineAction> = emptyList(),
    val lastRun: Long = 0L
)

// ======================================================================
//  Fondos que cambian solos
// ======================================================================

@Serializable
enum class RotationMode(val label: String) { OFF("Apagado"), INTERVAL("Cada cierto tiempo"), DAY_NIGHT("Día y noche") }

@Serializable
enum class RotationKind { STATIC, PHOTO, DRAWING }

/** Un fondo dentro de la rotación: diseño generado, foto o dibujo. */
@Serializable
data class RotationEntry(val kind: RotationKind, val ref: String)

@Serializable
data class WallpaperRotation(
    val mode: RotationMode = RotationMode.OFF,
    val intervalMin: Int = 60,
    val onUnlock: Boolean = false,
    val shuffle: Boolean = true,
    val target: String = "BOTH",
    val entries: List<RotationEntry> = emptyList(),
    val index: Int = 0,
    val day: RotationEntry? = null,
    val night: RotationEntry? = null,
    val dayStartHour: Int = 7,
    val nightStartHour: Int = 20,
    val liveDay: String? = null,
    val liveNight: String? = null,
    val lastApplied: String = ""
)

// ======================================================================
//  Pantalla al cargar y pantalla de bloqueo
// ======================================================================

@Serializable
data class ChargingScreenSettings(
    val enabled: Boolean = false,
    val sceneId: String? = "lv_stars",
    val blackBackground: Boolean = false,
    val clockFont: FontStyle = FontStyle.LIGHT,
    val clockColor: Int = 0xFFFFFFFF.toInt(),
    val accent: Int = 0xFF34C759.toInt(),
    val showBattery: Boolean = true,
    val showAlarm: Boolean = true,
    val dim: Float = 0.35f,
    val onlyWhenLocked: Boolean = false
)

@Serializable
data class LockScreenSettings(
    val enabled: Boolean = false,
    val background: LauncherBackground = LauncherBackground(),
    val clockFont: FontStyle = FontStyle.LIGHT,
    val clockColor: Int = 0xFFFFFFFF.toInt(),
    val clockSize: Int = 84,
    val showDate: Boolean = true,
    val showNotifications: Boolean = true,
    val hint: String = "Deslizá hacia arriba para desbloquear"
)

// ======================================================================
//  Barra lateral y burbuja
// ======================================================================

@Serializable
data class QuickShortcut(
    val id: String = newId(),
    val label: String,
    val action: TapAction,
    val data: String = ""
)

@Serializable
data class EdgePanelSettings(
    val enabled: Boolean = false,
    val rightSide: Boolean = true,
    val handleY: Float = 0.35f,
    val handleHeight: Float = 0.16f,
    val handleColor: Int = 0xFF8B6CFF.toInt(),
    val handleAlpha: Float = 0.55f,
    val bg1: Int = 0xF0140F24.toInt(),
    val bg2: Int = 0xF02A1B5C.toInt(),
    val textColor: Int = 0xFFFFFFFF.toInt(),
    val apps: List<String> = emptyList(),
    val shortcuts: List<QuickShortcut> = emptyList(),
    val tools: List<PanelTile> = listOf(PanelTile.FLASHLIGHT, PanelTile.SCREENSHOT, PanelTile.LOCK, PanelTile.SETTINGS),
    val columns: Int = 2
)

@Serializable
data class BubbleSettings(
    val enabled: Boolean = false,
    val sizeDp: Int = 56,
    val color1: Int = 0xFF8B6CFF.toInt(),
    val color2: Int = 0xFFFF7EB6.toInt(),
    val stickerId: String? = null,
    val alpha: Float = 0.92f,
    val rightSide: Boolean = true,
    val y: Float = 0.55f,
    val apps: List<String> = emptyList(),
    val shortcuts: List<QuickShortcut> = emptyList(),
    val tools: List<PanelTile> = listOf(PanelTile.FLASHLIGHT, PanelTile.SCREENSHOT, PanelTile.LOCK, PanelTile.SIMON)
)

// ======================================================================
//  Sonidos y vibración
// ======================================================================

@Serializable
enum class VibePattern(val label: String) {
    DEFAULT("La del sistema"),
    NONE("Sin vibración"),
    SHORT("Corta"),
    DOUBLE("Doble"),
    LONG("Larga"),
    HEARTBEAT("Latido"),
    SOS("SOS"),
    RAPID("Ráfaga");

    val waveform: LongArray?
        get() = when (this) {
            DEFAULT, NONE -> null
            SHORT -> longArrayOf(0, 120)
            DOUBLE -> longArrayOf(0, 120, 120, 120)
            LONG -> longArrayOf(0, 700)
            HEARTBEAT -> longArrayOf(0, 90, 120, 160, 600, 90, 120, 160)
            SOS -> longArrayOf(0, 100, 100, 100, 100, 100, 250, 300, 100, 300, 100, 300, 250, 100, 100, 100, 100, 100)
            RAPID -> longArrayOf(0, 50, 50, 50, 50, 50, 50, 50, 50, 50)
        }
}

@Serializable
data class SoundRule(
    val id: String = newId(),
    val name: String = "Nueva regla",
    val enabled: Boolean = true,
    /** Paquete de la app, o vacío para todas. */
    val packageName: String = "",
    /** Si no está vacío, la notificación tiene que mencionar esto (ej: un contacto). */
    val contains: String = "",
    val soundUri: String? = null,
    val soundName: String = "",
    val vibration: VibePattern = VibePattern.DEFAULT,
    val volume: Float = 0.8f
)

@Serializable
data class SoundSettings(
    val enabled: Boolean = false,
    val rules: List<SoundRule> = emptyList(),
    val respectSilent: Boolean = true
)
