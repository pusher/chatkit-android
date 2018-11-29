package com.pusher.chatkit.memberships

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.pusher.chatkit.memberships.MembershipSubscriptionEvent.*
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

internal object MembershipSubscriptionEventParser : DataParser<MembershipSubscriptionEvent> {
    override fun invoke(body: String): Result<MembershipSubscriptionEvent, Error> =
        body.parseAs<JsonElement>()
            .map { it.takeIf { it.isJsonObject }?.asJsonObject }
            .flatMap { it.orElse { Errors.other("") } }
            .flatMap { json: JsonObject -> json.toMembershipSubscriptionEvent() }

    private fun JsonObject.toMembershipSubscriptionEvent(): Result<MembershipSubscriptionEvent, Error> =
        eventName.flatMap { eventName: String -> data.flatMap { it.parseEvent(eventName) } }

    private inline val JsonObject.eventName: Result<String, Error>
        get() = getValue("event_name").flatMap { it.asString() }

    private inline val JsonObject.data: Result<JsonObject, Error>
        get() = getValue("data").flatMap { it.asObject() }

    private fun JsonObject.parseEvent(eventName: String): Result<MembershipSubscriptionEvent, Error> =
        when(eventName) {
            "initial_state" -> parseAs<InitialState>()
            "user_joined" -> parseAs<UserJoined>()
            "user_left" -> parseAs<UserLeft>()
            else -> Errors.other("Invalid event name: $eventName").asFailure()
        }.map { it }
}
