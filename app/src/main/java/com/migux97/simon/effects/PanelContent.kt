package com.migux97.simon.effects

import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.migux97.simon.MainActivity
import com.migux97.simon.data.IconShape
import com.migux97.simon.data.ImageStore
import com.migux97.simon.data.PanelSettings
import com.migux97.simon.data.PanelTile
import com.migux97.simon.data.StudioStore
import com.migux97.simon.launcher.SystemActions
import com.migux97.simon.widget.Draw
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun panelShape(shape: IconShape): Shape = when (shape) {
    IconShape.CIRCLE -> CircleShape
    IconShape.SQUARE -> RoundedCornerShape(6.dp)
    IconShape.ROUNDED -> RoundedCornerShape(14.dp)
    IconShape.TEARDROP -> RoundedCornerShape(topStart = 6.dp, topEnd = 50.dp, bottomEnd = 50.dp, bottomStart = 50.dp)
    IconShape.HEXAGON, IconShape.FLOWER -> CircleShape
    IconShape.SQUIRCLE -> RoundedCornerShape(30)
}

internal fun tileIcon(t: PanelTile): ImageVector = when (t) {
    PanelTile.FLASHLIGHT -> Icons.Filled.FlashlightOn
    PanelTile.WIFI -> Icons.Filled.Wifi
    PanelTile.BLUETOOTH -> Icons.Filled.Bluetooth
    PanelTile.MOBILE_DATA -> Icons.Filled.SignalCellularAlt
    PanelTile.SOUND -> Icons.Filled.VolumeUp
    PanelTile.DND -> Icons.Filled.DoNotDisturbOn
    PanelTile.ROTATION -> Icons.Filled.ScreenRotation
    PanelTile.LOCATION -> Icons.Filled.LocationOn
    PanelTile.AIRPLANE -> Icons.Filled.Flight
    PanelTile.SCREENSHOT -> Icons.Filled.Screenshot
    PanelTile.LOCK -> Icons.Filled.Lock
    PanelTile.SETTINGS -> Icons.Filled.Settings
    PanelTile.CAMERA -> Icons.Filled.PhotoCamera
    PanelTile.SIMON -> Icons.Filled.AutoAwesome
    PanelTile.BATTERY -> Icons.Filled.BatteryChargingFull
    PanelTile.NFC -> Icons.Filled.Nfc
}

/** Estado actual de un acceso rápido (encendido/apagado) cuando se puede leer. */
internal fun tileActive(context: Context, t: PanelTile): Boolean = try {
    when (t) {
        PanelTile.FLASHLIGHT -> SystemActions.torchOn
        PanelTile.WIFI -> (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).isWifiEnabled
        PanelTile.BLUETOOTH -> context.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled == true
        PanelTile.SOUND -> (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager).ringerMode == AudioManager.RINGER_MODE_NORMAL
        PanelTile.DND -> (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).currentInterruptionFilter > NotificationManager.INTERRUPTION_FILTER_ALL
        PanelTile.ROTATION -> Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1
        PanelTile.AIRPLANE -> Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
        else -> false
    }
} catch (e: Exception) {
    false
}

internal fun tileLabel(context: Context, t: PanelTile): String = when (t) {
    PanelTile.SOUND -> when ((context.getSystemService(Context.AUDIO_SERVICE) as AudioManager).ringerMode) {
        AudioManager.RINGER_MODE_VIBRATE -> "Vibrar"
        AudioManager.RINGER_MODE_SILENT -> "Silencio"
        else -> "Sonido"
    }
    PanelTile.BATTERY -> "${(context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)}%"
    else -> t.label
}

private fun open(context: Context, intent: Intent, service: SimonAccessibilityService) {
    service.hidePanel()
    SystemActions.start(context, intent)
}

