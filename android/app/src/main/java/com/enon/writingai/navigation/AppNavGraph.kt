package com.enon.writingai.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.enon.writingai.feature.about.AboutScreen
import com.enon.writingai.feature.capture.CaptureScreen
import com.enon.writingai.feature.history.HistoryDetailScreen
import com.enon.writingai.feature.gallery.GalleryImportScreen
import com.enon.writingai.feature.history.HistoryScreen
import com.enon.writingai.feature.home.HomeScreen
import com.enon.writingai.feature.preprocessing.PreprocessScreen
import com.enon.writingai.feature.result.ResultScreen
import com.enon.writingai.feature.scan.ScanScreen
import com.enon.writingai.feature.settings.SettingsScreen
import com.enon.writingai.feature.splash.SplashScreen

private data class BottomMenuItem(
    val destination: Destination,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val bottomMenuItems = listOf(
    BottomMenuItem(
        destination = Destination.Home,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
    ),
    BottomMenuItem(
        destination = Destination.History,
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History,
    ),
    BottomMenuItem(
        destination = Destination.Settings,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
    ),
)

@Composable
fun AppNavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val topLevelRoutes = setOf(
        Destination.Home.route,
        Destination.History.route,
        Destination.Settings.route,
    )

    val showBottomMenu = currentRoute in topLevelRoutes

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        bottomBar = {
            if (showBottomMenu) {
                AppBottomMenu(
                    currentRoute = currentRoute,
                    onSelect = { destination -> navController.navigateToTopLevel(destination) },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Splash.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Destination.Splash.route) {
                SplashScreen(
                    onFinished = {
                        navController.navigateToTopLevel(Destination.Home)
                    },
                )
            }
            composable(Destination.Home.route) {
                HomeScreen(
                    onCaptureClick = { navController.navigate(Destination.Capture.route) },
                    onGalleryClick = { navController.navigate(Destination.Gallery.route) },
                    onHistoryClick = { navController.navigateToTopLevel(Destination.History) },
                    onSettingsClick = { navController.navigateToTopLevel(Destination.Settings) },
                    onAboutClick = { navController.navigate(Destination.About.route) },
                )
            }
            composable(Destination.Capture.route) {
                CaptureScreen(
                    onBack = { navController.popBackStack() },
                    onContinue = { navController.navigate(Destination.Preprocess.route) },
                )
            }
            composable(Destination.Gallery.route) {
                GalleryImportScreen(
                    onBack = { navController.popBackStack() },
                    onContinue = { navController.navigate(Destination.Preprocess.route) },
                )
            }
            composable(Destination.Preprocess.route) {
                PreprocessScreen(
                    onBack = { navController.popBackStack() },
                    onContinue = { navController.navigate(Destination.Scan.route) },
                )
            }
            composable(Destination.Scan.route) {
                ScanScreen(
                    onBack = { navController.popBackStack() },
                    onResult = { navController.navigate(Destination.Result.route) },
                )
            }
            composable(Destination.Result.route) {
                ResultScreen(
                    onBack = { navController.navigateToMainMenu() },
                    onHistoryClick = { navController.navigateToTopLevel(Destination.History) },
                )
            }
            composable(Destination.History.route) {
                HistoryScreen(
                    onBack = null,
                    onEntryClick = { entryId ->
                        navController.navigate(Destination.HistoryDetail.createRoute(entryId))
                    },
                )
            }
            composable(
                route = Destination.HistoryDetail.route,
                arguments = listOf(
                    navArgument(Destination.HISTORY_ENTRY_ID_ARG) {
                        type = NavType.StringType
                    },
                ),
            ) { backStackEntry ->
                HistoryDetailScreen(
                    entryId = backStackEntry.arguments?.getString(Destination.HISTORY_ENTRY_ID_ARG).orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Destination.Settings.route) {
                SettingsScreen(
                    onBack = null,
                    onAboutClick = { navController.navigate(Destination.About.route) },
                )
            }
            composable(Destination.About.route) {
                AboutScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

private fun NavHostController.navigateToTopLevel(destination: Destination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavHostController.navigateToMainMenu() {
    navigate(Destination.Home.route) {
        popUpTo(graph.findStartDestination().id)
        launchSingleTop = true
    }
}

@Composable
private fun AppBottomMenu(
    currentRoute: String?,
    onSelect: (Destination) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.96f),
            tonalElevation = 5.dp,
            shadowElevation = 10.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                bottomMenuItems.forEach { item ->
                    val selected = currentRoute == item.destination.route
                    BottomMenuButton(
                        item = item,
                        selected = selected,
                        onClick = {
                            if (!selected) {
                                onSelect(item.destination)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.BottomMenuButton(
    item: BottomMenuItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            Color.Transparent
        },
        label = "bottom-menu-container",
    )
    val iconContainerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        label = "bottom-menu-icon-container",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "bottom-menu-content",
    )

    Row(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .background(containerColor)
            .then(
                if (selected) {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(22.dp),
                    )
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(iconContainerColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                contentDescription = item.destination.title,
                tint = contentColor,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = item.destination.title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = contentColor,
        )
    }
}
