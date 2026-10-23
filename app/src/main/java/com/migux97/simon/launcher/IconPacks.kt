package com.migux97.simon.launcher

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

data class IconPackInfo(val packageName: String, val label: String)

/**
 * Soporte para packs de íconos de terceros (el formato estándar "appfilter.xml"
 * que usan Nova, ADW y casi todos los packs de la Play Store).
 */
object IconPacks {
    private val actions = listOf(
        "org.adw.launcher.THEMES",
        "com.novalauncher.THEME",
        "com.gau.go.launcherex.theme",
        "org.adw.launcher.icons.ACTION_PICK_ICON"
    )
    private val maps = HashMap<String, Map<String, String>>()

    fun installed(context: Context): List<IconPackInfo> {
        val pm = context.packageManager
        return actions.flatMap { action ->
            try {
                pm.queryIntentActivities(Intent(action), PackageManager.GET_META_DATA)
            } catch (e: Exception) {
                emptyList()
            }
        }.map { it.activityInfo.packageName }
            .distinct()
            .mapNotNull { pkg ->
                try {
                    IconPackInfo(pkg, pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString())
                } catch (e: Exception) {
                    null
                }
            }.sortedBy { it.label.lowercase() }
    }

    @SuppressLint("DiscouragedApi")
    private fun mapFor(context: Context, pack: String): Map<String, String> {
        maps[pack]?.let { return it }
        val result = HashMap<String, String>()
        try {
            val res = context.packageManager.getResourcesForApplication(pack)
            val parser: XmlPullParser? = run {
                val id = res.getIdentifier("appfilter", "xml", pack)
                if (id != 0) res.getXml(id) else try {
                    XmlPullParserFactory.newInstance().newPullParser().apply {
                        setInput(res.assets.open("appfilter.xml"), "UTF-8")
                    }
                } catch (e: Exception) {
                    null
                }
            }
            if (parser != null) {
                var event = parser.eventType
                while (event != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG && parser.name == "item") {
                        val comp = parser.getAttributeValue(null, "component") ?: ""
                        val drawable = parser.getAttributeValue(null, "drawable") ?: ""
                        if (comp.startsWith("ComponentInfo{") && drawable.isNotBlank()) {
                            val key = comp.removePrefix("ComponentInfo{").removeSuffix("}")
                            result[key] = drawable
                        }
                    }
                    event = parser.next()
                }
            }
        } catch (_: Exception) {
        }
        maps[pack] = result
        return result
    }

    /** Ícono del pack para una app ("paquete/actividad"), o null si el pack no la tiene. */
    @SuppressLint("DiscouragedApi")
    fun drawable(context: Context, pack: String, appKey: String): Drawable? {
        val name = mapFor(context, pack)[appKey] ?: return null
        return try {
            val res: Resources = context.packageManager.getResourcesForApplication(pack)
            val id = res.getIdentifier(name, "drawable", pack)
            if (id == 0) null else res.getDrawable(id, null)
        } catch (e: Exception) {
            null
        }
    }

    fun forget(pack: String) {
        maps.remove(pack)
    }
}
