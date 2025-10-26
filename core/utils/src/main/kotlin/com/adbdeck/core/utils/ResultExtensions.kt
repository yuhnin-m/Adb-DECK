package com.adbdeck.core.utils

import kotlinx.coroutines.CancellationException

/**
 * Аналог [kotlin.runCatching] для suspend-кода, который не подавляет отмену корутин.
 *
 * Стандартный [kotlin.runCatching] перехватывает [CancellationException], что может
 * ломать cooperative cancellation в цепочках suspend-вызовов.
 */
suspend inline fun <T> runCatchingPreserveCancellation(
    block: suspend () -> T,
): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }
