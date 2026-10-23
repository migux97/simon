package com.migux97.simon.launcher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.migux97.simon.data.StudioStore
import com.migux97.simon.ui.components.LocalAnimationsActive
import com.migux97.simon.ui.components.rememberAnimationsAllowed

/** "Páginas" especiales donde viven los elementos de cada capa libre. */
const val OVERLAY_PAGE = -1
const val LOCK_PAGE = -2
const val CHARGE_PAGE = -3

/** Lo que se dibuja encima del launcher original (sin tomar toques). */
@Composable
fun HomeOverlayContent() {
    val items by StudioStore.homeItemsFlow.collectAsStateWithLifecycle()
    val settings by StudioStore.launcherFlow.collectAsStateWithLifecycle()
    val ui = remember { LauncherUi() }
    val allowed = rememberAnimationsAllowed()
    CompositionLocalProvider(LocalAnimationsActive provides allowed) {
        PageCanvas(OVERLAY_PAGE, items.filter { it.page == OVERLAY_PAGE }.sortedBy { it.z }, settings, ui)
    }
}
