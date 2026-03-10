package com.tungnk123.snapcut.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.tungnk123.snapcut.feature.editor.EditorScreen
import com.tungnk123.snapcut.feature.picker.PickerScreen
import com.tungnk123.snapcut.feature.sticker.StickerHistoryScreen
import kotlinx.serialization.Serializable

// Type-safe routes using @Serializable (Navigation Compose 2.8+)
@Serializable
object PickerRoute

@Serializable
data class EditorRoute(val encodedUri: String)

@Serializable
object StickerHistoryRoute

@Composable
fun SnapCutNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = PickerRoute
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
                onNavigateToHistory = {
                    navController.navigate(StickerHistoryRoute)
                }
            )
        }

        composable<StickerHistoryRoute> {
            StickerHistoryScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
