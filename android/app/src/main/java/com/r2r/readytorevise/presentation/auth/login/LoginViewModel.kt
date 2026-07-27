    package com.r2r.readytorevise.presentation.auth.login

    import androidx.lifecycle.viewModelScope
    import com.r2r.readytorevise.domain.validation.EmailValidator
    import com.r2r.readytorevise.domain.validation.LoginPasswordValidator
    import com.r2r.readytorevise.domain.validation.ValidationResult
    import com.r2r.readytorevise.presentation.base.BaseViewModel
    import com.r2r.readytorevise.domain.repository.AuthRepository
    import kotlinx.coroutines.delay
    import kotlinx.coroutines.launch

    class LoginViewModel(
        private val authRepository: AuthRepository
    ) : BaseViewModel<
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
                        ValidationResult.Success -> ""
                    },

                    showPasswordError = false,

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

        private fun login() {

            if (!currentState.isLoginEnabled) return

            viewModelScope.launch {

                updateState {
                    copy(
                        isLoading = true
                    )
                }

                val result = authRepository.login(currentState.email, currentState.password)

                result.onSuccess {
                    updateState {
                        copy(
                            isLoading = false,
                            showPasswordError = false,
                            passwordError = ""
                        )
                    }
                    sendEffect(LoginEffect.NavigateToDashboard)
                }.onFailure { error ->
                    updateState {
                        copy(
                            isLoading = false,
                            showPasswordError = true,
                            passwordError = error.message ?: "Login failed"
                        )
                    }
                }
            }
        }

        private fun navigateToRegister() {
            viewModelScope.launch {
                sendEffect(LoginEffect.NavigateToRegister)
            }
        }
    }