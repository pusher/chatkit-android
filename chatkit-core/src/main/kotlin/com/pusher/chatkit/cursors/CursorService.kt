package com.pusher.chatkit.cursors

import com.google.gson.JsonElement
import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.subscription.ResolvableSubscription
import com.pusher.chatkit.util.Throttler
import com.pusher.chatkit.util.parseAs
import com.pusher.platform.RequestOptions
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import com.pusher.util.mapResult

internal class CursorService(
    private val client: PlatformClient,
    private val logger: Logger
) {
//    private val cursorStore = CursorStore()

    private val setReadCursorThrottler =
            Throttler { options: RequestOptions ->
                client.doRequest<JsonElement>(
                        options = options,
                        responseParser = { it.parseAs() }
                )
            }

    fun setReadCursor(
        userId: String,
        roomId: String,
        position: Int
    ) = setReadCursorThrottler.throttle(
            RequestOptions(
                    method = "PUT",
                    path = "/cursors/0/rooms/$roomId/users/$userId",
                    body = """{ "position" : $position }"""
            )
    ).mapResult {
        //        cursorStore[userId] += Cursor(
//                userId = userId,
//                roomId = roomId,
//                position = position
//        )
    }

    @Suppress("UNUSED_PARAMETER")
    fun subscribeForRoom(
        roomId: String
//        consumer: (ChatEvent) -> Unit
    ) {
//           : ResolvableSubscription<CursorSubscriptionEvent>
//        return subscribe(
//                "/cursors/0/rooms/${URLEncoder.encode(roomId, "UTF-8")}",
//                consumer
//        )
    }

    @Suppress("UNUSED_PARAMETER")
    private fun subscribe(
        path: String
//        consumer: ChatManagerEventConsumer
    ) = ResolvableSubscription(
            client = client,
            path = path,
            listeners = SubscriptionListeners(
                    onEvent = {
                        // event -> applyEvent(event.body).forEach(consumer)
                    },
                    onError = {
                        // error -> applyEvent(CursorSubscriptionEvent.OnError(error)).forEach(consumer)
                    }
            ),
            messageParser = CursorSubscriptionEventParser,
            description = "Cursor user $path",
            logger = logger
    )
}
