package com.migux97.simon.canva

import com.migux97.simon.data.CustomWidget
import com.migux97.simon.data.ElementType
import com.migux97.simon.data.FontStyle
import com.migux97.simon.data.Motion
import com.migux97.simon.data.ProgressSource
import com.migux97.simon.data.ProgressStyle
import com.migux97.simon.data.ShapeKind
import com.migux97.simon.data.WElement
import com.migux97.simon.data.newId

private const val WHITE = 0xFFFFFFFF.toInt()

/**
 * Algo listo para agregar: uno o más elementos dentro de un bloque. Las
 * medidas internas son fracciones del bloque; el bloque mide [w] del ancho del
 * lienzo y su alto es [hW] veces ese mismo ancho (así se ve igual en un widget
 * apaisado que en la pantalla del teléfono).
 */
class ElementBlock(val label: String, val w: Float, val hW: Float, val elements: List<WElement>)

/** Ubica el bloque centrado en el lienzo (corrido un poco si ya hay otros) con ids nuevos. */
fun ElementBlock.placeOn(aspect: Float, existing: Int): List<WElement> {
    val bw = w.coerceAtMost(0.96f)
    // hW está en anchos del lienzo; como fracción del alto hay que multiplicar por ancho/alto.
    // Si el bloque se achicó para entrar, el alto se achica igual.
    val bh = (hW * (bw / w) * aspect).coerceIn(0.02f, 0.9f)
    val off = (existing % 6) * 0.025f
    val bx = ((1f - bw) / 2f + off).coerceIn(0f, 1f - bw)
    val by = ((1f - bh) / 2f + off).coerceIn(0f, 1f - bh)
    val group = if (elements.size > 1) newId() else null
    return elements.map { e ->
        e.copy(id = newId(), x = bx + e.x * bw, y = by + e.y * bh, w = e.w * bw, h = e.h * bh, groupId = group)
    }
}

private fun text(t: String, font: FontStyle = FontStyle.SANS, color: Int = WHITE, bold: Boolean = false, shadow: Boolean = true, x: Float = 0f, y: Float = 0f, w: Float = 1f, h: Float = 1f, align: Int = 1) =
    WElement(type = ElementType.TEXT, text = t, font = font, textColor = color, bold = bold, shadow = shadow, x = x, y = y, w = w, h = h, align = align)

private fun shape(kind: ShapeKind, c1: Int, c2: Int = c1, gradient: Boolean = false, x: Float = 0f, y: Float = 0f, w: Float = 1f, h: Float = 1f, alpha: Float = 1f, motion: Motion = Motion.NONE, corner: Float = 0.25f) =
    WElement(type = ElementType.SHAPE, shape = kind, fill1 = c1, fill2 = c2, gradient = gradient, x = x, y = y, w = w, h = h, alpha = alpha, motion = motion, corner = corner)

object CanvaPresets {

    // ------------------------------------------------------------ texto

    val textStyles = listOf(
        ElementBlock("Agregar un título", 0.84f, 0.17f, listOf(text("Tu título", FontStyle.BLACK))),
        ElementBlock("Agregar un subtítulo", 0.72f, 0.11f, listOf(text("Un subtítulo", FontStyle.SANS_BOLD))),
        ElementBlock("Agregar un poco de texto", 0.62f, 0.07f, listOf(text("Escribí lo que quieras", FontStyle.SANS)))
    )

