package com.pusher.chatkit.cursors

import com.google.gson.JsonElement
import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.subscription.ResolvableSubscription
import com.pusher.chatkit.users.UserSubscriptionEvent
import com.pusher.chatkit.util.Throttler
import com.pusher.chatkit.util.parseAs
import com.pusher.platform.RequestOptions
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import com.pusher.util.mapResult
import elements.Errors

class CursorService(
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
    @Suppress("UNUSED_PARAMETER")
    internal fun populateInitial(event: UserSubscriptionEvent.InitialState) {
//        cursorStore.initialiseContents(event.readStates.mapNotNull { it.cursor })
    }

    fun close() {
//        cursorStore.clear()
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

    // TODO v2: this signature isn't correct, the cursor should be an optional
    // e.g. for the case where you have a new room with no messages (or cursors) yet!
    // The error should additionally be more descriptive instead of just assuming you aren't
    // subscribed to the room!
    @Suppress("UNUSED_PARAMETER")
    fun getReadCursor(userId: String, roomId: String) {
        // : Result<Cursor?, Error> {
//        cursorStore[userId][roomId]?.asSuccess() ?: notSubscribedToRoom(roomId).asFailure()
    }

    @Suppress("UNUSED_PARAMETER")
    private fun notSubscribedToRoom(name: String) =
            Errors.other("Must be subscribed to room $name to access member's read cursors")

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

    @Suppress("UNUSED_PARAMETER")
    internal fun applyEvent(event: UserSubscriptionEvent) {}
//            cursorStore.applyEvent(event)

    private fun applyEvent(event: CursorSubscriptionEvent) =
            listOf<CursorSubscriptionEvent>(event).map(::enrichEvent)
    //            cursorStore.applyEvent(event).map(::enrichEvent)
    @Suppress("UNUSED_PARAMETER")
    private fun enrichEvent(event: CursorSubscriptionEvent) {}
    // : ChatEvent =
//            when (event) {
//                is CursorSubscriptionEvent.OnCursorSet -> ChatEvent.NewReadCursor(event.cursor)
//                else -> ChatEvent.NoEvent
//            }
}
