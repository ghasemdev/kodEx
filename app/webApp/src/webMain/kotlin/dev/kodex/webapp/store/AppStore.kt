package dev.kodex.webapp.store

sealed class Intent

data class State(val loading: Boolean = false)
