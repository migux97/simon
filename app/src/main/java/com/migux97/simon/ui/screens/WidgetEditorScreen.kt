package com.migux97.simon.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.migux97.simon.data.BgStyle
import com.migux97.simon.data.Catalog
import com.migux97.simon.data.Decoration
import com.migux97.simon.data.FontStyle
import com.migux97.simon.data.ImageStore
import com.migux97.simon.data.Lookup
import com.migux97.simon.data.Repository
import com.migux97.simon.data.WidgetDesign
import com.migux97.simon.data.WidgetKind
import com.migux97.simon.data.WidgetSize
import com.migux97.simon.data.newId
import com.migux97.simon.ui.Nav
import com.migux97.simon.ui.components.BackTopBar
import com.migux97.simon.ui.components.ChoiceChips
import com.migux97.simon.ui.components.ColorRow
import com.migux97.simon.ui.components.LabeledSlider
import com.migux97.simon.ui.components.Panel
import com.migux97.simon.ui.components.PreviewBackdrop
import com.migux97.simon.ui.components.WidgetPreview
import com.migux97.simon.widget.Draw
import com.migux97.simon.widget.Place
import com.migux97.simon.widget.RefreshWorker
import com.migux97.simon.widget.WeatherService
import com.migux97.simon.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

private enum class PhotoTarget { BACKGROUND, MAIN, SECOND }

/** Publica un diseño: lo guarda y redibuja los widgets que lo usan. */
fun saveAndRefresh(context: android.content.Context, design: WidgetDesign) {
    Repository.saveDesign(design)
    WidgetUpdater.updateDesign(context, design.id)
    WidgetUpdater.scheduleTick(context)
    if (design.kind == WidgetKind.WEATHER && design.city.isNotBlank()) RefreshWorker.runNow(context)
}

/** Fija un diseño en la pantalla de inicio (o explica cómo hacerlo a mano). */
fun pinDesign(context: android.content.Context, design: WidgetDesign) {
    val ok = WidgetUpdater.requestPin(context, design)
    Toast.makeText(
        context,
        if (ok) "Confirmá para agregarlo a tu pantalla de inicio"
        else "Tu launcher no permite agregarlo desde acá: mantené presionada la pantalla de inicio → Widgets → Simón (${design.size.label}).",
        Toast.LENGTH_LONG
    ).show()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WidgetEditorScreen(nav: Nav, designId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val original = remember(designId) { Lookup.design(designId) }
    if (original == null) {
        Column(Modifier.fillMaxSize()) {
            BackTopBar("Diseño", onBack = { nav.back() })
            Text("Este diseño ya no existe.", modifier = Modifier.padding(24.dp))
        }
        return
    }
    val isSaved = remember(designId) { Repository.design(designId) != null }
    var d by remember(designId) { mutableStateOf(original) }
    var savedId by remember(designId) { mutableStateOf(if (isSaved) designId else null) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var refresh by remember { mutableIntStateOf(0) }
    var confirmExit by remember { mutableStateOf(false) }
    val sid = savedId
    val dirty = if (sid == null) d != original else Repository.design(sid) != d

    var photoTarget by remember { mutableStateOf(PhotoTarget.MAIN) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val path = withContext(Dispatchers.IO) { ImageStore.importImage(context, uri) }
            if (path == null) {
                Toast.makeText(context, "No se pudo abrir la foto", Toast.LENGTH_SHORT).show()
            } else {
                d = when (photoTarget) {
                    PhotoTarget.BACKGROUND -> d.copy(photoPath = path, bgStyle = BgStyle.PHOTO)
                    PhotoTarget.MAIN -> d.copy(photoPath = path)
                    PhotoTarget.SECOND -> d.copy(photoPath2 = path)
                }
            }
        }
    }
    fun pickPhoto(target: PhotoTarget) {
        photoTarget = target
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    fun commit(): WidgetDesign {
        val id = savedId ?: newId()
        val final = d.copy(id = id)
        saveAndRefresh(context, final)
        savedId = id
        d = final
        return final
    }

    BackHandler(enabled = dirty) { confirmExit = true }

    Column(Modifier.fillMaxSize()) {
        BackTopBar(d.name, onBack = { if (dirty) confirmExit = true else nav.back() }) {
            IconButton(onClick = {
                commit()
                Toast.makeText(context, "Guardado en Mío", Toast.LENGTH_SHORT).show()
            }) { Icon(Icons.Filled.Check, contentDescription = "Guardar") }
        }
        // Vista previa
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(PreviewBackdrop)
                .padding(vertical = 22.dp, horizontal = if (d.size == WidgetSize.SMALL) 90.dp else 28.dp),
            contentAlignment = Alignment.Center
        ) {
            key(refresh) {
                WidgetPreview(d, Modifier.fillMaxWidth(if (d.size == WidgetSize.LARGE) 0.78f else 1f))
            }
        }
        ScrollableTabRow(selectedTabIndex = tab, edgePadding = 12.dp, containerColor = MaterialTheme.colorScheme.background) {
            listOf("Estilo", "Contenido", "Fondo", "Texto", "Stickers", "Detalles").forEachIndexed { i, t ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) })
            }
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            when (tab) {
                0 -> StyleTab(d) { d = it }
                1 -> ContentTab(d, onChange = { d = it }, pickPhoto = ::pickPhoto, onWeather = { refresh++ })
                2 -> BackgroundTab(d, onChange = { d = it }, pickPhoto = { pickPhoto(PhotoTarget.BACKGROUND) })
                3 -> TextTab(d) { d = it }
                4 -> com.migux97.simon.ui.components.StickerAttachEditor(d.stickers) { d = d.copy(stickers = it) }
                5 -> DetailsTab(d) { d = it }
            }
        }
        Surface(tonalElevation = 3.dp) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(onClick = {
                    commit()
                    Toast.makeText(context, "Guardado en Mío", Toast.LENGTH_SHORT).show()
                }, modifier = Modifier.weight(1f)) { Text("Guardar") }
                Button(onClick = { pinDesign(context, commit()) }, modifier = Modifier.weight(1.4f)) { Text("Agregar a inicio") }
            }
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("¿Guardar los cambios?") },
            text = { Text("Hiciste cambios en este diseño.") },
            confirmButton = { TextButton(onClick = { commit(); confirmExit = false; nav.back() }) { Text("Guardar") } },
            dismissButton = { TextButton(onClick = { confirmExit = false; nav.back() }) { Text("Descartar") } }
        )
    }
}

