package com.example.ui.navigation

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.ui.editor.EditorScreen
import com.example.ui.home.StudioHomeScreen
import com.example.viewmodel.EditorViewModel
import com.example.viewmodel.ImageEditorViewModel
import com.example.viewmodel.StudioHomeViewModel
import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
data class EditorRoute(val projectId: Long)

@OptIn(UnstableApi::class)
@Composable
fun VeloCutNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = HomeRoute
    ) {
        composable<HomeRoute> {
            val homeViewModel: StudioHomeViewModel = viewModel()
            val imageEditorViewModel: ImageEditorViewModel = viewModel()
            StudioHomeScreen(
                viewModel = homeViewModel,
                imageEditorViewModel = imageEditorViewModel,
                onOpenProject = { projectId ->
                    navController.navigate(EditorRoute(projectId = projectId))
                }
            )
        }

        composable<EditorRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<EditorRoute>()
            val editorViewModel: EditorViewModel = viewModel()
            EditorScreen(
                projectId = route.projectId,
                viewModel = editorViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
