package com.migux97.simon.data

/** Stickers prediseñados: cada forma en varias combinaciones de color. */
object StickerCatalog {
    private val combos = listOf(
        0xFFFF4F81 to 0xFFFF9EC4,
        0xFFFFC94D to 0xFFFF7E5F,
        0xFF8B6CFF to 0xFF12D8FA,
        0xFF34C759 to 0xFFD4FC79,
        0xFFFFFFFF to 0xFFE0E0E0,
        0xFF1C1C1E to 0xFF5A5A60
    )

    val builtins: List<StickerInfo> by lazy {
        BuiltinSticker.entries.flatMap { b ->
            combos.mapIndexed { i, (c1, c2) ->
                StickerInfo(
                    id = "bi_${b.name}_$i",
                    name = b.label,
                    kind = StickerKind.BUILTIN,
                    builtin = b,
                    color1 = c1.toInt(),
                    color2 = c2.toInt()
                )
            }
        }
    }
}
