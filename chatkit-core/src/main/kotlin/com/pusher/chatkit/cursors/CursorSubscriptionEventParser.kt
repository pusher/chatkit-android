package com.pusher.chatkit.cursors

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.pusher.chatkit.util.asObject
import com.pusher.chatkit.util.asString
import com.pusher.chatkit.util.getValue
import com.pusher.chatkit.util.parseAs
import com.pusher.platform.network.DataParser
import com.pusher.util.Result
import com.pusher.util.asSuccess
import com.pusher.util.orElse
import elements.Error
import elements.Errors

internal object CursorSubscriptionEventParser : DataParser<CursorSubscriptionEvent> {
    override fun invoke(body: String): Result<CursorSubscriptionEvent, Error> =
            body.parseAs<JsonElement>()
                    .map { it.takeIf { it.isJsonObject }?.asJsonObject }
                    .flatMap { it.orElse { Errors.other("") } }
                    .flatMap { json: JsonObject -> json.toCursorSubscriptionEvent() }

    private fun JsonObject.toCursorSubscriptionEvent(): Result<CursorSubscriptionEvent, Error> =
            eventName.flatMap { eventName: String -> data.flatMap { it.parseEvent(eventName) } }

    private inline val JsonObject.eventName: Result<String, Error>
        get() = getValue("event_name").flatMap { it.asString() }

    private inline val JsonObject.data: Result<JsonObject, Error>
        get() = getValue("data").flatMap { it.asObject() }

    private fun JsonObject.parseEvent(eventName: String): Result<CursorSubscriptionEvent, Error> =
            when (eventName) {
                "new_cursor" -> parseAs<Cursor>().map(CursorSubscriptionEvent::OnCursorSet)
                "cursor_set" -> CursorSubscriptionEvent.NoEvent.asSuccess()
                "initial_state" -> parseAs<CursorSubscriptionEvent.InitialState>()
                else -> CursorSubscriptionEvent.OnError(
                        Errors.other("Unexpected event name $eventName")
                ).asSuccess<CursorSubscriptionEvent, Error>()
            }.map { it }
}