    /** Datos que se actualizan solos. */
    val liveTexts = listOf(
        ElementBlock("Hora", 0.7f, 0.24f, listOf(text("{hora}", FontStyle.LIGHT))),
        ElementBlock("Hora 12 h", 0.7f, 0.24f, listOf(text("{hora12} {ampm}", FontStyle.LIGHT))),
        ElementBlock("Fecha", 0.7f, 0.08f, listOf(text("{dia} {num} de {mes}"))),
        ElementBlock("Saludo", 0.66f, 0.12f, listOf(text("{saludo}", FontStyle.CURSIVE, 0xFFFFD166.toInt()))),
        ElementBlock("Batería", 0.36f, 0.12f, listOf(text("{bateria}", FontStyle.SANS_BOLD))),
        ElementBlock("Clima", 0.36f, 0.14f, listOf(text("{clima}", FontStyle.LIGHT))),
        ElementBlock("Año", 0.34f, 0.12f, listOf(text("{año}", FontStyle.MONO))),
        ElementBlock("% del día", 0.4f, 0.1f, listOf(text("{progresodia}", FontStyle.CONDENSED)))
    )

    /** Combinaciones de letras ya armadas. */
    val textCombos = listOf(
        ElementBlock("Hora y fecha", 0.8f, 0.34f, listOf(
            text("{hora}", FontStyle.LIGHT, y = 0f, h = 0.72f),
            text("{dia} {num} de {mes}", FontStyle.SANS, y = 0.74f, h = 0.24f)
        )),
        ElementBlock("Saludo y hora", 0.8f, 0.36f, listOf(
            text("{saludo}", FontStyle.CURSIVE, 0xFFFFD166.toInt(), h = 0.34f),
            text("{hora}", FontStyle.BLACK, y = 0.36f, h = 0.64f)
        )),
        ElementBlock("Frase con líneas", 0.86f, 0.2f, listOf(
            shape(ShapeKind.LINE, WHITE, x = 0.2f, y = 0f, w = 0.6f, h = 0.12f, alpha = 0.8f),
            text("Hoy va a ser un gran día", FontStyle.SERIF, y = 0.22f, h = 0.56f),
            shape(ShapeKind.LINE, WHITE, x = 0.2f, y = 0.88f, w = 0.6f, h = 0.12f, alpha = 0.8f)
        )),
        ElementBlock("Etiqueta", 0.6f, 0.14f, listOf(
            shape(ShapeKind.ROUNDED, 0xFFFF7EB6.toInt(), 0xFF8B6CFF.toInt(), gradient = true, corner = 0.5f),
            text("¡Nuevo!", FontStyle.SANS_BOLD, shadow = false, x = 0.08f, y = 0.18f, w = 0.84f, h = 0.64f)
        )),
        ElementBlock("Número grande", 0.7f, 0.3f, listOf(
            text("{num}", FontStyle.BLACK, x = 0f, w = 0.42f, align = 2),
            text("{dia}", FontStyle.SANS_BOLD, x = 0.48f, y = 0.18f, w = 0.52f, h = 0.32f, align = 0),
            text("{mes}", FontStyle.SANS, x = 0.48f, y = 0.52f, w = 0.52f, h = 0.28f, align = 0)
        ))
    )

    // ---------------------------------------------------------- elementos

    private val shapeColors = mapOf(
        ShapeKind.RECT to (0xFF8B6CFF.toInt() to 0xFF12D8FA.toInt()),
        ShapeKind.ROUNDED to (0xFFFF7EB6.toInt() to 0xFF8B6CFF.toInt()),
        ShapeKind.CIRCLE to (0xFFFFD166.toInt() to 0xFFFF7E5F.toInt()),
        ShapeKind.STAR to (0xFFFFC94D.toInt() to 0xFFFF7E5F.toInt()),
        ShapeKind.HEART to (0xFFFF4F81.toInt() to 0xFFFF9EC4.toInt()),
        ShapeKind.TRIANGLE to (0xFF34C759.toInt() to 0xFFD4FC79.toInt()),
        ShapeKind.HEXAGON to (0xFF12D8FA.toInt() to 0xFF8B6CFF.toInt()),
        ShapeKind.LINE to (WHITE to WHITE),
        ShapeKind.SPARKLE to (WHITE to 0xFFFFD166.toInt())
    )

