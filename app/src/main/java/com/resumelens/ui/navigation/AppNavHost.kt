package com.resumelens.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.resumelens.R
import com.resumelens.ui.analyze.AnalyzeRoute
import com.resumelens.ui.history.HistoryRoute
import com.resumelens.ui.result.ResultRoute
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

// Type-safe routes: arguments are checked by the compiler instead of being string templates.
@Serializable data object AnalyzeDestination
@Serializable data object HistoryDestination
@Serializable data class ResultDestination(val analysisId: Long)

private data class TopLevel(val route: Any, val routeClass: KClass<*>, val label: Int, val icon: ImageVector)

private val topLevel = listOf(
    TopLevel(AnalyzeDestination, AnalyzeDestination::class, R.string.nav_analyze, Icons.Outlined.AutoAwesome),
    TopLevel(HistoryDestination, HistoryDestination::class, R.string.nav_history, Icons.Outlined.History),
)

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val showBottomBar = topLevel.any { destination?.hasRoute(it.routeClass) == true }

    fun navigateTopLevel(route: Any) = navController.navigate(route) {
        // Standard bottom-nav behaviour: one copy of each tab, and each tab keeps its state.
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    Scaffold(
        // Inner screens handle their own insets; this Scaffold only reserves space for the bottom bar.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    topLevel.forEach { item ->
                        NavigationBarItem(
                            selected = destination?.hasRoute(item.routeClass) == true,
                            onClick = { navigateTopLevel(item.route) },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.label)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = AnalyzeDestination,
            // consumeWindowInsets stops inner Scaffolds from padding for the nav bar a second time.
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            composable<AnalyzeDestination> {
                AnalyzeRoute(onOpenResult = { id -> navController.navigate(ResultDestination(id)) })
            }
            composable<HistoryDestination> {
                HistoryRoute(
                    onOpen = { id -> navController.navigate(ResultDestination(id)) },
                    onStartAnalysis = { navigateTopLevel(AnalyzeDestination) },
                )
            }
            composable<ResultDestination> {
                ResultRoute(onBack = { navController.popBackStack() })
            }
        }
    }
}
