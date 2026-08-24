package com.r2r.readytorevise.presentation.profile

import androidx.lifecycle.viewModelScope
import com.r2r.readytorevise.domain.repository.AuthRepository
import com.r2r.readytorevise.domain.repository.DashboardRepository
import com.r2r.readytorevise.presentation.base.BaseViewModel
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val dashboardRepository: DashboardRepository,
) : BaseViewModel<ProfileUiState, ProfileEvent, ProfileEffect>(ProfileUiState()) {

    init {
        load()
    }

    override fun onEvent(event: ProfileEvent) {
        when (event) {
            ProfileEvent.RetryLoad -> load()
            ProfileEvent.LogoutClicked -> logout()
        }
    }

    private fun load() {
        updateState { copy(loading = true, error = null) }

        viewModelScope.launch {
            authRepository.profile()
                .onSuccess { profile ->
                    updateState {
                        copy(loading = false, error = null, email = profile.email)
                    }

                    dashboardRepository.getDashboard()
                        .onSuccess { dashboard ->
                            updateState { copy(name = dashboard.user.name) }
                        }
                }
                .onFailure { throwable ->
                    updateState {
                        copy(
                            loading = false,
                            error = throwable.message ?: "Couldn't load profile.",
                        )
                    }
                }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            authRepository.logout()

            sendEffect(ProfileEffect.LoggedOut)
        }
    }
}
