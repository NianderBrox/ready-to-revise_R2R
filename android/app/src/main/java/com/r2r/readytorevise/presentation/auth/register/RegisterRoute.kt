package com.r2r.readytorevise.presentation.auth.register

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.initializer
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.viewModelFactory
import com.r2r.readytorevise.di.AppContainer

@Composable
fun RegisterRoute(
    appContainer: AppContainer,
    navController: NavHostController
) {

    val factory = viewModelFactory {
        initializer {
            RegisterViewModel(appContainer.authRepository)
        }
    }

    val viewModel: RegisterViewModel = viewModel(factory = factory)

    val state by viewModel.state.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is RegisterEffect.NavigateToLogin -> {
                    navController.popBackStack()
                }
                is RegisterEffect.ShowSnackbar -> {
                    launch {
                        snackbarHostState.showSnackbar(effect.message)
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        RegisterScreen(
            modifier = Modifier.padding(padding),
            navController = navController,
            state = state,
            onEvent = viewModel::onEvent
        )
    }
}