package com.pusher.chatkit

import com.google.common.truth.Truth

inline fun <T, reified S: T> List<T>.assertTypedSingletonList(): S {
    Truth.assertThat(this).hasSize(1)
    Truth.assertThat(this.first()).isInstanceOf(S::class.java)
    return this.first() as S
}