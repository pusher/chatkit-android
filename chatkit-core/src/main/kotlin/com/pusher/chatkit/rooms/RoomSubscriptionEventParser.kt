package com.pusher.chatkit.rooms

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.pusher.chatkit.CustomData
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
import java.util.Date

internal object RoomSubscriptionEventParserV2 : DataParser<RoomSubscriptionEvent> {
    override fun invoke(body: String): Result<RoomSubscriptionEvent, Error> =
            body.parseAs<JsonElement>()
                    .map { it.takeIf { it.isJsonObject }?.asJsonObject }
                    .flatMap { it.orElse { Errors.other("") } }
                    .flatMap { json: JsonObject -> json.toRoomSubscriptionEvent() }

    private fun JsonObject.toRoomSubscriptionEvent(): Result<RoomSubscriptionEvent, Error> =
            eventName.flatMap { eventName: String -> data.flatMap { it.parseEvent(eventName) } }

    private inline val JsonObject.eventName: Result<String, Error>
        get() = getValue("event_name").flatMap { it.asString() }

    private inline val JsonObject.data: Result<JsonObject, Error>
        get() = getValue("data").flatMap { it.asObject() }

    private fun JsonObject.parseEvent(eventName: String): Result<RoomSubscriptionEvent, Error> = when (eventName) {
        "new_message" -> parseAs<com.pusher.chatkit.messages.Message>().map { message -> RoomSubscriptionEvent.NewMessage(message) }
        "is_typing" -> parseAs<RoomSubscriptionEvent.UserIsTyping>()
        else -> Errors.other("Invalid event name: $eventName").asFailure()
    }.map { it } // generics :O
}

internal object RoomSubscriptionEventParserV3 : DataParser<RoomSubscriptionEvent> {
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
                        parseAs<V3MessageBody>().map(RoomSubscriptionEvent::NewMultipartMessage)
                    "is_typing" -> parseAs<RoomSubscriptionEvent.UserIsTyping>()
                    "message_deleted" -> parseAs<RoomSubscriptionEvent.MessageDeleted>()
                    else -> Errors.other("Invalid event name: $eventName").asFailure()
                }.map { it } // generics :O
            } catch (e: Throwable) {
                Errors.other("Error: $e").asFailure()
            }
}

internal data class V3MessageBody(
    val id: Int,
    val userId: String,
    val roomId: String,
    val parts: List<V3PartBody>,
    val createdAt: Date,
    val updatedAt: Date,
    val deletedAt: Date?
)

internal data class V3PartBody(
    val content: String?,
    val type: String,
    val url: String?,
    val attachment: V3AttachmentBody?
)

internal data class V3AttachmentBody(
    val id: String,
    val downloadUrl: String,
    val refreshUrl: String,
    val expiration: Date,
    val name: String?,
    val customData: CustomData?,
    val size: Int
)
