package com.pusher.chatkit.users

import com.pusher.chatkit.rooms.api.JoinedRoomApiType
import com.pusher.chatkit.rooms.api.JoinedRoomApiTypeMapper
import com.pusher.chatkit.rooms.api.RoomMembershipApiType
import com.pusher.chatkit.rooms.api.RoomReadStateApiType
import com.pusher.chatkit.rooms.state.JoinedRoomsStateDiffer
import com.pusher.chatkit.state.JoinedRoom
import com.pusher.chatkit.state.JoinedRoomsReceived
import com.pusher.chatkit.state.LeftRoom
import com.pusher.chatkit.state.RoomDeleted
import com.pusher.chatkit.state.RoomUpdated
import com.pusher.chatkit.users.api.UserApiType
import com.pusher.chatkit.users.api.UserApiTypeMapper
import com.pusher.chatkit.users.api.UserSubscriptionDispatcher
import com.pusher.chatkit.users.api.UserSubscriptionEvent
import com.pusher.chatkit.util.DateApiTypeMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.reduxkotlin.Dispatcher
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
        createdAt = "2020-02-27T17:12:10Z",
        updatedAt = "2020-02-27T17:12:10Z",
        name = "name",
        avatarUrl = null,
        customData = null,
        online = false)

    describe("given no existing state") {

        val dispatcher = mockk<Dispatcher>(relaxed = true)
        val differ = mockk<JoinedRoomsStateDiffer>(relaxed = true)
        val dateApiTypeMapper = DateApiTypeMapper()
        val joinedRoomApiTypeMapper = JoinedRoomApiTypeMapper(dateApiTypeMapper)
        val userApiTypeMapper = UserApiTypeMapper(dateApiTypeMapper)
        val userSubscriptionDispatcher = UserSubscriptionDispatcher(
            userApiTypeMapper = userApiTypeMapper,
            joinedRoomApiTypeMapper = joinedRoomApiTypeMapper,
            joinedRoomsStateDiffer = differ,
            dispatcher = dispatcher
        )

        every { differ.stateGetter().joinedRoomsState } returns null

        describe("when I receive an InitialState event") {
            val event = UserSubscriptionEvent.InitialState(
                currentUser = simpleUser,
                rooms = listOf(simpleJoinedRoomApiType),
                readStates = listOf(RoomReadStateApiType("id1", 1, null)),
                memberships = listOf(RoomMembershipApiType("id1", listOf()))
            )
            userSubscriptionDispatcher.onEvent(event)

            it("then JoinedRoomsReceived is dispatched") {
                verify(exactly = 1) { dispatcher(JoinedRoomsReceived(
                    rooms = joinedRoomApiTypeMapper.toRoomInternalTypes(event.rooms),
                    unreadCounts = joinedRoomApiTypeMapper.toUnreadCounts(event.readStates)
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

            it("then JoinedRoom is dispatched") {
                verify(exactly = 1) { dispatcher(JoinedRoom(
                    room = joinedRoomApiTypeMapper.toRoomInternalType(event.room),
                    unreadCount = 1
                )) }
            }
        }

        describe("when I receive a RemovedFromRoomEvent") {
            val event = UserSubscriptionEvent.RemovedFromRoomEvent("room1")
            userSubscriptionDispatcher.onEvent(event)

            it("then the dispatcher will send a LeftRoom action") {
                verify(exactly = 1) { dispatcher(LeftRoom(roomId = event.roomId)) }
            }
        }

        describe("when I receive a RoomDeletedEvent") {
            val event = UserSubscriptionEvent.RoomDeletedEvent("room1")
            userSubscriptionDispatcher.onEvent(event)

            it("then DeleteRoom is dispatched") {
                verify(exactly = 1) { dispatcher(RoomDeleted(roomId = event.roomId)) }
            }
        }

        describe("when I receive a RoomUpdatedEvent") {
            val event = UserSubscriptionEvent.RoomUpdatedEvent(
                room = simpleJoinedRoomApiType
            )
            userSubscriptionDispatcher.onEvent(event)

            it("then UpdateRoom is dispatched") {
                verify(exactly = 1) { dispatcher(RoomUpdated(
                    room = joinedRoomApiTypeMapper.toRoomInternalType(event.room)
                )) }
            }
        }
    }

    describe("given existing state") {

        val dispatcher = mockk<Dispatcher>(relaxed = true)
        val differ = mockk<JoinedRoomsStateDiffer>(relaxed = true)
        val dateApiTypeMapper = DateApiTypeMapper()
        val userApiTypeMapper = UserApiTypeMapper(dateApiTypeMapper)
        val joinedRoomApiTypeMapper = JoinedRoomApiTypeMapper(dateApiTypeMapper)
        val userSubscriptionDispatcher = UserSubscriptionDispatcher(
            userApiTypeMapper = userApiTypeMapper,
            joinedRoomApiTypeMapper = joinedRoomApiTypeMapper,
            joinedRoomsStateDiffer = differ,
            dispatcher = dispatcher
        )

        every { differ.stateExists() } returns true

        describe("when a new InitialState event is received") {
            val event = UserSubscriptionEvent.InitialState(
                currentUser = simpleUser,
                rooms = listOf(simpleJoinedRoomApiType),
                readStates = listOf(RoomReadStateApiType("id1", 1, null)),
                memberships = listOf(RoomMembershipApiType("id1", listOf()))
            )
            userSubscriptionDispatcher.onEvent(event)

            it("then the differ toActions will be called") {
                verify(exactly = 1) {
                    differ.toActions(
                        joinedRoomApiTypeMapper.toRoomInternalTypes(event.rooms),
                        joinedRoomApiTypeMapper.toUnreadCounts(event.readStates))
                }
            }
        }
    }
})
