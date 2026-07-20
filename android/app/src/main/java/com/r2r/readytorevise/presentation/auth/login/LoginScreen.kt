package com.r2r.readytorevise.presentation.auth.login


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.r2r.readytorevise.presentation.auth.login.LoginEvent.*
import com.r2r.readytorevise.ui.components.R2RButton
import com.r2r.readytorevise.ui.components.R2RPasswordField
import com.r2r.readytorevise.ui.components.R2RTextField
import com.r2r.readytorevise.ui.spacing.Spacing


@Composable
fun LoginScreen(

    state: LoginUiState,

    onEvent: (LoginEvent) -> Unit

) {

    Column {

        R2RTextField(

            value = state.email,

            onValueChange = {
                onEvent(
                    LoginEvent.EmailChanged(it)
                )
            },

            label = "Email"

        )

        Spacer(
            Modifier.height(Spacing.MD)
        )

        R2RPasswordField(

            value = state.password,

            onValueChange = {

                onEvent(
                    LoginEvent.PasswordChanged(it)
                )

            },

            label = "Password"

        )

        Spacer(
            Modifier.height(Spacing.MD)
        )

        R2RButton(

            text = "Login"

        ) {

            onEvent(
                LoginEvent.LoginClicked
            )

        }

    }

}