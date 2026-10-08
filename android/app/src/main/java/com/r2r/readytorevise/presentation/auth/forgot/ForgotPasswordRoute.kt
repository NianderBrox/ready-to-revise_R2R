package com.r2r.readytorevise.presentation.auth.forgot

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.r2r.readytorevise.di.AppContainer
import kotlinx.coroutines.launch

@Composable
fun ForgotPasswordRoute(
    appContainer: AppContainer,
    navController: NavHostController,
    onSuccess: (String) -> Unit = {},
) {
    val factory = viewModelFactory {
        initializer {
            ForgotPasswordViewModel(appContainer.authRepository)
        }
    }

    val viewModel: ForgotPasswordViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ForgotPasswordEffect.ShowSnackbar -> {
                    launch { snackbarHostState.showSnackbar(effect.message) }
                }
                is ForgotPasswordEffect.NavigateToLogin -> {
                    onSuccess(effect.message)
                    navController.popBackStack()
                }
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color(0xFF2E7D32),
                    contentColor = Color.White
                )
            }
        }
    ) { padding ->
        ForgotPasswordScreen(
            modifier = Modifier.padding(padding),
            navController = navController,
            state = state,
            onEvent = viewModel::onEvent,
        )
    }
}
