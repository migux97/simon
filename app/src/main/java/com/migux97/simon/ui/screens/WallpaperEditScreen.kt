package com.migux97.simon.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesomeMosaic
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.migux97.simon.canva.CanvaEditor
import com.migux97.simon.canva.CanvaPresets
import com.migux97.simon.canva.CanvaSection
import com.migux97.simon.canva.CanvaState
import com.migux97.simon.canva.CanvaTool
import com.migux97.simon.canva.ElementBlock
import com.migux97.simon.canva.Hint
import com.migux97.simon.canva.MyMediaRows
import com.migux97.simon.canva.PageTemplateGrid
import com.migux97.simon.canva.ShapesAndGraphics
import com.migux97.simon.canva.TextPanel
import com.migux97.simon.canva.placeOn
import com.migux97.simon.custom.CustomWidgetLive
import com.migux97.simon.custom.CustomWidgetRenderer
import com.migux97.simon.data.AppliedWallpaper
import com.migux97.simon.data.CustomWidget
import com.migux97.simon.data.ElementType
import com.migux97.simon.data.ImageStore
import com.migux97.simon.data.Repository
import com.migux97.simon.data.ShapeKind
import com.migux97.simon.data.WElement
import com.migux97.simon.data.newId
import com.migux97.simon.ui.Nav
import com.migux97.simon.ui.components.ConfirmDialog
import com.migux97.simon.ui.components.UnsavedChangesDialog
import com.migux97.simon.wallpaper.StaticWallpapers
import com.migux97.simon.wallpaper.WallpaperReader
import com.migux97.simon.wallpaper.WallpaperTarget
import com.migux97.simon.widget.RenderEnv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Lo último que armaste sobre cada fondo, para seguir editándolo con todas sus capas. */
@Serializable
private data class WallDesign(val design: CustomWidget, val homeId: Int = 0, val lockId: Int = 0)

private object WallDesigns {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private fun prefs(c: Context) = c.getSharedPreferences("simon_walls", Context.MODE_PRIVATE)

    fun load(c: Context, lock: Boolean): WallDesign? = try {
        prefs(c).getString(if (lock) "lock" else "home", null)?.let { json.decodeFromString(WallDesign.serializer(), it) }
    } catch (_: Exception) {
        null
    }

    fun save(c: Context, lock: Boolean, d: WallDesign) {
        prefs(c).edit().putString(if (lock) "lock" else "home", json.encodeToString(WallDesign.serializer(), d)).apply()
    }
}

/** La foto de fondo como primera capa: ocupa toda la pantalla y viene bloqueada para no moverla sin querer. */
private fun backgroundLayer(path: String) = WElement(
    type = ElementType.IMAGE, mediaPath = path, shape = ShapeKind.RECT,
    x = 0f, y = 0f, w = 1f, h = 1f, locked = true
)

private fun newDesign(path: String, aspect: Float, lock: Boolean): CustomWidget {
    // La imagen se recorta al centro con la forma de la pantalla (como la muestra Android), así no se estira.
    val bg = backgroundLayer(path)
    val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
    android.graphics.BitmapFactory.decodeFile(path, opts)
    val imgAspect = if (opts.outWidth > 0 && opts.outHeight > 0) opts.outWidth.toFloat() / opts.outHeight else aspect
    val crop = if (imgAspect > aspect) {
        val w = aspect / imgAspect
        com.migux97.simon.data.PhotoAdjust(cropL = (1f - w) / 2f, cropR = (1f + w) / 2f)
    } else {
        val h = imgAspect / aspect
        com.migux97.simon.data.PhotoAdjust(cropT = (1f - h) / 2f, cropB = (1f + h) / 2f)
    }
    return CustomWidget(
        id = "wall_" + newId(), name = if (lock) "Fondo de bloqueo" else "Fondo de inicio",
        aspect = aspect, bgAlpha = 0f, corner = 0f, elements = listOf(bg.copy(photo = crop))
    )
}

/**
 * Tu fondo de inicio o de bloqueo en el editor tipo Canva: le sumás texto,
 * stickers, formas y fotos, lo recortás o le cambiás los colores, y lo volvés a poner.
 */
