package dev.kodex.webapp.di

import org.koin.dsl.module

// import dev.kodex.shared.landing.LandingStatsRepository
// import dev.kodex.webapp.pages.landing.LandingViewModel
// import dev.kodex.webapp.pages.landing.data.LandingStatsRepositoryImpl
// import org.koin.dsl.bind

val appModule = module {
    includes(networkModule)

//    single<LandingStatsRepositoryImpl>() bind LandingStatsRepository::class
//    factory<LandingViewModel>()
}
