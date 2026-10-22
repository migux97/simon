package com.migux97.simon.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.migux97.simon.data.Catalog
import com.migux97.simon.data.Repository
import com.migux97.simon.data.WidgetDesign
import com.migux97.simon.data.WidgetKind
import com.migux97.simon.data.WidgetSize
import com.migux97.simon.data.newId
import com.migux97.simon.ui.components.ChoiceChips
import com.migux97.simon.ui.components.WidgetOnBackdrop
import com.migux97.simon.ui.theme.SimonTheme

/** Se abre al agregar un widget de Simón desde el launcher: elegís el diseño. */
class WidgetConfigureActivity : ComponentActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Repository.init(this)
        enableEdgeToEdge()
        widgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        setResult(Activity.RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val info = AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId)
        val size = WidgetUpdater.sizeForProvider(info?.provider?.className)

        setContent {
            SimonTheme {
                Surface(Modifier.fillMaxSize()) {
                    val mine by Repository.designs.collectAsStateWithLifecycle()
                    var kind by remember { mutableStateOf<WidgetKind?>(null) }
                    val own = mine.filter { it.size == size && (kind == null || it.kind == kind) }
                    val templates = Catalog.templatesFor(kind, size)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Column(Modifier.statusBarsPadding()) {
                                Text("Elegí un diseño", style = MaterialTheme.typography.headlineMedium)
                                Text("Widget ${size.label.lowercase()} (${size.cellsW}×${size.cellsH})", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            ChoiceChips(listOf<WidgetKind?>(null) + WidgetKind.entries, kind, { it?.label ?: "Todos" }, { kind = it }, Modifier.padding(vertical = 4.dp))
                        }
                        if (own.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) { Text("Mis diseños", style = MaterialTheme.typography.titleMedium) }
                            items(own, key = { "o" + it.id }, span = { GridItemSpan(if (size == WidgetSize.SMALL) 1 else 2) }) { d ->
                                Option(d) { choose(d, saved = true) }
                            }
                            item(span = { GridItemSpan(maxLineSpan) }) { Text("Plantillas", style = MaterialTheme.typography.titleMedium) }
                        }
                        items(templates, key = { "t" + it.id }, span = { GridItemSpan(if (size == WidgetSize.SMALL) 1 else 2) }) { d ->
                            Option(d) { choose(d, saved = false) }
                        }
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun Option(d: WidgetDesign, onClick: () -> Unit) {
        Column(Modifier.clickable(onClick = onClick)) {
            WidgetOnBackdrop(d, Modifier.fillMaxWidth())
            Text(d.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
        }
    }

    private fun choose(design: WidgetDesign, saved: Boolean) {
        val final = if (saved) design else design.copy(id = newId())
        if (!saved) Repository.saveDesign(final)
        Repository.bindWidget(widgetId, final.id)
        val mgr = AppWidgetManager.getInstance(this)
        WidgetUpdater.update(this, mgr, widgetId)
        WidgetUpdater.scheduleTick(this)
        if (final.kind == WidgetKind.WEATHER) RefreshWorker.runNow(this)
        setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        // Si es un diseño nuevo que necesita datos (foto, ciudad, fecha), abrir el editor.
        val needsSetup = !saved && final.kind in setOf(WidgetKind.PHOTO, WidgetKind.WEATHER, WidgetKind.COUPLE, WidgetKind.COUNTDOWN)
        if (needsSetup) {
            startActivity(
                Intent(this, com.migux97.simon.MainActivity::class.java)
                    .putExtra(com.migux97.simon.MainActivity.EXTRA_EDIT_DESIGN, final.id)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        finish()
    }
}
