package com.migux97.simon.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Process
import android.os.UserHandle
import android.provider.Settings
import android.util.LruCache
import android.widget.Toast
import androidx.core.graphics.drawable.toBitmap
import com.migux97.simon.data.AppOverride
import com.migux97.simon.data.IconForeground
import com.migux97.simon.data.IconShape
import com.migux97.simon.data.IconStyle
import com.migux97.simon.data.ImageStore
import com.migux97.simon.data.LauncherSettings
import com.migux97.simon.data.MediaLibrary
import com.migux97.simon.data.StudioStore
import com.migux97.simon.icons.IconChanger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Una app que se puede abrir desde el launcher. */
data class LaunchApp(
    val key: String,
    val packageName: String,
    val className: String,
    val label: String,
    val user: UserHandle,
    val icon: Drawable
) {
    val component: ComponentName get() = ComponentName(packageName, className)
}

/** Lista de apps instaladas, siempre al día. */
object AppCatalog {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _apps = MutableStateFlow<List<LaunchApp>>(emptyList())
    val apps: StateFlow<List<LaunchApp>> = _apps.asStateFlow()
    private var registered = false

    fun key(pkg: String, cls: String) = "$pkg/$cls"

    fun ensureLoaded(context: Context) {
        val app = context.applicationContext
        if (!registered) {
            registered = true
            val la = app.getSystemService(LauncherApps::class.java)
            la?.registerCallback(object : LauncherApps.Callback() {
                override fun onPackageRemoved(packageName: String?, user: UserHandle?) = refresh(app)
                override fun onPackageAdded(packageName: String?, user: UserHandle?) = refresh(app)
                override fun onPackageChanged(packageName: String?, user: UserHandle?) = refresh(app)
                override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = refresh(app)
                override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = refresh(app)
            })
        }
        if (_apps.value.isEmpty()) refresh(app)
    }

    fun refresh(context: Context) {
        scope.launch {
            val la = context.getSystemService(LauncherApps::class.java) ?: return@launch
            val user = Process.myUserHandle()
            val list = try {
                la.getActivityList(null, user).map { info ->
                    LaunchApp(
                        key = key(info.componentName.packageName, info.componentName.className),
                        packageName = info.componentName.packageName,
                        className = info.componentName.className,
                        label = info.label?.toString() ?: info.componentName.packageName,
                        user = info.user,
                        icon = info.getIcon(0)
                    )
                }.sortedBy { it.label.lowercase() }
            } catch (e: Exception) {
                emptyList()
            }
            IconResolver.clear()
            _apps.value = list
        }
    }

    fun find(key: String?): LaunchApp? = key?.let { k -> _apps.value.firstOrNull { it.key == k } }

    fun label(app: LaunchApp): String = StudioStore.override(app.key)?.label?.takeIf { it.isNotBlank() } ?: app.label

    fun launch(context: Context, app: LaunchApp, bounds: Rect? = null) {
        try {
            val la = context.getSystemService(LauncherApps::class.java)
            la.startMainActivity(app.component, app.user, bounds, null)
        } catch (e: Exception) {
            try {
                context.packageManager.getLaunchIntentForPackage(app.packageName)?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(it)
                }
            } catch (_: Exception) {
                Toast.makeText(context, "No se pudo abrir ${app.label}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun launchKey(context: Context, key: String?) {
        val app = find(key)
        if (app != null) launch(context, app) else if (key != null) {
            val pkg = key.substringBefore('/')
            context.packageManager.getLaunchIntentForPackage(pkg)?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(it)
            }
        }
    }

    fun openInfo(context: Context, app: LaunchApp) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
        }
    }

    fun uninstall(context: Context, app: LaunchApp) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
        }
    }
}

/** Decide cómo se ve cada ícono: personalizado, pack de íconos, estilo global u original. */
object IconResolver {
    private val cache = object : LruCache<String, Bitmap>(40 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }

    val defaultStyle = IconStyle(IconShape.SQUIRCLE, 0xFF8B6CFF.toInt(), 0xFFFF7EB6.toInt(), IconForeground.ORIGINAL, 0xFFFFFFFF.toInt())

    fun clear() = cache.evictAll()

    fun icon(context: Context, app: LaunchApp, settings: LauncherSettings, override: AppOverride?, size: Int): Bitmap {
        val cacheKey = "${app.key}|$size|${override.hashCode()}|${settings.globalStyle.hashCode()}|${settings.iconPack}"
        cache.get(cacheKey)?.let { return it }
        val packDrawable = if (settings.iconPack != null && override?.useIconPack != false)
            IconPacks.drawable(context, settings.iconPack, app.key) else null
        val base = packDrawable ?: app.icon
        val label = override?.label ?: app.label
        val custom = when {
            override?.imagePath != null -> ImageStore.load(override.imagePath, 512)
            override?.drawingId != null -> MediaLibrary.loadDrawing(override.drawingId, 512)
            else -> null
        }
        val bmp = when {
            custom != null -> IconChanger.render(override?.style ?: settings.globalStyle ?: defaultStyle, base, label, custom, size)
            override?.style != null -> IconChanger.render(override.style, base, label, null, size)
            packDrawable != null -> packDrawable.toBitmap(size, size)
            settings.globalStyle != null -> IconChanger.render(settings.globalStyle, base, label, null, size)
            else -> base.toBitmap(size, size)
        }.let { b ->
            val stickers = override?.stickers.orEmpty()
            if (stickers.isEmpty()) b else {
                // Los stickers pueden sobresalir un poco del ícono.
                val out = b.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
                com.migux97.simon.custom.StickerPainter.drawAttached(android.graphics.Canvas(out), stickers, size.toFloat(), size.toFloat())
                out
            }
        }
        cache.put(cacheKey, bmp)
        return bmp
    }
}
