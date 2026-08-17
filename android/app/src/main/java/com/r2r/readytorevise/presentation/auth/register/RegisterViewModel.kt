package com.r2r.readytorevise.presentation.auth.register

import androidx.lifecycle.viewModelScope
import com.r2r.readytorevise.domain.repository.AuthRepository
import com.r2r.readytorevise.presentation.base.BaseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val authRepository: AuthRepository
) : BaseViewModel<
        RegisterUiState,
        RegisterEvent,
        RegisterEffect
        >(RegisterUiState()) {

    override fun onEvent(event: RegisterEvent) {

        when (event) {

            is RegisterEvent.NameChanged -> {
                updateState {
                    copy(
                        name = event.name,
                        isRegisterEnabled = isFormValid(
                            event.name,
                            email,
                            password,
                            confirmPassword
                        )
                    )
                }
            }

            is RegisterEvent.EmailChanged -> {
                updateState {
                    copy(
                        email = event.email,
                        isRegisterEnabled = isFormValid(
                            name,
                            event.email,
                            password,
                            confirmPassword
                        )
                    )
                }
            }

            is RegisterEvent.PasswordChanged -> {
                updateState {
                    copy(
                        password = event.password,
                        isRegisterEnabled = isFormValid(
                            name,
                            email,
                            event.password,
                            confirmPassword
                        )
                    )
                }
            }

            is RegisterEvent.ConfirmPasswordChanged -> {
                updateState {
                    copy(
                        confirmPassword = event.password,
                        isRegisterEnabled = isFormValid(
                            name,
                            email,
                            password,
                            event.password
                        )
                    )
                }
            }

            RegisterEvent.RegisterClicked -> {
                register()
            }

            RegisterEvent.LoginClicked -> {
            }
        }
    }

    private fun isFormValid(
        name: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {

        return name.isNotBlank() &&
                email.isNotBlank() &&
                password.length >= 6 &&
                password == confirmPassword
    }

    private fun register() {

        if (!currentState.isRegisterEnabled) return

        viewModelScope.launch {

            updateState {
                copy(isLoading = true)
            }

            val result = authRepository.register(
                name = currentState.name,
                email = currentState.email,
                password = currentState.password
            )

            result.onSuccess {
                updateState {
                    copy(isLoading = false)
                }
                sendEffect(RegisterEffect.ShowSnackbar("Registration successful! Please login."))
                delay(1500)
                sendEffect(RegisterEffect.NavigateToLogin)
            }.onFailure { error ->
                updateState {
                    copy(isLoading = false)
                }
                sendEffect(RegisterEffect.ShowSnackbar(error.message ?: "Registration failed"))
            }

        }
    }
}