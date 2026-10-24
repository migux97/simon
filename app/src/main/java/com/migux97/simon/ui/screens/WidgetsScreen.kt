package com.migux97.simon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.migux97.simon.custom.CustomWidgetLive
import com.migux97.simon.data.Catalog
import com.migux97.simon.data.Categories
import com.migux97.simon.data.Category
import com.migux97.simon.ui.components.CategoryCard
import com.migux97.simon.ui.components.CategoryHeader
import com.migux97.simon.ui.components.WidgetPreview
import com.migux97.simon.data.CustomWidget
import com.migux97.simon.data.Repository
import com.migux97.simon.data.StudioStore
import com.migux97.simon.data.WidgetDesign
import com.migux97.simon.data.WidgetKind
import com.migux97.simon.data.WidgetSize
import com.migux97.simon.ui.Nav
import com.migux97.simon.ui.components.ChoiceChips
import com.migux97.simon.ui.components.GradientBadge
import com.migux97.simon.ui.components.PreviewBackdrop
import com.migux97.simon.ui.components.WidgetOnBackdrop
import com.migux97.simon.ui.theme.SimonGradient
import com.migux97.simon.widget.WidgetUpdater

/** Algo que está puesto en la pantalla de inicio: un diseño o un widget creado. */
private sealed interface Placed {
    val count: Int

    data class Design(val design: WidgetDesign, override val count: Int) : Placed
    data class Custom(val widget: CustomWidget, override val count: Int) : Placed
}

