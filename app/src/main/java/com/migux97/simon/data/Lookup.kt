package com.migux97.simon.data

/** Busca contenido por id en lo guardado por el usuario y en el catálogo. */
object Lookup {
    fun design(id: String): WidgetDesign? =
        Repository.design(id)
            ?: Catalog.widgetTemplates.firstOrNull { it.id == id }
            ?: Catalog.themes.flatMap { it.widgets }.firstOrNull { it.id == id }

    fun live(id: String): LiveConfig? =
        Repository.savedLive.value.firstOrNull { it.id == id }
            ?: Catalog.liveWallpapers.firstOrNull { it.id == id }

    fun static(id: String): StaticWallpaper? =
        Repository.staticById(id)
            ?: Catalog.staticWallpapers.firstOrNull { it.id == id }
            ?: Catalog.themes.map { it.wallpaper }.firstOrNull { it.id == id }

    fun theme(id: String): ThemePack? = Catalog.themes.firstOrNull { it.id == id }
}
