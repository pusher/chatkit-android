package com.pusher.chatkit.rooms

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.pusher.chatkit.CustomData
import com.pusher.chatkit.messages.multipart.*
import com.pusher.chatkit.util.asObject
import com.pusher.chatkit.util.asString
import com.pusher.chatkit.util.getValue
import com.pusher.chatkit.util.parseAs
import com.pusher.platform.network.DataParser
import com.pusher.util.*
import elements.Error
import elements.Errors
import java.net.URL
import java.text.SimpleDateFormat

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

internal class RoomSubscriptionEventParserV3(
        private val refresher: UrlRefresher
) : DataParser<RoomSubscriptionEvent> {
    override fun invoke(body: String): Result<RoomSubscriptionEvent, Error> {
        println(body)
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
                        parseAs<V3MessageBody>().flatMap { body ->
                            body.parts.map(::makePart).collect().map { parts ->
                                Message(
                                        id = body.id,
                                        senderId = body.userId,
                                        roomId = body.roomId,
                                        createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").parse(body.createdAt),
                                        updatedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").parse(body.updatedAt),
                                        parts = parts
                                )
                            }
                        }.map(RoomSubscriptionEvent::NewMultipartMessage)
                    "is_typing" -> parseAs<RoomSubscriptionEvent.UserIsTyping>()
                    else -> Errors.other("Invalid event name: $eventName").asFailure()
                }.map { it } // generics :O
            } catch (e: Throwable) {
                println(e)
                Errors.other("Error: $e").asFailure()
            }

    private fun makePart(body: V3PartBody): Result<Part, Error> =
            try {
                when {
                    body.content != null ->
                        Part(
                                partType = PartType.InlinePayload,
                                payload = Payload.InlinePayload(
                                        type = body.type,
                                        content = body.content
                                )
                        ).asSuccess()
                    body.url != null ->
                        Part(
                                partType = PartType.UrlPayload,
                                payload = Payload.UrlPayload(
                                        type = body.type,
                                        url = URL(body.url)
                                )
                        ).asSuccess()
                    body.attachment != null ->
                        Part(
                                partType = PartType.AttachmentPayload,
                                payload = Payload.AttachmentPayload(
                                        type = body.type,
                                        size = body.attachment.size,
                                        name = body.attachment.name,
                                        customData = body.attachment.customData,
                                        refreshUrl = body.attachment.refreshUrl,
                                        downloadUrl = body.attachment.downloadUrl,
                                        expiration = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").parse(body.attachment.expiration),
                                        refresher = refresher
                                )
                        ).asSuccess()
                    else ->
                        Errors.other("Invalid part entity, no content, url or attachment found.").asFailure()
                }
            } catch (e: Exception) {
                Errors.other(e).asFailure()
            }
}

data class V3MessageBody(
        val id: Int,
        val userId: String,
        val roomId: String,
        val parts: List<V3PartBody>,
        val createdAt: String,
        val updatedAt: String
)

data class V3PartBody(
        val content: String?,
        val type: String,
        val url: String?,
        val attachment: V3AttachmentBody?
)

data class V3AttachmentBody(
        val id: String,
        val downloadUrl: String,
        val refreshUrl: String,
        val expiration: String,
        val name: String?,
        val customData: CustomData,
        val size: Int
)