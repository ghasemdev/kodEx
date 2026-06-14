package dev.kodex.webapp.pages.landing

import dev.kodex.shared.landing.LandingStatsRepository
import dev.kodex.shared.ui.UiState
import dev.kodex.webapp.core.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory

@Factory
class LandingViewModel(
    private val repository: LandingStatsRepository,
) : ViewModel() {
    val uiState: StateFlow<LandingUiState>
        field: MutableStateFlow<LandingUiState> = MutableStateFlow(LandingUiState())

    init {
        viewModelScope.launch {
            uiState.update { current ->
                current.copy(
                    statsState = try {
                        UiState.Success(repository.fetchStats())
                    } catch (e: Exception) {
                        UiState.Error(e.message)
                    },
                )
            }
        }
    }
}
