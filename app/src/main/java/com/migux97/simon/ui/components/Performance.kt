package com.migux97.simon.ui.components

import android.content.Context
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.migux97.simon.data.StudioStore
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

/**
 * Si las animaciones deben correr en esta parte de la pantalla. Se apaga
 * cuando la pantalla no se ve, en páginas que no están a la vista y en ahorro
 * de batería, para no gastar CPU ni batería.
 */
val LocalAnimationsActive = compositionLocalOf { true }

/** Bucle de cuadros con tope de FPS. [onFrame] recibe los ms desde el inicio. */
suspend fun frameLoop(onFrame: (Long) -> Unit) {
    val start = System.currentTimeMillis()
    var last = 0L
    while (currentCoroutineContext().isActive) {
        withFrameMillis { }
        val now = System.currentTimeMillis()
        val gap = 1000L / StudioStore.perf.maxFps.coerceIn(10, 60)
        if (now - last >= gap - 2) {
            last = now
            onFrame(now - start)
        }
    }
}

fun isPowerSave(context: Context): Boolean =
    (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isPowerSaveMode == true

/** Animaciones permitidas según el ciclo de vida y el ahorro de batería. */
@Composable
fun rememberAnimationsAllowed(): Boolean {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val state by owner.lifecycle.currentStateAsState()
    val perf by StudioStore.performanceFlow.collectAsStateWithLifecycle()
    val resumed = state.isAtLeast(Lifecycle.State.STARTED)
    return resumed && !(perf.batterySaverPause && isPowerSave(context))
}
