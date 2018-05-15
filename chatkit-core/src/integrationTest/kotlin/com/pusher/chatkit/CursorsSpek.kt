package com.pusher.chatkit

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.Rooms.GENERAL
import com.pusher.chatkit.Users.ALICE
import com.pusher.chatkit.Users.PUSHERINO
import com.pusher.chatkit.cursors.Cursor
import com.pusher.chatkit.rooms.RoomSubscriptionEvent
import com.pusher.chatkit.test.FutureValue
import com.pusher.chatkit.test.InstanceActions.newRoom
import com.pusher.chatkit.test.InstanceActions.newUsers
import com.pusher.chatkit.test.InstanceSupervisor
import com.pusher.chatkit.test.InstanceSupervisor.setUpInstanceWith
import com.pusher.platform.network.Wait
import com.pusher.platform.network.wait
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

class CursorsSpek : Spek({

    afterEachTest(InstanceSupervisor::tearDownInstance)
    afterEachTest(::closeChatManagers)

    describe("ChatManager with cursors") {

        it("notifies when '$ALICE' reads messages") {
            setUpInstanceWith(newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().wait().assumeSuccess()
            val alice = chatFor(ALICE).connect().wait().assumeSuccess()

            val messageId = pusherino.sendMessage(pusherino.generalRoom, "Hey there").wait().assumeSuccess()

            var initialState by FutureValue<Any?>(Wait.ForEver)
            var receivedCursor by FutureValue<Cursor>(Wait.ForEver)

            pusherino.subscribeToRoom(pusherino.generalRoom, messageLimit = 0) { event ->
                when (event) {
                    is RoomSubscriptionEvent.InitialReadCursors -> initialState = event
                    is RoomSubscriptionEvent.NewReadCursor -> receivedCursor = event.cursor
                }
            }

            checkNotNull(initialState)

            alice.setReadCursor(alice.generalRoom, messageId).wait().assumeSuccess()

            receivedCursor.apply {
                assertThat(position).isEqualTo(messageId)
                assertThat(userId).isEqualTo(ALICE)
                assertThat(roomId).isEqualTo(pusherino.generalRoom.id)
            }
        }

        it("should avoid multiple setReadCursor calls") {
            setUpInstanceWith(newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val cursorsReceived = mutableListOf<Cursor>()
            val pusherino = chatFor(PUSHERINO).connect().wait().assumeSuccess()
            val alice = chatFor(ALICE).connect().wait().assumeSuccess()

            val firstMessageId = pusherino.sendMessage(pusherino.generalRoom, "Hey there").wait().assumeSuccess()
            val secondMessageId = pusherino.sendMessage(pusherino.generalRoom, "How are you doing?").wait().assumeSuccess()

            var initialState by FutureValue<Any?>(Wait.ForEver)
            var secondMessageCursor by FutureValue<Cursor>()

            pusherino.subscribeToRoom(pusherino.generalRoom) { event ->
                when (event) {
                    is RoomSubscriptionEvent.InitialReadCursors -> initialState = event
                    is RoomSubscriptionEvent.NewReadCursor -> {
                        cursorsReceived += event.cursor
                        if (event.cursor.position == secondMessageId) secondMessageCursor = event.cursor
                    }
                }
            }

            checkNotNull(initialState)

            alice.setReadCursor(alice.generalRoom, firstMessageId)
            alice.setReadCursor(alice.generalRoom, secondMessageId).wait()

            assertThat(cursorsReceived).containsExactly(secondMessageCursor)
        }

    }

})
