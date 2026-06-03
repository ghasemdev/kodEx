package dev.kodex.webapp.pages.landing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.kilua.core.IComponent
import dev.kilua.html.div
import dev.kodex.shared.session.SessionState
import dev.kodex.webapp.di.org.koin.compose.koinInject
import dev.kodex.webapp.layout.Footer
import dev.kodex.webapp.layout.GlobalNavBar
import dev.kodex.webapp.layout.PageTransition
import dev.kodex.webapp.pages.landing.sections.CreateExamSection
import dev.kodex.webapp.pages.landing.sections.ExamTypesSection
import dev.kodex.webapp.pages.landing.sections.GamificationSection
import dev.kodex.webapp.pages.landing.sections.HeroSection
import dev.kodex.webapp.pages.landing.sections.LeaderboardSection
import dev.kodex.webapp.pages.landing.sections.ProblemsSection

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
        GlobalNavBar(session = SessionState.Guest)
        PageTransition(id = "landing-page-transition") {
            HeroSection(statsState = uiState.statsState)
            ExamTypesSection()
            GamificationSection()
            LeaderboardSection()
            ProblemsSection()
            CreateExamSection()
        }
        Footer()
    }
}
