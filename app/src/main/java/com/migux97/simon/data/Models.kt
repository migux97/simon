package com.migux97.simon.data

import kotlinx.serialization.Serializable
import java.util.UUID

/** Tipos de widget disponibles. */
@Serializable
enum class WidgetKind(val label: String, val description: String) {
    DIGITAL_CLOCK("Reloj digital", "Hora y fecha en grande"),
    ANALOG_CLOCK("Reloj analógico", "Agujas con tu estilo"),
    CALENDAR("Calendario", "El mes o el día de hoy"),
    BATTERY("Batería", "Nivel y estado de carga"),
    PHOTO("Foto", "Tu foto favorita con marco"),
    QUOTE("Frase", "Una frase o nota"),
    COUPLE("Pareja", "Días juntos con dos fotos"),
    COUNTDOWN("Cuenta regresiva", "Días que faltan para algo"),
    WEATHER("Clima", "Temperatura de tu ciudad");

    /** Los widgets que dependen de la hora se refrescan cada minuto. */
    val needsMinuteTick: Boolean
        get() = this == DIGITAL_CLOCK || this == ANALOG_CLOCK
}

@Serializable
enum class WidgetSize(val label: String, val cellsW: Int, val cellsH: Int) {
    SMALL("Pequeño", 2, 2),
    MEDIUM("Mediano", 4, 2),
    LARGE("Grande", 4, 4);

    /** Proporción ancho/alto aproximada usada para las vistas previas. */
    val aspect: Float
        get() = when (this) {
            SMALL -> 1f
            MEDIUM -> 2.15f
            LARGE -> 1.05f
        }
}

@Serializable
enum class BgStyle(val label: String) {
    SOLID("Color"),
    GRADIENT("Degradado"),
    PHOTO("Foto"),
    GLASS("Vidrio")
}

@Serializable
enum class FontStyle(val label: String, val family: String, val bold: Boolean = false) {
    SANS("Moderna", "sans-serif"),
    SANS_BOLD("Negrita", "sans-serif", true),
    LIGHT("Fina", "sans-serif-light"),
    BLACK("Gruesa", "sans-serif-black"),
    CONDENSED("Angosta", "sans-serif-condensed"),
    SERIF("Clásica", "serif"),
    MONO("Código", "monospace"),
    CURSIVE("Manuscrita", "cursive"),
    CASUAL("Divertida", "casual")
}

@Serializable
enum class Decoration(val label: String) {
    NONE("Ninguna"),
    STARS("Estrellas"),
    HEARTS("Corazones"),
    DOTS("Puntos"),
    WAVES("Ondas"),
    BUBBLES("Burbujas"),
    SPARKLES("Destellos")
}

/** Un diseño de widget completo y editable. */
@Serializable
data class WidgetDesign(
    val id: String = newId(),
    val name: String,
    val kind: WidgetKind,
    val size: WidgetSize,
    val bgStyle: BgStyle = BgStyle.GRADIENT,
    val color1: Int,
    val color2: Int,
    val textColor: Int = 0xFFFFFFFF.toInt(),
    val accentColor: Int = 0xFFFFD166.toInt(),
    val gradientAngle: Int = 135,
    val cornerRadius: Int = 26,
    val bgAlpha: Int = 255,
    val font: FontStyle = FontStyle.SANS,
    val decoration: Decoration = Decoration.NONE,
    val photoPath: String? = null,
    val photoPath2: String? = null,
    val text: String = "",
    val text2: String = "",
    val dateMillis: Long = 0L,
    val use24h: Boolean = true,
    val city: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0
)

@Serializable
enum class SceneType(val label: String) {
    AURORA("Aurora"),
    GRADIENT_FLOW("Degradado vivo"),
    STARFIELD("Estrellas"),
    BUBBLES("Burbujas"),
    RAIN("Lluvia"),
    SNOW("Nieve"),
    FIREFLIES("Luciérnagas"),
    WAVES("Olas"),
    CONFETTI("Confeti"),
    HEARTS("Corazones"),
    MATRIX("Código"),
    NEON_GRID("Neón retro"),
    VIDEO("Mi video")
}

/** Configuración del fondo animado activo. */
@Serializable
data class LiveConfig(
    val id: String = newId(),
    val name: String,
    val scene: SceneType,
    val color1: Int,
    val color2: Int,
    val color3: Int,
    val speed: Float = 1f,
    val density: Float = 1f,
    val videoPath: String? = null,
    val showClock: Boolean = false,
    val clockColor: Int = 0xFFFFFFFF.toInt(),
    val clockFont: FontStyle = FontStyle.LIGHT
)

@Serializable
enum class StaticPattern(val label: String) {
    LINEAR("Degradado"),
    RADIAL("Resplandor"),
    MESH("Malla"),
    WAVES("Ondas"),
    DOTS("Lunares"),
    STRIPES("Rayas"),
    BLOBS("Manchas"),
    SUNSET("Atardecer")
}

/** Fondo estático generado en el dispositivo. */
@Serializable
data class StaticWallpaper(
    val id: String = newId(),
    val name: String,
    val pattern: StaticPattern,
    val color1: Int,
    val color2: Int,
    val color3: Int
)

@Serializable
enum class IconShape(val label: String) {
    CIRCLE("Círculo"),
    SQUIRCLE("Suave"),
    ROUNDED("Redondeado"),
    SQUARE("Cuadrado"),
    TEARDROP("Gota"),
    HEXAGON("Hexágono"),
    FLOWER("Flor")
}

@Serializable
enum class IconForeground(val label: String) {
    ORIGINAL("Ícono original"),
    TINTED("Ícono a color"),
    LETTER("Inicial")
}

/** Estilo para los íconos personalizados. */
@Serializable
data class IconStyle(
    val shape: IconShape = IconShape.SQUIRCLE,
    val bg1: Int,
    val bg2: Int,
    val foreground: IconForeground = IconForeground.TINTED,
    val fgColor: Int = 0xFFFFFFFF.toInt(),
    val font: FontStyle = FontStyle.SANS_BOLD
)

/** Un acceso directo que el usuario creó con un ícono personalizado. */
@Serializable
data class CustomIcon(
    val id: String = newId(),
    val packageName: String,
    val label: String,
    val style: IconStyle,
    val customImagePath: String? = null
)

/** Un tema completo: fondo + widgets + estilo de íconos. */
data class ThemePack(
    val id: String,
    val name: String,
    val description: String,
    val wallpaper: StaticWallpaper,
    val live: LiveConfig?,
    val widgets: List<WidgetDesign>,
    val iconStyle: IconStyle
)

fun newId(): String = UUID.randomUUID().toString()
