package dev.kodex.shared.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// JS is single-threaded; Fetch API is non-blocking. Default = main event loop.
actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
