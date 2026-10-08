package com.r2r.readytorevise.presentation.auth.forgot

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.r2r.readytorevise.R
import com.r2r.readytorevise.ui.components.common.R2RButton
import com.r2r.readytorevise.ui.components.common.R2RPasswordField
import com.r2r.readytorevise.ui.components.common.R2RTextField
import com.r2r.readytorevise.ui.theme.Background
import com.r2r.readytorevise.ui.theme.OnSurfaceVariant
import com.r2r.readytorevise.ui.theme.SkyBlueDark

@Composable
fun ForgotPasswordScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    state: ForgotPasswordUiState,
    onEvent: (ForgotPasswordEvent) -> Unit,
) {
    BackHandler { navController.popBackStack() }

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
            Text(
                text = "Reset Password",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (state.step == ForgotStep.EMAIL) {
                    "Enter your account email and we'll send you a 6-digit reset code."
                } else {
                    "Enter the code sent to ${state.email} and choose a new password."
                },
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (state.step == ForgotStep.EMAIL) {
                R2RTextField(
                    value = state.email,
                    onValueChange = { onEvent(ForgotPasswordEvent.EmailChanged(it)) },
                    label = "Email",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                state.emailError?.let {
                    if (state.email.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = it, color = Color.Red, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                R2RButton(
                    text = "Send Code",
                    loadingText = "Sending...",
                    loading = state.isLoading,
                    enabled = state.canSendCode,
                    onClick = { onEvent(ForgotPasswordEvent.SendCodeClicked) }
                )
            } else {
                R2RTextField(
                    value = state.otp,
                    onValueChange = { onEvent(ForgotPasswordEvent.OtpChanged(it)) },
                    label = "6-digit reset code",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )

                Spacer(modifier = Modifier.height(18.dp))

                R2RPasswordField(
                    value = state.newPassword,
                    onValueChange = { onEvent(ForgotPasswordEvent.NewPasswordChanged(it)) },
                    label = "New Password",
                    isError = state.passwordError != null,
                    errorText = state.passwordError ?: ""
                )

                Spacer(modifier = Modifier.height(18.dp))

                R2RPasswordField(
                    value = state.confirmPassword,
                    onValueChange = { onEvent(ForgotPasswordEvent.ConfirmPasswordChanged(it)) },
                    label = "Confirm New Password"
                )

                Spacer(modifier = Modifier.height(28.dp))

                R2RButton(
                    text = "Reset Password",
                    loadingText = "Resetting...",
                    loading = state.isLoading,
                    enabled = state.canReset,
                    onClick = { onEvent(ForgotPasswordEvent.ResetClicked) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                val resendLabel = if (state.resendCooldownSeconds > 0) {
                    "Resend code in ${state.resendCooldownSeconds}s"
                } else {
                    "Resend code"
                }

                Text(
                    modifier = Modifier.clickable(enabled = state.resendCooldownSeconds == 0) {
                        onEvent(ForgotPasswordEvent.ResendCodeClicked)
                    },
                    text = resendLabel,
                    color = if (state.resendCooldownSeconds == 0) SkyBlueDark else OnSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    modifier = Modifier.clickable {
                        onEvent(ForgotPasswordEvent.ChangeEmailClicked)
                    },
                    text = "Use a different email",
                    color = OnSurfaceVariant
                )
            }
        }
    }
}
