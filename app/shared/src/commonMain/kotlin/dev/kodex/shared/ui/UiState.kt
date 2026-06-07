package dev.kodex.shared.ui

import androidx.compose.runtime.Immutable

@Immutable
sealed class UiState<out T> {
    @Immutable
    data object Loading : UiState<Nothing>()

    @Immutable
    data class Success<out T>(val data: T) : UiState<T>()

    @Immutable
    data class Error(val message: String? = null) : UiState<Nothing>()
}
