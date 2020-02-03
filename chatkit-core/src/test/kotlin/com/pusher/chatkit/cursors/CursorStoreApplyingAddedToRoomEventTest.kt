package com.pusher.chatkit.cursors

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.pusher.chatkit.rooms.api.RoomReadStateApiType
import com.pusher.chatkit.users.UserInternalEvent
import com.pusher.chatkit.users.UserSubscriptionEvent
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object CursorStoreApplyingAddedToRoomEventTest : Spek({

    val subject by memoized { CursorStore() }

    describe("given empty CursorStore") {
        describe("when applying AddedToRoomEvent with no Cursor") {
            lateinit var applyEventResult: List<UserInternalEvent>
            beforeEachTest {
                applyEventResult = subject.applyEvent(UserSubscriptionEvent.AddedToRoomEvent(
                        room = mock(), // should not be used by CursorStore
                        readState = RoomReadStateApiType(
                                roomId = "roomId1",
                                unreadCount = 2,
                                cursor = null
                        ),
                        membership = mock() // should not be used by CursorStore
                ))
            }

            it("then the result indicates that nothing has been applied") {
                assertThat(applyEventResult).isEmpty()
            }
        }
        describe("when applying AddedToRoomEvent with a Cursor") {
            val cursor = Cursor("alice", "roomId1", position = 1)

            lateinit var applyEventResult: List<UserInternalEvent>
            beforeEachTest {
                applyEventResult = subject.applyEvent(UserSubscriptionEvent.AddedToRoomEvent(
                        room = mock(), // should not be used by CursorStore
                        readState = RoomReadStateApiType(
                                roomId = "roomId1",
                                unreadCount = 2,
                                cursor = cursor
                        ),
                        membership = mock() // should not be used by CursorStore
                ))
            }

            it("then the result indicates that the new cursor has been stored") {
                assertThat(applyEventResult).containsExactly(UserInternalEvent.NewCursor(cursor))
            }
            it("then the store contains the new cursor") {
                assertThat(subject[cursor.userId][cursor.roomId]).isEqualTo(cursor)
            }
        }
    }

    describe("given CursorStore with a Cursor") {
        val initialCursor = Cursor("alice", "roomId1", position = 1)
        beforeEachTest { subject.initialiseContents(listOf(initialCursor)) }

        describe("when applying AddedToRoomEvent with the same Cursor") {

            lateinit var applyEventResult: List<UserInternalEvent>
            beforeEachTest {
                applyEventResult = subject.applyEvent(UserSubscriptionEvent.AddedToRoomEvent(
                        room = mock(), // should not be used by CursorStore
                        readState = RoomReadStateApiType(
                                roomId = "roomId1",
                                unreadCount = 2,
                                cursor = initialCursor
                        ),
                        membership = mock() // should not be used by CursorStore
                ))
            }

            it("then the result indicates that nothing has been applied") {
                assertThat(applyEventResult).isEmpty()
            }
            it("then the store contains the cursor") {
                assertThat(subject[initialCursor.userId][initialCursor.roomId])
                        .isEqualTo(initialCursor)
            }
        }
        describe("when applying AddedToRoomEvent with a new cursor") {
            val cursor = Cursor("alice", "roomId1", position = 2)

            lateinit var applyEventResult: List<UserInternalEvent>
            beforeEachTest {
                applyEventResult = subject.applyEvent(UserSubscriptionEvent.AddedToRoomEvent(
                        room = mock(), // should not be used by CursorStore
                        readState = RoomReadStateApiType(
                                roomId = "roomId1",
                                unreadCount = 2,
                                cursor = cursor
                        ),
                        membership = mock() // should not be used by CursorStore
                ))
            }

            it("then the result indicates that the new cursor has been stored") {
                assertThat(applyEventResult).containsExactly(UserInternalEvent.NewCursor(cursor))
            }
            it("then the store contains the new cursor") {
                assertThat(subject[cursor.userId][cursor.roomId]).isEqualTo(cursor)
            }
        }
        describe("when applying AddedToRoomEvent for another room with no Cursor") {
            lateinit var applyEventResult: List<UserInternalEvent>
            beforeEachTest {
                applyEventResult = subject.applyEvent(UserSubscriptionEvent.AddedToRoomEvent(
                        room = mock(), // should not be used by CursorStore
                        readState = RoomReadStateApiType(
                                roomId = "roomId1",
                                unreadCount = 2,
                                cursor = null
                        ),
                        membership = mock() // should not be used by CursorStore
                ))
            }

            it("then the result indicates that nothing has been applied") {
                assertThat(applyEventResult).isEmpty()
            }
            it("then the store contains the initial cursor") {
                assertThat(subject[initialCursor.userId][initialCursor.roomId])
                        .isEqualTo(initialCursor)
            }
        }
        describe("when applying AddedToRoomEvent for another room with a Cursor") {
            val cursor = Cursor("alice", "roomId2", position = 2)

            lateinit var applyEventResult: List<UserInternalEvent>
            beforeEachTest {
                applyEventResult = subject.applyEvent(UserSubscriptionEvent.AddedToRoomEvent(
                        room = mock(), // should not be used by CursorStore
                        readState = RoomReadStateApiType(
                                roomId = "roomId1",
                                unreadCount = 2,
                                cursor = cursor
                        ),
                        membership = mock() // should not be used by CursorStore
                ))
            }

            it("then the result indicates that the new cursor has been stored") {
                assertThat(applyEventResult).containsExactly(UserInternalEvent.NewCursor(cursor))
            }
            it("then the store contains the new cursor") {
                assertThat(subject[cursor.userId][cursor.roomId]).isEqualTo(cursor)
            }
            it("then the store contains the initial cursor") {
                assertThat(subject[initialCursor.userId][initialCursor.roomId])
                        .isEqualTo(initialCursor)
            }
        }
    }

})