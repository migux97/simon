package com.migux97.simon.data

import kotlinx.serialization.Serializable

// ======================================================================
//  Movimiento (animaciones de íconos, stickers y elementos de widgets)
// ======================================================================

@Serializable
enum class Motion(val label: String) {
    NONE("Ninguno"),
    FLOAT("Flotar"),
    PULSE("Latir"),
    BOUNCE("Rebotar"),
    WIGGLE("Temblar"),
    SWING("Péndulo"),
    SPIN("Girar"),
    BLINK("Parpadear"),
    GLOW("Brillar"),
    JELLY("Gelatina")
}

// ======================================================================
//  Launcher
// ======================================================================

@Serializable
enum class ItemType { APP, CUSTOM_WIDGET, DESIGN_WIDGET, SYSTEM_WIDGET, STICKER, FOLDER }

@Serializable
enum class StickerSource { IMAGE, DRAWING, ANIMATION, VIDEO, SCENE, LIBRARY }

/** Cualquier cosa ubicada libremente en la pantalla de inicio. Posición y tamaño en fracciones de la página. */
@Serializable
data class HomeItem(
    val id: String = newId(),
    val type: ItemType,
    val page: Int = 0,
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    val rotation: Float = 0f,
    val alpha: Float = 1f,
    val z: Int = 0,
    val locked: Boolean = false,
    val motion: Motion = Motion.NONE,
    val motionSpeed: Float = 1f,
    // App
    val appKey: String? = null,
    val showLabel: Boolean = true,
    // Widgets
    val customWidgetId: String? = null,
    val designId: String? = null,
    val appWidgetId: Int = -1,
    // Sticker
    val stickerSource: StickerSource = StickerSource.IMAGE,
    val mediaPath: String? = null,
    val mediaId: String? = null,
    val liveId: String? = null,
    // Carpeta
    val title: String = "",
    val folderApps: List<String> = emptyList(),
    val folderColor: Int = 0x66FFFFFF,
    val stickers: List<StickerAttach> = emptyList()
)

/** Personalización de una app concreta (vale en inicio, dock, cajón y carpetas). */
@Serializable
data class AppOverride(
    val appKey: String,
    val label: String? = null,
    val style: IconStyle? = null,
    val imagePath: String? = null,
    val drawingId: String? = null,
    val useIconPack: Boolean = true,
    val motion: Motion? = null,
    val stickers: List<StickerAttach> = emptyList()
)

@Serializable
enum class LauncherBgType(val label: String) {
    SYSTEM("Fondo del sistema"),
    COLOR("Color"),
    GRADIENT("Degradado"),
    IMAGE("Foto"),
    VIDEO("Video"),
    SCENE("Escena animada"),
    ANIMATION("Mi animación"),
    DRAWING("Mi dibujo")
}

@Serializable
data class LauncherBackground(
    val type: LauncherBgType = LauncherBgType.SYSTEM,
    val color1: Int = 0xFF1A1033.toInt(),
    val color2: Int = 0xFF6C4CF5.toInt(),
    val path: String? = null,
    val mediaId: String? = null,
    val liveId: String? = null,
    val dim: Float = 0f,
    val parallax: Boolean = true
)

@Serializable
enum class GestureAction(val label: String) {
    NONE("Nada"),
    DRAWER("Abrir el cajón de apps"),
    NOTIFICATIONS("Notificaciones del sistema"),
    SIMON_PANEL("Panel de Simón"),
    LOCK("Bloquear pantalla"),
    EDIT("Editar pantalla"),
    SEARCH("Buscar apps"),
    OPEN_SIMON("Abrir Simón"),
    FLASHLIGHT("Linterna")
}

@Serializable
enum class PageTransition(val label: String) {
    SLIDE("Deslizar"),
    CUBE("Cubo"),
    FADE("Fundido"),
    ZOOM("Zoom"),
    ROTATE("Giro"),
    STACK("Pila")
}

