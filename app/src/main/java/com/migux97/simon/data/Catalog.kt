package com.migux97.simon.data

import java.util.Calendar

/** Paleta de colores base de la que salen widgets, fondos y temas. */
data class Palette(
    val key: String,
    val name: String,
    val c1: Int,
    val c2: Int,
    val text: Int,
    val accent: Int,
    val bg: BgStyle = BgStyle.GRADIENT,
    val font: FontStyle = FontStyle.SANS,
    val decoration: Decoration = Decoration.NONE,
    val angle: Int = 135
)

private fun c(hex: Long): Int = hex.toInt()

/** Todo el contenido incluido en la app. Es contenido original de Simón. */
object Catalog {

    val palettes = listOf(
        Palette("lavanda", "Lavanda", c(0xFF8B6CFF), c(0xFFFF7EB6), c(0xFFFFFFFF), c(0xFFFFD166), font = FontStyle.SANS, decoration = Decoration.SPARKLES),
        Palette("oceano", "Océano", c(0xFF1FA2FF), c(0xFF12D8FA), c(0xFFFFFFFF), c(0xFFFFE66D), font = FontStyle.LIGHT, decoration = Decoration.WAVES),
        Palette("atardecer", "Atardecer", c(0xFFFF7E5F), c(0xFFFEB47B), c(0xFFFFFFFF), c(0xFF5B2A86), font = FontStyle.SANS_BOLD, angle = 90),
        Palette("menta", "Menta", c(0xFFD4FC79), c(0xFF96E6A1), c(0xFF1E3A2F), c(0xFF2E7D5B), font = FontStyle.CASUAL, decoration = Decoration.DOTS),
        Palette("noche", "Noche", c(0xFF0F2027), c(0xFF2C5364), c(0xFFFFFFFF), c(0xFF7FDBFF), font = FontStyle.LIGHT, decoration = Decoration.STARS),
        Palette("rosa", "Rosa pastel", c(0xFFFFDDE1), c(0xFFEE9CA7), c(0xFF5A2340), c(0xFFFF4F81), font = FontStyle.CURSIVE, decoration = Decoration.HEARTS),
        Palette("carbon", "Carbón", c(0xFF1C1C1E), c(0xFF2C2C2E), c(0xFFFFFFFF), c(0xFFFF9F0A), bg = BgStyle.SOLID, font = FontStyle.MONO),
        Palette("papel", "Papel", c(0xFFF5F0E6), c(0xFFEDE4D3), c(0xFF3B3024), c(0xFFC0623B), bg = BgStyle.SOLID, font = FontStyle.SERIF),
        Palette("vidrio", "Vidrio", c(0xFFFFFFFF), c(0xFFFFFFFF), c(0xFFFFFFFF), c(0xFFFFFFFF), bg = BgStyle.GLASS, font = FontStyle.LIGHT),
        Palette("neon", "Neón", c(0xFF12002E), c(0xFF3A0CA3), c(0xFF00F5D4), c(0xFFF72585), font = FontStyle.BLACK),
        Palette("durazno", "Durazno", c(0xFFFFE5D4), c(0xFFFFC6A5), c(0xFF6B3E26), c(0xFFE76F51), decoration = Decoration.BUBBLES),
        Palette("bosque", "Bosque", c(0xFF134E5E), c(0xFF71B280), c(0xFFFFFFFF), c(0xFFF2E8CF), font = FontStyle.CONDENSED),
        Palette("limon", "Limón", c(0xFFFFF9C4), c(0xFFFFE082), c(0xFF4E3B00), c(0xFFF57F17), font = FontStyle.CASUAL, decoration = Decoration.SPARKLES),
        Palette("cielo", "Cielo", c(0xFFA1C4FD), c(0xFFC2E9FB), c(0xFF1F3A68), c(0xFF3D5AFE), decoration = Decoration.BUBBLES),
        Palette("cereza", "Cereza", c(0xFFB31217), c(0xFFE52D27), c(0xFFFFFFFF), c(0xFFFFE0E0), font = FontStyle.SANS_BOLD, decoration = Decoration.HEARTS),
        Palette("uva", "Uva", c(0xFF41295A), c(0xFF2F0743), c(0xFFF3E8FF), c(0xFFC77DFF), font = FontStyle.SERIF, decoration = Decoration.STARS)
    )

