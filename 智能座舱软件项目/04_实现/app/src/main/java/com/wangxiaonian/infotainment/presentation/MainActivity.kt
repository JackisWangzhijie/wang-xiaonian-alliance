package com.wangxiaonian.infotainment.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wangxiaonian.infotainment.feature.hvac.HvacScreen
import com.wangxiaonian.infotainment.feature.hvac.HvacViewModel
import com.wangxiaonian.infotainment.feature.launcher.LauncherScreen
import com.wangxiaonian.infotainment.feature.launcher.LauncherViewModel
import com.wangxiaonian.infotainment.feature.media.MediaScreen
import com.wangxiaonian.infotainment.feature.media.MediaViewModel
import com.wangxiaonian.infotainment.feature.navigation.NavigationScreen
import com.wangxiaonian.infotainment.feature.navigation.NavigationViewModel
import com.wangxiaonian.infotainment.feature.notification.NotificationScreen
import com.wangxiaonian.infotainment.feature.notification.NotificationViewModel
import com.wangxiaonian.infotainment.feature.settings.SettingsScreen
import com.wangxiaonian.infotainment.feature.settings.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * 主 Activity
 * 车载信息娱乐系统主入口
 *
 * @author 王小年联盟
 * @version 1.0
 * @trace SRS-001, DD-系统框架-003
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val launcherViewModel: LauncherViewModel by viewModels()
    private val notificationViewModel: NotificationViewModel by viewModels()
    private val navigationViewModel: NavigationViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val hvacViewModel: HvacViewModel by viewModels()
    private val mediaViewModel: MediaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置全屏沉浸式体验
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            CarCockpitTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CarInfotainmentApp(
                        launcherViewModel = launcherViewModel,
                        notificationViewModel = notificationViewModel,
                        navigationViewModel = navigationViewModel,
                        settingsViewModel = settingsViewModel,
                        hvacViewModel = hvacViewModel,
                        mediaViewModel = mediaViewModel
                    )
                }
            }
        }
    }
}

/**
 * 应用主入口 Composable
 */
@Composable
fun CarInfotainmentApp(
    launcherViewModel: LauncherViewModel,
    notificationViewModel: NotificationViewModel,
    navigationViewModel: NavigationViewModel,
    settingsViewModel: SettingsViewModel,
    hvacViewModel: HvacViewModel,
    mediaViewModel: MediaViewModel
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Launcher.route
    ) {
        composable(Screen.Launcher.route) {
            LauncherScreen(
                viewModel = launcherViewModel,
                onNavigateToNotifications = {
                    navController.navigate(Screen.Notifications.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToNavigation = {
                    navController.navigate(Screen.Navigation.route)
                },
                onNavigateToHvac = {
                    navController.navigate(Screen.Hvac.route)
                },
                onNavigateToMedia = {
                    navController.navigate(Screen.Media.route)
                }
            )
        }

        composable(Screen.Notifications.route) {
            NotificationScreen(
                viewModel = notificationViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Navigation.route) {
            NavigationScreen(
                viewModel = navigationViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Hvac.route) {
            HvacScreen(
                viewModel = hvacViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Media.route) {
            MediaScreen(
                viewModel = mediaViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

/**
 * 路由定义
 */
sealed class Screen(val route: String) {
    object Launcher : Screen("launcher")
    object Notifications : Screen("notifications")
    object Settings : Screen("settings")
    object Navigation : Screen("navigation")
    object Hvac : Screen("hvac")
    object Media : Screen("media")
}
