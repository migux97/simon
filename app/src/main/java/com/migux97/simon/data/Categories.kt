package com.migux97.simon.data

/** Categorías para encontrar las cosas por tema. Una cosa puede estar en varias. */
enum class Category(val label: String, val emoji: String) {
    RELOJ("Reloj", "⏰"),
    FECHA("Fecha y calendario", "📅"),
    PAREJA("Pareja y amor", "💕"),
    FRASES("Frases", "💬"),
    CLIMA("Clima", "⛅"),
    BATERIA("Batería y progreso", "🔋"),
    MINIMAL("Minimalista", "◻️"),
    ESTETICO("Estético", "✨"),
    TIERNO("Divertido y tierno", "🧸"),
    OSCURO("Oscuro y neón", "🌙"),
    NATURALEZA("Naturaleza", "🌿"),
    FIESTAS("Fiestas", "🎉")
}

/** Qué categorías tiene cada cosa del catálogo. */
object Categories {

    /** El estilo que aporta cada paleta. */
    private val byPalette: Map<String, Set<Category>> = mapOf(
        "lavanda" to setOf(Category.ESTETICO),
        "oceano" to setOf(Category.NATURALEZA),
        "atardecer" to setOf(Category.ESTETICO, Category.NATURALEZA),
        "menta" to setOf(Category.TIERNO, Category.NATURALEZA),
        "noche" to setOf(Category.OSCURO),
        "rosa" to setOf(Category.PAREJA, Category.TIERNO),
        "carbon" to setOf(Category.MINIMAL, Category.OSCURO),
        "papel" to setOf(Category.MINIMAL),
        "vidrio" to setOf(Category.MINIMAL, Category.ESTETICO),
        "neon" to setOf(Category.OSCURO),
        "durazno" to setOf(Category.ESTETICO, Category.TIERNO),
        "bosque" to setOf(Category.NATURALEZA),
        "limon" to setOf(Category.TIERNO),
        "cielo" to setOf(Category.ESTETICO),
        "cereza" to setOf(Category.PAREJA),
        "uva" to setOf(Category.OSCURO, Category.ESTETICO)
    )

    private val paletteByColor: Map<Int, String> by lazy { Catalog.palettes.associate { it.c1 to it.key } }

    /** La categoría que mejor lo representa (para elegir la muestra de cada tarjeta). */
    fun main(d: WidgetDesign): Category? = of(d).firstOrNull { c -> kindOf(d).contains(c) }

    private fun kindOf(d: WidgetDesign) = when (d.kind) {
        WidgetKind.DIGITAL_CLOCK, WidgetKind.ANALOG_CLOCK -> setOf(Category.RELOJ)
        WidgetKind.CALENDAR, WidgetKind.COUNTDOWN -> setOf(Category.FECHA)
        WidgetKind.BATTERY -> setOf(Category.BATERIA)
        WidgetKind.COUPLE -> setOf(Category.PAREJA)
        // Un widget de foto vacío no es una buena muestra: va después de los de pareja.
        WidgetKind.PHOTO -> emptySet()
        WidgetKind.QUOTE -> setOf(Category.FRASES)
        WidgetKind.WEATHER -> setOf(Category.CLIMA)
    }

    fun of(d: WidgetDesign): Set<Category> {
        val kind = when (d.kind) {
            WidgetKind.DIGITAL_CLOCK, WidgetKind.ANALOG_CLOCK -> setOf(Category.RELOJ)
            WidgetKind.CALENDAR -> setOf(Category.FECHA)
            WidgetKind.COUNTDOWN -> setOf(Category.FECHA) +
                if (d.text.contains("cumple", true) || d.text.contains("año", true) || d.text.contains("vacaciones", true)) setOf(Category.FIESTAS) else emptySet()
            WidgetKind.BATTERY -> setOf(Category.BATERIA)
            WidgetKind.PHOTO -> setOf(Category.PAREJA, Category.ESTETICO)
            WidgetKind.QUOTE -> setOf(Category.FRASES)
            WidgetKind.COUPLE -> setOf(Category.PAREJA)
            WidgetKind.WEATHER -> setOf(Category.CLIMA)
        }
        val style = paletteByColor[d.color1]?.let { byPalette[it] } ?: emptySet()
        return kind + style
    }