internal fun runTile(context: Context, t: PanelTile, service: SimonAccessibilityService) {
    when (t) {
        PanelTile.FLASHLIGHT -> SystemActions.toggleFlashlight(context)
        PanelTile.WIFI -> open(context, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Intent(Settings.Panel.ACTION_WIFI) else Intent(Settings.ACTION_WIFI_SETTINGS), service)
        PanelTile.BLUETOOTH -> open(context, Intent(Settings.ACTION_BLUETOOTH_SETTINGS), service)
        PanelTile.MOBILE_DATA -> open(context, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY) else Intent(Settings.ACTION_DATA_ROAMING_SETTINGS), service)
        PanelTile.SOUND -> {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val next = when (am.ringerMode) {
                AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
                AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
                else -> AudioManager.RINGER_MODE_NORMAL
            }
            try { am.ringerMode = next } catch (e: SecurityException) {
                open(context, Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS), service)
            }
        }
        PanelTile.DND -> {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.isNotificationPolicyAccessGranted) {
                nm.setInterruptionFilter(
                    if (nm.currentInterruptionFilter > NotificationManager.INTERRUPTION_FILTER_ALL) NotificationManager.INTERRUPTION_FILTER_ALL
                    else NotificationManager.INTERRUPTION_FILTER_PRIORITY
                )
            } else {
                Toast.makeText(context, "Dale a Simón permiso de «No molestar»", Toast.LENGTH_LONG).show()
                open(context, Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS), service)
            }
        }
        PanelTile.ROTATION -> {
            if (Settings.System.canWrite(context)) {
                val on = Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1
                Settings.System.putInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, if (on) 0 else 1)
            } else open(context, Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}")), service)
        }
        PanelTile.LOCATION -> open(context, Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), service)
        PanelTile.AIRPLANE -> open(context, Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS), service)
        PanelTile.SCREENSHOT -> service.screenshot()
        PanelTile.LOCK -> { service.hidePanel(); service.lock() }
        PanelTile.SETTINGS -> open(context, Intent(Settings.ACTION_SETTINGS), service)
        PanelTile.CAMERA -> open(context, Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA), service)
        PanelTile.SIMON -> open(context, Intent(context, MainActivity::class.java), service)
        PanelTile.BATTERY -> open(context, Intent(Intent.ACTION_POWER_USAGE_SUMMARY), service)
        PanelTile.NFC -> open(context, Intent(Settings.ACTION_NFC_SETTINGS), service)
    }
}

