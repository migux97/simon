package com.migux97.simon.tools

import android.app.Activity
import android.app.AlarmManager
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.migux97.simon.data.Lookup
import com.migux97.simon.data.Repository
import com.migux97.simon.data.StudioStore
import com.migux97.simon.effects.SimonNotificationListener
import com.migux97.simon.launcher.CHARGE_PAGE
import com.migux97.simon.launcher.LOCK_PAGE
import com.migux97.simon.launcher.LauncherBackgroundLayer
import com.migux97.simon.launcher.LauncherUi
import com.migux97.simon.launcher.PageCanvas
import com.migux97.simon.ui.components.LocalAnimationsActive
import com.migux97.simon.ui.components.ScenePreview
import com.migux97.simon.ui.components.rememberAnimationsAllowed
import com.migux97.simon.ui.theme.SimonTheme
import com.migux97.simon.widget.Draw
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

private val esAR = Locale("es", "AR")

/** Abre la pantalla de carga si corresponde. */
object ChargingLauncher {
    fun maybeShow(context: Context) {
        val s = StudioStore.chargingSettings
        if (!s.enabled) return
        if (s.onlyWhenLocked && !SimonRuntime.isLocked(context)) return
        show(context)
    }

    fun show(context: Context) {
        // Android solo deja abrir pantallas desde segundo plano con el permiso de superposición.
        if (!Settings.canDrawOverlays(context)) return
        try {
            context.startActivity(Intent(context, ChargingActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION))
        } catch (_: Exception) {
        }
    }
}

/** Abre la pantalla de bloqueo propia al prender la pantalla. */
object LockLauncher {
    fun maybeShow(context: Context) {
        val s = StudioStore.lockSettings
        if (!s.enabled || !SimonRuntime.isLocked(context)) return
        // Si se está cargando y la pantalla de carga está activa, gana esa.
        val bm = context.getSystemService(BatteryManager::class.java)
        if (StudioStore.chargingSettings.enabled && bm?.isCharging == true) {
            ChargingLauncher.maybeShow(context)
            return
        }
        if (!Settings.canDrawOverlays(context)) return
        try {
            context.startActivity(Intent(context, LockActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION))
        } catch (_: Exception) {
        }
    }
}

/** Configuración común: verse sobre la pantalla de bloqueo, a pantalla completa. */
private fun Activity.overLockScreen(turnOn: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        setShowWhenLocked(true)
        if (turnOn) setTurnScreenOn(true)
    } else {
        @Suppress("DEPRECATION")
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or (if (turnOn) WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON else 0))
    }
    WindowCompat.getInsetsController(window, window.decorView).apply {
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        hide(WindowInsetsCompat.Type.systemBars())
    }
}

// ======================================================================
//  Pantalla de carga (tipo mesita de luz)
// ======================================================================

class ChargingActivity : ComponentActivity() {
    private val unplugged = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) = finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Repository.init(this)
        enableEdgeToEdge()
        overLockScreen(turnOn = true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        ContextCompat.registerReceiver(this, unplugged, IntentFilter(Intent.ACTION_POWER_DISCONNECTED), ContextCompat.RECEIVER_NOT_EXPORTED)
        setContent { SimonTheme { ChargingContent(onClose = { finish() }) } }
    }

    override fun onDestroy() {
        try { unregisterReceiver(unplugged) } catch (_: Exception) {}
        super.onDestroy()
    }
}

@Composable
private fun rememberNow(): Date {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L - System.currentTimeMillis() % 1000L)
            now = System.currentTimeMillis()
        }
    }
    return Date(now)
}

@Composable
private fun ChargingContent(onClose: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val s by StudioStore.chargingFlow.collectAsStateWithLifecycle()
    val items by StudioStore.homeItemsFlow.collectAsStateWithLifecycle()
    val launcher by StudioStore.launcherFlow.collectAsStateWithLifecycle()
    val now = rememberNow()
    val bm = context.getSystemService(BatteryManager::class.java)
    var level by remember { mutableIntStateOf(bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 0) }
    // Protección para pantallas OLED: el contenido se corre un poquito cada minuto.
    var shift by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: level
            shift = Random.nextLong()
        }
    }
    val dx = ((shift % 13) - 6).toInt()
    val dy = (((shift / 13) % 13) - 6).toInt()
    val alarm = remember(now.time / 60000) {
        context.getSystemService(AlarmManager::class.java)?.nextAlarmClock?.triggerTime
    }
    val pulse by rememberInfiniteTransition(label = "carga").animateFloat(0.4f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse), label = "p")
    val allowed = rememberAnimationsAllowed()

    CompositionLocalProvider(LocalAnimationsActive provides allowed) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(onClick = onClose)
        ) {
            if (!s.blackBackground) s.sceneId?.let { Lookup.live(it) }?.let { ScenePreview(it, Modifier.fillMaxSize()) }
            if (s.dim > 0f) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = s.dim.coerceIn(0f, 0.95f))))
            PageCanvas(CHARGE_PAGE, items.filter { it.page == CHARGE_PAGE }.sortedBy { it.z }, launcher, remember { LauncherUi() })
            Column(
                Modifier.fillMaxSize().offset(dx.dp, dy.dp).statusBarsPadding().navigationBarsPadding().padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    SimpleDateFormat("HH:mm", esAR).format(now),
                    color = Color(s.clockColor),
                    fontSize = 96.sp,
                    fontFamily = FontFamily(Draw.typeface(s.clockFont))
                )
                Text(
                    SimpleDateFormat("EEEE d 'de' MMMM", esAR).format(now).replaceFirstChar { it.titlecase(esAR) },
                    color = Color(s.clockColor).copy(alpha = 0.8f), fontSize = 18.sp
                )
                if (s.showBattery) {
                    Spacer(Modifier.height(36.dp))
                    Box(contentAlignment = Alignment.Center) {
                        Canvas(Modifier.size(150.dp)) {
                            val stroke = 12.dp.toPx()
                            drawArc(Color.White.copy(alpha = 0.15f), 0f, 360f, false, style = Stroke(stroke))
                            drawArc(Color(s.accent), -90f, 360f * level / 100f, false, style = Stroke(stroke, cap = StrokeCap.Round))
                            drawCircle(Color(s.accent).copy(alpha = 0.18f * pulse), radius = size.minDimension / 2 - stroke * 1.5f, center = Offset(size.width / 2, size.height / 2))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$level%", color = Color.White, fontSize = 34.sp)
                            Text("Cargando", color = Color(s.accent).copy(alpha = pulse), fontSize = 13.sp)
                        }
                    }
                }
                if (s.showAlarm && alarm != null) {
                    Spacer(Modifier.height(24.dp))
                    Text("⏰ " + SimpleDateFormat("EEE HH:mm", esAR).format(Date(alarm)), color = Color.White.copy(alpha = 0.75f), fontSize = 16.sp)
                }
                Spacer(Modifier.height(30.dp))
                Text("Tocá para salir", color = Color.White.copy(alpha = 0.35f), fontSize = 12.sp)
            }
        }
    }
}

