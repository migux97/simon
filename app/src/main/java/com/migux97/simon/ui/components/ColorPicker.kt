package com.migux97.simon.ui.components

import android.graphics.Color as AColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

val QuickColors = listOf(
    0xFFFFFFFF, 0xFF000000, 0xFF1C1C1E, 0xFF8E8E93, 0xFFFF5A5F, 0xFFFF7E5F, 0xFFFF9F0A, 0xFFFFD166,
    0xFFD4FC79, 0xFF34C759, 0xFF2E7D5B, 0xFF12D8FA, 0xFF1FA2FF, 0xFF3D5AFE, 0xFF6C4CF5, 0xFF8B6CFF,
    0xFFC77DFF, 0xFFFF7EB6, 0xFFFF4F81, 0xFFEE9CA7, 0xFFF5F0E6, 0xFF3B3024, 0xFF0F2027, 0xFF00F5D4
).map { it.toInt() }

/** Selector de color: matiz + saturación/brillo + muestras rápidas + código HEX. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerDialog(initial: Int, title: String, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    val hsv = remember { FloatArray(3).also { AColor.colorToHSV(initial, it) } }
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var sat by remember { mutableFloatStateOf(hsv[1]) }
    var value by remember { mutableFloatStateOf(hsv[2]) }
    val current = AColor.HSVToColor(floatArrayOf(hue, sat, value))
    var hex by remember { mutableStateOf(toHex(initial)) }

    fun setFrom(color: Int) {
        val arr = FloatArray(3)
        AColor.colorToHSV(color, arr)
        hue = arr[0]; sat = arr[1]; value = arr[2]
        hex = toHex(color)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Cuadro saturación (x) / brillo (y)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(14.dp))
                ) {
                    val hueColor = Color(AColor.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                    Canvas(
                        Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .pointerInput(Unit) {
                                fun handle(o: Offset) {
                                    sat = (o.x / size.width).coerceIn(0f, 1f)
                                    value = 1f - (o.y / size.height).coerceIn(0f, 1f)
                                    hex = toHex(AColor.HSVToColor(floatArrayOf(hue, sat, value)))
                                }
                                detectTapGestures { handle(it) }
                            }
                            .pointerInput(Unit) {
                                detectDragGestures { change, _ ->
                                    sat = (change.position.x / size.width).coerceIn(0f, 1f)
                                    value = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                                    hex = toHex(AColor.HSVToColor(floatArrayOf(hue, sat, value)))
                                }
                            }
                    ) {
                        drawRect(Brush.horizontalGradient(listOf(Color.White, hueColor)))
                        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                        val pos = Offset(sat * size.width, (1 - value) * size.height)
                        drawCircle(Color.White, 11.dp.toPx(), pos, style = Stroke(3.dp.toPx()))
                        drawCircle(Color(current), 8.dp.toPx(), pos)
                    }
                }
                // Barra de matiz
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .pointerInput(Unit) {
                            detectTapGestures {
                                hue = (it.x / size.width * 360f).coerceIn(0f, 359.9f)
                                hex = toHex(AColor.HSVToColor(floatArrayOf(hue, sat, value)))
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                hue = (change.position.x / size.width * 360f).coerceIn(0f, 359.9f)
                                hex = toHex(AColor.HSVToColor(floatArrayOf(hue, sat, value)))
                            }
                        }
                ) {
                    val colors = (0..6).map { Color(AColor.HSVToColor(floatArrayOf(it * 60f % 360f, 1f, 1f))) }
                    drawRect(Brush.horizontalGradient(colors))
                    val x = hue / 360f * size.width
                    drawCircle(Color.White, size.height / 2 - 2.dp.toPx(), Offset(x, size.height / 2), style = Stroke(3.dp.toPx()))
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickColors.forEach { col ->
                        Box(
                            Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(col))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                .clickable { setFrom(col) }
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(current))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = hex,
                        onValueChange = { txt ->
                            hex = txt.uppercase().take(7)
                            parseHex(hex)?.let { c ->
                                val arr = FloatArray(3); AColor.colorToHSV(c, arr)
                                hue = arr[0]; sat = arr[1]; value = arr[2]
                            }
                        },
                        singleLine = true,
                        label = { Text("HEX") },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { onPick(current); onDismiss() }) { Text("Listo") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

fun toHex(color: Int): String = "#%06X".format(0xFFFFFF and color)

fun parseHex(text: String): Int? {
    val clean = text.trim().removePrefix("#")
    if (clean.length != 6) return null
    return clean.toLongOrNull(16)?.let { (0xFF000000 or it).toInt() }
}

/** Fila con una muestra de color que abre el selector. */
@Composable
fun ColorRow(label: String, color: Int, onChange: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { open = true }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(toHex(color), style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(color))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
        )
    }
    if (open) ColorPickerDialog(color, label, onDismiss = { open = false }, onPick = onChange)
}
