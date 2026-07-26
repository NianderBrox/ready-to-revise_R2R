package com.r2r.readytorevise.presentation.auth.register

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController

@Composable
fun RegisterRoute(
    navController: NavHostController
) {

    val viewModel: RegisterViewModel = viewModel()

    val state by viewModel.state.collectAsState()

    RegisterScreen(
        navController = navController,
        state = state,
        onEvent = viewModel::onEvent
    )
}