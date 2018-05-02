package com.pusher.chatkit.network

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.pusher.util.Result
import com.pusher.util.orElse
import elements.Error
import elements.Errors

internal fun JsonObject.getValue(key: String) : Result<JsonElement, Error> =
    get(key).orElse { Errors.other("Value for $key not found") }

internal fun JsonElement.asString(): Result<String, Error> =
    takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.asString.orElse { Errors.other("Expected a String") }

internal fun JsonElement.asObject(): Result<JsonObject, Error> =
    takeIf { it.isJsonObject }?.asJsonObject.orElse { Errors.other("Expected an object") }
