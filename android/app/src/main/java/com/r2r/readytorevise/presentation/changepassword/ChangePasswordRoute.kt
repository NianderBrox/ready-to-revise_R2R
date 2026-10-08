package com.r2r.readytorevise.presentation.changepassword

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.r2r.readytorevise.di.AppContainer
import com.r2r.readytorevise.ui.components.common.R2RButton
import com.r2r.readytorevise.ui.components.common.R2RPasswordField
import com.r2r.readytorevise.ui.theme.Background
import com.r2r.readytorevise.ui.theme.SkyBlue
import kotlinx.coroutines.launch

@Composable
fun ChangePasswordRoute(
    appContainer: AppContainer,
    navController: NavController,
) {
    val factory = viewModelFactory {
        initializer {
            ChangePasswordViewModel(appContainer.authRepository)
        }
    }

    val viewModel: ChangePasswordViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ChangePasswordEffect.ShowSnackbar -> {
                    launch { snackbarHostState.showSnackbar(effect.message) }
                }
                is ChangePasswordEffect.Finished -> {
                    launch { snackbarHostState.showSnackbar(effect.message) }
                    navController.popBackStack()
                }
            }
        }
    }

    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
        ChangePasswordScreen(
            state = state,
            onEvent = viewModel::onEvent,
            navController = navController,
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = Color(0xFF2E7D32),
                contentColor = Color.White
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    state: ChangePasswordUiState,
    onEvent: (ChangePasswordEvent) -> Unit,
    navController: NavController,
) {
    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("Change Password") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SkyBlue)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            R2RPasswordField(
                value = state.currentPassword,
                onValueChange = { onEvent(ChangePasswordEvent.CurrentPasswordChanged(it)) },
                label = "Current Password",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                ),
            )

            Spacer(modifier = Modifier.height(18.dp))

            R2RPasswordField(
                value = state.newPassword,
                onValueChange = { onEvent(ChangePasswordEvent.NewPasswordChanged(it)) },
                label = "New Password",
                isError = state.passwordError != null,
                errorText = state.passwordError ?: "",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                ),
            )

            Spacer(modifier = Modifier.height(18.dp))

            R2RPasswordField(
                value = state.confirmPassword,
                onValueChange = { onEvent(ChangePasswordEvent.ConfirmPasswordChanged(it)) },
                label = "Confirm New Password",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { onEvent(ChangePasswordEvent.SubmitClicked) }
                ),
            )

            Spacer(modifier = Modifier.height(28.dp))

            R2RButton(
                text = "Change Password",
                loadingText = "Updating...",
                loading = state.isLoading,
                enabled = state.canSubmit,
                onClick = { onEvent(ChangePasswordEvent.SubmitClicked) }
            )
        }
    }
}