    fun palette(key: String) = palettes.first { it.key == key }

    private val quotes = listOf(
        "Un paso a la vez." to "",
        "Hoy también cuenta." to "",
        "Lo simple también es hermoso." to "",
        "Respirá hondo y seguí." to "",
        "Hacé más de lo que te hace bien." to "",
        "Días chiquitos, cambios grandes." to ""
    )

    /** Aplica una paleta a un tipo y tamaño de widget. */
    fun designFrom(p: Palette, kind: WidgetKind, size: WidgetSize, id: String = newId(), variant: Int = 0): WidgetDesign {
        val now = Calendar.getInstance()
        val base = WidgetDesign(
            id = id,
            name = "${kind.label} · ${p.name}",
            kind = kind,
            size = size,
            bgStyle = p.bg,
            color1 = p.c1,
            color2 = p.c2,
            textColor = p.text,
            accentColor = p.accent,
            gradientAngle = p.angle,
            font = p.font,
            decoration = p.decoration,
            use24h = true
        )
        return when (kind) {
            WidgetKind.QUOTE -> {
                val q = quotes[variant % quotes.size]
                base.copy(text = q.first, text2 = q.second)
            }
            WidgetKind.COUPLE -> base.copy(
                text = "Vos", text2 = "Yo",
                dateMillis = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -365 - variant * 40) }.timeInMillis,
                decoration = if (p.decoration == Decoration.NONE) Decoration.HEARTS else p.decoration
            )
            WidgetKind.COUNTDOWN -> {
                val titles = listOf("Vacaciones", "Mi cumpleaños", "Fin de año", "El viaje", "Recibirme", "El torneo")
                base.copy(
                    text = titles[variant % titles.size],
                    dateMillis = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 12 + variant * 17) }.timeInMillis
                )
            }
            WidgetKind.PHOTO -> base.copy(cornerRadius = 30, decoration = Decoration.NONE)
            WidgetKind.WEATHER -> base.copy(city = "", decoration = Decoration.NONE)
            else -> base
        }
    }

    /** Plantillas listas: cada tipo de widget en 6 estilos y 3 tamaños. */
    val widgetTemplates: List<WidgetDesign> by lazy {
        val list = mutableListOf<WidgetDesign>()
        WidgetKind.entries.forEachIndexed { k, kind ->
            for (i in 0 until 6) {
                val p = palettes[(k * 5 + i * 3) % palettes.size]
                for (size in WidgetSize.entries) {
                    list += designFrom(p, kind, size, id = "tpl_${kind.name}_${size.name}_$i", variant = i)
                }
            }
        }
        list
    }

    fun templatesFor(kind: WidgetKind?, size: WidgetSize?): List<WidgetDesign> =
        widgetTemplates.filter { (kind == null || it.kind == kind) && (size == null || it.size == size) }

    // ---------------------------------------------------------------- fondos

    val staticWallpapers: List<StaticWallpaper> by lazy {
        val patterns = StaticPattern.entries
        palettes.flatMapIndexed { i, p ->
            listOf(
                StaticWallpaper("st_${p.key}_a", "${p.name} · ${patterns[i % patterns.size].label}", patterns[i % patterns.size], p.c1, p.c2, p.accent),
                StaticWallpaper("st_${p.key}_b", "${p.name} · ${patterns[(i + 3) % patterns.size].label}", patterns[(i + 3) % patterns.size], p.c2, p.c1, p.accent)
            )
        }
    }

    val liveWallpapers: List<LiveConfig> = listOf(
        LiveConfig("lv_aurora", "Aurora austral", SceneType.AURORA, c(0xFF050A1F), c(0xFF34E89E), c(0xFF8E2DE2)),
        LiveConfig("lv_aurora_rosa", "Aurora rosa", SceneType.AURORA, c(0xFF1A0B2E), c(0xFFFF7EB6), c(0xFF7AF5FF)),
        LiveConfig("lv_flow", "Degradado vivo", SceneType.GRADIENT_FLOW, c(0xFF8B6CFF), c(0xFFFF7EB6), c(0xFF12D8FA)),
        LiveConfig("lv_flow_sunset", "Atardecer vivo", SceneType.GRADIENT_FLOW, c(0xFFFF7E5F), c(0xFFFEB47B), c(0xFF6A3093)),
        LiveConfig("lv_stars", "Viaje estelar", SceneType.STARFIELD, c(0xFF000814), c(0xFFFFFFFF), c(0xFF7FDBFF)),
        LiveConfig("lv_stars_violeta", "Galaxia violeta", SceneType.STARFIELD, c(0xFF10002B), c(0xFFE0AAFF), c(0xFFFF6FA8), speed = 0.7f),
        LiveConfig("lv_bubbles", "Burbujas", SceneType.BUBBLES, c(0xFF1FA2FF), c(0xFFFFFFFF), c(0xFF12D8FA)),
        LiveConfig("lv_bubbles_pastel", "Burbujas pastel", SceneType.BUBBLES, c(0xFFFFDDE1), c(0xFFFFFFFF), c(0xFFA1C4FD)),
        LiveConfig("lv_rain", "Lluvia de noche", SceneType.RAIN, c(0xFF0F2027), c(0xFF9EC5FF), c(0xFF2C5364)),
        LiveConfig("lv_snow", "Nevada", SceneType.SNOW, c(0xFF1E3C72), c(0xFFFFFFFF), c(0xFF2A5298)),
        LiveConfig("lv_fireflies", "Luciérnagas", SceneType.FIREFLIES, c(0xFF0B1D13), c(0xFFFFE66D), c(0xFF134E5E)),
        LiveConfig("lv_fireflies_rosa", "Chispas rosas", SceneType.FIREFLIES, c(0xFF1A0B2E), c(0xFFFF7EB6), c(0xFF3A0CA3)),
        LiveConfig("lv_waves", "Mar en calma", SceneType.WAVES, c(0xFFA1C4FD), c(0xFF1FA2FF), c(0xFF0B3C6B)),
        LiveConfig("lv_waves_sunset", "Olas al atardecer", SceneType.WAVES, c(0xFFFFB88C), c(0xFFDE6262), c(0xFF5B2A86)),
        LiveConfig("lv_confetti", "Fiesta", SceneType.CONFETTI, c(0xFF12002E), c(0xFFFFD166), c(0xFFF72585)),
        LiveConfig("lv_hearts", "Lluvia de corazones", SceneType.HEARTS, c(0xFFFFDDE1), c(0xFFFF4F81), c(0xFFEE9CA7)),
        LiveConfig("lv_matrix", "Código verde", SceneType.MATRIX, c(0xFF000000), c(0xFF39FF14), c(0xFF0B3D0B)),
        LiveConfig("lv_matrix_cyan", "Código cian", SceneType.MATRIX, c(0xFF020617), c(0xFF00F5D4), c(0xFF0EA5E9)),
        LiveConfig("lv_neon", "Neón retro", SceneType.NEON_GRID, c(0xFF12002E), c(0xFFF72585), c(0xFF00F5D4)),
        LiveConfig("lv_neon_sol", "Sol sintético", SceneType.NEON_GRID, c(0xFF0D0221), c(0xFFFF9F0A), c(0xFFB5179E))
    )

    // ----------------------------------------------------------------- temas

    private fun iconStyleFor(p: Palette, shape: IconShape) = IconStyle(
        shape = shape,
        bg1 = p.c1,
        bg2 = p.c2,
        foreground = IconForeground.TINTED,
        fgColor = if (p.bg == BgStyle.GLASS) c(0xFF333333) else p.text,
        font = p.font
    )

    private fun theme(
        id: String, name: String, description: String, paletteKey: String,
        pattern: StaticPattern, live: String?, shape: IconShape,
        kinds: List<Pair<WidgetKind, WidgetSize>>,
        wpColors: Triple<Int, Int, Int>? = null
    ): ThemePack {
        val p = palette(paletteKey)
        val wp = wpColors ?: Triple(p.c1, p.c2, p.accent)
        return ThemePack(
            id = id,
            name = name,
            description = description,
            wallpaper = StaticWallpaper("th_${id}_wp", name, pattern, wp.first, wp.second, wp.third),
            live = live?.let { key -> liveWallpapers.first { it.id == key } },
            widgets = kinds.mapIndexed { i, (k, s) -> designFrom(p, k, s, id = "th_${id}_$i", variant = i) },
            iconStyle = iconStyleFor(p, shape)
        )
    }

    val themes: List<ThemePack> by lazy {
        listOf(
            theme("sueno", "Sueño lila", "Lilas y rosas con destellos.", "lavanda", StaticPattern.MESH, "lv_flow", IconShape.SQUIRCLE,
                listOf(WidgetKind.DIGITAL_CLOCK to WidgetSize.MEDIUM, WidgetKind.CALENDAR to WidgetSize.SMALL, WidgetKind.QUOTE to WidgetSize.SMALL)),
            theme("marea", "Marea", "Azules frescos y olas.", "oceano", StaticPattern.WAVES, "lv_waves", IconShape.CIRCLE,
                listOf(WidgetKind.WEATHER to WidgetSize.MEDIUM, WidgetKind.ANALOG_CLOCK to WidgetSize.SMALL, WidgetKind.BATTERY to WidgetSize.SMALL)),
            theme("medianoche", "Medianoche", "Oscuro, sereno y estrellado.", "noche", StaticPattern.RADIAL, "lv_stars", IconShape.ROUNDED,
                listOf(WidgetKind.ANALOG_CLOCK to WidgetSize.LARGE, WidgetKind.BATTERY to WidgetSize.SMALL, WidgetKind.CALENDAR to WidgetSize.SMALL)),
            theme("enamorados", "Enamorados", "Para tu persona favorita.", "rosa", StaticPattern.BLOBS, "lv_hearts", IconShape.FLOWER,
                listOf(WidgetKind.COUPLE to WidgetSize.MEDIUM, WidgetKind.PHOTO to WidgetSize.SMALL, WidgetKind.COUNTDOWN to WidgetSize.SMALL)),
            theme("minimal", "Minimal", "Negro, limpio y con detalles naranjas.", "carbon", StaticPattern.LINEAR, null, IconShape.SQUARE,
                listOf(WidgetKind.DIGITAL_CLOCK to WidgetSize.SMALL, WidgetKind.CALENDAR to WidgetSize.MEDIUM, WidgetKind.BATTERY to WidgetSize.SMALL)),
            theme("cuaderno", "Cuaderno", "Tonos papel y letra clásica.", "papel", StaticPattern.DOTS, null, IconShape.ROUNDED,
                listOf(WidgetKind.QUOTE to WidgetSize.MEDIUM, WidgetKind.CALENDAR to WidgetSize.LARGE, WidgetKind.DIGITAL_CLOCK to WidgetSize.SMALL)),
            theme("retro", "Retro neón", "Ochentoso y brillante.", "neon", StaticPattern.STRIPES, "lv_neon", IconShape.HEXAGON,
                listOf(WidgetKind.DIGITAL_CLOCK to WidgetSize.MEDIUM, WidgetKind.BATTERY to WidgetSize.SMALL, WidgetKind.COUNTDOWN to WidgetSize.SMALL)),
            theme("ocaso", "Ocaso", "Naranjas cálidos del atardecer.", "atardecer", StaticPattern.SUNSET, "lv_flow_sunset", IconShape.TEARDROP,
                listOf(WidgetKind.WEATHER to WidgetSize.SMALL, WidgetKind.DIGITAL_CLOCK to WidgetSize.LARGE, WidgetKind.QUOTE to WidgetSize.SMALL)),
            theme("selva", "Selva", "Verdes profundos con luciérnagas.", "bosque", StaticPattern.BLOBS, "lv_fireflies", IconShape.SQUIRCLE,
                listOf(WidgetKind.ANALOG_CLOCK to WidgetSize.SMALL, WidgetKind.WEATHER to WidgetSize.MEDIUM, WidgetKind.CALENDAR to WidgetSize.SMALL)),
            theme("cristal", "Cristal", "Widgets de vidrio sobre un cielo suave.", "vidrio", StaticPattern.MESH, "lv_aurora_rosa", IconShape.CIRCLE,
                listOf(WidgetKind.DIGITAL_CLOCK to WidgetSize.MEDIUM, WidgetKind.BATTERY to WidgetSize.SMALL, WidgetKind.WEATHER to WidgetSize.SMALL),
                wpColors = Triple(c(0xFFFF9A9E), c(0xFFA18CD1), c(0xFF84FAB0)))
        )
    }
}
