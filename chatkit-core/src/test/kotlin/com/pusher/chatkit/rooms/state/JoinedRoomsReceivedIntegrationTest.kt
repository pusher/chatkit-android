package com.pusher.chatkit.rooms.state

import com.pusher.chatkit.rooms.api.JoinedRoomApiType
import com.pusher.chatkit.rooms.api.JoinedRoomApiTypeMapper
import com.pusher.chatkit.rooms.api.RoomMembershipApiType
import com.pusher.chatkit.rooms.api.RoomReadStateApiType
import com.pusher.chatkit.state.ChatState
import com.pusher.chatkit.state.CurrentUserReceived
import com.pusher.chatkit.state.LeftRoom
import com.pusher.chatkit.state.ReconnectJoinedRoom
import com.pusher.chatkit.state.RoomUpdated
import com.pusher.chatkit.state.State
import com.pusher.chatkit.users.api.UserApiType
import com.pusher.chatkit.users.api.UserApiTypeMapper
import com.pusher.chatkit.users.api.UserSubscriptionDispatcher
import com.pusher.chatkit.users.api.UserSubscriptionEvent
import com.pusher.chatkit.util.DateApiTypeMapper
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.verify
import org.reduxkotlin.Dispatcher
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

internal object JoinedRoomsReceivedIntegrationTest : Spek({

    fun generateApiTypeRoom(id: String, name: String) = JoinedRoomApiType(
        id = id,
        name = name,
        createdById = "person1",
        pushNotificationTitleOverride = "notification override",
        private = false,
        customData = mapOf("highlight" to "blue"),
        lastMessageAt = "2020-02-27T17:12:10Z",
        updatedAt = "2020-02-27T17:12:20Z",
        createdAt = "2020-02-27T17:12:30Z",
        deletedAt = null
    )
    val roomApiTypeOne = generateApiTypeRoom("id1", "room1")
    val roomApiTypeOneUpdated = generateApiTypeRoom("id1", "room1-updated")
    val roomApiTypeTwo = generateApiTypeRoom("id2", "room2")
    val roomApiTypeThree = generateApiTypeRoom("id3", "room3")

    val simpleUser = UserApiType(
        id = "user1",
        createdAt = "2020-03-12T14:33:20Z",
        updatedAt = "2020-03-12T14:33:20Z",
        name = "name",
        avatarUrl = null,
        customData = null,
        online = false
    )

    describe("given existing state with one room") {
        val dateApiTypeMapper = DateApiTypeMapper()
        val joinedRoomApiTypeMapper = JoinedRoomApiTypeMapper(dateApiTypeMapper)
        val userApiTypeMapper = UserApiTypeMapper(dateApiTypeMapper)
        val dispatcher by memoized { mockk<Dispatcher>(relaxed = true) }
        val testState = State.initial().with(
            ChatState.initial().with(
                JoinedRoomsState(
                    rooms = mapOf(
                        "id1" to joinedRoomApiTypeMapper.toRoomInternalType(roomApiTypeOne)
                    ),
                    unreadCounts = mapOf(
                        "id1" to 1
                    )
                )
            )
        )
        val differ = JoinedRoomsStateDiffer { testState }
        val userSubscriptionDispatcher by memoized {
            UserSubscriptionDispatcher(
                joinedRoomApiTypeMapper = joinedRoomApiTypeMapper,
                joinedRoomsStateDiffer = differ,
                dispatcher = dispatcher,
                userApiTypeMapper = userApiTypeMapper
            )
        }

        describe("when the same InitialState event is received") {
            val event = UserSubscriptionEvent.InitialState(
                currentUser = simpleUser,
                rooms = listOf(roomApiTypeOne),
                readStates = listOf(
                    RoomReadStateApiType("id1", 1, null)
                ),
                memberships = listOf(
                    RoomMembershipApiType("id1", listOf())
                )
            )

            beforeEachTest {
                userSubscriptionDispatcher.onEvent(event)
            }

            it("then no actions are dispatched") {
                verify(exactly = 1) {
                    dispatcher(CurrentUserReceived(userApiTypeMapper.toUserInternalType(simpleUser)))
                }
                confirmVerified(dispatcher)
            }
        }

        describe("when a new InitialState event is received with a new room") {
            val event = UserSubscriptionEvent.InitialState(
                currentUser = simpleUser,
                rooms = listOf(
                    roomApiTypeOne,
                    roomApiTypeTwo
                ),
                readStates = listOf(
                    RoomReadStateApiType("id1", 1, null),
                    RoomReadStateApiType("id2", 2, null)
                ),
                memberships = listOf(
                    RoomMembershipApiType("id1", listOf()),
                    RoomMembershipApiType("id2", listOf())
                )
            )

            beforeEachTest {
                userSubscriptionDispatcher.onEvent(event)
            }

            it("then ReconnectJoinedRoom is dispatched") {
                verify(exactly = 1) {
                    dispatcher(CurrentUserReceived(userApiTypeMapper.toUserInternalType(simpleUser)))
                    dispatcher(
                        ReconnectJoinedRoom(
                            room = joinedRoomApiTypeMapper.toRoomInternalType(event.rooms.last()),
                            unreadCount = 2
                        ))
                }
                confirmVerified(dispatcher)
            }
        }

        describe("when a new InitialState event is received with a room removed") {
            val event = UserSubscriptionEvent.InitialState(
                currentUser = simpleUser,
                rooms = listOf(),
                readStates = listOf(),
                memberships = listOf()
            )

            beforeEachTest {
                userSubscriptionDispatcher.onEvent(event)
            }

            it("then the LeftRoom action is dispatched") {
                verify(exactly = 1) {
                    dispatcher(CurrentUserReceived(userApiTypeMapper.toUserInternalType(simpleUser)))
                    dispatcher(LeftRoom(roomId = "id1"))
                }
                confirmVerified(dispatcher)
            }
        }

        describe("when a new InitialState event is received with a room updated") {
            val event = UserSubscriptionEvent.InitialState(
                currentUser = simpleUser,
                rooms = listOf(roomApiTypeOneUpdated),
                readStates = listOf(
                    RoomReadStateApiType("id1", 1, null)
                ),
                memberships = listOf(
                    RoomMembershipApiType("id1", listOf())
                )
            )

            beforeEachTest {
                userSubscriptionDispatcher.onEvent(event)
            }

            it("then the RoomUpdated action is dispatched") {
                verify(exactly = 1) {
                    dispatcher(CurrentUserReceived(userApiTypeMapper.toUserInternalType(simpleUser)))
                    dispatcher(RoomUpdated(
                        room = joinedRoomApiTypeMapper.toRoomInternalType(roomApiTypeOneUpdated)
                    ))
                }
                confirmVerified(dispatcher)
            }
        }
    }

    describe("given existing state with two rooms") {
        val dateApiTypeMapper = DateApiTypeMapper()
        val joinedRoomApiTypeMapper = JoinedRoomApiTypeMapper(dateApiTypeMapper)
        val userApiTypeMapper = UserApiTypeMapper(dateApiTypeMapper)
        val dispatcher by memoized { mockk<Dispatcher>(relaxed = true) }
        val testState = State.initial().with(
            ChatState.initial().with(
                JoinedRoomsState(
                    rooms = mapOf(
                        "id1" to joinedRoomApiTypeMapper.toRoomInternalType(roomApiTypeOne),
                        "id2" to joinedRoomApiTypeMapper.toRoomInternalType(roomApiTypeTwo)
                    ),
                    unreadCounts = mapOf(
                        "id1" to 1,
                        "id2" to 2
                    )
                )
            )
        )
        val differ = JoinedRoomsStateDiffer { testState }
        val userSubscriptionDispatcher by memoized {
            UserSubscriptionDispatcher(
                joinedRoomApiTypeMapper = joinedRoomApiTypeMapper,
                joinedRoomsStateDiffer = differ,
                dispatcher = dispatcher,
                userApiTypeMapper = userApiTypeMapper
            )
        }

        describe("when a second InitialState event is received with several changes") {
            val event = UserSubscriptionEvent.InitialState(
                currentUser = simpleUser,
                rooms = listOf(
                    roomApiTypeOneUpdated,
                    roomApiTypeThree
                ),
                readStates = listOf(
                    RoomReadStateApiType("id1", 4, null),
                    RoomReadStateApiType("id3", 3, null)
                ),
                memberships = listOf(
                    RoomMembershipApiType("id1", listOf()),
                    RoomMembershipApiType("id3", listOf())
                )
            )

            beforeEachTest {
                userSubscriptionDispatcher.onEvent(event)
            }

            it("then the correct actions will be dispatched") {
                verify(exactly = 1) {
                    dispatcher(CurrentUserReceived(userApiTypeMapper.toUserInternalType(simpleUser)))
                    dispatcher(CurrentUserReceived(currentUser = userApiTypeMapper.toUserInternalType(simpleUser)))
                    dispatcher(RoomUpdated(
                        room = joinedRoomApiTypeMapper.toRoomInternalType(roomApiTypeOneUpdated)
                    ))
                    dispatcher(LeftRoom(roomId = "id2"))
                    dispatcher(ReconnectJoinedRoom(
                        room = joinedRoomApiTypeMapper.toRoomInternalType(roomApiTypeThree),
                        unreadCount = 3
                    ))
                }

                confirmVerified(dispatcher)
            }
        }
    }
})
