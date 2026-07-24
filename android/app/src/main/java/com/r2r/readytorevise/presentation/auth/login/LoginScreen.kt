package com.r2r.readytorevise.presentation.auth.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.r2r.readytorevise.ui.components.common.R2RButton
import com.r2r.readytorevise.ui.components.common.R2RPasswordField
import com.r2r.readytorevise.ui.components.common.R2RTextField
import androidx.compose.ui.tooling.preview.Preview
import com.r2r.readytorevise.ui.components.auth.AuthHeader
import com.r2r.readytorevise.ui.theme.ReadyToReviseTheme
import com.r2r.readytorevise.ui.theme.spacing


@Composable
fun LoginScreen(
    state: LoginUiState,
    onEvent: (LoginEvent) -> Unit,
    modifier: Modifier = Modifier

){
    val passwordFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val spacing = MaterialTheme.spacing
    val focusManager = LocalFocusManager.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        AuthHeader(
            title = "Ready to Revise",
            subtitle = "Revise smarter. Remember longer."
        )

        Spacer(modifier = Modifier.height(spacing.xxl))


        R2RTextField(
            value = state.email,
            onValueChange = {
                onEvent(LoginEvent.EmailChanged(it))
            },
            label = "Email",

            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),

            keyboardActions = KeyboardActions(
                onNext = {
                    passwordFocusRequester.requestFocus()
                }
            )
        )
        Spacer(modifier = Modifier.height(spacing.md))


        R2RPasswordField(
            modifier = Modifier.focusRequester(passwordFocusRequester),

            value = state.password,

            onValueChange = {
                onEvent(LoginEvent.PasswordChanged(it))
            },

            label = "Password",

            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),

            keyboardActions = KeyboardActions(
                onDone = {

                    keyboardController?.hide()

                    if (state.isLoginEnabled) {
                        onEvent(LoginEvent.LoginClicked)
                    }
                }
            )
        )
        Spacer(modifier = Modifier.height(spacing.sm))

        Text(
            modifier = Modifier
                .align(Alignment.End)
                .clickable {
                    // TODO
                },
            text = "Forgot Password?",
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(spacing.xl))

        R2RButton(

            text = "Login",

            loadingText = "Logging in...",

            loading = state.isLoading,

            enabled = state.isLoginEnabled,

            onClick = {
                onEvent(LoginEvent.LoginClicked)
            }
        )

        Spacer(modifier = Modifier.height(spacing.lg))

        Text(
            text = "Don't have an account?",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(spacing.sm))

        Text(
            modifier = Modifier.clickable {
                onEvent(LoginEvent.RegisterClicked)
            },
            text = "Create Account",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

//preview
@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    ReadyToReviseTheme {
        LoginScreen(
            state = LoginUiState(),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthHeaderPreview() {

    ReadyToReviseTheme {

        AuthHeader(
            title = "Ready to Revise",
            subtitle = "Revise smarter. Remember longer."
        )

    }
}