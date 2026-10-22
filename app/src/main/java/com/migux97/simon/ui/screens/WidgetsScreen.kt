package com.migux97.simon.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.migux97.simon.data.Catalog
import com.migux97.simon.data.WidgetKind
import com.migux97.simon.data.WidgetSize
import com.migux97.simon.ui.Nav
import com.migux97.simon.ui.components.ChoiceChips
import com.migux97.simon.ui.components.WidgetOnBackdrop

@Composable
fun WidgetsScreen(nav: Nav) {
    var kindName by rememberSaveable { mutableStateOf<String?>(null) }
    var sizeName by rememberSaveable { mutableStateOf<String?>(null) }
    val kind = kindName?.let { WidgetKind.valueOf(it) }
    val size = sizeName?.let { WidgetSize.valueOf(it) }
    val items = remember(kind, size) { Catalog.templatesFor(kind, size) }

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text("Widgets", style = MaterialTheme.typography.headlineLarge)
            Text("${Catalog.widgetTemplates.size} diseños para personalizar", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ChoiceChips(
            options = listOf<WidgetKind?>(null) + WidgetKind.entries,
            selected = kind,
            label = { it?.label ?: "Todos" },
            onSelect = { kindName = it?.name }
        )
        ChoiceChips(
            options = listOf<WidgetSize?>(null) + WidgetSize.entries,
            selected = size,
            label = { it?.let { s -> "${s.label} ${s.cellsW}×${s.cellsH}" } ?: "Todos los tamaños" },
            onSelect = { sizeName = it?.name },
            modifier = Modifier.padding(top = 4.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(items, key = { it.id }, span = { GridItemSpan(if (it.size == WidgetSize.SMALL) 1 else 2) }) { d ->
                Column(Modifier.clickable { nav.editor(d.id) }) {
                    WidgetOnBackdrop(d, Modifier.fillMaxWidth())
                    Text(d.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, modifier = Modifier.padding(start = 4.dp, top = 6.dp))
                    Text(d.size.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}