    val shapes = ShapeKind.entries.map { k ->
        val (c1, c2) = shapeColors[k] ?: (0xFF8B6CFF.toInt() to 0xFFFF7EB6.toInt())
        if (k == ShapeKind.LINE) ElementBlock(k.label, 0.7f, 0.04f, listOf(shape(k, c1)))
        else ElementBlock(k.label, 0.34f, 0.34f, listOf(shape(k, c1, c2, gradient = k != ShapeKind.SPARKLE)))
    }

    val graphics = listOf(
        ElementBlock("Barra de batería", 0.8f, 0.07f, listOf(
            WElement(type = ElementType.PROGRESS, x = 0f, y = 0f, w = 1f, h = 1f, progress = ProgressSource.BATTERY, progressStyle = ProgressStyle.BAR, fill1 = 0xFF34C759.toInt(), fill2 = WHITE)
        )),
        ElementBlock("Anillo de batería", 0.4f, 0.4f, listOf(
            WElement(type = ElementType.PROGRESS, x = 0f, y = 0f, w = 1f, h = 1f, progress = ProgressSource.BATTERY, progressStyle = ProgressStyle.RING, fill1 = 0xFF34C759.toInt(), fill2 = WHITE),
            text("{bateria}", FontStyle.SANS_BOLD, x = 0.2f, y = 0.36f, w = 0.6f, h = 0.28f)
        )),
        ElementBlock("Progreso del día", 0.8f, 0.07f, listOf(
            WElement(type = ElementType.PROGRESS, x = 0f, y = 0f, w = 1f, h = 1f, progress = ProgressSource.DAY, progressStyle = ProgressStyle.DOTS, fill1 = 0xFFFFD166.toInt(), fill2 = WHITE)
        )),
        ElementBlock("Progreso del año", 0.4f, 0.4f, listOf(
            WElement(type = ElementType.PROGRESS, x = 0f, y = 0f, w = 1f, h = 1f, progress = ProgressSource.YEAR, progressStyle = ProgressStyle.RING, fill1 = 0xFFFF9F0A.toInt(), fill2 = WHITE),
            text("{año}", FontStyle.MONO, x = 0.2f, y = 0.38f, w = 0.6f, h = 0.24f)
        )),
        ElementBlock("Reloj de agujas", 0.44f, 0.44f, listOf(
            WElement(type = ElementType.CLOCK, x = 0f, y = 0f, w = 1f, h = 1f, fill1 = WHITE, fill2 = 0xFFFF7EB6.toInt())
        ))
    )

    /** Movimiento lindo por defecto para algunas formas (corazones que laten, destellos que brillan). */
    fun withMotion(e: WElement): WElement = when (e.shape) {
        ShapeKind.HEART -> e.copy(motion = Motion.PULSE)
        ShapeKind.SPARKLE -> e.copy(motion = Motion.BLINK)
        else -> e
    }
}

// ======================================================================
//  Plantillas completas
// ======================================================================