@Composable
fun WallpaperEditScreen(nav: Nav, which: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lock = which == "lock"
    val (sw, sh) = remember { StaticWallpapers.screenSize(context) }
    val aspect = sw.toFloat() / sh
    var design by remember { mutableStateOf<CustomWidget?>(null) }
    var problem by remember { mutableStateOf<String?>(null) }
    var resumeTick by remember { mutableStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumeTick++ }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val path = withContext(Dispatchers.IO) { ImageStore.importImage(context, uri, 2600) } ?: return@launch
            design = newDesign(path, aspect, lock)
            problem = null
        }
    }
    val askAccess = com.migux97.simon.ui.components.rememberWallpaperAccess { resumeTick++ }

    // Arranca con lo que tenés puesto: tu diseño anterior (si sigue puesto) o la imagen de tu fondo.
    LaunchedEffect(resumeTick) {
        if (design != null) return@LaunchedEffect
        val saved = WallDesigns.load(context, lock)
        val currentId = WallpaperReader.currentId(context, lock)
        val savedId = if (lock) saved?.lockId else saved?.homeId
        if (saved != null && savedId != 0 && savedId == currentId) {
            design = saved.design
            return@LaunchedEffect
        }
        if (!lock && WallpaperReader.isLive(context)) {
            problem = "live"
            return@LaunchedEffect
        }
        if (!WallpaperReader.hasAccess(context)) {
            problem = "access"
            return@LaunchedEffect
        }
        val path = withContext(Dispatchers.IO) { WallpaperReader.load(context, lock)?.let { ImageStore.saveBitmap(context, it) } }
        if (path == null) problem = "read" else { design = newDesign(path, aspect, lock); problem = null }
    }

    val d = design
    if (d == null) {
        WallpaperStart(
            lock = lock,
            problem = problem,
            onAccess = askAccess,
            onPickPhoto = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onBack = { nav.back() }
        )
        return
    }
    WallpaperCanva(nav, d, lock, aspect, sw, sh)
}

@Composable
private fun WallpaperStart(lock: Boolean, problem: String?, onAccess: () -> Unit, onPickPhoto: () -> Unit, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xFF15111F)), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (problem) {
                null -> CircularProgressIndicator()
                "live" -> {
                    Text("Tenés un fondo animado en el inicio", color = Color.White, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                    Text("Un fondo animado no se puede editar como imagen. Elegí una foto para armar un fondo nuevo, o cambiá la animación desde Fondos.", color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center)
                    Button(onClick = onPickPhoto) { Text("Elegir una foto") }
                }
                "access" -> {
                    Text(if (lock) "Para editar tu fondo de bloqueo" else "Para editar tu fondo de inicio", color = Color.White, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                    Text(
                        "Android solo deja ver la imagen de tu fondo con dos permisos: fotos («Permitir todo») y acceso a los archivos. Simón los usa solo para leer tu fondo.",
                        color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center
                    )
                    Button(onClick = onAccess) { Text("Ver mi fondo real") }
                    OutlinedButton(onClick = onPickPhoto) { Text("Mejor elijo la foto de la galería") }
                }
                else -> {
                    Text("No se pudo leer tu fondo", color = Color.White, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                    Text("Elegí la foto desde la galería para editarla.", color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center)
                    Button(onClick = onPickPhoto) { Text("Elegir una foto") }
                }
            }
            TextButton(onClick = onBack) { Text("Volver", color = Color.White) }
        }
    }
}

