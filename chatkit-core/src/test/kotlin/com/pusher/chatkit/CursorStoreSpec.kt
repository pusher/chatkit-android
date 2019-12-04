package com.pusher.chatkit

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.cursors.Cursor
import com.pusher.chatkit.cursors.CursorSubscriptionEvent
import com.pusher.chatkit.cursors.CursorsStore
import com.pusher.chatkit.users.ReadStateApiType
import com.pusher.chatkit.users.User
import com.pusher.chatkit.users.UserSubscriptionEvent
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

class CursorStoreSpec : Spek({
    given("cursor store") {
        val subject = CursorsStore()

        subject.initialiseContents(
                listOf(
                        Cursor("callum", "1", 1, "2017-11-29T16:59:58Z"),
                        Cursor("mike", "1", 2, "2017-11-29T16:59:58Z")
                )
        )

        on("on receiving InitialState events from room subscription") {
            val events = subject.applyEvent(CursorSubscriptionEvent.InitialState(
                    listOf(
                            Cursor("callum", "1", 1, "2017-11-29T16:59:58Z"),
                            Cursor("mike", "1", 3, "2017-11-29T16:59:59Z"),
                            Cursor("viv", "1", 3, "2017-11-29T16:59:59Z")
                    )
            ))

            it("should emit events describing the difference in state") {
                assertThat(events).containsExactly(
                        CursorSubscriptionEvent.OnCursorSet(Cursor("mike", "1", 3, "2017-11-29T16:59:59Z")),
                        CursorSubscriptionEvent.OnCursorSet(Cursor("viv", "1", 3, "2017-11-29T16:59:59Z"))
                )
            }

            it("should update the contents of the store") {
                assertThat(subject["callum"]["1"]).isEqualTo(Cursor("callum", "1", 1, "2017-11-29T16:59:58Z"))
                assertThat(subject["mike"]["1"]).isEqualTo(Cursor("mike", "1", 3, "2017-11-29T16:59:59Z"))
                assertThat(subject["viv"]["1"]).isEqualTo(Cursor("viv", "1", 3, "2017-11-29T16:59:59Z"))
            }
        }
    }

    given("another cursor store") {
        val subject = CursorsStore()

        val constantRoom1Cursor = Cursor("callum", "1", 1, "2017-11-29T16:59:58Z")
        val initialRoom2Cursor = Cursor("callum", "2", 2, "2017-11-29T16:59:58Z")

        subject.initialiseContents(listOf(
                constantRoom1Cursor,
                initialRoom2Cursor
        ))

        on("receiving subsequent InitialState events from user subscription") {

            val newRoom2Cursor = Cursor("callum", "2", 3, "2017-11-29T16:59:59Z")
            val newRoom3Cursor = Cursor("callum", "3", 4, "2017-11-29T16:59:59Z")

            val events = subject.applyEvent(UserSubscriptionEvent.InitialState(
                    readStates = listOf(
                            ReadStateApiType("1", 0, constantRoom1Cursor),
                            ReadStateApiType("2", 0, newRoom2Cursor),
                            ReadStateApiType("3", 0, newRoom3Cursor)
                    ),
                    currentUser = User("callum", "", "", null, null, null),
                    _rooms = listOf(), // normally would be there but not needed for this test
                    memberships = listOf() // ditto
            ))

            it("should emit events describing the difference in state") {
                assertThat(events).containsExactly(
                        UserSubscriptionEvent.ReadStateUpdatedEvent(
                                ReadStateApiType("2", 0, newRoom2Cursor)),
                        UserSubscriptionEvent.ReadStateUpdatedEvent(
                                ReadStateApiType("3", 0, newRoom3Cursor))
                )
            }

            it("should update the contents of the store") {
                assertThat(subject["callum"]["1"]).isEqualTo(constantRoom1Cursor)
                assertThat(subject["callum"]["2"]).isEqualTo(newRoom2Cursor)
                assertThat(subject["callum"]["3"]).isEqualTo(newRoom3Cursor)
            }
        }
    }
})
