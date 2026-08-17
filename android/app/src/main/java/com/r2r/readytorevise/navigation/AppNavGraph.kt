package com.r2r.readytorevise.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import com.r2r.readytorevise.presentation.analytics.AnalyticsScreen
import com.r2r.readytorevise.presentation.auth.login.LoginRoute
import com.r2r.readytorevise.presentation.auth.register.RegisterRoute
import com.r2r.readytorevise.presentation.dashboard.DashboardScreen
import com.r2r.readytorevise.presentation.profile.ProfileScreen

import com.r2r.readytorevise.presentation.upload.UploadScreen
import com.r2r.readytorevise.presentation.upload.DocumentUploadScreen
import com.r2r.readytorevise.presentation.processing.ProcessingScreen
import com.r2r.readytorevise.presentation.quiz.RevisionScreen
import com.r2r.readytorevise.presentation.upload.UploadViewModel


@Composable
fun AppNavGraph(
    appContainer: com.r2r.readytorevise.di.AppContainer,
    modifier: Modifier = Modifier
) {

    val isLoggedIn by appContainer.authRepository
        .isLoggedIn
        .collectAsState(initial = null)


    if (isLoggedIn == null) {

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

            CircularProgressIndicator()

        }

        return
    }


    val navController = rememberNavController()


    val uploadViewModel: UploadViewModel = viewModel()



    NavHost(

        navController = navController,

        startDestination =
            if (isLoggedIn == true)
                Screen.Dashboard.route
            else
                Screen.Login.route,

        modifier = modifier

    ) {



        composable(Screen.Login.route) {

            LoginRoute(

                appContainer = appContainer,

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

            RegisterRoute(

                appContainer = appContainer,

                navController = navController

            )

        }




        composable(Screen.Dashboard.route) {

            DashboardScreen(

                navController = navController,

                uploadViewModel = uploadViewModel

            )

        }





        composable(Screen.Preview.route) {


            UploadScreen(

                navController = navController,

                uploadViewModel = uploadViewModel

            )

        }





        composable(Screen.Processing.route) {

            ProcessingScreen(navController)

        }


        composable(Screen.DocumentPreview.route) {

            DocumentUploadScreen(
                navController = navController,
                uploadViewModel = uploadViewModel
            )

        }




        composable(Screen.Revision.route) {

            RevisionScreen(

                uploadViewModel = uploadViewModel,

                onBackClick = {
                    navController.popBackStack()
                }

            )

        }









        composable(Screen.Analytics.route) {

            AnalyticsScreen(navController)

        }




        composable(Screen.Profile.route) {

            ProfileScreen(navController)

        }


    }

}