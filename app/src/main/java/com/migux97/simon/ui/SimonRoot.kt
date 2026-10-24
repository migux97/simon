package com.migux97.simon.ui

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.migux97.simon.draw.AnimationScreen
import com.migux97.simon.draw.DrawingScreen
import com.migux97.simon.ui.screens.BuilderScreen
import com.migux97.simon.ui.screens.WallpaperEditScreen
import com.migux97.simon.ui.screens.CreateScreen
import com.migux97.simon.ui.screens.EffectsScreen
import com.migux97.simon.ui.screens.HomeScreen
import com.migux97.simon.ui.screens.LauncherSettingsScreen
import com.migux97.simon.ui.screens.OverlayScreen
import com.migux97.simon.ui.screens.PerformanceScreen
import com.migux97.simon.ui.screens.ToolsHubScreen
import com.migux97.simon.ui.screens.RoutinesScreen
import com.migux97.simon.ui.screens.RoutineEditorScreen
import com.migux97.simon.ui.screens.RotationScreen
import com.migux97.simon.ui.screens.ChargingSettingsScreen
import com.migux97.simon.ui.screens.LockSettingsScreen
import com.migux97.simon.ui.screens.EdgeSettingsScreen
import com.migux97.simon.ui.screens.BubbleSettingsScreen
import com.migux97.simon.ui.screens.SoundsScreen
import com.migux97.simon.ui.screens.BackupScreen
import com.migux97.simon.ui.screens.IconEditorScreen
import com.migux97.simon.ui.screens.IconsScreen
import com.migux97.simon.ui.screens.LiveDetailScreen
import com.migux97.simon.ui.screens.MineScreen
import com.migux97.simon.ui.screens.StaticDetailScreen
import com.migux97.simon.ui.screens.ThemeDetailScreen
import com.migux97.simon.ui.screens.WallpapersScreen
import com.migux97.simon.ui.screens.WidgetEditorScreen
import com.migux97.simon.ui.screens.WidgetsScreen

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "Inicio", Icons.Filled.AutoAwesome),
    Tab("widgets", "Widgets", Icons.Filled.Widgets),
    Tab("wallpapers", "Fondos", Icons.Filled.Wallpaper),
    Tab("create", "Crear", Icons.Filled.Brush),
    Tab("mine", "Mío", Icons.Filled.Person)
)

/** Acciones de navegación que comparten las pantallas. */
class Nav(private val nav: NavHostController) {
    fun back() = nav.popBackStack()
    fun go(route: String) = nav.navigate(route)
    fun editor(designId: String) = nav.navigate("editor/$designId")
    fun live(id: String) = nav.navigate("live/$id")
    fun static(id: String) = nav.navigate("static/$id")
    fun theme(id: String) = nav.navigate("theme/$id")
    fun iconsWithTheme(themeId: String) = nav.navigate("iconsTheme/$themeId")
    fun iconEditor(pkg: String, themeId: String? = null) = nav.navigate("iconedit/$pkg?theme=${themeId ?: ""}")
    fun tab(route: String) = nav.navigate(route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun SimonRoot(editDesignId: String?, route: String?, onEditConsumed: () -> Unit) {
    val navController = rememberNavController()
    val nav = Nav(navController)
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val showBar = tabs.any { it.route == current }

    LaunchedEffect(editDesignId, route) {
        if (editDesignId != null) {
            nav.editor(editDesignId)
            onEditConsumed()
        } else if (route != null) {
            nav.go(route)
            onEditConsumed()
        }
    }

    Scaffold(
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = current == tab.route,
                            onClick = { nav.tab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        // Las barras del sistema ya quedan fuera por este padding: se marcan como usadas para
        // que las barras de arriba de cada pantalla no sumen otra vez el alto de la barra de estado.
        NavHost(navController, startDestination = "home", modifier = Modifier.padding(padding).consumeWindowInsets(padding)) {
            composable("home") { HomeScreen(nav) }
            composable("widgets") { WidgetsScreen(nav) }
            composable("wallpapers") { WallpapersScreen(nav) }
            composable("icons") { IconsScreen(nav, themeId = null) }
            composable("create") { CreateScreen(nav) }
            composable("builder/{id}") { BuilderScreen(nav, it.arguments?.getString("id") ?: "new") }
            composable("wallEdit/{which}") { WallpaperEditScreen(nav, it.arguments?.getString("which") ?: "home") }
            composable("drawing/{id}") { DrawingScreen(nav, it.arguments?.getString("id") ?: "new") }
            composable("animation/{id}") { AnimationScreen(nav, it.arguments?.getString("id") ?: "new") }
            composable("effects") { EffectsScreen(nav) }
            composable("launcherSettings") { LauncherSettingsScreen(nav) }
            composable("overlay") { OverlayScreen(nav) }
            composable("performance") { PerformanceScreen(nav) }
            composable("tools") { ToolsHubScreen(nav) }
            composable("routines") { RoutinesScreen(nav) }
            composable("routine/{id}") { RoutineEditorScreen(nav, it.arguments?.getString("id") ?: "new") }
            composable("rotation") { RotationScreen(nav) }
            composable("charging") { ChargingSettingsScreen(nav) }
            composable("lockscreen") { LockSettingsScreen(nav) }
            composable("edge") { EdgeSettingsScreen(nav) }
            composable("bubble") { BubbleSettingsScreen(nav) }
            composable("sounds") { SoundsScreen(nav) }
            composable("backup") { BackupScreen(nav) }
            composable("mine") { MineScreen(nav) }
            composable("iconsTheme/{id}") { IconsScreen(nav, themeId = it.arguments?.getString("id")) }
            composable("editor/{id}") { WidgetEditorScreen(nav, it.arguments?.getString("id") ?: "") }
            composable("live/{id}") { LiveDetailScreen(nav, it.arguments?.getString("id") ?: "") }
            composable("static/{id}") { StaticDetailScreen(nav, it.arguments?.getString("id") ?: "") }
            composable("theme/{id}") { ThemeDetailScreen(nav, it.arguments?.getString("id") ?: "") }
            composable("iconedit/{pkg}?theme={theme}") {
                IconEditorScreen(nav, it.arguments?.getString("pkg") ?: "", it.arguments?.getString("theme")?.takeIf { t -> t.isNotBlank() })
            }
        }
    }
}
