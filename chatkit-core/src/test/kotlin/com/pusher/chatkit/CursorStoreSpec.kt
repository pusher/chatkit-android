package com.pusher.chatkit

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.cursors.Cursor
import com.pusher.chatkit.cursors.CursorSubscriptionEvent
import com.pusher.chatkit.cursors.CursorsStore
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

        subject.initialiseContents(
                listOf(
                        Cursor("callum", "1", 1, "2017-11-29T16:59:58Z"),
                        Cursor("callum", "2", 2, "2017-11-29T16:59:58Z")
                )
        )

        on("receiving subsequent InitialState events from user subscription") {
            val events = subject.applyEvent(UserSubscriptionEvent.InitialState(
                    cursors = listOf(
                            Cursor("callum", "1", 1, "2017-11-29T16:59:58Z"),
                            Cursor("callum", "2", 3, "2017-11-29T16:59:59Z"),
                            Cursor("callum", "3", 4, "2017-11-29T16:59:59Z")
                    ),
                    currentUser = User("myuser", "", "", null, null, null),
                    rooms = listOf()

            ))

            it("should emit events describing the difference in state") {
                assertThat(events).containsExactly(
                        UserSubscriptionEvent.NewCursor(Cursor("callum", "2", 3, "2017-11-29T16:59:59Z")),
                        UserSubscriptionEvent.NewCursor(Cursor("callum", "3", 4, "2017-11-29T16:59:59Z"))
                )
            }

            it("should update the contents of the store") {
                assertThat(subject["callum"]["1"]).isEqualTo(Cursor("callum", "1", 1, "2017-11-29T16:59:58Z"))
                assertThat(subject["callum"]["2"]).isEqualTo(Cursor("callum", "2", 3, "2017-11-29T16:59:59Z"))
                assertThat(subject["callum"]["3"]).isEqualTo(Cursor("callum", "3", 4, "2017-11-29T16:59:59Z"))
            }
        }
    }
})
