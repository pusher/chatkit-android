package com.pusher.chatkit.users

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.pusher.chatkit.network.asObject
import com.pusher.chatkit.network.asString
import com.pusher.chatkit.network.getValue
import com.pusher.chatkit.network.parseAs
import com.pusher.platform.network.DataParser
import com.pusher.util.Result
import com.pusher.util.asFailure
import com.pusher.util.orElse
import elements.Error
import elements.Errors

internal object UserSubscriptionEventParser : DataParser<UserSubscriptionEvent> {

    override fun invoke(body: String): Result<UserSubscriptionEvent, Error> =
        body.parseAs<JsonElement>()
            .map { it.takeIf { it.isJsonObject }?.asJsonObject }
            .flatMap { it.orElse { Errors.other("") } }
            .flatMap { json: JsonObject -> json.toUserSubscriptionEvent() }

    private fun JsonObject.toUserSubscriptionEvent(): Result<UserSubscriptionEvent, Error> =
        eventName.flatMap { eventName: String -> data.flatMap { it.parseEvent(eventName) }  }

    private inline val JsonObject.eventName: Result<String, Error>
        get() = getValue("event_name").flatMap { it.asString() }

    private inline val JsonObject.data: Result<JsonObject, Error>
        get() = getValue("data").flatMap { it.asObject() }

    private fun JsonObject.parseEvent(eventName: String): Result<UserSubscriptionEvent, Error> = when (eventName) {
        "initial_state" -> parseAs<InitialState>()
        "added_to_room" -> parseAs<AddedToRoomEvent>()
        "removed_from_room" -> parseAs<RemovedFromRoomEvent>()
        "room_updated" -> parseAs<RoomUpdatedEvent>()
        "room_deleted" -> parseAs<RoomDeletedEvent>()
        "user_joined" -> parseAs<UserJoinedEvent>()
        "user_left" -> parseAs<UserLeftEvent>()
        else -> Errors.other("Invalid event name: $eventName").asFailure()
    }.map { it } // Generics -_-

}
