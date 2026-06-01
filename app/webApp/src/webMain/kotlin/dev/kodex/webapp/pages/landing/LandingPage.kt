package dev.kodex.webapp.pages.landing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.kilua.core.IComponent
import dev.kilua.html.div
import dev.kodex.webapp.di.org.koin.compose.koinInject
import dev.kodex.webapp.pages.landing.sections.ExamTypesSection
import dev.kodex.webapp.pages.landing.sections.HeroSection

@Composable
fun IComponent.LandingPage() {
    val viewModel = koinInject<LandingViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    DisposableEffect(Unit) {
        onDispose { viewModel.onCleared() }
    }

    LandingPage(uiState = uiState)
}

@Composable
private fun IComponent.LandingPage(uiState: LandingUiState) {
    div(className = "min-h-screen bg-surface text-on-surface") {
        HeroSection(statsState = uiState.statsState)
        ExamTypesSection()
    }
}
