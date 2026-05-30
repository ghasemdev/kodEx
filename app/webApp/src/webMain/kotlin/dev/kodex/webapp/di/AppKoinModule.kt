package dev.kodex.webapp.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module

@Module(includes = [NetworkKoinModule::class])
@ComponentScan("dev.kodex.webapp")
class AppKoinModule

@KoinApplication(modules = [AppKoinModule::class])
object KoinApp