/** Diseños completos de widget para empezar. Se aplican sobre el widget que estás editando. */
object WidgetTemplates {
    val all: List<CustomWidget> by lazy {
        listOf(
            CustomWidget(
                name = "Saludo con hora", aspect = 2.1f, bg1 = 0xFF0F2027.toInt(), bg2 = 0xFF2C5364.toInt(),
                elements = listOf(
                    text("{saludo}", FontStyle.CURSIVE, 0xFFFFD166.toInt(), shadow = false, x = 0.06f, y = 0.1f, w = 0.6f, h = 0.2f, align = 0),
                    text("{hora}", FontStyle.LIGHT, shadow = false, x = 0.06f, y = 0.3f, w = 0.55f, h = 0.42f, align = 0),
                    text("{dia} {num} de {mes}", shadow = false, x = 0.06f, y = 0.74f, w = 0.6f, h = 0.15f, align = 0),
                    WElement(type = ElementType.PROGRESS, progress = ProgressSource.BATTERY, progressStyle = ProgressStyle.RING, x = 0.68f, y = 0.18f, w = 0.27f, h = 0.64f, fill1 = 0xFF34C759.toInt(), fill2 = WHITE),
                    text("{bateria}", shadow = false, x = 0.7f, y = 0.4f, w = 0.23f, h = 0.2f)
                )
            ),
            CustomWidget(
                name = "Corazones", aspect = 1f, bg1 = 0xFFFFDDE1.toInt(), bg2 = 0xFFEE9CA7.toInt(),
                elements = listOf(
                    shape(ShapeKind.HEART, 0xFFFF4F81.toInt(), x = 0.25f, y = 0.12f, w = 0.5f, h = 0.5f, motion = Motion.PULSE),
                    text("{hora}", FontStyle.CASUAL, 0xFF5A2340.toInt(), shadow = false, x = 0.1f, y = 0.62f, w = 0.8f, h = 0.22f),
                    shape(ShapeKind.SPARKLE, WHITE, x = 0.75f, y = 0.08f, w = 0.15f, h = 0.15f, motion = Motion.BLINK)
                )
            ),
            CustomWidget(
                name = "Progreso del año", aspect = 3f, bg1 = 0xFF1C1C1E.toInt(), bg2 = 0xFF2C2C2E.toInt(), bgGradient = false,
                elements = listOf(
                    text("{año}", FontStyle.MONO, 0xFFFF9F0A.toInt(), shadow = false, x = 0.05f, y = 0.12f, w = 0.3f, h = 0.35f, align = 0),
                    WElement(type = ElementType.PROGRESS, progress = ProgressSource.YEAR, progressStyle = ProgressStyle.DOTS, x = 0.05f, y = 0.58f, w = 0.9f, h = 0.25f, fill1 = 0xFFFF9F0A.toInt(), fill2 = WHITE)
                )
            ),
            CustomWidget(
                name = "Fecha grande", aspect = 2f, bg1 = 0xFFFF7E5F.toInt(), bg2 = 0xFFFEB47B.toInt(),
                elements = listOf(
                    text("{num}", FontStyle.BLACK, x = 0.05f, y = 0.1f, w = 0.42f, h = 0.8f, align = 2),
                    text("{dia}", FontStyle.SANS_BOLD, x = 0.52f, y = 0.26f, w = 0.44f, h = 0.24f, align = 0),
                    text("{mes} {año}", FontStyle.SANS, x = 0.52f, y = 0.52f, w = 0.44f, h = 0.2f, align = 0)
                )
            ),
            CustomWidget(
                name = "Batería", aspect = 1f, bg1 = 0xFF0B1D13.toInt(), bg2 = 0xFF134E5E.toInt(),
                elements = listOf(
                    WElement(type = ElementType.PROGRESS, progress = ProgressSource.BATTERY, progressStyle = ProgressStyle.RING, x = 0.14f, y = 0.1f, w = 0.72f, h = 0.72f, fill1 = 0xFF34C759.toInt(), fill2 = 0xFFD4FC79.toInt(), gradient = true),
                    text("{bateria}", FontStyle.SANS_BOLD, shadow = false, x = 0.25f, y = 0.34f, w = 0.5f, h = 0.24f),
                    text("{cargando}", FontStyle.SANS, 0xFFD4FC79.toInt(), shadow = false, x = 0.1f, y = 0.84f, w = 0.8f, h = 0.12f)
                )
            ),
            CustomWidget(
                name = "Reloj de agujas", aspect = 1f, bg1 = 0xFF2A1B5C.toInt(), bg2 = 0xFF6C4CF5.toInt(),
                elements = listOf(
                    WElement(type = ElementType.CLOCK, x = 0.12f, y = 0.06f, w = 0.76f, h = 0.76f, fill1 = WHITE, fill2 = 0xFFFF7EB6.toInt()),
                    text("{diacorto} {num}", FontStyle.SANS_BOLD, shadow = false, x = 0.2f, y = 0.83f, w = 0.6f, h = 0.12f)
                )
            ),
            CustomWidget(
                name = "Frase", aspect = 3f, bg1 = 0xFFFFFFFF.toInt(), bg2 = 0xFFF3E7E9.toInt(),
                elements = listOf(
                    text("«Hacé de hoy un gran día»", FontStyle.SERIF, 0xFF3A2E39.toInt(), shadow = false, x = 0.06f, y = 0.18f, w = 0.88f, h = 0.4f),
                    text("— {dia}", FontStyle.CURSIVE, 0xFF8B6CFF.toInt(), shadow = false, x = 0.5f, y = 0.62f, w = 0.44f, h = 0.22f, align = 2)
                )
            ),
            CustomWidget(
                name = "Clima", aspect = 2f, bg1 = 0xFF1FA2FF.toInt(), bg2 = 0xFF12D8FA.toInt(),
                elements = listOf(
                    WElement(type = ElementType.WEATHER_ICON, x = 0.05f, y = 0.12f, w = 0.36f, h = 0.76f),
                    text("{clima}", FontStyle.LIGHT, shadow = false, x = 0.45f, y = 0.1f, w = 0.5f, h = 0.46f, align = 0),
                    text("{ciudad}", FontStyle.SANS_BOLD, shadow = false, x = 0.45f, y = 0.58f, w = 0.5f, h = 0.16f, align = 0),
                    text("{cielo}", FontStyle.SANS, shadow = false, x = 0.45f, y = 0.75f, w = 0.5f, h = 0.13f, align = 0)
                )
            )
        )
    }
}