@Serializable
data class LauncherSettings(
    val pages: Int = 2,
    val background: LauncherBackground = LauncherBackground(),
    val hideStatusBar: Boolean = false,
    val hideNavBar: Boolean = false,
    val iconSizeDp: Int = 58,
    val showLabels: Boolean = true,
    val labelColor: Int = 0xFFFFFFFF.toInt(),
    val labelFont: FontStyle = FontStyle.SANS,
    val labelSizeSp: Int = 12,
    val labelShadow: Boolean = true,
    val globalStyle: IconStyle? = null,
    val iconPack: String? = null,
    val globalMotion: Motion = Motion.NONE,
    val dockEnabled: Boolean = true,
    val dockApps: List<String> = emptyList(),
    val dockColor: Int = 0x33FFFFFF,
    val dockRadius: Int = 28,
    val drawerColor: Int = 0xE6100C1C.toInt(),
    val drawerColumns: Int = 4,
    val drawerTextColor: Int = 0xFFFFFFFF.toInt(),
    val hiddenApps: List<String> = emptyList(),
    val swipeUp: GestureAction = GestureAction.DRAWER,
    val swipeDown: GestureAction = GestureAction.NOTIFICATIONS,
    val doubleTap: GestureAction = GestureAction.LOCK,
    val longPress: GestureAction = GestureAction.EDIT,
    val transition: PageTransition = PageTransition.SLIDE,
    val snapToGrid: Boolean = false,
    val gridColumns: Int = 4,
    val gridRows: Int = 6,
    val pageIndicator: Boolean = true,
    val initialized: Boolean = false
)

// ======================================================================
//  Constructor de widgets
// ======================================================================

@Serializable
enum class ElementType(val label: String) {
    TEXT("Texto"),
    CLOCK("Reloj de agujas"),
    SHAPE("Forma"),
    IMAGE("Foto"),
    VIDEO("Video"),
    DRAWING("Dibujo"),
    ANIMATION("Animación"),
    SCENE("Escena animada"),
    PROGRESS("Progreso"),
    APP_ICON("Ícono de app"),
    WEATHER_ICON("Ícono del clima"),
    STICKER("Sticker")
}

@Serializable
enum class ShapeKind(val label: String) {
    RECT("Rectángulo"),
    ROUNDED("Redondeado"),
    CIRCLE("Círculo"),
    STAR("Estrella"),
    HEART("Corazón"),
    TRIANGLE("Triángulo"),
    HEXAGON("Hexágono"),
    LINE("Línea"),
    SPARKLE("Destello")
}

@Serializable
enum class ProgressSource(val label: String) {
    BATTERY("Batería"),
    DAY("Día"),
    WEEK("Semana"),
    MONTH("Mes"),
    YEAR("Año"),
    CUSTOM("Valor fijo")
}

@Serializable
enum class ProgressStyle(val label: String) { BAR("Barra"), RING("Anillo"), DOTS("Puntos") }

@Serializable
enum class TapAction(val label: String) {
    NONE("Nada"),
    OPEN_APP("Abrir una app"),
    OPEN_URL("Abrir un link"),
    DIAL("Llamar a un número"),
    FLASHLIGHT("Prender/apagar linterna"),
    OPEN_DRAWER("Abrir cajón de apps"),
    OPEN_PANEL("Abrir panel de Simón"),
    OPEN_SIMON("Abrir Simón"),
    LOCK("Bloquear pantalla")
}

@Serializable
data class WElement(
    val id: String = newId(),
    val type: ElementType,
    val x: Float = 0.1f,
    val y: Float = 0.1f,
    val w: Float = 0.8f,
    val h: Float = 0.3f,
    val rotation: Float = 0f,
    val alpha: Float = 1f,
    // Texto
    val text: String = "",
    val font: FontStyle = FontStyle.SANS,
    val textColor: Int = 0xFFFFFFFF.toInt(),
    val align: Int = 1,
    val bold: Boolean = false,
    val shadow: Boolean = false,
    // Forma / colores
    val shape: ShapeKind = ShapeKind.ROUNDED,
    val fill1: Int = 0xFF8B6CFF.toInt(),
    val fill2: Int = 0xFFFF7EB6.toInt(),
    val gradient: Boolean = false,
    val gradientAngle: Int = 135,
    val strokeColor: Int = 0xFFFFFFFF.toInt(),
    val strokeWidth: Float = 0f,
    val corner: Float = 0.25f,
    // Medios
    val mediaPath: String? = null,
    val mediaId: String? = null,
    val liveId: String? = null,
    // Progreso
    val progress: ProgressSource = ProgressSource.BATTERY,
    val progressStyle: ProgressStyle = ProgressStyle.BAR,
    val customValue: Float = 0.5f,
    // Ícono de app
    val appKey: String? = null,
    // Movimiento y acción
    val motion: Motion = Motion.NONE,
    val motionSpeed: Float = 1f,
    val action: TapAction = TapAction.NONE,
    val actionData: String = "",
    // Capa oculta en el constructor (no se dibuja)
    val hidden: Boolean = false
)

