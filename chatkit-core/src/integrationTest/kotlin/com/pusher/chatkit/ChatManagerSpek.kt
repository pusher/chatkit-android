package com.pusher.chatkit

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.ChatEvent.*
import com.pusher.chatkit.Rooms.GENERAL
import com.pusher.chatkit.Users.ALICE
import com.pusher.chatkit.Users.PUSHERINO
import com.pusher.chatkit.Users.SUPER_USER
import com.pusher.chatkit.cursors.Cursor
import com.pusher.chatkit.messages.multipart.Message
import com.pusher.chatkit.presence.Presence
import com.pusher.chatkit.rooms.Room
import com.pusher.chatkit.rooms.RoomEvent
import com.pusher.chatkit.rooms.RoomListeners
import com.pusher.chatkit.rooms.toCallback
import com.pusher.chatkit.test.InstanceActions.createDefaultRole
import com.pusher.chatkit.test.InstanceActions.newRoom
import com.pusher.chatkit.test.InstanceActions.newUser
import com.pusher.chatkit.test.InstanceActions.newUsers
import com.pusher.chatkit.test.InstanceActions.setCursor
import com.pusher.chatkit.test.InstanceSupervisor.setUpInstanceWith
import com.pusher.chatkit.test.InstanceSupervisor.tearDownInstance
import com.pusher.chatkit.test.run
import com.pusher.chatkit.users.User
import com.pusher.chatkit.util.FutureValue
import com.pusher.util.Result
import mockitox.stub
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import elements.Error as ElementsError

