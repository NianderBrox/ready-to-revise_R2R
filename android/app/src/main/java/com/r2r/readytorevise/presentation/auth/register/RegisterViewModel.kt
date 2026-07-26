package com.r2r.readytorevise.presentation.auth.register

import androidx.lifecycle.viewModelScope
import com.r2r.readytorevise.presentation.base.BaseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RegisterViewModel : BaseViewModel<
        RegisterUiState,
        RegisterEvent,
        Nothing
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
                // Navigation will be added later
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

            delay(1500)

            updateState {
                copy(isLoading = false)
            }

        }
    }
}