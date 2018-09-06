package com.pusher.chatkit

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.ChatManagerEvent.*
import com.pusher.chatkit.Rooms.GENERAL
import com.pusher.chatkit.Users.ALICE
import com.pusher.chatkit.Users.PUSHERINO
import com.pusher.chatkit.cursors.Cursor
import com.pusher.chatkit.messages.Message
import com.pusher.chatkit.rooms.Room
import com.pusher.chatkit.rooms.RoomSubscriptionEvent
import com.pusher.chatkit.rooms.RoomSubscriptionListeners
import com.pusher.chatkit.rooms.toCallback
import com.pusher.chatkit.test.FutureValue
import com.pusher.chatkit.test.InstanceActions.createDefaultRole
import com.pusher.chatkit.test.InstanceActions.newRoom
import com.pusher.chatkit.test.InstanceActions.newUser
import com.pusher.chatkit.test.InstanceActions.newUsers
import com.pusher.chatkit.test.InstanceSupervisor.setUpInstanceWith
import com.pusher.chatkit.test.InstanceSupervisor.tearDownInstance
import com.pusher.chatkit.users.User
import com.pusher.platform.network.wait
import com.pusher.util.Result
import mockitox.stub
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import elements.Error as ElementsError

class ChatManagerSpek : Spek({
    beforeEachTest(::tearDownInstance)
    afterEachTest(::tearDownInstance)
    afterEachTest(::closeChatManagers)

    describe("ChatManager with valid instance") {

        it("loads current user") {
            setUpInstanceWith(createDefaultRole(), newUser(PUSHERINO))

            val user = chatFor(PUSHERINO).connect().wait()
            val userId = user.assumeSuccess().id

            assertThat(userId).isEqualTo(PUSHERINO)
        }

        it("loads user rooms") {
            setUpInstanceWith(createDefaultRole(), newUser(PUSHERINO), newRoom(GENERAL, PUSHERINO))

            val user = chatFor(PUSHERINO).connect().wait().assumeSuccess()
            val roomNames = user.rooms.map { room -> room.name }

            assertThat(roomNames).containsExactly(GENERAL)
        }

        it("loads users related to current user") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val user = chatFor(PUSHERINO).connect().wait().assumeSuccess()
            user.rooms.forEach { room -> user.subscribeToRoom(room) { } }

            val users = user.users.wait()
            val relatedUserIds = users.recover { emptyList() }.map { it.id }

            assertThat(relatedUserIds).containsAllOf(ALICE, PUSHERINO)
        }

        it("subscribes to a room and receives message from alice") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().wait()
            val alice = chatFor(ALICE).connect().wait()

            val room = pusherino.assumeSuccess().generalRoom

            var messageReceived by FutureValue<Message>()

            pusherino.assumeSuccess().subscribeToRoom(room, RoomSubscriptionListeners(
                onNewMessage = { message -> messageReceived = message },
                onErrorOccurred = { e -> error("error: $e") }
            ))

            val messageResult = alice.assumeSuccess().sendMessage(room, "message text").wait()

            check(messageResult is Result.Success)
            assertThat(messageReceived.text).isEqualTo("message text")
        }


        it("receives current user with listeners instead of callback") {
            setUpInstanceWith(createDefaultRole(), newUser(PUSHERINO))

            val user = chatFor(PUSHERINO).connect(ChatManagerListeners()).wait()
            val userId = user.assumeSuccess().id

            assertThat(userId).isEqualTo(PUSHERINO)
        }

    }

    val currentUser = stub<CurrentUser>("currentUser")
    val user = stub<User>("user")
    val room = stub<Room>("room")
    val message = stub<Message>("message")
    val cursor = stub<Cursor>("cursor")
    val error = stub<elements.Error>("error")
    val roomId = 123

    describe("ChatManagerListeners") {

        it("maps from callback to listeners") {

            val actual = mutableListOf<Any>()

            val consume = ChatManagerListeners(
                onErrorOccurred = { actual += "onErrorOccurred" to it },
                onCurrentUserAddedToRoom = { actual += "onCurrentUserAddedToRoom" to it },
                onCurrentUserReceived = { actual += "onCurrentUserReceived" to it },
                onCurrentUserRemovedFromRoom = { actual += "onCurrentUserRemovedFromRoom" to it },
                onNewReadCursor = { actual += "onNewReadCursor" to it },
                onRoomDeleted = { actual += "onRoomDeleted" to it },
                onRoomUpdated = { actual += "onRoomUpdated" to it },
                onUserCameOnline = { actual += "onUserCameOnline" to it },
                onUserJoinedRoom = { u, r -> actual += "onUserJoinedRoom" to u to r },
                onUserLeftRoom = { u, r -> actual += "onUserLeftRoom" to u to r },
                onUserStartedTyping = { u, r -> actual += "onUserStartedTyping" to u to r },
                onUserStoppedTyping = { u, r -> actual += "onUserStoppedTyping" to u to r },
                onUserWentOffline = { actual += "onUserWentOffline" to it }
            ).toCallback()

            consume(CurrentUserReceived(currentUser))
            consume(UserStartedTyping(user, room))
            consume(UserStoppedTyping(user, room))
            consume(UserJoinedRoom(user, room))
            consume(UserLeftRoom(user, room))
            consume(UserCameOnline(user))
            consume(UserWentOffline(user))
            consume(CurrentUserAddedToRoom(room))
            consume(CurrentUserRemovedFromRoom(roomId))
            consume(RoomUpdated(room))
            consume(NoEvent)
            consume(RoomDeleted(roomId))
            consume(NewReadCursor(cursor))
            consume(ErrorOccurred(error))

            assertThat(actual).containsExactly(
                "onErrorOccurred" to error,
                "onCurrentUserAddedToRoom" to room,
                "onCurrentUserReceived" to currentUser,
                "onCurrentUserRemovedFromRoom" to roomId,
                "onNewReadCursor" to cursor,
                "onRoomDeleted" to roomId,
                "onRoomUpdated" to room,
                "onUserCameOnline" to user,
                "onUserJoinedRoom" to user to room,
                "onUserLeftRoom" to user to room,
                "onUserStartedTyping" to user to room,
                "onUserStoppedTyping" to user to room,
                "onUserWentOffline" to user
            )

        }

    }

    describe("RoomSubscriptionListener") {

        it("maps from callback to listeners") {

            val actual = mutableListOf<Any>()

            val consume = RoomSubscriptionListeners(
                onErrorOccurred = { actual += "onErrorOccurred" to it },
                onNewReadCursor = { actual += "onNewReadCursor" to it },
                onRoomDeleted = { actual += "onRoomDeleted" to it },
                onRoomUpdated = { actual += "onRoomUpdated" to it },
                onUserCameOnline = { actual += "onUserCameOnline" to it },
                onUserStartedTyping = { actual += "onUserStartedTyping" to it },
                onUserWentOffline = { actual += "onUserWentOffline" to it },
                onNewMessage = { actual += "onNewMessage" to it },
                onUserJoined = { actual += "onUserJoined" to it },
                onUserLeft = { actual += "onUserLeft" to it }
            ).toCallback()

            consume(RoomSubscriptionEvent.UserStartedTyping(user))
            consume(RoomSubscriptionEvent.UserJoined(user))
            consume(RoomSubscriptionEvent.UserLeft(user))
            consume(RoomSubscriptionEvent.UserCameOnline(user))
            consume(RoomSubscriptionEvent.UserWentOffline(user))
            consume(RoomSubscriptionEvent.RoomUpdated(room))
            consume(RoomSubscriptionEvent.RoomDeleted(roomId))
            consume(RoomSubscriptionEvent.NewReadCursor(cursor))
            consume(RoomSubscriptionEvent.ErrorOccurred(error))
            consume(RoomSubscriptionEvent.NewMessage(message))

            assertThat(actual).containsExactly(
                "onUserStartedTyping" to user,
                "onUserJoined" to user,
                "onUserLeft" to user,
                "onUserCameOnline" to user,
                "onUserWentOffline" to user,
                "onRoomUpdated" to room,
                "onRoomDeleted" to roomId,
                "onNewReadCursor" to cursor,
                "onErrorOccurred" to error,
                "onNewMessage" to message
            )

        }

    }

})

