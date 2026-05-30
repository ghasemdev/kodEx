package dev.kodex.webapp.pages.landing

import dev.kodex.shared.landing.LandingStats
import dev.kodex.shared.ui.UiState

data class LandingUiState(
    val statsState: UiState<LandingStats> = UiState.Loading,
)