private fun LazyGridScope.sectionTitle(text: String, sub: String? = null) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        Column(Modifier.padding(top = 14.dp, bottom = 2.dp)) {
            Text(text, style = MaterialTheme.typography.titleLarge)
            sub?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

private fun onHomeLabel(count: Int) = if (count <= 1) "En inicio" else "En inicio ×$count"

@Composable
fun WidgetsScreen(nav: Nav) {
    val context = LocalContext.current
    var catName by rememberSaveable { mutableStateOf<String?>(null) }
    var sizeName by rememberSaveable { mutableStateOf<String?>(null) }
    val cat = catName?.let { Category.valueOf(it) }
    val size = sizeName?.let { WidgetSize.valueOf(it) }
    // Las plantillas por categoría (reloj, pareja…); adentro de cada una se filtra por tamaño.
    val groups = remember { Categories.group(Catalog.widgetTemplates) { Categories.of(it) } }
    val templates = remember(cat, size) {
        // Primero los que son de ese tipo (en «Pareja», los widgets de pareja antes que los relojes rosas).
        groups.firstOrNull { it.first == cat }?.second.orEmpty().filter { size == null || it.size == size }
            .sortedByDescending { Categories.main(it) == cat }
    }

    val designs by Repository.designs.collectAsStateWithLifecycle()
    val bindings by Repository.bindings.collectAsStateWithLifecycle()
    val created by StudioStore.widgetsFlow.collectAsStateWithLifecycle()

    // Widgets que de verdad están en la pantalla de inicio (agrupados por lo que muestran).
    val placedBindings = remember(bindings) { WidgetUpdater.placedBindings(context) }
    val placed: List<Placed> = remember(placedBindings, designs, created) {
        placedBindings.values.groupingBy { it }.eachCount().mapNotNull { (value, n) ->
            if (value.startsWith(WidgetUpdater.CUSTOM_PREFIX)) {
                val id = value.removePrefix(WidgetUpdater.CUSTOM_PREFIX)
                created.firstOrNull { it.id == id }?.let { Placed.Custom(it, n) }
            } else {
                designs.firstOrNull { it.id == value }?.let { Placed.Design(it, n) }
            }
        }
    }
    val placedCountByDesign = remember(placedBindings) { placedBindings.values.groupingBy { it }.eachCount() }
    val savedFiltered = designs

    // Al entrar o salir de una categoría se va a su título.
    val grid = rememberLazyGridState()
    LaunchedEffect(catName) {
        if (catName != null) grid.scrollToItem((2 + placed.size).coerceAtMost((grid.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)))
    }
    LazyVerticalGrid(
        state = grid,
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.statusBarsPadding().padding(horizontal = 4.dp)) {
                Text("Widgets", style = MaterialTheme.typography.headlineLarge)
                Text("Tocá cualquier widget para editarlo", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // 1) Lo que ya está en la pantalla de inicio
        sectionTitle(
            "En tu pantalla de inicio",
            if (placed.isEmpty()) "Todavía no agregaste widgets de Simón a tu inicio. Elegí un diseño de abajo y tocá «Agregar a inicio»."
            else "Tocá uno para cambiarlo: el cambio se ve directo en tu inicio."
        )
        items(
            placed,
            key = {
                when (it) {
                    is Placed.Design -> "p_d_" + it.design.id
                    is Placed.Custom -> "p_c_" + it.widget.id
                }
            },
            span = {
                when (it) {
                    is Placed.Design -> GridItemSpan(if (it.design.size == WidgetSize.SMALL) 1 else 2)
                    is Placed.Custom -> GridItemSpan(if (it.widget.aspect < 1.2f) 1 else 2)
                }
            }
        ) { p ->
            when (p) {
                is Placed.Design -> Column(Modifier.clickable { nav.editor(p.design.id) }) {
                    Box {
                        WidgetOnBackdrop(p.design, Modifier.fillMaxWidth())
                        GradientBadge(onHomeLabel(p.count), SimonGradient, Modifier.align(Alignment.TopStart).padding(8.dp))
                    }
                    Text(p.design.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, modifier = Modifier.padding(start = 4.dp, top = 6.dp))
                    Text("Diseño de plantilla · ${p.design.size.label}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
                }
                is Placed.Custom -> Column(Modifier.clickable { nav.go("builder/${p.widget.id}") }) {
                    Box {
                        CustomWidgetTile(p.widget, Modifier.fillMaxWidth())
                        GradientBadge(onHomeLabel(p.count), SimonGradient, Modifier.align(Alignment.TopStart).padding(8.dp))
                    }
                    Text(p.widget.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, modifier = Modifier.padding(start = 4.dp, top = 6.dp))
                    Text("Creado por vos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }

        // 2) Diseños guardados
        if (cat == null) {
            sectionTitle(
                "Tus diseños guardados",
                if (designs.isEmpty()) "Cuando guardes un diseño lo vas a encontrar acá." else null
            )
            items(savedFiltered, key = { "s_" + it.id }, span = { GridItemSpan(if (it.size == WidgetSize.SMALL) 1 else 2) }) { d ->
                val n = placedCountByDesign[d.id] ?: 0
                Column(Modifier.clickable { nav.editor(d.id) }) {
                    Box {
                        WidgetOnBackdrop(d, Modifier.fillMaxWidth())
                        if (n > 0) GradientBadge(onHomeLabel(n), SimonGradient, Modifier.align(Alignment.TopStart).padding(8.dp))
                    }
                    Text(d.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, modifier = Modifier.padding(start = 4.dp, top = 6.dp))
                    Text(d.size.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }

        // 3) Plantillas por categoría
        if (cat == null) {
            sectionTitle("Plantillas por categoría", "${Catalog.widgetTemplates.size} diseños listos. Elegí una categoría.")
            items(groups, key = { "c_" + it.first.name }) { (c, list) ->
                CategoryCard(c, list.size, onClick = { catName = c.name; sizeName = null }) {
                    // La muestra es de ese tipo (un reloj en «Reloj», un widget de pareja en «Pareja»…).
                    val small = list.filter { it.size == WidgetSize.SMALL }
                    val sample = small.firstOrNull { Categories.main(it) == c } ?: small.firstOrNull() ?: list.first()
                    WidgetPreview(sample, Modifier.align(Alignment.TopCenter).padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 52.dp).fillMaxWidth())
                }
            }
        } else {
            item(span = { GridItemSpan(maxLineSpan) }, key = "cat_header") {
                Column {
                    CategoryHeader(cat, templates.size, onBack = { catName = null; sizeName = null })
                    ChoiceChips(
                        options = listOf<WidgetSize?>(null) + WidgetSize.entries,
                        selected = size,
                        label = { it?.let { s -> "${s.label} ${s.cellsW}×${s.cellsH}" } ?: "Todos los tamaños" },
                        onSelect = { sizeName = it?.name }
                    )
                }
            }
            items(templates, key = { "t_" + it.id }, span = { GridItemSpan(if (it.size == WidgetSize.SMALL) 1 else 2) }) { d ->
                Column(Modifier.clickable { nav.editor(d.id) }) {
                    WidgetOnBackdrop(d, Modifier.fillMaxWidth())
                    Text(d.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, modifier = Modifier.padding(start = 4.dp, top = 6.dp))
                    Text(d.size.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}

/** Widget creado en el constructor sobre el fondo de muestra. */
@Composable
internal fun CustomWidgetTile(widget: CustomWidget, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(PreviewBackdrop)
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(widget.aspect.coerceIn(0.4f, 4f))) {
            CustomWidgetLive(widget, Modifier.fillMaxSize(), interactive = false)
        }
    }
}
