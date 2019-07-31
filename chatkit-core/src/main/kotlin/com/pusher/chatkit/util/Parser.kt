package com.pusher.chatkit.util

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.pusher.util.Result
import com.pusher.util.asFailure
import com.pusher.util.asSuccess
import elements.Error
import elements.Errors

private val GSON = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .serializeNulls()
        .create()

internal inline fun <reified A> typeToken() =
        object : TypeToken<A>() {}.type

internal inline fun <reified A> JsonElement.parseAs(): Result<A, Error> =
        safeParse { GSON.fromJson<A>(this, typeToken<A>()) }

internal inline fun <reified A> String.parseAs(): Result<A, Error> =
        safeParse { GSON.fromJson<A>(this, typeToken<A>()) }

internal fun <A> A.toJson(): Result<String, Error> =
        safeParse { GSON.toJson(this) }

private fun <A> safeParse(block: () -> A): Result<A, Error> =
        try {
            block().asSuccess()
        } catch (e: Exception) {
            Errors.other(e).asFailure()
        }
