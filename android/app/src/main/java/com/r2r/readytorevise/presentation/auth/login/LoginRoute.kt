package com.r2r.readytorevise.presentation.auth.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.LaunchedEffect
@Composable
fun LoginRoute(
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit
) {

    val viewModel: LoginViewModel = viewModel()

    val state = viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {

        viewModel.effect.collect { effect ->

            when (effect) {

                LoginEffect.NavigateToDashboard -> {
                    onLoginSuccess()
                }

                LoginEffect.NavigateToRegister -> {
                    onRegisterClick()
                }

                is LoginEffect.ShowError -> {
                    // SnackbarHostState later
                }
            }
        }
    }

    LoginScreen(
        state = state.value,
        onEvent = viewModel::onEvent
    )
}
