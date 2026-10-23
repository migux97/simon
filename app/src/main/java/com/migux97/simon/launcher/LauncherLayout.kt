package com.migux97.simon.launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import com.migux97.simon.data.Catalog
import com.migux97.simon.data.HomeItem
import com.migux97.simon.data.ItemType
import com.migux97.simon.data.LauncherSettings
import com.migux97.simon.data.Repository
import com.migux97.simon.data.StudioStore
import com.migux97.simon.data.WidgetKind
import com.migux97.simon.data.WidgetSize
import com.migux97.simon.data.newId
import kotlin.math.roundToInt

/** Reglas de ubicación de los elementos en la pantalla de inicio. */
object LauncherLayout {

    fun cellW(s: LauncherSettings) = 1f / s.gridColumns
    fun cellH(s: LauncherSettings) = 0.84f / s.gridRows

    /** Ajusta la posición a la grilla si está activado. */
    fun snap(item: HomeItem, s: LauncherSettings): HomeItem {
        if (!s.snapToGrid) return item
        val cw = cellW(s); val ch = cellH(s)
        val top = 0.06f
        return item.copy(
            x = ((item.x / cw).roundToInt() * cw).coerceIn(0f, 1f - item.w.coerceAtMost(1f)),
            y = (top + ((item.y - top) / ch).roundToInt() * ch).coerceIn(0f, 0.98f)
        )
    }

    /** Busca un hueco libre en la página para un elemento nuevo del tamaño dado. */
    fun freeSpot(page: Int, w: Float, h: Float, s: LauncherSettings): Pair<Float, Float> {
        val occupied = StudioStore.items.filter { it.page == page }
        val cw = cellW(s); val ch = cellH(s)
        for (row in 0 until s.gridRows) {
            for (col in 0 until s.gridColumns) {
                val x = col * cw
                val y = 0.06f + row * ch
                if (x + w > 1.001f || y + h > 0.92f) continue
                val clash = occupied.any { o -> x < o.x + o.w && x + w > o.x && y < o.y + o.h && y + h > o.y }
                if (!clash) return x to y
            }
        }
        return 0.3f to 0.4f
    }

    fun appItem(appKey: String, page: Int, s: LauncherSettings): HomeItem {
        val w = cellW(s); val h = cellH(s)
        val (x, y) = freeSpot(page, w, h, s)
        return HomeItem(type = ItemType.APP, page = page, x = x, y = y, w = w, h = h, appKey = appKey, z = StudioStore.maxZ(page) + 1)
    }

    /** Clave de la app que atiende un intent (teléfono, cámara…), si está en el cajón. */
    private fun keyFor(context: Context, intent: Intent): String? {
        val pkg = context.packageManager.resolveActivity(intent, 0)?.activityInfo?.packageName ?: return null
        return AppCatalog.apps.value.firstOrNull { it.packageName == pkg }?.key
    }

    /** Primera vez que se abre el launcher: armar una pantalla linda por defecto. */
    fun createDefault(context: Context) {
        val apps = AppCatalog.apps.value
        if (apps.isEmpty()) return
        val s = StudioStore.launcherSettings
        val dock = listOfNotNull(
            keyFor(context, Intent(Intent.ACTION_DIAL)),
            keyFor(context, Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))),
            keyFor(context, Intent(MediaStore.ACTION_IMAGE_CAPTURE)),
            keyFor(context, Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")))
        ).distinct()
        val items = mutableListOf<HomeItem>()
        // Reloj grande arriba
        val clockTemplate = Catalog.templatesFor(WidgetKind.DIGITAL_CLOCK, WidgetSize.MEDIUM).first()
        val clock = clockTemplate.copy(id = newId(), bgStyle = com.migux97.simon.data.BgStyle.GLASS)
        Repository.saveDesign(clock)
        items += HomeItem(type = ItemType.DESIGN_WIDGET, page = 0, x = 0.04f, y = 0.07f, w = 0.92f, h = 0.2f, designId = clock.id, z = 1)
        // Primeras apps en grilla
        val cw = cellW(s); val ch = cellH(s)
        apps.filter { it.key !in dock && it.packageName != context.packageName }.take(8).forEachIndexed { i, app ->
            val col = i % s.gridColumns
            val row = i / s.gridColumns
            items += HomeItem(type = ItemType.APP, page = 0, x = col * cw, y = 0.32f + row * ch, w = cw, h = ch, appKey = app.key, z = 2 + i)
        }
        // Simón en la segunda página
        apps.firstOrNull { it.packageName == context.packageName }?.let {
            items += HomeItem(type = ItemType.APP, page = 1, x = 0f, y = 0.06f, w = cw, h = ch, appKey = it.key, z = 1)
        }
        StudioStore.setItems(items)
        StudioStore.updateLauncher { it.copy(dockApps = dock, initialized = true) }
    }

    fun bringToFront(item: HomeItem) = StudioStore.upsertItem(item.copy(z = StudioStore.maxZ(item.page) + 1))

    fun sendToBack(item: HomeItem) {
        val minZ = StudioStore.items.filter { it.page == item.page }.minOfOrNull { it.z } ?: 0
        StudioStore.upsertItem(item.copy(z = minZ - 1))
    }

    /** Copia un elemento al lado del original. Los widgets del sistema no se duplican (cada uno tiene su id propio). */
    fun duplicate(item: HomeItem): Boolean {
        if (item.type == ItemType.SYSTEM_WIDGET) return false
        StudioStore.upsertItem(
            item.copy(
                id = newId(),
                x = (item.x + 0.05f).coerceAtMost(0.9f),
                y = (item.y + 0.04f).coerceAtMost(0.9f),
                z = StudioStore.maxZ(item.page) + 1
            )
        )
        return true
    }
}
