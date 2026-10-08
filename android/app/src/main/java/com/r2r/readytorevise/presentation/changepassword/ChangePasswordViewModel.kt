package com.r2r.readytorevise.presentation.changepassword

import androidx.lifecycle.viewModelScope
import com.r2r.readytorevise.domain.repository.AuthRepository
import com.r2r.readytorevise.domain.validation.RegistrationPasswordValidator
import com.r2r.readytorevise.domain.validation.ValidationResult
import com.r2r.readytorevise.presentation.base.BaseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChangePasswordViewModel(
    private val authRepository: AuthRepository,
) : BaseViewModel<
        ChangePasswordUiState,
        ChangePasswordEvent,
        ChangePasswordEffect
        >(ChangePasswordUiState()) {

    private val passwordValidator = RegistrationPasswordValidator()

    override fun onEvent(event: ChangePasswordEvent) {
        when (event) {
            is ChangePasswordEvent.CurrentPasswordChanged -> update { it.copy(currentPassword = event.password) }
            is ChangePasswordEvent.NewPasswordChanged -> update {
                it.copy(
                    newPassword = event.password,
                    passwordError = (passwordValidator.validate(event.password) as? ValidationResult.Error)?.message,
                )
            }
            is ChangePasswordEvent.ConfirmPasswordChanged -> update { state ->
                state.copy(
                    confirmPassword = event.password,
                    passwordError = if (event.password.isNotEmpty() && event.password != state.newPassword) {
                        "Passwords do not match"
                    } else state.passwordError,
                )
            }
            ChangePasswordEvent.SubmitClicked -> submit()
        }
    }

    private fun update(reducer: (ChangePasswordUiState) -> ChangePasswordUiState) {
        updateState {
            val next = reducer(this)
            next.copy(
                canSubmit = next.currentPassword.isNotBlank() &&
                    passwordValidator.validate(next.newPassword) is ValidationResult.Success &&
                    next.newPassword == next.confirmPassword,
            )
        }
    }

    private fun submit() {
        if (currentState.isLoading || !currentState.canSubmit) return

        viewModelScope.launch {
            updateState { copy(isLoading = true) }
            authRepository.changePassword(currentState.currentPassword, currentState.newPassword)
                .onSuccess {
                    updateState { copy(isLoading = false) }
                    delay(400)
                    sendEffect(ChangePasswordEffect.Finished("Password updated"))
                }
                .onFailure { error ->
                    updateState { copy(isLoading = false) }
                    sendEffect(ChangePasswordEffect.ShowSnackbar(error.message ?: "Couldn't change password"))
                }
        }
    }
}