// ------------------------------------------------------------------- pestañas

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StyleTab(d: WidgetDesign, onChange: (WidgetDesign) -> Unit) {
    Panel {
        Text("Paletas", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Catalog.palettes.forEach { p ->
                val selected = d.color1 == p.c1 && d.color2 == p.c2 && d.textColor == p.text
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp)) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(p.c1), Color(p.c2))))
                            .border(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            .clickable {
                                onChange(
                                    d.copy(
                                        color1 = p.c1, color2 = p.c2, textColor = p.text, accentColor = p.accent,
                                        bgStyle = if (d.bgStyle == BgStyle.PHOTO) BgStyle.PHOTO else p.bg,
                                        font = p.font, decoration = if (d.kind == WidgetKind.PHOTO) Decoration.NONE else p.decoration,
                                        gradientAngle = p.angle
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(Modifier.size(12.dp).clip(CircleShape).background(Color(p.accent)))
                    }
                    Text(p.name, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }
    }
    Panel {
        Text("Tipo de widget", style = MaterialTheme.typography.titleMedium)
    }
    ChoiceChips(WidgetKind.entries, d.kind, { it.label }, { onChange(d.copy(kind = it)) })
    Panel {
        Text("Tamaño", style = MaterialTheme.typography.titleMedium)
        Text("Pequeño 2×2 · Mediano 4×2 · Grande 4×4", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    ChoiceChips(WidgetSize.entries, d.size, { it.label }, { onChange(d.copy(size = it)) })
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun BackgroundTab(d: WidgetDesign, onChange: (WidgetDesign) -> Unit, pickPhoto: () -> Unit) {
    Panel { Text("Tipo de fondo", style = MaterialTheme.typography.titleMedium) }
    ChoiceChips(BgStyle.entries, d.bgStyle, { it.label }, {
        if (it == BgStyle.PHOTO && d.photoPath == null && d.kind != WidgetKind.PHOTO) pickPhoto() else onChange(d.copy(bgStyle = it))
    })
    Panel {
        ColorRow(if (d.bgStyle == BgStyle.SOLID) "Color" else "Color 1", d.color1) { onChange(d.copy(color1 = it)) }
        if (d.bgStyle != BgStyle.SOLID) ColorRow("Color 2", d.color2) { onChange(d.copy(color2 = it)) }
        if (d.bgStyle == BgStyle.GRADIENT || d.bgStyle == BgStyle.GLASS) {
            LabeledSlider("Dirección", d.gradientAngle.toFloat(), 0f..360f, "${d.gradientAngle}°") { onChange(d.copy(gradientAngle = it.roundToInt())) }
        }
        LabeledSlider("Opacidad", d.bgAlpha.toFloat(), 0f..255f, "${(d.bgAlpha / 2.55f).roundToInt()}%") { onChange(d.copy(bgAlpha = it.roundToInt())) }
        if (d.bgStyle == BgStyle.PHOTO && d.kind != WidgetKind.PHOTO) {
            FilledTonalButton(onClick = pickPhoto) {
                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Cambiar foto de fondo")
            }
        }
    }
    Panel { Text("Decoración", style = MaterialTheme.typography.titleMedium) }
    ChoiceChips(Decoration.entries, d.decoration, { it.label }, { onChange(d.copy(decoration = it)) })
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun TextTab(d: WidgetDesign, onChange: (WidgetDesign) -> Unit) {
    Panel {
        Text("Tipografía", style = MaterialTheme.typography.titleMedium)
        FontStyle.entries.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { f ->
                    val selected = f == d.font
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { onChange(d.copy(font = f)) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Aa 12", fontFamily = FontFamily(Draw.typeface(f)), style = MaterialTheme.typography.titleLarge)
                            Text(f.label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    Panel {
        ColorRow("Color del texto", d.textColor) { onChange(d.copy(textColor = it)) }
        ColorRow("Color de acento", d.accentColor) { onChange(d.copy(accentColor = it)) }
    }
}

@Composable
private fun DetailsTab(d: WidgetDesign, onChange: (WidgetDesign) -> Unit) {
    Panel {
        OutlinedTextField(d.name, { onChange(d.copy(name = it.take(40))) }, label = { Text("Nombre del diseño") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        LabeledSlider("Esquinas redondeadas", d.cornerRadius.toFloat(), 0f..50f, "${d.cornerRadius}") { onChange(d.copy(cornerRadius = it.roundToInt())) }
        if (d.kind == WidgetKind.DIGITAL_CLOCK || d.kind == WidgetKind.ANALOG_CLOCK) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Formato 24 horas", modifier = Modifier.weight(1f))
                Switch(d.use24h, { onChange(d.copy(use24h = it)) })
            }
        }
    }
    Panel {
        Text("Tocá el widget en tu pantalla de inicio para volver a este editor. Los cambios que guardes se aplican al instante en todos los widgets que usan este diseño.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContentTab(d: WidgetDesign, onChange: (WidgetDesign) -> Unit, pickPhoto: (PhotoTarget) -> Unit, onWeather: () -> Unit) {
    var dateOpen by remember { mutableStateOf(false) }
    val dateFmt = remember { SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("es", "AR")) }
    when (d.kind) {
        WidgetKind.DIGITAL_CLOCK, WidgetKind.ANALOG_CLOCK -> Panel {
            Text("La hora se actualiza sola cada minuto.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Formato 24 horas", modifier = Modifier.weight(1f))
                Switch(d.use24h, { onChange(d.copy(use24h = it)) })
            }
        }
        WidgetKind.CALENDAR -> Panel {
            Text("Muestra el día de hoy y el mes. La semana empieza el lunes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        WidgetKind.BATTERY -> Panel {
            Text("Muestra el nivel de batería y si está cargando. Se actualiza cada 15 minutos y cada vez que abrís Simón.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        WidgetKind.PHOTO -> Panel {
            PhotoButton(if (d.photoPath == null) "Elegir foto" else "Cambiar foto") { pickPhoto(PhotoTarget.MAIN) }
            OutlinedTextField(d.text, { onChange(d.copy(text = it.take(40))) }, label = { Text("Texto sobre la foto (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Marco de color", modifier = Modifier.weight(1f))
                Switch(d.bgAlpha >= 20, { onChange(d.copy(bgAlpha = if (it) 255 else 0)) })
            }
        }
        WidgetKind.QUOTE -> Panel {
            OutlinedTextField(d.text, { onChange(d.copy(text = it.take(160))) }, label = { Text("Frase") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(d.text2, { onChange(d.copy(text2 = it.take(40))) }, label = { Text("Autor (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        WidgetKind.COUPLE -> Panel {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(d.text, { onChange(d.copy(text = it.take(16))) }, label = { Text("Nombre 1") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(d.text2, { onChange(d.copy(text2 = it.take(16))) }, label = { Text("Nombre 2") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PhotoButton(if (d.photoPath == null) "Foto 1" else "Cambiar 1", Modifier.weight(1f)) { pickPhoto(PhotoTarget.MAIN) }
                PhotoButton(if (d.photoPath2 == null) "Foto 2" else "Cambiar 2", Modifier.weight(1f)) { pickPhoto(PhotoTarget.SECOND) }
            }
            DateButton("Juntos desde", if (d.dateMillis == 0L) "Elegir fecha" else dateFmt.format(Date(d.dateMillis))) { dateOpen = true }
        }
        WidgetKind.COUNTDOWN -> Panel {
            OutlinedTextField(d.text, { onChange(d.copy(text = it.take(40))) }, label = { Text("¿Para qué falta?") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            DateButton("Fecha", if (d.dateMillis == 0L) "Elegir fecha" else dateFmt.format(Date(d.dateMillis))) { dateOpen = true }
        }
        WidgetKind.WEATHER -> CitySearch(d, onChange, onWeather)
    }
    Spacer(Modifier.height(12.dp))

    if (dateOpen) {
        val initialUtc = remember(d.dateMillis) {
            val local = Calendar.getInstance().apply { if (d.dateMillis != 0L) timeInMillis = d.dateMillis }
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
            }.timeInMillis
        }
        val state = rememberDatePickerState(initialSelectedDateMillis = initialUtc)
        DatePickerDialog(
            onDismissRequest = { dateOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { utc ->
                        val u = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utc }
                        val local = Calendar.getInstance().apply {
                            clear()
                            set(u.get(Calendar.YEAR), u.get(Calendar.MONTH), u.get(Calendar.DAY_OF_MONTH), 12, 0)
                        }
                        onChange(d.copy(dateMillis = local.timeInMillis))
                    }
                    dateOpen = false
                }) { Text("Listo") }
            },
            dismissButton = { TextButton(onClick = { dateOpen = false }) { Text("Cancelar") } }
        ) { DatePicker(state) }
    }
}

@Composable
private fun PhotoButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = modifier) {
        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(text, maxLines = 1)
    }
}

@Composable
private fun DateButton(label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun CitySearch(d: WidgetDesign, onChange: (WidgetDesign) -> Unit, onWeather: () -> Unit) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Place>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }
    fun search() {
        loading = true
        scope.launch {
            results = WeatherService.searchCity(query)
            loading = false
            searched = true
        }
    }
    Panel {
        Text(if (d.city.isBlank()) "Elegí tu ciudad" else "Ciudad: ${d.city}", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                query, { query = it },
                label = { Text("Buscar ciudad") }, singleLine = true,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { search() }, enabled = query.isNotBlank() && !loading) {
                if (loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else Icon(Icons.Filled.Search, contentDescription = "Buscar")
            }
        }
        if (searched && results.isEmpty() && !loading) {
            Text("No encontré esa ciudad. Revisá la conexión o probá otro nombre.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        results.forEachIndexed { i, place ->
            if (i > 0) HorizontalDivider()
            ListItem(
                headlineContent = { Text(place.name) },
                supportingContent = { Text(place.label) },
                modifier = Modifier.clickable {
                    onChange(d.copy(city = place.name, lat = place.lat, lon = place.lon))
                    results = emptyList()
                    query = ""
                    scope.launch {
                        WeatherService.fetch(place.lat, place.lon)?.let { Repository.putWeather(place.lat, place.lon, it) }
                        onWeather()
                    }
                }
            )
        }
        Text("Datos del clima: Open-Meteo. Se actualiza cada 30 minutos aprox.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
