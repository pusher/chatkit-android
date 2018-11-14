package com.pusher.chatkit

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.ChatEvent.*
import com.pusher.chatkit.Rooms.GENERAL
import com.pusher.chatkit.Users.ALICE
import com.pusher.chatkit.Users.PUSHERINO
import com.pusher.chatkit.cursors.Cursor
import com.pusher.chatkit.messages.Message
import com.pusher.chatkit.presence.Presence
import com.pusher.chatkit.rooms.Room
import com.pusher.chatkit.rooms.RoomEvent
import com.pusher.chatkit.rooms.RoomListeners
import com.pusher.chatkit.rooms.toCallback
import com.pusher.chatkit.util.FutureValue
import com.pusher.chatkit.test.InstanceActions.createDefaultRole
import com.pusher.chatkit.test.InstanceActions.newRoom
import com.pusher.chatkit.test.InstanceActions.newUser
import com.pusher.chatkit.test.InstanceActions.newUsers
import com.pusher.chatkit.test.InstanceSupervisor.setUpInstanceWith
import com.pusher.chatkit.test.InstanceSupervisor.tearDownInstance
import com.pusher.chatkit.users.User
import com.pusher.util.Result
import mockitox.stub
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
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

            var messageReceived by FutureValue<Message>()

            pusherino.assumeSuccess().subscribeToRoom(room, RoomListeners(
                onMessage = { message -> messageReceived = message },
                onErrorOccurred = { e -> error("error: $e") }
            ))

            val messageResult = alice.assumeSuccess().sendMessage(room, "message text")

            check(messageResult is Result.Success)
            assertThat(messageReceived.text).isEqualTo("message text")
        }

        it("receives current user with listeners instead of callback") {
            setUpInstanceWith(createDefaultRole(), newUser(PUSHERINO))

            val user = chatFor(PUSHERINO).connect(ChatListeners())
            val userId = user.assumeSuccess().id

            assertThat(userId).isEqualTo(PUSHERINO)
        }
    }

    val currentUser = stub<SynchronousCurrentUser>("currentUser")
    val user = stub<User>("user")
    val room = stub<Room>("room")
    val message = stub<Message>("message")
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
            consume(RoomEvent.Message(message))

            assertThat(actual).containsExactly(
                "onUserStartedTyping" to user,
                "onUserJoined" to user,
                "onUserLeft" to user,
                "onPresenceChanged" to user,
                "onRoomUpdated" to room,
                "onRoomDeleted" to roomId,
                "onNewReadCursor" to cursor,
                "onErrorOccurred" to error,
                "onMessage" to message
            )
        }
    }
})

