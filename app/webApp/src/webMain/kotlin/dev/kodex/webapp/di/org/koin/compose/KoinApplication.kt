package dev.kodex.webapp.di.org.koin.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.currentComposer
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.scope.Scope
import org.koin.mp.KoinPlatform

/**
 * Internal API.
 * Current Koin Scope, as default with Default Koin context root scope
 *
 * @see ComposeContextWrapper
 */
@KoinInternalApi
internal val LocalKoinScopeContext: ProvidableCompositionLocal<ComposeContextWrapper<Scope>> =
    compositionLocalOf { ComposeContextWrapper(getDefaultRootScope()) { getDefaultRootScope() } }

@OptIn(KoinInternalApi::class)
private fun getDefaultRootScope() = KoinPlatform.getKoin().scopeRegistry.rootScope

/**
 * Retrieve the current Koin scope from the composition.
 *
 * @author jjkester
 *
 */
@OptIn(InternalComposeApi::class, KoinInternalApi::class)
@Composable
fun currentKoinScope(): Scope = currentComposer.run {
    try {
        val currentScope = consume(LocalKoinScopeContext).getValue()
        if (currentScope.closed) {
            consume(LocalKoinScopeContext).resetValue()
                ?: error("Can't get Koin scope. Scope '$currentScope' is closed")
        } else {
            currentScope
        }
    } catch (e: Exception) {
        consume(LocalKoinScopeContext).resetValue()
            ?: error("Can't get Koin scope due to error: $e")
    }
}
