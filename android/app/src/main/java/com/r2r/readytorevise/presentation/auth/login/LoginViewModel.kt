package com.r2r.readytorevise.presentation.auth.login

import androidx.lifecycle.viewModelScope
import com.r2r.readytorevise.presentation.base.BaseViewModel
import kotlinx.coroutines.launch

class LoginViewModel : BaseViewModel<
        LoginUiState,
        LoginEvent,
        LoginEffect
        >(LoginUiState()) {

    override fun onEvent(event: LoginEvent) {

        when (event) {

            is LoginEvent.EmailChanged -> {

                updateState {
                    copy(email = event.email)
                }

            }

            is LoginEvent.PasswordChanged -> {

                updateState {
                    copy(password = event.password)
                }

            }

            LoginEvent.LoginClicked -> {

                viewModelScope.launch {
                    sendEffect(LoginEffect.NavigateToDashboard)
                }

            }

            LoginEvent.RegisterClicked -> {

                viewModelScope.launch {
                    sendEffect(LoginEffect.NavigateToRegister)
                }

            }
        }
    }
}