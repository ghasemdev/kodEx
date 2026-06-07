package dev.kodex.shared.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Suppress("PropertyName", "RedundantSuppression")
actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
