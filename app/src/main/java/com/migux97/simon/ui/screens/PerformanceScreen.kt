package com.migux97.simon.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.migux97.simon.data.PerformanceSettings
import com.migux97.simon.data.StudioStore
import com.migux97.simon.ui.Nav
import com.migux97.simon.ui.components.BackTopBar
import com.migux97.simon.ui.components.ChoiceChips
import com.migux97.simon.ui.components.Panel

@Composable
fun PerformanceScreen(nav: Nav) {
    val p by StudioStore.performanceFlow.collectAsStateWithLifecycle()
    fun set(t: (PerformanceSettings) -> PerformanceSettings) = StudioStore.updatePerformance(t)

    Column(Modifier.fillMaxSize()) {
        BackTopBar("Rendimiento y batería", onBack = { nav.back() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Panel {
                Text(
                    "Simón ya pausa todas las animaciones cuando no se ven: con la pantalla apagada, dentro de otras apps y en las páginas del launcher que no estás mirando. Acá podés ajustar cuánto anima cuando sí se ve.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Panel { Text("Cuadros por segundo", style = MaterialTheme.typography.titleMedium) }
            ChoiceChips(listOf(15, 24, 30, 45, 60), p.maxFps, {
                when (it) { 15 -> "15 · ahorro"; 24 -> "24"; 30 -> "30 · equilibrado"; 45 -> "45"; else -> "60 · fluido" }
            }, { v -> set { it.copy(maxFps = v) } })
            Panel {
                Toggle("Animar solo la página que estás viendo", p.onlyVisiblePage) { v -> set { it.copy(onlyVisiblePage = v) } }
                Toggle("Pausar animaciones en ahorro de batería", p.batterySaverPause) { v -> set { it.copy(batterySaverPause = v) } }
                Toggle("Reducir movimiento (íconos y stickers quietos)", p.reduceMotion) { v -> set { it.copy(reduceMotion = v) } }
                Toggle("Permitir videos en el launcher", p.allowVideo) { v -> set { it.copy(allowVideo = v) } }
            }
            Panel {
                Text("Modo liviano", style = MaterialTheme.typography.titleMedium)
                Text("Para teléfonos con poca memoria: 15 cuadros por segundo, sin movimiento y sin videos.", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { set { PerformanceSettings(maxFps = 15, onlyVisiblePage = true, reduceMotion = true, allowVideo = false, batterySaverPause = true) } }) { Text("Activar modo liviano") }
                OutlinedButton(onClick = { set { PerformanceSettings() } }) { Text("Volver a lo recomendado") }
            }
        }
    }
}

@Composable
private fun Toggle(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f)); Switch(value, onChange)
    }
}
