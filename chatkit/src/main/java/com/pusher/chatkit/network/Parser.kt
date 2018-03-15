package com.pusher.chatkit.network

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.pusher.util.Result
import com.pusher.util.asFailure
import com.pusher.util.asSuccess
import elements.Error
import elements.Errors
import java.io.Reader
import java.lang.reflect.Type

private val GSON = GsonBuilder()
    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
    .create()

private val typeTokenCache = mutableMapOf<Class<*>, Type>()

internal inline fun <reified A> typeToken(key: Class<A> = A::class.java) = when {
    typeTokenCache.containsKey(key) -> typeTokenCache[key]
    else -> object : TypeToken<A>() {}.type.also { typeTokenCache[key] = it }
}

internal inline fun <reified A> Reader.parseAs(): Result<A, Error> = safeParse {
    GSON.fromJson<A>(this, typeToken<A>())
}

internal inline fun <reified A> String.parseAs(): Result<A, Error> = safeParse {
    GSON.fromJson<A>(this, typeToken<A>())
}

internal inline fun <reified A> JsonElement.parseAs(): Result<A, Error> = safeParse {
    GSON.fromJson<A>(this, typeToken<A>())
}

internal fun <A> A.toJson(): Result<String, Error> = safeParse {
    GSON.toJson(this)
}

internal inline fun <reified A> Reader?.parseOr(f: () -> A): Result<A, Error> =
    this?.parseAs() ?: f().asSuccess()

internal inline fun <reified A> JsonElement?.parseOr(f: () -> A): Result<A, Error> =
    this?.parseAs() ?: f().asSuccess()



private fun <A> safeParse(block: () -> A): Result<A, Error> = try {
    block().asSuccess()
} catch (e: Exception) {
    Errors.other(e).asFailure()
}