/** El panel deslizable de Simón, totalmente personalizable. */
@Composable
fun PanelContent(onClose: () -> Unit, service: SimonAccessibilityService) {
    val context = LocalContext.current
    val fx by StudioStore.effectsFlow.collectAsStateWithLifecycle()
    val s: PanelSettings = fx.panel
    val notifications by SimonNotificationListener.notifications.collectAsStateWithLifecycle()
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(1000); tick++ } }
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    val text = Color(s.textColor)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(onClick = onClose)
    ) {
        AnimatedVisibility(visibleState = visible, enter = slideInVertically { -it } + fadeIn()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = s.corner.dp, bottomEnd = s.corner.dp))
                    .background(Brush.verticalGradient(listOf(Color(s.bg1), Color(s.bg2))))
                    .clickable(enabled = false) { }
                    .pointerInput(Unit) {
                        var total = 0f
                        detectVerticalDragGestures(onDragStart = { total = 0f }, onVerticalDrag = { _, dy -> total += dy }, onDragEnd = { if (total < -80f) onClose() })
                    }
            ) {
                Box {
                    s.bgImage?.let { path ->
                        ImageStore.load(path, 1200)?.let { Image(it.asImageBitmap(), null, Modifier.matchParentSize(), contentScale = ContentScale.Crop, alpha = 0.45f) }
                    }
                    Column(Modifier.statusBarsPadding().padding(18.dp)) {
                        if (s.showClock) {
                            @Suppress("UNUSED_VARIABLE") val t = tick
                            val now = Date()
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(now), color = text, fontSize = 44.sp, fontFamily = FontFamily(Draw.typeface(s.clockFont)))
                                Spacer(Modifier.width(12.dp))
                                Text(SimpleDateFormat("EEE d MMM", Locale("es", "AR")).format(now), color = text.copy(alpha = 0.8f), fontSize = 15.sp, modifier = Modifier.padding(bottom = 8.dp))
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Cerrar", tint = text) }
                            }
                        }
                        // Accesos rápidos
                        s.tiles.chunked(s.columns.coerceIn(3, 6)).forEach { row ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                row.forEach { t ->
                                    @Suppress("UNUSED_VARIABLE") val refresh = tick
                                    val active = tileActive(context, t)
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp)) {
                                        Box(
                                            Modifier
                                                .size(54.dp)
                                                .clip(panelShape(s.tileShape))
                                                .background(Color(if (active) s.tileActive else s.tileColor))
                                                .clickable { runTile(context, t, service); tick++ },
                                            contentAlignment = Alignment.Center
                                        ) { Icon(tileIcon(t), t.label, tint = text) }
                                        Text(tileLabel(context, t), color = text, fontSize = 11.sp, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
                                    }
                                }
                                repeat(s.columns.coerceIn(3, 6) - row.size) { Spacer(Modifier.width(72.dp)) }
                            }
                        }
                        if (s.showBrightness) BrightnessRow(context, s, service)
                        if (s.showNotifications) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                                Text(if (notifications.isEmpty()) "Sin notificaciones" else "Notificaciones", color = text, fontSize = 16.sp, modifier = Modifier.weight(1f))
                                if (notifications.any { it.clearable }) TextButton(onClick = { SimonNotificationListener.dismissAll() }) { Text("Borrar todo", color = text) }
                            }
                            if (SimonNotificationListener.instance == null) {
                                Text("Activá el acceso a notificaciones en Simón → Efectos para verlas acá.", color = text.copy(alpha = 0.7f), fontSize = 12.sp)
                            }
                            LazyColumn(Modifier.heightIn(max = 380.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(notifications, key = { it.key }) { n ->
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(Color(s.tileColor))
                                            .clickable { service.hidePanel(); SimonNotificationListener.open(n) }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        n.icon?.let { Image(it.asImageBitmap(), null, Modifier.size(36.dp).clip(CircleShape)) }
                                        Spacer(Modifier.width(10.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text("${n.appLabel} · ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(n.time))}", color = text.copy(alpha = 0.7f), fontSize = 11.sp)
                                            if (n.title.isNotBlank()) Text(n.title, color = text, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            if (n.text.isNotBlank()) Text(n.text, color = text.copy(alpha = 0.85f), fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        }
                                        if (n.clearable) IconButton(onClick = { SimonNotificationListener.dismiss(n.key) }) { Icon(Icons.Filled.Close, "Descartar", tint = text) }
                                    }
                                }
                            }
                        }
                        Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
                            Box(Modifier.width(40.dp).aspectRatio(8f).clip(CircleShape).background(text.copy(alpha = 0.5f)))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrightnessRow(context: Context, s: PanelSettings, service: SimonAccessibilityService) {
    val canWrite = Settings.System.canWrite(context)
    var value by remember {
        mutableFloatStateOf(
            try { Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f } catch (e: Exception) { 0.5f }
        )
    }
    val text = Color(s.textColor)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(Icons.Filled.BrightnessMedium, "Brillo", tint = text)
        Spacer(Modifier.width(8.dp))
        if (canWrite) {
            Slider(
                value = value,
                onValueChange = {
                    value = it
                    try {
                        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, (it * 255).toInt().coerceIn(1, 255))
                    } catch (_: Exception) {
                    }
                },
                colors = SliderDefaults.colors(thumbColor = Color(s.tileActive), activeTrackColor = Color(s.tileActive)),
                modifier = Modifier.weight(1f)
            )
        } else {
            TextButton(onClick = { open(context, Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}")), service) }) {
                Text("Permitir controlar el brillo", color = text)
            }
        }
    }
}
