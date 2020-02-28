package com.pusher.chatkit.users

import com.pusher.chatkit.rooms.api.JoinedRoomApiType
import com.pusher.chatkit.rooms.api.JoinedRoomApiTypeMapper
import com.pusher.chatkit.rooms.api.RoomMembershipApiType
import com.pusher.chatkit.rooms.api.RoomReadStateApiType
import com.pusher.chatkit.rooms.state.DeletedRoom
import com.pusher.chatkit.rooms.state.JoinedRoom
import com.pusher.chatkit.rooms.state.JoinedRoomInternalType
import com.pusher.chatkit.rooms.state.JoinedRoomsReceived
import com.pusher.chatkit.rooms.state.LeftRoom
import com.pusher.chatkit.rooms.state.UpdatedRoom
import com.pusher.chatkit.state.ChatkitState
import com.pusher.chatkit.users.api.UserApiType
import com.pusher.chatkit.users.api.UserSubscriptionDispatcher
import com.pusher.chatkit.users.api.UserSubscriptionEvent
import io.mockk.mockk
import io.mockk.verify
import org.reduxkotlin.Dispatcher
import org.reduxkotlin.GetState
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class UserSubscriptionDispatcherTest : Spek({

    val simpleJoinedRoomApiType = JoinedRoomApiType(
            id = "id1",
            name = "room1",
            createdById = "person1",
            pushNotificationTitleOverride = "notification override",
            private = false,
            customData = mapOf("highlight" to "blue"),
            lastMessageAt = "2020-02-27T17:12:10Z",
            updatedAt = "2020-02-27T17:12:20Z",
            createdAt = "2020-02-27T17:12:30Z",
            deletedAt = null
    )

    val simpleUser = UserApiType(
            id = "user1",
            createdAt =  "",
            updatedAt = "",
            name = "name",
            avatarURL = null,
            customData = null,
            online = false)

    describe("given a user subscription dispatcher") {

        val state = mockk<GetState<ChatkitState>>(relaxed = true)
        val dispatcher = mockk<Dispatcher>(relaxed = true)
        val joinedRoomApiTypeMapper = JoinedRoomApiTypeMapper()
        val userSubscriptionDispatcher = UserSubscriptionDispatcher(
                stateGetter = state,
                joinedRoomApiTypeMapper = joinedRoomApiTypeMapper,
                dispatcher = dispatcher
                )

        describe("when I receive an InitialState event") {
            val event = UserSubscriptionEvent.InitialState(
                    currentUser = simpleUser,
                    rooms = listOf(simpleJoinedRoomApiType),
                    readStates = listOf(RoomReadStateApiType("id1", 1, null)),
                    memberships = listOf(RoomMembershipApiType("id1", listOf()))
            )
            userSubscriptionDispatcher.onEvent(event)

            it("then the dispatcher should send a JoinedRoomsReceived action") {
                verify(exactly = 1) { dispatcher(JoinedRoomsReceived(
                        rooms = joinedRoomApiTypeMapper.toManyRoomInternal(event.rooms),
                        unreadCounts = joinedRoomApiTypeMapper.toManyUnreadCounts(event.readStates)
                )) }
            }
        }

        describe("when I receive an AddedToRoomEvent") {
            val event = UserSubscriptionEvent.AddedToRoomEvent(
                    room = simpleJoinedRoomApiType,
                    readState = RoomReadStateApiType("id1", 1, null),
                    membership = RoomMembershipApiType("id1", listOf())
            )
            userSubscriptionDispatcher.onEvent(event)

            it("then the dispatcher should send a JoinedRoom action") {
                verify(exactly = 1) { dispatcher(JoinedRoom(
                        room = joinedRoomApiTypeMapper.toRoomInternal(event.room),
                        unreadCount = 1
                )) }
            }
        }

        describe("when I receive a RemovedFromRoomEvent") {
            val event = UserSubscriptionEvent.RemovedFromRoomEvent("room1")
            userSubscriptionDispatcher.onEvent(event)

            it("then the dispatcher should send a LeftRoom action") {
                verify(exactly = 1) { dispatcher(LeftRoom(roomId = event.roomId)) }
            }
        }

        describe("when I receive a RoomDeletedEvent") {
            val event = UserSubscriptionEvent.RoomDeletedEvent("room1")
            userSubscriptionDispatcher.onEvent(event)

            it("then the dispatcher should send a DeleteRoom action") {
                verify(exactly = 1) { dispatcher(DeletedRoom(roomId = event.roomId)) }
            }
        }

        describe("when I receive a RoomUpdatedEvent") {
            val event = UserSubscriptionEvent.RoomUpdatedEvent(
                    room = simpleJoinedRoomApiType
            )
            userSubscriptionDispatcher.onEvent(event)

            it("then the dispatcher should send a JoinedRoom action") {
                verify(exactly = 1) { dispatcher(UpdatedRoom(
                        room = joinedRoomApiTypeMapper.toRoomInternal(event.room)
                )) }
            }
        }
    }
})
