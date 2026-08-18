package com.r2r.readytorevise.presentation.auth.register

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.r2r.readytorevise.R
import com.r2r.readytorevise.ui.components.common.R2RButton
import com.r2r.readytorevise.ui.components.common.R2RPasswordField
import com.r2r.readytorevise.ui.components.common.R2RTextField
import com.r2r.readytorevise.ui.theme.Background
import com.r2r.readytorevise.ui.theme.OnSurfaceVariant
import com.r2r.readytorevise.ui.theme.ReadyToReviseTheme
import com.r2r.readytorevise.ui.theme.SkyBlueDark

@Composable
fun RegisterScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    state: RegisterUiState,
    onEvent: (RegisterEvent) -> Unit
) {
    BackHandler {
        navController.popBackStack()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.auth_banner),
                contentDescription = "Auth Banner",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Crop
            )
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopStart)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
                loadingText = "Creating account...",
                loading = state.isLoading,
                enabled = state.isRegisterEnabled,
                onClick = {
                    onEvent(RegisterEvent.RegisterClicked)
                }
            )

            Spacer(modifier = Modifier.height(25.dp))

            Text(
                text = "Already have an account?",
                color = OnSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Login",
                color = SkyBlueDark,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable {
                    navController.popBackStack()
                }
            )
        }
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
