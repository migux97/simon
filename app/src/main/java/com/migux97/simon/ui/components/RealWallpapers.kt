package com.migux97.simon.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.setValue
import com.migux97.simon.data.LiveConfig
import com.migux97.simon.wallpaper.WallpaperReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * La imagen real de tu fondo de inicio o de bloqueo (si diste el permiso).
 * Si no se puede leer, se muestran sus colores. Se vuelve a leer al volver a la app.
 */
@Composable
fun RealWallpaperThumb(lock: Boolean, modifier: Modifier = Modifier, refresh: Int = 0) {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    val bmp by produceState<Bitmap?>(null, lock, tick, refresh) {
        value = withContext(Dispatchers.IO) { WallpaperReader.load(context, lock, maxSide = 700) }
    }
    val b = bmp
    if (b != null) Image(b.asImageBitmap(), null, modifier, contentScale = ContentScale.Crop)
    else PhoneWallpaperBackdrop(modifier, lock = lock)
}

/**
 * Pide lo que falta para leer el fondo, en orden: primero el permiso de fotos
 * (diálogo común) y después «Acceso a todos los archivos» (Ajustes).
 */
@Composable
fun rememberWallpaperAccess(onResult: () -> Unit = {}): () -> Unit {
    val context = LocalContext.current
    fun next() {
        when (WallpaperReader.missing(context)) {
            WallpaperReader.Missing.ALL_FILES -> {
                android.widget.Toast.makeText(context, "Último paso: activá «Permitir acceso a todos los archivos» para Simón y volvé", android.widget.Toast.LENGTH_LONG).show()
                try { context.startActivity(WallpaperReader.allFilesIntent(context)) } catch (_: Exception) {}
            }
            else -> Unit
        }
        onResult()
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            android.widget.Toast.makeText(context, "Activá «Fotos y videos» → «Permitir todo» para ver tu fondo", android.widget.Toast.LENGTH_LONG).show()
            try { context.startActivity(WallpaperReader.appSettingsIntent(context)) } catch (_: Exception) {}
            onResult()
        } else next()
    }
    return {
        if (WallpaperReader.missing(context) == WallpaperReader.Missing.PHOTOS) ask.launch(WallpaperReader.PERMISSION) else next()
    }
}

/**
 * «Tus fondos ahora»: tu fondo de inicio y de bloqueo tal como están. Tocás
 * uno y se abre en el editor. Si el inicio tiene un fondo animado de Simón, abre esa animación.
 */
@Composable
fun RealWallpapersCard(liveOnHome: LiveConfig?, onEditHome: () -> Unit, onEditLock: () -> Unit, onEditLive: (LiveConfig) -> Unit) {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    val access = remember(tick) { WallpaperReader.hasAccess(context) }
    val askAccess = rememberWallpaperAccess { tick++ }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Tus fondos ahora", style = MaterialTheme.typography.titleMedium)
        Text(
            if (access) "Tocá uno para editarlo: le sumás texto, stickers y fotos, lo recortás o le cambiás los colores."
            else "Tocá uno para editarlo. Para ver la imagen real de tu fondo, Android pide dos permisos: fotos y acceso a los archivos.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            WallThumb("Inicio", Modifier.weight(1f), onClick = { if (liveOnHome != null) onEditLive(liveOnHome) else onEditHome() }) {
                if (liveOnHome != null) SceneThumb(liveOnHome, Modifier.fillMaxSize()) else RealWallpaperThumb(false, Modifier.fillMaxSize(), tick)
            }
            WallThumb("Bloqueo", Modifier.weight(1f), onClick = onEditLock) { RealWallpaperThumb(true, Modifier.fillMaxSize(), tick) }
        }
        if (!access) FilledTonalButton(onClick = askAccess) { Text("Ver mi fondo real") }
    }
}

@Composable
private fun WallThumb(label: String, modifier: Modifier, onClick: () -> Unit, content: @Composable () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth(0.56f).aspectRatio(9f / 19f).clip(RoundedCornerShape(16.dp)).background(Color.Black)) {
            content()
            Box(
                Modifier.align(Alignment.BottomEnd).padding(6.dp).clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.55f)).padding(6.dp)
            ) { Icon(Icons.Filled.Edit, "Editar", tint = Color.White, modifier = Modifier.size(16.dp)) }
        }
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 4.dp))
    }
}
