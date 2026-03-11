package com.tungnk123.snapcut.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.tungnk123.snapcut.feature.editor.EditorScreen
import com.tungnk123.snapcut.feature.picker.PickerScreen
import com.tungnk123.snapcut.feature.settings.SettingsScreen
import com.tungnk123.snapcut.feature.sticker.StickerHistoryScreen
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable object PickerRoute
@Serializable data class EditorRoute(val encodedUri: String)
@Serializable object StickerHistoryRoute
@Serializable object SettingsRoute

private data class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String,
)

private val topLevelDestinations = listOf(
    TopLevelDestination(
        route = PickerRoute,
        routeClass = PickerRoute::class,
        selectedIcon = Icons.Filled.PhotoLibrary,
        unselectedIcon = Icons.Outlined.PhotoLibrary,
        label = "Gallery",
    ),
    TopLevelDestination(
        route = StickerHistoryRoute,
        routeClass = StickerHistoryRoute::class,
        selectedIcon = Icons.Filled.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome,
        label = "Stickers",
    ),
    TopLevelDestination(
        route = SettingsRoute,
        routeClass = SettingsRoute::class,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        label = "Settings",
    ),
)

@Composable
fun SnapCutNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = topLevelDestinations.any { dest ->
        currentDestination?.hasRoute(dest.routeClass) == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topLevelDestinations.forEach { dest ->
                        val selected = currentDestination?.hasRoute(dest.routeClass) == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = dest.label,
                                )
                            },
                            label = { Text(dest.label) },
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = PickerRoute,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            composable<PickerRoute> {
                PickerScreen(
                    onImagePicked = { uri ->
                        navController.navigate(EditorRoute(encodedUri = Uri.encode(uri.toString())))
                    }
                )
            }

            composable<EditorRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<EditorRoute>()
                val uri = Uri.parse(Uri.decode(route.encodedUri))
                EditorScreen(
                    imageUri = uri,
                    onBack = { navController.popBackStack() },
                )
            }

            composable<StickerHistoryRoute> {
                StickerHistoryScreen()
            }

            composable<SettingsRoute> {
                SettingsScreen()
            }
        }
    }
}
