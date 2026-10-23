package com.migux97.simon.launcher

import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import com.migux97.simon.MainActivity
import com.migux97.simon.data.TapAction
import com.migux97.simon.effects.SimonAccessibilityService

/** Acciones del sistema que usan el launcher, los widgets y el panel. */
object SystemActions {

    @Volatile
    var torchOn = false
        private set
    private var torchCallbackRegistered = false

    fun isDefaultLauncher(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val res = context.packageManager.resolveActivity(intent, 0)
        return res?.activityInfo?.packageName == context.packageName
    }

    /** Abre la pantalla del sistema para elegir a Simón como launcher. */
    fun requestDefaultLauncher(context: Context): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = context.getSystemService(RoleManager::class.java)
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME) && !rm.isRoleHeld(RoleManager.ROLE_HOME)) {
                return rm.createRequestRoleIntent(RoleManager.ROLE_HOME)
            }
        }
        return Intent(Settings.ACTION_HOME_SETTINGS)
    }

    @SuppressLint("WrongConstant")
    fun expandNotifications(context: Context) {
        try {
            val service = context.getSystemService("statusbar")
            val cls = Class.forName("android.app.StatusBarManager")
            cls.getMethod("expandNotificationsPanel").invoke(service)
        } catch (e: Exception) {
            SimonAccessibilityService.instance?.openNotifications()
                ?: Toast.makeText(context, "Tu teléfono no permite abrir las notificaciones desde acá", Toast.LENGTH_SHORT).show()
        }
    }

    fun lockScreen(context: Context) {
        val svc = SimonAccessibilityService.instance
        if (svc != null && svc.lock()) return
        Toast.makeText(context, "Para bloquear con un gesto, activá el servicio de accesibilidad de Simón (Efectos → Panel).", Toast.LENGTH_LONG).show()
    }

    fun openSimonPanel(context: Context) {
        val svc = SimonAccessibilityService.instance
        if (svc != null) svc.showPanel() else expandNotifications(context)
    }

    fun toggleFlashlight(context: Context) {
        try {
            val cm = context.getSystemService(CameraManager::class.java) ?: return
            if (!torchCallbackRegistered) {
                torchCallbackRegistered = true
                cm.registerTorchCallback(object : CameraManager.TorchCallback() {
                    override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                        torchOn = enabled
                    }
                }, null)
            }
            val id = cm.cameraIdList.firstOrNull {
                cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return
            torchOn = !torchOn
            cm.setTorchMode(id, torchOn)
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo usar la linterna", Toast.LENGTH_SHORT).show()
        }
    }

    fun openUrl(context: Context, url: String) {
        if (url.isBlank()) return
        val full = if (url.startsWith("http")) url else "https://$url"
        start(context, Intent(Intent.ACTION_VIEW, Uri.parse(full)))
    }

    fun dial(context: Context, number: String) {
        start(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:${number.trim()}")))
    }

    fun openSimon(context: Context) = start(context, Intent(context, MainActivity::class.java))

    fun start(context: Context, intent: Intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo abrir", Toast.LENGTH_SHORT).show()
        }
    }

    /** Ejecuta la acción configurada en un elemento o widget. */
    fun run(context: Context, action: TapAction, data: String, openDrawer: () -> Unit = {}) {
        when (action) {
            TapAction.NONE -> Unit
            TapAction.OPEN_APP -> AppCatalog.launchKey(context, data.ifBlank { null })
            TapAction.OPEN_URL -> openUrl(context, data)
            TapAction.DIAL -> dial(context, data)
            TapAction.FLASHLIGHT -> toggleFlashlight(context)
            TapAction.OPEN_DRAWER -> openDrawer()
            TapAction.OPEN_PANEL -> openSimonPanel(context)
            TapAction.OPEN_SIMON -> openSimon(context)
            TapAction.LOCK -> lockScreen(context)
        }
    }
}
