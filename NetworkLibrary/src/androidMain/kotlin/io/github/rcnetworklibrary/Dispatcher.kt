package io.github.rcnetworklibrary

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Android Application Dispatcher
 */
internal actual val ApplicationDispatcher: CoroutineDispatcher = Dispatchers.Default