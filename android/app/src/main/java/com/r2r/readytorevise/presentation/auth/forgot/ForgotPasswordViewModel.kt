package com.r2r.readytorevise.presentation.auth.forgot

import androidx.lifecycle.viewModelScope
import com.r2r.readytorevise.domain.repository.AuthRepository
import com.r2r.readytorevise.domain.validation.EmailValidator
import com.r2r.readytorevise.domain.validation.RegistrationPasswordValidator
import com.r2r.readytorevise.domain.validation.ValidationResult
import com.r2r.readytorevise.presentation.base.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ForgotPasswordViewModel(
    private val authRepository: AuthRepository
) : BaseViewModel<
        ForgotPasswordUiState,
        ForgotPasswordEvent,
        ForgotPasswordEffect
        >(ForgotPasswordUiState()) {

    private val emailValidator = EmailValidator()
    private val passwordValidator = RegistrationPasswordValidator()
    private var cooldownJob: Job? = null

    override fun onEvent(event: ForgotPasswordEvent) {
        when (event) {
            is ForgotPasswordEvent.EmailChanged -> updateState {
                copy(
                    email = event.email,
                    emailError = errorOrNull(emailValidator.validate(event.email)),
                    canSendCode = emailValidator.validate(event.email) is ValidationResult.Success,
                )
            }
            is ForgotPasswordEvent.OtpChanged -> {
                val digits = event.otp.filter { it.isDigit() }.take(6)
                updateState {
                    copy(
                        otp = digits,
                        otpError = when {
                            digits.isEmpty() -> "Reset code is required"
                            digits.length < 6 -> "Enter the 6-digit code"
                            else -> null
                        },
                        canReset = canReset(digits, newPassword, confirmPassword),
                    )
                }
            }
            is ForgotPasswordEvent.NewPasswordChanged -> updateState {
                copy(
                    newPassword = event.password,
                    passwordError = when (val v = passwordValidator.validate(event.password)) {
                        is ValidationResult.Error -> v.message
                        ValidationResult.Success -> null
                    },
                    canReset = canReset(otp, event.password, confirmPassword),
                )
            }
            is ForgotPasswordEvent.ConfirmPasswordChanged -> updateState {
                copy(
                    confirmPassword = event.password,
                    passwordError = if (event.password.isNotEmpty() && event.password != newPassword) {
                        "Passwords do not match"
                    } else passwordError,
                    canReset = canReset(otp, newPassword, event.password),
                )
            }
            ForgotPasswordEvent.SendCodeClicked -> sendCode()
            ForgotPasswordEvent.ResendCodeClicked -> sendCode()
            ForgotPasswordEvent.ResetClicked -> reset()
            ForgotPasswordEvent.ChangeEmailClicked -> updateState {
                copy(step = ForgotStep.EMAIL, otp = "", canReset = false)
            }
        }
    }

    private fun canReset(otp: String, password: String, confirm: String): Boolean =
        otp.length == 6 &&
            passwordValidator.validate(password) is ValidationResult.Success &&
            password == confirm

    private fun errorOrNull(result: ValidationResult): String? =
        (result as? ValidationResult.Error)?.message

    private fun sendCode() {
        val email = currentState.email
        if (currentState.isLoading || emailValidator.validate(email) !is ValidationResult.Success) return
        if (currentState.resendCooldownSeconds > 0 && currentState.step == ForgotStep.CODE) return

        viewModelScope.launch {
            updateState { copy(isLoading = true) }
            authRepository.forgotPassword(email)
                .onSuccess {
                    updateState { copy(isLoading = false, step = ForgotStep.CODE) }
                    sendEffect(ForgotPasswordEffect.ShowSnackbar("Reset code sent to $email"))
                    startCooldown(30)
                }
                .onFailure { error ->
                    updateState { copy(isLoading = false) }
                    sendEffect(ForgotPasswordEffect.ShowSnackbar(error.message ?: "Couldn't send code"))
                }
        }
    }

    private fun reset() {
        if (currentState.isLoading || !currentState.canReset) return

        viewModelScope.launch {
            updateState { copy(isLoading = true) }
            authRepository.resetPassword(
                currentState.email,
                currentState.otp,
                currentState.newPassword,
            )
                .onSuccess {
                    updateState { copy(isLoading = false) }
                    delay(400)
                    sendEffect(ForgotPasswordEffect.NavigateToLogin("Password updated. Log in with your new password."))
                }
                .onFailure { error ->
                    updateState { copy(isLoading = false) }
                    sendEffect(ForgotPasswordEffect.ShowSnackbar(error.message ?: "Couldn't reset password"))
                }
        }
    }

    private fun startCooldown(seconds: Int) {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            for (remaining in seconds downTo 1) {
                updateState { copy(resendCooldownSeconds = remaining) }
                delay(1000)
            }
            updateState { copy(resendCooldownSeconds = 0) }
        }
    }
}
