package com.r2r.readytorevise.presentation.auth.login

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LoginRoute(
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit
) {

    val viewModel: LoginViewModel = viewModel()

    val state by viewModel.state.collectAsState()

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    LaunchedEffect(viewModel) {

        viewModel.effect.collect { effect ->

            when (effect) {

                LoginEffect.NavigateToDashboard -> {
                    onLoginSuccess()
                }

                LoginEffect.NavigateToRegister -> {
                    onRegisterClick()
                }

                is LoginEffect.ShowSnackbar -> {

                    snackbarHostState.showSnackbar(
                        message = effect.message
                    )

                }
            }
        }
    }

    Scaffold(

        snackbarHost = {
            SnackbarHost(snackbarHostState)
        }

    ) { padding ->

        LoginScreen(

            modifier = Modifier.padding(padding),

            state = state,

            onEvent = viewModel::onEvent

        )

    }

}