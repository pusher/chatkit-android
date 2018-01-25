package com.pusher.chatkit

/**
 * Code from https://github.com/michaelbull/kotlin-result
 *
 * Copyright (c) 2017 Michael Bull (https://www.michael-bull.com)
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
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