// ======================================================================
//  Pantalla de bloqueo propia
// ======================================================================

/**
 * Se muestra encima de la pantalla de bloqueo del sistema. Al deslizar hacia
 * arriba pide el desbloqueo normal (PIN, huella o patrón), así que la
 * seguridad del teléfono no cambia.
 */
class LockActivity : ComponentActivity() {
    private val unlocked = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) = finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Repository.init(this)
        enableEdgeToEdge()
        overLockScreen(turnOn = false)
        ContextCompat.registerReceiver(this, unlocked, IntentFilter(Intent.ACTION_USER_PRESENT), ContextCompat.RECEIVER_NOT_EXPORTED)
        // Atrás no hace nada: para salir se desliza hacia arriba.
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        })
        setContent { SimonTheme { LockContent(onUnlock = ::unlock) } }
    }

    private fun unlock() {
        val km = getSystemService(KeyguardManager::class.java)
        if (km == null || !km.isKeyguardLocked) {
            finish()
            return
        }
        km.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
            override fun onDismissSucceeded() = finish()
        })
    }

    override fun onDestroy() {
        try { unregisterReceiver(unlocked) } catch (_: Exception) {}
        super.onDestroy()
    }
}

@Composable
private fun LockContent(onUnlock: () -> Unit) {
    val s by StudioStore.lockFlow.collectAsStateWithLifecycle()
    val items by StudioStore.homeItemsFlow.collectAsStateWithLifecycle()
    val launcher by StudioStore.launcherFlow.collectAsStateWithLifecycle()
    val notifications by SimonNotificationListener.notifications.collectAsStateWithLifecycle()
    val now = rememberNow()
    val pager = rememberPagerState { 1 }
    val allowed = rememberAnimationsAllowed()

    CompositionLocalProvider(LocalAnimationsActive provides allowed) {
        Box(
            Modifier
                .fillMaxSize()
                // Con «Fondo del sistema» se ve el fondo real de la pantalla de bloqueo detrás.
                .background(if (s.background.type == com.migux97.simon.data.LauncherBgType.SYSTEM) Color.Black.copy(alpha = 0.15f) else Color.Black)
                .pointerInput(Unit) {
                    var total = 0f
                    detectVerticalDragGestures(onDragStart = { total = 0f }, onVerticalDrag = { _, d -> total += d }, onDragEnd = { if (total < -150f) onUnlock() })
                }
        ) {
            LauncherBackgroundLayer(s.background.copy(parallax = false), pager)
            PageCanvas(LOCK_PAGE, items.filter { it.page == LOCK_PAGE }.sortedBy { it.z }, launcher, remember { LauncherUi() })
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(48.dp))
                Text(
                    SimpleDateFormat("HH:mm", esAR).format(now),
                    color = Color(s.clockColor),
                    fontSize = s.clockSize.sp,
                    fontFamily = FontFamily(Draw.typeface(s.clockFont))
                )
                if (s.showDate) Text(
                    SimpleDateFormat("EEEE d 'de' MMMM", esAR).format(now).replaceFirstChar { it.titlecase(esAR) },
                    color = Color(s.clockColor).copy(alpha = 0.85f), fontSize = 17.sp
                )
                Spacer(Modifier.weight(1f))
                if (s.showNotifications && notifications.isNotEmpty()) {
                    notifications.take(4).forEach { n ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(18.dp))
                                .background(Color.Black.copy(alpha = 0.4f)).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            n.icon?.let { Image(it.asImageBitmap(), null, Modifier.size(30.dp).clip(CircleShape)) }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(n.appLabel, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                // Por privacidad, sobre la pantalla de bloqueo solo se ve el título.
                                Text(n.title.ifBlank { "Notificación nueva" }, color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    if (notifications.size > 4) Text("y ${notifications.size - 4} más", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                }
                Spacer(Modifier.height(20.dp))
                Text("︿", color = Color.White.copy(alpha = 0.8f), fontSize = 22.sp)
                Text(s.hint, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
