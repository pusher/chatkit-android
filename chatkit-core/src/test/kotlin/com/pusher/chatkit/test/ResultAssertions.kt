package com.pusher.chatkit.test

import com.google.common.truth.DefaultSubject
import com.google.common.truth.Subject
import com.google.common.truth.Truth.assertThat
import com.pusher.util.Result
import elements.Error

object ResultAssertions {

    fun <A> assertSuccess(result: Result<A, Error>): Subject<DefaultSubject, Any> {
        assertThat(result).isInstanceOf(Result.Success::class.java)
        return assertThat((result as Result.Success).value)
    }

    @Suppress("UNCHECKED_CAST")
    fun <A> onSuccess(result: Result<A, Error>, block: (A) -> Unit) {
        assertThat(result).isInstanceOf(Result.Success::class.java)
        (result as Result.Success).value.let(block)
    }

    fun <A> assertFailure(result: Result<A, Error>): Subject<DefaultSubject, Any> {
        assertThat(result).isInstanceOf(Result.Failure::class.java)
        return assertThat((result as Result.Failure).error)
    }

    fun <A, B> assertFailure(result: Result<A, Error>, block: (Error) -> B): Subject<DefaultSubject, Any> {
        assertThat(result).isInstanceOf(Result.Failure::class.java)
        return assertThat((result as Result.Failure).error.let(block))
    }

}
