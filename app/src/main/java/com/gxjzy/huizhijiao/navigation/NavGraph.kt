package com.gxjzy.huizhijiao.navigation

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gxjzy.huizhijiao.api.ApiClient
import com.gxjzy.huizhijiao.ui.components.GlassBackground
import com.gxjzy.huizhijiao.ui.pages.*
import com.gxjzy.huizhijiao.ui.tabs.ManageDataCache

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val MAIN = "main"
    const val ADMIN = "admin"
    const val SETTINGS = "settings"
    const val WEEKLY_REPORT = "weekly_report"
    const val MONTHLY_SUMMARY = "monthly_summary"
    const val INTERNSHIP_SUMMARY = "internship_summary"
    const val WEEK_MANAGE = "week_manage"
    const val MONTH_MANAGE = "month_manage"
    const val TEMPLATE = "template"
    const val REPORT_DETAIL = "report_detail/{type}/{id}/{title}"
}

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    Box(modifier = modifier.fillMaxSize()) {
        GlassBackground(modifier = Modifier.fillMaxSize()) {}
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { slideInHorizontally(tween(250, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(250)) },
            exitTransition = { slideOutHorizontally(tween(200, easing = FastOutLinearInEasing)) { -it / 3 } + fadeOut(tween(150)) },
            popEnterTransition = { slideInHorizontally(tween(250, easing = FastOutSlowInEasing)) { -it / 3 } + fadeIn(tween(250)) },
            popExitTransition = { slideOutHorizontally(tween(200, easing = FastOutLinearInEasing)) { it } + fadeOut(tween(150)) }
        ) {
            composable(Routes.SPLASH) {
                SplashScreen(
                    onNavigateToLogin = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.SPLASH) {
                                inclusive = true
                            }
                        }
                    },
                    onNavigateToMain = {
                        navController.navigate(Routes.MAIN) {
                            popUpTo(Routes.SPLASH) {
                                inclusive = true
                            }
                        }
                    },
                    onNavigateToAdmin = {
                        navController.navigate(Routes.ADMIN) {
                            popUpTo(Routes.SPLASH) {
                                inclusive = true
                            }
                        }
                    }
                )
            }
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = { role ->
                        val dest = if (role == "admin") Routes.ADMIN else Routes.MAIN
                        navController.navigate(dest) { popUpTo(Routes.LOGIN) { inclusive = true } }
                    }
                )
            }
            composable(Routes.MAIN) {
                MainScreen(
                    onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                    onNavigateToWeeklyReport = { navController.navigate(Routes.WEEKLY_REPORT) },
                    onNavigateToMonthlySummary = { navController.navigate(Routes.MONTHLY_SUMMARY) },
                    onNavigateToInternshipSummary = { navController.navigate(Routes.INTERNSHIP_SUMMARY) },
                    onNavigateToWeekManage = { navController.navigate(Routes.WEEK_MANAGE) },
                    onNavigateToMonthManage = { navController.navigate(Routes.MONTH_MANAGE) },
                    onNavigateToTemplate = { navController.navigate(Routes.TEMPLATE) },
                    onNavigateToReportDetail = { type, id, title ->
                        navController.navigate("report_detail/$type/$id/$title")
                    },
                    onLogout = {
                        ApiClient.clearCache(); ManageDataCache.clear(); navController.navigate(
                        Routes.LOGIN
                    ) { popUpTo(0) { inclusive = true } }
                    }
                )
            }
            composable(Routes.ADMIN) {
                AdminScreen(onLogout = {
                    ApiClient.clearCache(); ManageDataCache.clear(); navController.navigate(
                    Routes.LOGIN
                ) { popUpTo(0) { inclusive = true } }
                })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onLogout = {
                        ApiClient.clearCache(); ManageDataCache.clear(); navController.navigate(
                        Routes.LOGIN
                    ) { popUpTo(0) { inclusive = true } }
                    }
                )
            }
            composable(Routes.WEEKLY_REPORT) { WeeklyReportScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.MONTHLY_SUMMARY) { MonthlySummaryScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.INTERNSHIP_SUMMARY) { InternshipSummaryScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.WEEK_MANAGE) { WeekManageScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.MONTH_MANAGE) { MonthManageScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.TEMPLATE) { TemplateScreen(onBack = { navController.popBackStack() }) }
            composable(
                route = Routes.REPORT_DETAIL,
                arguments = listOf(
                    androidx.navigation.navArgument("type") {
                        type = androidx.navigation.NavType.StringType
                    },
                    androidx.navigation.navArgument("id") {
                        type = androidx.navigation.NavType.StringType
                    },
                    androidx.navigation.navArgument("title") {
                        type = androidx.navigation.NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val type = backStackEntry.arguments?.getString("type") ?: "weekly"
                val id = backStackEntry.arguments?.getString("id") ?: "0"
                val title = backStackEntry.arguments?.getString("title") ?: "报告详情"
                ReportDetailScreen(
                    type = type,
                    id = id,
                    title = title,
                    onBack = { navController.popBackStack() })
            }
        }
    }
}
