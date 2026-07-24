package com.r2r.readytorevise.presentation.auth.login

import androidx.lifecycle.viewModelScope
import com.r2r.readytorevise.domain.validation.EmailValidator
import com.r2r.readytorevise.domain.validation.LoginPasswordValidator
import com.r2r.readytorevise.domain.validation.ValidationResult
import com.r2r.readytorevise.presentation.base.BaseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoginViewModel : BaseViewModel<
        LoginUiState,
        LoginEvent,
        LoginEffect
        >(LoginUiState()) {

    private val emailValidator = EmailValidator()

    private val passwordValidator = LoginPasswordValidator()

    override fun onEvent(event: LoginEvent) {
        when (event) {

            is LoginEvent.EmailChanged -> {
                onEmailChanged(event.email)
            }

            is LoginEvent.PasswordChanged -> {
                onPasswordChanged(event.password)
            }

            LoginEvent.LoginClicked -> {
                login()
            }

            LoginEvent.RegisterClicked -> {
                navigateToRegister()
            }
        }
    }

    private fun onEmailChanged(email: String) {

        val validation = emailValidator.validate(email)

        updateState {
            copy(
                email = email,

                emailError = when (validation) {
                    is ValidationResult.Error -> validation.message
                    ValidationResult.Success -> null
                },

                isLoginEnabled = isLoginEnabled(
                    email = email,
                    password = password
                )
            )
        }
    }

    private fun onPasswordChanged(password: String) {

        val validation = passwordValidator.validate(password)

        updateState {
            copy(
                password = password,

                passwordError = when (validation) {
                    is ValidationResult.Error -> validation.message
                    ValidationResult.Success -> null
                },

                isLoginEnabled = isLoginEnabled(
                    email = email,
                    password = password
                )
            )
        }
    }

    private fun isLoginEnabled(
        email: String,
        password: String
    ): Boolean {
        return emailValidator.validate(email) is ValidationResult.Success &&
                passwordValidator.validate(password) is ValidationResult.Success
    }

    //for now faking the network call
    private fun login() {

        if (!currentState.isLoginEnabled) return

        viewModelScope.launch {

            updateState {
                copy(
                    isLoading = true
                )
            }

            // TODO Replace with LoginUseCase
            delay(2000)

            updateState {
                copy(
                    isLoading = false
                )
            }

            sendEffect(
                LoginEffect.ShowSnackbar(
                    "Login API not implemented yet."
                )
            )
        }
    }

    private fun navigateToRegister() {
        viewModelScope.launch {
            sendEffect(LoginEffect.NavigateToRegister)
        }
    }
}