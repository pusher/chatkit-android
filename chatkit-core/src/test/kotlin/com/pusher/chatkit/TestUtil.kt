package com.pusher.chatkit

import com.google.common.truth.Truth
import com.pusher.util.Result
import elements.Error

inline fun <T, reified S : T> List<T>.assertTypedSingletonList(): S {
    Truth.assertThat(this).hasSize(1)
    Truth.assertThat(this.first()).isInstanceOf(S::class.java)
    return this.first() as S
}

fun <A> Result<A, Error>.assumeSuccess(): A = when (this) {
    is Result.Success -> value
    is Result.Failure -> error("Failure: $error")
}
