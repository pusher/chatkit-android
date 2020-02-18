package com.pusher.chatkit.rooms

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.pusher.chatkit.messages.api.MessageApiType
import com.pusher.chatkit.util.asObject
import com.pusher.chatkit.util.asString
import com.pusher.chatkit.util.getValue
import com.pusher.chatkit.util.parseAs
import com.pusher.platform.network.DataParser
import com.pusher.util.Result
import com.pusher.util.asFailure
import com.pusher.util.orElse
import elements.Error
import elements.Errors

internal object RoomSubscriptionEventParser : DataParser<RoomSubscriptionEvent> {
    override fun invoke(body: String): Result<RoomSubscriptionEvent, Error> {
        return body.parseAs<JsonElement>()
                .map { it.takeIf { it.isJsonObject }?.asJsonObject }
                .flatMap { it.orElse { Errors.other("") } }
                .flatMap { json: JsonObject -> json.toRoomSubscriptionEvent() }
    }

    private fun JsonObject.toRoomSubscriptionEvent(): Result<RoomSubscriptionEvent, Error> =
            eventName.flatMap { eventName: String -> data.flatMap { it.parseEvent(eventName) } }

    private inline val JsonObject.eventName: Result<String, Error>
        get() = getValue("event_name").flatMap { it.asString() }

    private inline val JsonObject.data: Result<JsonObject, Error>
        get() = getValue("data").flatMap { it.asObject() }

    private fun JsonObject.parseEvent(eventName: String): Result<RoomSubscriptionEvent, Error> =
            try {
                when (eventName) {
                    "new_message" ->
                        parseAs<MessageApiType>().map(RoomSubscriptionEvent::NewMessage)
                    "is_typing" -> parseAs<RoomSubscriptionEvent.UserIsTyping>()
                    "message_deleted" -> parseAs<RoomSubscriptionEvent.MessageDeleted>()
                    else -> Errors.other("Invalid event name: $eventName").asFailure()
                }.map { it } // generics :O
            } catch (e: Throwable) {
                Errors.other("Error: $e").asFailure()
            }
}