class ChatManagerSpek : Spek({
    beforeEachTest(::tearDownInstance)
    afterEachTest(::closeChatManagers)

    describe("SynchronousChatManager") {

        it("loads current user") {
            setUpInstanceWith(
                    createDefaultRole(),
                    newUser(
                            id = PUSHERINO,
                            name = "pusherino",
                            avatarUrl = "https://example.com/face.png",
                            customData = mapOf("custom" to "data")
                    )
            )

            val user = chatFor(PUSHERINO).connect().assumeSuccess()

            assertThat(user.id).isEqualTo(PUSHERINO)
            assertThat(user.name).isEqualTo("pusherino")
            assertThat(user.avatarURL).isEqualTo("https://example.com/face.png")
            assertThat(user.customData).isEqualTo(mapOf("custom" to "data"))
        }

        it("loads current user and reports via event") {
            setUpInstanceWith(
                    createDefaultRole(),
                    newUser(
                            id = PUSHERINO,
                            name = "pusherino",
                            avatarUrl = "https://example.com/face.png",
                            customData = mapOf("custom" to "data")
                    )
            )

            var firstEvent by FutureValue<ChatEvent>()

            chatFor(PUSHERINO).connect(
                    consumer = { e: ChatEvent -> firstEvent = e }
            )

            assertThat(firstEvent is CurrentUserReceived).isTrue()
            with (firstEvent as CurrentUserReceived) {
                assertThat(currentUser.id).isEqualTo(PUSHERINO)
                assertThat(currentUser.name).isEqualTo("pusherino")
                assertThat(currentUser.avatarURL).isEqualTo("https://example.com/face.png")
                assertThat(currentUser.customData).isEqualTo(mapOf("custom" to "data"))
            }
        }

        it("emits only one current user received event") {
            setUpInstanceWith(
                    createDefaultRole(),
                    newUser(
                            id = PUSHERINO,
                            name = "pusherino",
                            avatarUrl = "https://example.com/face.png",
                            customData = mapOf("custom" to "data")
                    ),
                    newRoom(GENERAL, SUPER_USER)
            )

            val events = mutableListOf<ChatEvent>()
            val done = CountDownLatch(1)

            chatFor(PUSHERINO).connect(
                    consumer = { e: ChatEvent ->
                        events.add(e)
                        if (e is ChatEvent.AddedToRoom) {
                            done.countDown()
                        }
                    }
            ).assumeSuccess()

            // We'll just use this event to mark that the initialisation really is over and we're
            // received an event which happened after the user connected
            val superUser = chatFor(SUPER_USER).connect().assumeSuccess()
            superUser.addUsersToRoom(superUser.generalRoom.id, listOf(PUSHERINO))

            done.await()
            assertThat(events.count { it is ChatEvent.CurrentUserReceived }).isEqualTo(1)
        }

        it("loads user rooms") {
            setUpInstanceWith(createDefaultRole(), newUser(PUSHERINO), newRoom(GENERAL, PUSHERINO))

            val user = chatFor(PUSHERINO).connect().assumeSuccess()
            val roomNames = user.rooms.map { room -> room.name }

            assertThat(roomNames).containsExactly(GENERAL)
        }

        it("loads users related to current user") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val user = chatFor(PUSHERINO).connect().assumeSuccess()
            user.rooms.forEach { room -> user.subscribeToRoom(room) { } }

            val users = user.users
            val relatedUserIds = users.recover { emptyList() }.map { it.id }

            assertThat(relatedUserIds).containsAllOf(ALICE, PUSHERINO)
        }

        it("subscribes to a room and receives message from alice") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect()
            val alice = chatFor(ALICE).connect()

            val room = pusherino.assumeSuccess().generalRoom

            var messageReceived by FutureValue<com.pusher.chatkit.messages.Message>()

            pusherino.assumeSuccess().subscribeToRoom(room, RoomListeners(
                    onMessage = { message -> messageReceived = message },
                    onErrorOccurred = { e -> error("error: $e") }
            ))

            val messageResult = alice.assumeSuccess().sendMessage(room, "message text")

            check(messageResult is Result.Success)
            assertThat(messageReceived.text).isEqualTo("message text")
        }

        it("receives current user with listeners instead of callback") {
            setUpInstanceWith(
                    createDefaultRole(),
                    newUsers(PUSHERINO, ALICE),
                    newRoom(GENERAL)
            )

            val user = chatFor(PUSHERINO).connect(ChatListeners())
            val userId = user.assumeSuccess().id

            assertThat(userId).isEqualTo(PUSHERINO)
        }
    }

    describe("ChatManager") {
        it("should not receive any events related to initial state") {
            setUpInstanceWith(
                    createDefaultRole(),
                    newUsers(PUSHERINO),
                    newRoom(GENERAL, PUSHERINO, SUPER_USER)
            )
            val superUser = chatFor(SUPER_USER).connect().assumeSuccess()
            setCursor(PUSHERINO, superUser.generalRoom.id, 2).run()

            val chatManager = ChatManager(
                    instanceLocator = INSTANCE_LOCATOR,
                    userId = PUSHERINO,
                    dependencies = TestChatkitDependencies(
                            tokenProvider = TestTokenProvider(INSTANCE_ID, PUSHERINO, AUTH_KEY_ID, AUTH_KEY_SECRET)
                    )
            )

            val currentUser = FutureValue<Result<CurrentUser, elements.Error>>()
            val events = ConcurrentLinkedQueue<ChatEvent>()

            chatManager.connect(
                    consumer = { e -> events.add(e) },
                    callback = { result ->
                        currentUser.set(result)
                    }
            )

            currentUser.get().assumeSuccess()
            assertThat(events).hasSize(1)
            val event = events.poll()
            when (event) {
                is ChatEvent.CurrentUserReceived -> {
                } // Pass
                else -> throw IllegalStateException("$event is not the expected ChatEvent.CurrentUserReceived")
            }
        }

        it("should not receive any events related to initial state, on reconnect") {
            setUpInstanceWith(
                    createDefaultRole(),
                    newUsers(PUSHERINO),
                    newRoom(GENERAL, PUSHERINO, SUPER_USER)
            )

            val superUser = chatFor(SUPER_USER).connect().assumeSuccess()
            superUser.setReadCursor(superUser.generalRoom, 1)

            setCursor(PUSHERINO, superUser.generalRoom.id, 2).run()

            val chatManager = ChatManager(
                    instanceLocator = INSTANCE_LOCATOR,
                    userId = PUSHERINO,
                    dependencies = TestChatkitDependencies(
                            tokenProvider = TestTokenProvider(INSTANCE_ID, PUSHERINO, AUTH_KEY_ID, AUTH_KEY_SECRET)
                    )
            ).blocking()

            chatManager.connect()

            // And again
            chatManager.close()

            val events = ConcurrentLinkedQueue<ChatEvent>()

            chatManager.connect(
                    consumer = { e -> events.add(e) }
            )

            assertThat(events).hasSize(1)
            val event = events.poll()
            when (event) {
                is ChatEvent.CurrentUserReceived -> {
                } // Pass
                else -> throw IllegalStateException("$event is not the expected ChatEvent.CurrentUserReceived")
            }

        }

    }

    val currentUser = stub<SynchronousCurrentUser>("currentUser")
    val user = stub<User>("user")
    val room = stub<Room>("room")
    val v2message = stub<com.pusher.chatkit.messages.Message>("message")
    val v3message = stub<Message>("message")
    val cursor = stub<Cursor>("cursor")
    val error = stub<elements.Error>("error")
    val roomId = "123"

    describe("ChatListeners") {

        it("maps from callback to listeners") {
            val actual = mutableListOf<Any>()
            val consume = ChatListeners(
                    onErrorOccurred = { actual += "onErrorOccurred" to it },
                    onAddedToRoom = { actual += "onAddedToRoom" to it },
                    onRemovedFromRoom = { actual += "onRemovedFromRoom" to it },
                    onCurrentUserReceived = { actual += "onCurrentUserReceived" to it },
                    onNewReadCursor = { actual += "onNewReadCursor" to it },
                    onRoomDeleted = { actual += "onRoomDeleted" to it },
                    onRoomUpdated = { actual += "onRoomUpdated" to it },
                    onPresenceChanged = { u, n, p -> actual += "onPresenceChanged" to u to n to p },
                    onUserJoinedRoom = { u, r -> actual += "onUserJoinedRoom" to u to r },
                    onUserLeftRoom = { u, r -> actual += "onUserLeftRoom" to u to r },
                    onUserStartedTyping = { u, r -> actual += "onUserStartedTyping" to u to r },
                    onUserStoppedTyping = { u, r -> actual += "onUserStoppedTyping" to u to r }
            ).toCallback()

            consume(CurrentUserReceived(currentUser))
            consume(UserStartedTyping(user, room))
            consume(UserStoppedTyping(user, room))
            consume(UserJoinedRoom(user, room))
            consume(UserLeftRoom(user, room))
            consume(PresenceChange(user, Presence.Online, Presence.Unknown))
            consume(AddedToRoom(room))
            consume(RemovedFromRoom(roomId))
            consume(RoomUpdated(room))
            consume(NoEvent)
            consume(RoomDeleted(roomId))
            consume(NewReadCursor(cursor))
            consume(ErrorOccurred(error))

            assertThat(actual).containsExactly(
                    "onErrorOccurred" to error,
                    "onCurrentUserReceived" to currentUser,
                    "onAddedToRoom" to room,
                    "onRemovedFromRoom" to roomId,
                    "onNewReadCursor" to cursor,
                    "onRoomDeleted" to roomId,
                    "onRoomUpdated" to room,
                    "onPresenceChanged" to user to Presence.Online to Presence.Unknown,
                    "onUserJoinedRoom" to user to room,
                    "onUserLeftRoom" to user to room,
                    "onUserStartedTyping" to user to room,
                    "onUserStoppedTyping" to user to room
            )
        }
    }

    describe("RoomSubscriptionListener") {
        it("maps from callback to listeners") {
            val actual = mutableListOf<Any>()

            val consume = RoomListeners(
                    onErrorOccurred = { actual += "onErrorOccurred" to it },
                    onNewReadCursor = { actual += "onNewReadCursor" to it },
                    onRoomDeleted = { actual += "onRoomDeleted" to it },
                    onRoomUpdated = { actual += "onRoomUpdated" to it },
                    onPresenceChange = { actual += "onPresenceChanged" to it },
                    onUserStartedTyping = { actual += "onUserStartedTyping" to it },
                    onMultipartMessage = { actual += "onMultipartMessage" to it },
                    onMessage = { actual += "onMessage" to it },
                    onUserJoined = { actual += "onUserJoined" to it },
                    onUserLeft = { actual += "onUserLeft" to it }
            ).toCallback()

            consume(RoomEvent.UserStartedTyping(user))
            consume(RoomEvent.UserJoined(user))
            consume(RoomEvent.UserLeft(user))
            consume(RoomEvent.PresenceChange(user, Presence.Online, Presence.Unknown))
            consume(RoomEvent.RoomUpdated(room))
            consume(RoomEvent.RoomDeleted(roomId))
            consume(RoomEvent.NewReadCursor(cursor))
            consume(RoomEvent.ErrorOccurred(error))
            consume(RoomEvent.Message(v2message))
            consume(RoomEvent.MultipartMessage(v3message))

            assertThat(actual).containsExactly(
                    "onUserStartedTyping" to user,
                    "onUserJoined" to user,
                    "onUserLeft" to user,
                    "onPresenceChanged" to user,
                    "onRoomUpdated" to room,
                    "onRoomDeleted" to roomId,
                    "onNewReadCursor" to cursor,
                    "onErrorOccurred" to error,
                    "onMessage" to v2message,
                    "onMultipartMessage" to v3message
            )
        }
    }
})
