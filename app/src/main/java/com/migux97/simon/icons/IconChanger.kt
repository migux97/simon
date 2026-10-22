package com.migux97.simon.icons

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.os.Build
import com.migux97.simon.data.IconForeground
import com.migux97.simon.data.IconShape
import com.migux97.simon.data.IconStyle
import com.migux97.simon.data.newId
import com.migux97.simon.widget.Draw
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

data class AppEntry(val packageName: String, val label: String, val icon: Drawable)

object IconChanger {

    suspend fun installedApps(context: Context): List<AppEntry> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != context.packageName }
            .map { AppEntry(it.activityInfo.packageName, it.loadLabel(pm).toString(), it.loadIcon(pm)) }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    fun shapePath(shape: IconShape, size: Float): Path {
        val s = size
        return when (shape) {
            IconShape.CIRCLE -> Path().apply { addCircle(s / 2, s / 2, s / 2, Path.Direction.CW) }
            IconShape.ROUNDED -> Path().apply { addRoundRect(RectF(0f, 0f, s, s), s * 0.22f, s * 0.22f, Path.Direction.CW) }
            IconShape.SQUARE -> Path().apply { addRoundRect(RectF(0f, 0f, s, s), s * 0.06f, s * 0.06f, Path.Direction.CW) }
            IconShape.TEARDROP -> Path().apply {
                val r = s / 2
                addRoundRect(RectF(0f, 0f, s, s), floatArrayOf(s * 0.08f, s * 0.08f, r, r, r, r, r, r), Path.Direction.CW)
            }
            IconShape.HEXAGON -> Draw.polygonPath(s / 2, s / 2, s / 2, 6, 0.0).also {
                // Redondear apenas las esquinas
            }
            IconShape.SQUIRCLE -> Path().apply {
                // Superelipse |x|^n + |y|^n = 1 con n = 4
                val n = 4.0
                val steps = 72
                for (i in 0..steps) {
                    val a = i * 2 * PI / steps
                    val ca = cos(a); val sa = sin(a)
                    val x = s / 2 + (s / 2 * abs(ca).pow(2 / n) * sign(ca)).toFloat()
                    val y = s / 2 + (s / 2 * abs(sa).pow(2 / n) * sign(sa)).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            IconShape.FLOWER -> Path().apply {
                val petals = 8
                val center = s / 2
                val petalR = s * 0.17f
                val ringR = s / 2 - petalR
                addCircle(center, center, ringR + petalR * 0.2f, Path.Direction.CW)
                for (i in 0 until petals) {
                    val a = i * 2 * PI / petals
                    addCircle(center + (cos(a) * ringR).toFloat(), center + (sin(a) * ringR).toFloat(), petalR, Path.Direction.CW)
                }
                op(Path(), Path.Op.UNION)
            }
        }
    }

    /** Dibuja el ícono personalizado. */
    fun render(
        style: IconStyle,
        appIcon: Drawable?,
        label: String,
        customImage: Bitmap?,
        size: Int = 192
    ): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val s = size.toFloat()
        val shape = shapePath(style.shape, s)
        c.save()
        c.clipPath(shape)
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = Draw.angledGradient(RectF(0f, 0f, s, s), 135, intArrayOf(style.bg1, style.bg2))
        }
        c.drawRect(0f, 0f, s, s, bg)
        if (customImage != null) {
            Draw.drawCenterCrop(c, customImage, RectF(0f, 0f, s, s))
        } else {
            when (style.foreground) {
                IconForeground.LETTER -> {
                    val initial = label.trim().firstOrNull()?.uppercase() ?: "?"
                    val tp = Draw.textPaint(style.font, style.fgColor, s * 0.5f, true).apply { textAlign = Paint.Align.CENTER }
                    c.drawText(initial, s / 2, s / 2 + tp.textSize * 0.36f, tp)
                }
                IconForeground.ORIGINAL -> appIcon?.let { drawForeground(c, it, s, null) }
                IconForeground.TINTED -> appIcon?.let { drawForeground(c, it, s, style.fgColor) }
            }
        }
        c.restore()
        return bmp
    }

    /** Dibuja la parte frontal del ícono de la app, opcionalmente teñida de un color. */
    private fun drawForeground(c: Canvas, icon: Drawable, s: Float, tint: Int?) {
        val layer = Bitmap.createBitmap(s.toInt(), s.toInt(), Bitmap.Config.ARGB_8888)
        val lc = Canvas(layer)
        var drawable: Drawable = icon
        var scale = 0.62f
        if (icon is AdaptiveIconDrawable) {
            val mono = if (tint != null && Build.VERSION.SDK_INT >= 33) icon.monochrome else null
            drawable = mono ?: icon.foreground ?: icon
            // La capa frontal de los íconos adaptativos tiene margen: se agranda.
            scale = 1.0f
        }
        val inset = s * (1 - scale) / 2
        drawable.setBounds(inset.toInt(), inset.toInt(), (s - inset).toInt(), (s - inset).toInt())
        drawable.draw(lc)
        if (tint != null) {
            val p = Paint().apply { color = tint; xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN) }
            lc.drawRect(0f, 0f, s, s, p)
        }
        c.drawBitmap(layer, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    }

    fun canPin(context: Context): Boolean =
        context.getSystemService(ShortcutManager::class.java)?.isRequestPinShortcutSupported == true

    /** Pide al launcher que agregue un acceso directo con el ícono nuevo. */
    fun pinShortcut(context: Context, packageName: String, label: String, icon: Bitmap): Boolean {
        val sm = context.getSystemService(ShortcutManager::class.java) ?: return false
        if (!sm.isRequestPinShortcutSupported) return false
        val launch = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        launch.action = Intent.ACTION_MAIN
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        val info = ShortcutInfo.Builder(context, "icon_${packageName}_${newId().take(8)}")
            .setShortLabel(label.ifBlank { packageName }.take(24))
            .setIcon(Icon.createWithBitmap(icon))
            .setIntent(launch)
            .build()
        return try {
            sm.requestPinShortcut(info, null)
        } catch (e: Exception) {
            false
        }
    }
}
