package dev.kodex.webapp.pages.landing

import androidx.compose.runtime.Immutable
import dev.kodex.shared.landing.LandingStats
import dev.kodex.shared.ui.UiState

@Immutable
data class LandingUiState(
    val statsState: UiState<LandingStats> = UiState.Loading,
)