@Composable
private fun WallpaperCanva(nav: Nav, initial: CustomWidget, lock: Boolean, aspect: Float, sw: Int, sh: Int) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = remember(initial.id) { CanvaState(initial, WidgetSource) {} }
    var applied by remember { mutableStateOf(initial) }
    var confirmExit by remember { mutableStateOf(false) }
    var choosing by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var pickSticker by remember { mutableStateOf(false) }
    var askTemplate by remember { mutableStateOf<com.migux97.simon.canva.PageTemplate?>(null) }
    val dirty = state.doc != applied

    fun add(list: List<WElement>) {
        if (list.isEmpty()) return
        state.update { it.copy(elements = it.elements + list) }
        state.select(list.map { it.id })
        state.panel = null
    }
    fun addBlock(b: ElementBlock) = add(b.placeOn(aspect, state.doc.elements.size).map { CanvaPresets.withMotion(it) })
    fun addOne(e: WElement, w: Float, hW: Float) = addBlock(ElementBlock(e.type.label, w, hW, listOf(e.copy(x = 0f, y = 0f, w = 1f, h = 1f))))

    var pickTarget by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val path = withContext(Dispatchers.IO) { ImageStore.importImage(context, uri, if (pickTarget == "bg") 2600 else 1600) } ?: return@launch
            if (pickTarget == "bg") {
                // Cambia la foto de la capa de fondo y deja todo lo demás.
                state.update { d ->
                    val bg = d.elements.firstOrNull()
                    if (bg != null && bg.type == ElementType.IMAGE && bg.locked) d.copy(elements = listOf(backgroundLayer(path).copy(id = bg.id)) + d.elements.drop(1))
                    else d.copy(elements = listOf(backgroundLayer(path)) + d.elements)
                }
            } else addOne(WElement(type = ElementType.IMAGE, mediaPath = path, shape = ShapeKind.ROUNDED, corner = 0.08f), 0.6f, 0.6f)
        }
    }
    fun pick(target: String) {
        pickTarget = target
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    fun apply(target: WallpaperTarget) {
        busy = true
        scope.launch {
            val doc = state.doc
            val ok = withContext(Dispatchers.IO) {
                val bmp = CustomWidgetRenderer.render(context, doc, sw, sh, RenderEnv.current(context))
                val path = ImageStore.saveBitmap(context, bmp)
                val ids = StaticWallpapers.applyTracked(context, bmp, target) ?: return@withContext false
                if (path != null) {
                    Repository.setCurrentStatic(AppliedWallpaper(photoPath = path, target = target.name, homeId = ids.first, lockId = ids.second, appliedAt = System.currentTimeMillis()))
                    Repository.addAppliedPhoto(path)
                }
                // Se guarda el diseño con sus capas para seguir editándolo la próxima vez.
                if (target != WallpaperTarget.LOCK) WallDesigns.save(context, false, WallDesign(doc, homeId = ids.first))
                if (target != WallpaperTarget.HOME) WallDesigns.save(context, true, WallDesign(doc, lockId = ids.second))
                true
            }
            busy = false
            if (ok) applied = doc
            Toast.makeText(context, if (ok) "¡Fondo aplicado!" else "No se pudo poner el fondo", Toast.LENGTH_SHORT).show()
        }
    }

    val tools = listOf(
        CanvaTool("tpl", Icons.Filled.AutoAwesomeMosaic, "Plantillas", content = {
            PageTemplateGrid(lock = lock) { t -> askTemplate = t }
        }),
        CanvaTool("text", Icons.Filled.TextFields, "Texto", content = { TextPanel(showLive = false) { addBlock(it) } }),
        CanvaTool("el", Icons.Filled.Category, "Elementos", content = { ShapesAndGraphics { addBlock(it) } }),
        CanvaTool("photo", Icons.Filled.Image, "Fotos", content = {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { pick("image") }) { Text("Sumar una foto") }
            }
            MyMediaRows(
                onDrawing = { id -> addOne(WElement(type = ElementType.DRAWING, mediaId = id, shape = ShapeKind.RECT), 0.5f, 0.5f) },
                onAnimation = { id -> addOne(WElement(type = ElementType.ANIMATION, mediaId = id, shape = ShapeKind.RECT), 0.5f, 0.5f) }
            )
        }),
        CanvaTool("st", Icons.Filled.EmojiEmotions, "Stickers", onClick = { pickSticker = true }),
        CanvaTool("bg", Icons.Filled.Wallpaper, "Fondo", content = {
            Hint("La foto de fondo es la primera capa y viene bloqueada. Tocala para recortarla o cambiarle los colores con «Ajustes».")
            Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { pick("bg") }) { Text("Cambiar la foto") }
                OutlinedButton(onClick = {
                    state.doc.elements.firstOrNull()?.let { state.select(listOf(it.id)) }
                    state.panel = null
                }) { Text("Elegir la foto de fondo") }
            }
        })
    )

    CanvaEditor(
        state = state,
        title = if (lock) "Fondo de bloqueo" else "Fondo de inicio",
        fullScreen = true,
        canvasAspect = aspect,
        onClose = { if (dirty) confirmExit = true else nav.back() },
        tools = tools,
        hint = "Tocá algo para elegirlo · arrastrá para moverlo · la foto de fondo está bloqueada: tocala para recortarla o ajustarla",
        hintKey = "canva_wall",
        loadPhoto = { id -> state.doc.elements.firstOrNull { it.id == id }?.loadPhoto() },
        topActions = {
            if (busy) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
            else TextButton(onClick = { choosing = true }) { Text("Aplicar", color = Color.White) }
        },
        editPanel = { l ->
            val e = state.doc.elements.firstOrNull { it.id == l.id }
            if (e != null) {
                fun upd(ne: WElement) = state.update(coalesce = true) { d -> d.copy(elements = d.elements.map { if (it.id == ne.id) ne else it }) }
                ElementProps(e, { upd(it) }, pickMedia = { }, pickApp = { })
                CanvaSection("Movimiento")
                Hint("El fondo es una imagen fija: el movimiento se ve solo acá.")
            }
        }
    ) {
        CustomWidgetLive(state.doc, Modifier.fillMaxSize(), interactive = false)
    }

    if (choosing) TargetDialog(
        title = "¿Dónde lo ponemos?",
        onDismiss = { choosing = false },
        onPick = { t -> choosing = false; apply(t) }
    )
    if (confirmExit) UnsavedChangesDialog(
        onSave = { confirmExit = false; choosing = true },
        onDiscard = { confirmExit = false; nav.back() },
        onDismiss = { confirmExit = false }
    )
    askTemplate?.let { t ->
        ConfirmDialog(
            title = "¿Sumar «${t.name}»?",
            message = "Se agrega encima de tu fondo. Si no te gusta, tocá «Deshacer».",
            confirm = "Sumar",
            onDismiss = { askTemplate = null },
            onConfirm = {
                askTemplate = null
                val g = newId()
                add(t.elements.map { it.copy(id = newId(), groupId = g) })
            }
        )
    }
    if (pickSticker) com.migux97.simon.ui.components.StickerPickerDialog(onDismiss = { pickSticker = false }) { info ->
        pickSticker = false
        addOne(WElement(type = ElementType.STICKER, mediaId = info.id), 0.34f, 0.34f)
    }
}