@Serializable
data class CustomWidget(
    val id: String = newId(),
    val name: String = "Mi widget",
    val aspect: Float = 2f,
    val bg1: Int = 0xFF2A1B5C.toInt(),
    val bg2: Int = 0xFF6C4CF5.toInt(),
    val bgGradient: Boolean = true,
    val bgAngle: Int = 135,
    val bgAlpha: Float = 1f,
    val corner: Float = 0.12f,
    val bgImage: String? = null,
    val city: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val elements: List<WElement> = emptyList(),
    val action: TapAction = TapAction.OPEN_SIMON,
    val actionData: String = ""
)

// ======================================================================
//  Dibujos y animaciones
// ======================================================================

@Serializable
data class DrawingInfo(
    val id: String = newId(),
    val name: String,
    val width: Int,
    val height: Int,
    val updatedAt: Long = System.currentTimeMillis(),
    /** Fondo elegido en el editor (para volver a abrirlo igual). Null = transparente. */
    val bgColor: Int? = null,
    /** Foto de fondo elegida en el editor, copiada dentro de la app. */
    val bgImagePath: String? = null,
    /** True si además se guardó la capa de trazos sola (sin fondo) para seguir editando. */
    val hasLayer: Boolean = false
)

@Serializable
data class AnimationInfo(
    val id: String = newId(),
    val name: String,
    val width: Int,
    val height: Int,
    val frames: Int,
    val fps: Int = 8,
    val loop: Boolean = true,
    val bounce: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

// ======================================================================
//  Efectos del sistema y panel
// ======================================================================

@Serializable
enum class EffectStyle(val label: String) {
    EDGE_LIGHT("Bordes de luz"),
    RIPPLE("Ondas"),
    CONFETTI("Confeti"),
    HEARTS("Corazones"),
    SPARKLES("Destellos"),
    FIREWORK("Fuegos artificiales"),
    PULSE_ICON("Ícono que late"),
    CURTAIN("Telón de color"),
    USER_ANIMATION("Mi animación"),
    SCENE("Escena animada")
}

@Serializable
enum class AppFilter(val label: String) { ALL("Todas las apps"), ONLY("Solo estas apps"), EXCEPT("Todas menos estas") }

@Serializable
data class EffectConfig(
    val enabled: Boolean = false,
    val style: EffectStyle = EffectStyle.EDGE_LIGHT,
    val color1: Int = 0xFF8B6CFF.toInt(),
    val color2: Int = 0xFFFF7EB6.toInt(),
    val useAppColor: Boolean = true,
    val durationMs: Int = 2500,
    val thickness: Float = 1f,
    val showIcon: Boolean = true,
    val showText: Boolean = true,
    val text: String = "",
    val font: FontStyle = FontStyle.SANS_BOLD,
    val animationId: String? = null,
    val liveId: String? = null,
    val filter: AppFilter = AppFilter.ALL,
    val apps: List<String> = emptyList()
)

@Serializable
enum class PanelZone(val label: String) { LEFT("Izquierda"), CENTER("Centro"), RIGHT("Derecha"), FULL("Todo el ancho") }

@Serializable
enum class PanelTile(val label: String) {
    FLASHLIGHT("Linterna"),
    WIFI("Wi-Fi"),
    BLUETOOTH("Bluetooth"),
    MOBILE_DATA("Datos"),
    SOUND("Sonido"),
    DND("No molestar"),
    ROTATION("Rotación"),
    LOCATION("Ubicación"),
    AIRPLANE("Modo avión"),
    SCREENSHOT("Captura"),
    LOCK("Bloquear"),
    SETTINGS("Ajustes"),
    CAMERA("Cámara"),
    SIMON("Simón"),
    BATTERY("Batería"),
    NFC("NFC")
}

@Serializable
data class PanelSettings(
    val enabled: Boolean = false,
    val zone: PanelZone = PanelZone.RIGHT,
    val zoneWidth: Float = 0.35f,
    val bg1: Int = 0xF0140F24.toInt(),
    val bg2: Int = 0xF02A1B5C.toInt(),
    val bgImage: String? = null,
    val tileColor: Int = 0x33FFFFFF,
    val tileActive: Int = 0xFF8B6CFF.toInt(),
    val textColor: Int = 0xFFFFFFFF.toInt(),
    val tileShape: IconShape = IconShape.SQUIRCLE,
    val columns: Int = 4,
    val tiles: List<PanelTile> = listOf(
        PanelTile.FLASHLIGHT, PanelTile.WIFI, PanelTile.BLUETOOTH, PanelTile.SOUND,
        PanelTile.DND, PanelTile.ROTATION, PanelTile.SCREENSHOT, PanelTile.LOCK
    ),
    val showClock: Boolean = true,
    val clockFont: FontStyle = FontStyle.LIGHT,
    val showBrightness: Boolean = true,
    val showNotifications: Boolean = true,
    val corner: Int = 28
)

@Serializable
data class EffectsSettings(
    val notification: EffectConfig = EffectConfig(),
    val unlock: EffectConfig = EffectConfig(style = EffectStyle.RIPPLE, text = "¡Hola!", showIcon = false),
    val screenOn: EffectConfig = EffectConfig(style = EffectStyle.SPARKLES, showIcon = false, showText = false),
    val charging: EffectConfig = EffectConfig(style = EffectStyle.CURTAIN, color1 = 0xFF34C759.toInt(), color2 = 0xFF12D8FA.toInt(), showIcon = false),
    val panel: PanelSettings = PanelSettings()
)

// ======================================================================
//  Stickers
// ======================================================================

@Serializable
enum class BuiltinSticker(val label: String) {
    HEART("Corazón"), STAR("Estrella"), SPARKLE("Destello"), CROWN("Corona"),
    FLOWER("Flor"), MOON("Luna"), SUN("Sol"), CLOUD("Nube"),
    MUSIC("Nota musical"), BOLT("Rayo"), FIRE("Fuego"), BOW("Moño"),
    CAT("Gatito"), PAW("Huellita"), SMILE("Carita"), DROP("Gota")
}

@Serializable
enum class StickerKind(val label: String) { BUILTIN("Prediseñado"), IMAGE("Foto"), DRAWING("Dibujo"), ANIMATION("Animación") }

@Serializable
data class StickerInfo(
    val id: String = newId(),
    val name: String,
    val kind: StickerKind,
    val builtin: BuiltinSticker? = null,
    val color1: Int = 0xFFFF4F81.toInt(),
    val color2: Int = 0xFFFF9EC4.toInt(),
    val outline: Boolean = true,
    val path: String? = null,
    val mediaId: String? = null
)

/** Un sticker pegado sobre algo. Centro y tamaño relativos al elemento que lo lleva. */
@Serializable
data class StickerAttach(
    val id: String = newId(),
    val stickerId: String,
    val x: Float = 0.8f,
    val y: Float = 0.2f,
    val size: Float = 0.4f,
    val rotation: Float = 0f,
    val motion: Motion = Motion.NONE,
    val motionSpeed: Float = 1f
)

// ======================================================================
//  Modo de uso y rendimiento
// ======================================================================

/**
 * Simón funciona completo con el launcher original del teléfono. El launcher
 * propio y la capa decorativa son opcionales y se activan por separado.
 */
@Serializable
data class AppMode(
    val launcherEnabled: Boolean = false,
    val overlayEnabled: Boolean = false
)

@Serializable
data class PerformanceSettings(
    val maxFps: Int = 30,
    val onlyVisiblePage: Boolean = true,
    val reduceMotion: Boolean = false,
    val allowVideo: Boolean = true,
    val batterySaverPause: Boolean = true
)
