package com.pusher.chatkit

/**
 * Code from https://github.com/michaelbull/kotlin-result
 *
 * See https://github.com/michaelbull/kotlin-result/blob/master/LICENSE for the license
 */

sealed class Result<out V, out E> {
    companion object {

        /**
         * Invokes a [function] and wraps it in a [Result], returning an [Err] if an [Exception]
         * was thrown, otherwise [Ok].
         */
        inline fun <T> of(function: () -> T): Result<T, Exception> {
            return try {
                Ok(function.invoke())
            } catch (ex: Exception) {
                Err(ex)
            }
        }
    }
}

/**
 * Represents a successful [Result], containing a [value].
 */
data class Ok<out V>(val value: V) : Result<V, Nothing>()

/**
 * Represents a failed [Result], containing an [error].
 */
data class Err<out E>(val error: E) : Result<Nothing, E>()
