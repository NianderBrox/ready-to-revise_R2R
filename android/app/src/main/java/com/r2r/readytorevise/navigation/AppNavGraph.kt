package com.r2r.readytorevise.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.r2r.readytorevise.presentation.analytics.AnalyticsScreen
import com.r2r.readytorevise.presentation.auth.register.RegisterScreen
import com.r2r.readytorevise.presentation.dashboard.DashboardScreen
import com.r2r.readytorevise.presentation.profile.ProfileScreen
import com.r2r.readytorevise.presentation.revision.RevisionScreen
import com.r2r.readytorevise.presentation.upload.UploadScreen

@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier
) {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route,
        modifier = modifier
    ) {

        composable(Screen.Login.route) {
            LoginRoute(
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) {
                            inclusive = true
                        }
                    }
                },
                onRegisterClick = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(navController)
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(navController)
        }

        composable(Screen.Upload.route) {
            UploadScreen(navController)
        }

        composable(Screen.Revision.route) {
            RevisionScreen(navController)
        }

        composable(Screen.Analytics.route) {
            AnalyticsScreen(navController)
        }

        composable(Screen.Profile.route) {
            ProfileScreen(navController)
        }
    }
}