    fun of(sw: StaticWallpaper): Set<Category> {
        val key = sw.id.removePrefix("st_").substringBeforeLast('_')
        return (byPalette[key] ?: emptySet()).ifEmpty { setOf(Category.ESTETICO) }
    }

    fun of(lv: LiveConfig): Set<Category> = when (lv.scene) {
        SceneType.AURORA -> setOf(Category.ESTETICO, Category.OSCURO, Category.NATURALEZA)
        SceneType.GRADIENT_FLOW -> setOf(Category.ESTETICO, Category.MINIMAL)
        SceneType.STARFIELD -> setOf(Category.OSCURO, Category.ESTETICO)
        SceneType.BUBBLES -> setOf(Category.TIERNO)
        SceneType.RAIN -> setOf(Category.CLIMA, Category.NATURALEZA, Category.OSCURO)
        SceneType.SNOW -> setOf(Category.CLIMA, Category.NATURALEZA, Category.FIESTAS)
        SceneType.FIREFLIES -> setOf(Category.NATURALEZA, Category.OSCURO)
        SceneType.WAVES -> setOf(Category.NATURALEZA)
        SceneType.CONFETTI -> setOf(Category.FIESTAS, Category.TIERNO)
        SceneType.HEARTS -> setOf(Category.PAREJA, Category.TIERNO)
        SceneType.MATRIX, SceneType.NEON_GRID -> setOf(Category.OSCURO)
        else -> setOf(Category.ESTETICO)
    }

    fun of(t: ThemePack): Set<Category> = when (t.id) {
        "sueno" -> setOf(Category.ESTETICO)
        "marea" -> setOf(Category.NATURALEZA, Category.CLIMA)
        "medianoche" -> setOf(Category.OSCURO)
        "enamorados" -> setOf(Category.PAREJA, Category.TIERNO)
        "minimal" -> setOf(Category.MINIMAL, Category.OSCURO)
        "cuaderno" -> setOf(Category.MINIMAL, Category.FRASES)
        "retro" -> setOf(Category.OSCURO)
        "ocaso" -> setOf(Category.ESTETICO, Category.NATURALEZA)
        "selva" -> setOf(Category.NATURALEZA)
        "cristal" -> setOf(Category.ESTETICO, Category.MINIMAL)
        else -> setOf(Category.ESTETICO)
    }

    fun of(s: StickerInfo): Set<Category> = when (s.builtin) {
        BuiltinSticker.HEART, BuiltinSticker.BOW -> setOf(Category.PAREJA, Category.TIERNO)
        BuiltinSticker.STAR, BuiltinSticker.SPARKLE, BuiltinSticker.CROWN -> setOf(Category.ESTETICO, Category.FIESTAS)
        BuiltinSticker.MOON -> setOf(Category.OSCURO, Category.ESTETICO)
        BuiltinSticker.SUN, BuiltinSticker.CLOUD, BuiltinSticker.DROP, BuiltinSticker.BOLT -> setOf(Category.CLIMA, Category.NATURALEZA)
        BuiltinSticker.FLOWER -> setOf(Category.NATURALEZA, Category.ESTETICO)
        BuiltinSticker.MUSIC, BuiltinSticker.FIRE -> setOf(Category.FIESTAS, Category.OSCURO)
        BuiltinSticker.CAT, BuiltinSticker.PAW, BuiltinSticker.SMILE -> setOf(Category.TIERNO)
        null -> emptySet()
    }

    /** Agrupa una lista por categoría, en el orden de [Category], dejando afuera las vacías. */
    fun <T> group(items: List<T>, of: (T) -> Set<Category>): List<Pair<Category, List<T>>> {
        val sets = items.map { it to of(it) }
        return Category.entries.mapNotNull { c ->
            sets.filter { c in it.second }.map { it.first }.takeIf { it.isNotEmpty() }?.let { c to it }
        }
    }
}