/** Una página completa: elementos con posiciones en fracciones de la pantalla. */
class PageTemplate(val name: String, val elements: List<WElement>)

object PageTemplates {
    val all: List<PageTemplate> by lazy {
        listOf(
            PageTemplate("Reloj gigante", listOf(
                text("{hora}", FontStyle.LIGHT, x = 0.06f, y = 0.3f, w = 0.88f, h = 0.13f),
                text("{dia} {num} de {mes}", FontStyle.SANS, x = 0.15f, y = 0.44f, w = 0.7f, h = 0.035f)
            )),
            PageTemplate("Saludo", listOf(
                text("{saludo}", FontStyle.CURSIVE, 0xFFFFD166.toInt(), x = 0.08f, y = 0.1f, w = 0.84f, h = 0.06f, align = 0),
                text("{hora}", FontStyle.BLACK, x = 0.08f, y = 0.165f, w = 0.6f, h = 0.08f, align = 0),
                text("{fecha}", FontStyle.SANS, x = 0.08f, y = 0.25f, w = 0.7f, h = 0.03f, align = 0)
            )),
            PageTemplate("Tarjeta", listOf(
                shape(ShapeKind.ROUNDED, 0x55FFFFFF, x = 0.06f, y = 0.09f, w = 0.88f, h = 0.15f, corner = 0.22f),
                text("{hora}", FontStyle.LIGHT, x = 0.1f, y = 0.1f, w = 0.5f, h = 0.08f, align = 0),
                text("{dia} {num} de {mes}", FontStyle.SANS_BOLD, x = 0.1f, y = 0.185f, w = 0.6f, h = 0.03f, align = 0),
                WElement(type = ElementType.PROGRESS, progress = ProgressSource.BATTERY, progressStyle = ProgressStyle.RING, x = 0.68f, y = 0.115f, w = 0.2f, h = 0.09f, fill1 = 0xFF34C759.toInt(), fill2 = WHITE),
                text("{bateria}", FontStyle.SANS_BOLD, x = 0.71f, y = 0.145f, w = 0.14f, h = 0.03f)
            )),
            PageTemplate("Corazones", listOf(
                shape(ShapeKind.HEART, 0xFFFF4F81.toInt(), 0xFFFF9EC4.toInt(), gradient = true, x = 0.08f, y = 0.12f, w = 0.18f, h = 0.08f, motion = Motion.PULSE),
                shape(ShapeKind.HEART, 0xFFFF9EC4.toInt(), x = 0.74f, y = 0.2f, w = 0.14f, h = 0.06f, motion = Motion.FLOAT),
                shape(ShapeKind.HEART, 0xFFFF4F81.toInt(), x = 0.16f, y = 0.7f, w = 0.12f, h = 0.055f, motion = Motion.FLOAT),
                shape(ShapeKind.HEART, 0xFFFF7EB6.toInt(), 0xFF8B6CFF.toInt(), gradient = true, x = 0.7f, y = 0.66f, w = 0.2f, h = 0.09f, motion = Motion.PULSE),
                text("te quiero", FontStyle.CURSIVE, 0xFFFFDDE1.toInt(), x = 0.2f, y = 0.46f, w = 0.6f, h = 0.06f)
            )),
            PageTemplate("Brillos", listOf(
                shape(ShapeKind.SPARKLE, WHITE, x = 0.06f, y = 0.08f, w = 0.12f, h = 0.055f, motion = Motion.BLINK),
                shape(ShapeKind.SPARKLE, 0xFFFFD166.toInt(), x = 0.8f, y = 0.14f, w = 0.1f, h = 0.045f, motion = Motion.BLINK),
                shape(ShapeKind.SPARKLE, WHITE, x = 0.12f, y = 0.62f, w = 0.08f, h = 0.036f, motion = Motion.BLINK),
                shape(ShapeKind.SPARKLE, 0xFFFFD166.toInt(), x = 0.84f, y = 0.72f, w = 0.12f, h = 0.055f, motion = Motion.BLINK),
                shape(ShapeKind.STAR, 0xFFFFC94D.toInt(), 0xFFFF7E5F.toInt(), gradient = true, x = 0.44f, y = 0.05f, w = 0.12f, h = 0.055f, motion = Motion.SPIN)
            )),
            PageTemplate("Batería y día", listOf(
                WElement(type = ElementType.PROGRESS, progress = ProgressSource.BATTERY, progressStyle = ProgressStyle.RING, x = 0.3f, y = 0.32f, w = 0.4f, h = 0.18f, fill1 = 0xFF34C759.toInt(), fill2 = WHITE),
                text("{bateria}", FontStyle.SANS_BOLD, x = 0.36f, y = 0.385f, w = 0.28f, h = 0.05f),
                WElement(type = ElementType.PROGRESS, progress = ProgressSource.DAY, progressStyle = ProgressStyle.DOTS, x = 0.12f, y = 0.54f, w = 0.76f, h = 0.025f, fill1 = 0xFFFFD166.toInt(), fill2 = WHITE),
                text("Ya pasó el {progresodia} del día", FontStyle.SANS, x = 0.12f, y = 0.575f, w = 0.76f, h = 0.025f)
            )),
            PageTemplate("Frase del día", listOf(
                shape(ShapeKind.LINE, WHITE, x = 0.3f, y = 0.4f, w = 0.4f, h = 0.008f, alpha = 0.8f),
                text("Hoy va a ser un gran día", FontStyle.SERIF, x = 0.08f, y = 0.415f, w = 0.84f, h = 0.045f),
                shape(ShapeKind.LINE, WHITE, x = 0.3f, y = 0.47f, w = 0.4f, h = 0.008f, alpha = 0.8f)
            )),
            PageTemplate("Reloj de agujas", listOf(
                WElement(type = ElementType.CLOCK, x = 0.2f, y = 0.18f, w = 0.6f, h = 0.27f, fill1 = WHITE, fill2 = 0xFFFF7EB6.toInt()),
                text("{dia} {num}", FontStyle.SANS_BOLD, x = 0.25f, y = 0.46f, w = 0.5f, h = 0.035f)
            ))
        )
    }
}
