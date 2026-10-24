package com.migux97.simon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.migux97.simon.data.Category

/**
 * Tarjeta de una categoría: una muestra de lo que tiene, el nombre y cuántas
 * cosas hay. Al tocarla se entra a la categoría.
 */
@Composable
fun CategoryCard(cat: Category, count: Int, modifier: Modifier = Modifier, previewAspect: Float = 1f, onClick: () -> Unit, preview: @Composable BoxScope.() -> Unit) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(previewAspect)
                .clip(RoundedCornerShape(18.dp))
                .background(PreviewBackdrop)
        ) {
            preview()
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.55f))))
            )
            Text(
                cat.emoji, fontSize = 22.sp,
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
            )
            Column(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
                Text(cat.label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(if (count == 1) "1 diseño" else "$count diseños", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
            }
        }
    }
}

/** Arriba de una categoría abierta: volver a todas y cuál estás mirando. */
@Composable
fun CategoryHeader(cat: Category, count: Int, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
            Text("  Categorías")
        }
        Text("${cat.emoji} ${cat.label}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("$count", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 8.dp))
    }
}

/** Versión chica (para paneles y diálogos): fichas con el ícono y el nombre de cada categoría. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryTiles(groups: List<Pair<Category, Int>>, onPick: (Category) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        groups.forEach { (c, n) ->
            Column(
                Modifier
                    .width(104.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { onPick(c) }
                    .padding(10.dp)
            ) {
                Text(c.emoji, fontSize = 22.sp)
                Text(c.label, style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("$n", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
