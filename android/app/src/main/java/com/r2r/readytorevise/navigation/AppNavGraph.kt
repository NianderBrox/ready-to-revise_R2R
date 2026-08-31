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
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import com.r2r.readytorevise.presentation.analytics.AnalyticsRoute
import com.r2r.readytorevise.presentation.auth.login.LoginRoute
import com.r2r.readytorevise.presentation.auth.register.RegisterRoute
import com.r2r.readytorevise.presentation.dashboard.DashboardRoute
import com.r2r.readytorevise.presentation.profile.ProfileRoute

import com.r2r.readytorevise.presentation.upload.UploadScreen
import com.r2r.readytorevise.presentation.upload.DocumentUploadScreen
import com.r2r.readytorevise.presentation.processing.ProcessingRoute
import com.r2r.readytorevise.quiz.RevisionRoute
import com.r2r.readytorevise.presentation.upload.UploadViewModel

import com.r2r.readytorevise.ui.SplashScreen
import com.r2r.readytorevise.notification.NotificationPreferences
import com.r2r.readytorevise.notification.NotificationSettingsRoute
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Composable
fun AppNavGraph(
    appContainer: com.r2r.readytorevise.di.AppContainer,
    modifier: Modifier = Modifier
) {
    var showSplash by remember {
        mutableStateOf(true)
    }

    var registrationSuccessMessage by remember {
        mutableStateOf<String?>(null)
    }

    if (showSplash) {

        SplashScreen(
            onFinished = {
                showSplash = false
            }
        )

        return
    }


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


    val appContext = LocalContext.current.applicationContext


    val uploadViewModel: UploadViewModel = viewModel(

        factory = viewModelFactory {

            initializer {

                UploadViewModel(
                    documentsRepository = appContainer.documentsRepository,
                    studyItemsRepository = appContainer.studyItemsRepository,
                    appContext = appContext,
                )

            }

        }

    )



    val notificationPreferences = remember { NotificationPreferences(appContext) }



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

                },

                registrationSuccessMessage = registrationSuccessMessage,

                onRegistrationSuccessMessageShown = {

                    registrationSuccessMessage = null

                }

            )

        }



        composable(Screen.Register.route) {

            RegisterRoute(

                appContainer = appContainer,

                navController = navController,

                onRegistrationSuccess = { message ->

                    registrationSuccessMessage = message

                }

            )

        }




        composable(Screen.Dashboard.route) {

            DashboardRoute(
                appContainer = appContainer,
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

            ProcessingRoute(
                uploadViewModel = uploadViewModel,
                navController = navController
            )

        }


        composable(Screen.DocumentPreview.route) {

            DocumentUploadScreen(
                navController = navController,
                uploadViewModel = uploadViewModel
            )

        }




        composable(Screen.Revision.route) {

            RevisionRoute(

                appContainer = appContainer,

                onBackClick = {
                    navController.popBackStack()
                }

            )

        }









        composable(Screen.Analytics.route) {

            AnalyticsRoute(
                appContainer = appContainer,
                navController = navController
            )

        }




        composable(Screen.Profile.route) {

            ProfileRoute(
                appContainer = appContainer,
                navController = navController
            )

        }

        composable(Screen.NotificationSettings.route) {
            NotificationSettingsRoute(
                preferences = notificationPreferences,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }


    }

}