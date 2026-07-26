package com.r2r.readytorevise.presentation.auth.register

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.r2r.readytorevise.ui.components.auth.AuthHeader
import com.r2r.readytorevise.ui.components.common.R2RButton
import com.r2r.readytorevise.ui.components.common.R2RPasswordField
import com.r2r.readytorevise.ui.components.common.R2RTextField
import com.r2r.readytorevise.ui.theme.ReadyToReviseTheme

@Composable
fun RegisterScreen(
    navController: NavHostController,
    state: RegisterUiState,
    onEvent: (RegisterEvent) -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF23336F))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),

        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(70.dp))

        AuthHeader(
            title = "Create Account",
            subtitle = "Start revising smarter today"
        )

        Spacer(modifier = Modifier.height(40.dp))

        R2RTextField(
            value = state.name,
            onValueChange = {
                onEvent(RegisterEvent.NameChanged(it))
            },
            label = "Full Name"
        )

        Spacer(modifier = Modifier.height(18.dp))

        R2RTextField(
            value = state.email,
            onValueChange = {
                onEvent(RegisterEvent.EmailChanged(it))
            },
            label = "Email"
        )

        Spacer(modifier = Modifier.height(18.dp))

        R2RPasswordField(
            value = state.password,
            onValueChange = {
                onEvent(RegisterEvent.PasswordChanged(it))
            },
            label = "Password"
        )

        Spacer(modifier = Modifier.height(18.dp))

        R2RPasswordField(
            value = state.confirmPassword,
            onValueChange = {
                onEvent(RegisterEvent.ConfirmPasswordChanged(it))
            },
            label = "Confirm Password"
        )

        Spacer(modifier = Modifier.height(30.dp))

        R2RButton(
            text = "Create Account",
            enabled = state.isRegisterEnabled,
            onClick = {
                navController.popBackStack()
            }
        )

        Spacer(modifier = Modifier.height(25.dp))

        Text(
            text = "Already have an account?",
            color = Color.White.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Login",
            color = Color(0xFFFFC857),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable {
                navController.popBackStack()
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RegisterPreview() {
    ReadyToReviseTheme {
        RegisterScreen(
            navController = rememberNavController(),
            state = RegisterUiState(),
            onEvent = {}
        )
    }
}