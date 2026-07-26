package com.r2r.readytorevise.presentation.auth.login

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.r2r.readytorevise.ui.components.auth.AuthHeader
import com.r2r.readytorevise.ui.components.common.R2RButton
import com.r2r.readytorevise.ui.components.common.R2RPasswordField
import com.r2r.readytorevise.ui.components.common.R2RTextField
import com.r2r.readytorevise.ui.theme.ReadyToReviseTheme

@Composable
fun LoginScreen(
    state: LoginUiState,
    onEvent: (LoginEvent) -> Unit,
    modifier: Modifier = Modifier
) {

    val passwordFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF23336F))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 40.dp),

        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(60.dp))

        AuthHeader(
            title = "Ready to Revise",
            subtitle = "Revise smarter. Remember longer."
        )

        Spacer(modifier = Modifier.height(50.dp))

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

        Spacer(modifier = Modifier.height(18.dp))
        R2RPasswordField(

            modifier = Modifier.focusRequester(passwordFocusRequester),

            value = state.password,

            onValueChange = {
                onEvent(LoginEvent.PasswordChanged(it))
            },

            label = "Password",

            isError = state.showPasswordError,

            errorText = state.passwordError,

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
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            modifier = Modifier
                .align(Alignment.End)
                .clickable { },
            text = "Forgot Password?",
            color = Color.White.copy(alpha = 0.85f)
        )

        Spacer(modifier = Modifier.height(28.dp))

        R2RButton(
            text = "Login",
            loadingText = "Logging in...",
            loading = state.isLoading,
            enabled = state.isLoginEnabled,
            onClick = {
                onEvent(LoginEvent.LoginClicked)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        R2RButton(
            text = "Offline mode",
            enabled = true,
            onClick = {
                // TODO Guest Login
            }
        )

        Spacer(modifier = Modifier.height(36.dp))

        Text(
            text = "Don't have an account?",
            color = Color.White.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            modifier = Modifier.clickable {
                onEvent(LoginEvent.RegisterClicked)
            },
            text = "Create Account",
            color = Color(0xFFFFC857),
            fontWeight = FontWeight.SemiBold
        )

    }
}

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