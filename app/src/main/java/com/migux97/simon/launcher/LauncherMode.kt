package com.migux97.simon.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import com.migux97.simon.data.StudioStore

/**
 * Prende o apaga el modo launcher. Apagado, Simón ni siquiera figura como
 * opción de pantalla de inicio: el teléfono sigue con su launcher original y
 * todo lo demás (widgets, fondos, íconos, efectos, capa decorativa) funciona igual.
 */
object LauncherMode {

    private fun alias(context: Context) = ComponentName(context.packageName, "${context.packageName}.launcher.HomeAlias")

    fun isEnabled(context: Context): Boolean =
        context.packageManager.getComponentEnabledSetting(alias(context)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED

    /** Activa el modo launcher y devuelve el intent para que el usuario lo elija como inicio. */
    fun enable(context: Context): Intent {
        context.packageManager.setComponentEnabledSetting(alias(context), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        StudioStore.updateMode { it.copy(launcherEnabled = true) }
        AppCatalog.ensureLoaded(context)
        return SystemActions.requestDefaultLauncher(context)
    }

    /**
     * Desactiva el modo launcher. Si Simón era la pantalla de inicio, el sistema
     * vuelve a preguntar o usa el launcher original.
     */
    fun disable(context: Context): Intent {
        val wasDefault = SystemActions.isDefaultLauncher(context)
        context.packageManager.setComponentEnabledSetting(alias(context), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        StudioStore.updateMode { it.copy(launcherEnabled = false) }
        return if (wasDefault) Intent(Settings.ACTION_HOME_SETTINGS) else Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    }

    /** Paquete del launcher que está usando el teléfono ahora. */
    fun currentHomePackage(context: Context): String? {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
    }
}
