package dev.kodex.server.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module

@Module
@ComponentScan("dev.kodex.server")
class ServerKoinModule
// Bindings are added per feature via @Single / @Factory on service classes.
// No explicit provider functions needed here until a service requires custom construction.

@KoinApplication(modules = [ServerKoinModule::class])
object KoinServerApplication
