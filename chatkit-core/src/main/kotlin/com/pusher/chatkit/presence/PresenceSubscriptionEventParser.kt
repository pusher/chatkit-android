package com.pusher.chatkit.presence

import com.google.gson.JsonElement
import com.google.gson.JsonObject
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

internal class PresenceSubscriptionEventParser(
        private val userId: String
) : DataParser<PresenceSubscriptionEvent> {

    override fun invoke(body: String): Result<PresenceSubscriptionEvent, Error> =
            body.parseAs<JsonElement>()
                    .map { it.takeIf { it.isJsonObject }?.asJsonObject }
                    .flatMap { it.orElse { Errors.other("") } }
                    .flatMap { json: JsonObject -> json.toPresenceSubscriptionEvent() }

    private fun JsonObject.toPresenceSubscriptionEvent(): Result<PresenceSubscriptionEvent, Error> =
            eventName.flatMap { eventName: String -> data.flatMap { it.parseEvent(eventName) }  }

    private inline val JsonObject.eventName: Result<String, Error>
        get() = getValue("event_name").flatMap { it.asString() }

    private inline val JsonObject.data: Result<JsonObject, Error>
        get() = getValue("data").flatMap { it.asObject() }

    private fun JsonObject.parseEvent(eventName: String): Result<PresenceSubscriptionEvent, Error> =
            when (eventName) {
                "presence_state" -> parseAs<PresenceStateBody>()
                        .map { parsedEvent ->
                            PresenceSubscriptionEvent.PresenceUpdate(
                                    UserPresence(
                                            when (parsedEvent.state) {
                                                "online" -> Presence.Online
                                                "offline" -> Presence.Offline
                                                else -> Presence.Unknown
                                            },
                                            userId
                                    )
                            )
                        }
                else -> Errors.other("Invalid event name: $eventName").asFailure()
            }.map { it } // generics -.-
}

private class PresenceStateBody(
        val state: String
)