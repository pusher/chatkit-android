package com.pusher.chatkit

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.Rooms.GENERAL
import com.pusher.chatkit.Users.ALICE
import com.pusher.chatkit.Users.PUSHERINO
import com.pusher.chatkit.cursors.Cursor
import com.pusher.chatkit.rooms.RoomSubscriptionEvent
import com.pusher.chatkit.test.InstanceActions.createDefaultRole
import com.pusher.chatkit.test.InstanceActions.newRoom
import com.pusher.chatkit.test.InstanceActions.newUsers
import com.pusher.chatkit.test.InstanceSupervisor
import com.pusher.chatkit.test.InstanceSupervisor.setUpInstanceWith
import com.pusher.platform.network.wait
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

class CursorsSpek : Spek({
    beforeEachTest(InstanceSupervisor::tearDownInstance)
    afterEachTest(InstanceSupervisor::tearDownInstance)
    afterEachTest(::closeChatManagers)

    describe("ChatManager with cursors") {

        it("notifies when '$ALICE' reads messages") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().wait().assumeSuccess()
            val alice = chatFor(ALICE).connect().wait().assumeSuccess()

            val messageId = pusherino.sendMessage(pusherino.generalRoom, "Hey there").wait().assumeSuccess()

            val receivedCursor by pusherino
                .subscribeRoomFor(GENERAL) { it as? RoomSubscriptionEvent.NewReadCursor }

            alice.setReadCursor(alice.generalRoom, messageId).wait().assumeSuccess()

            receivedCursor.cursor.apply {
                assertThat(position).isEqualTo(messageId)
                assertThat(userId).isEqualTo(ALICE)
                assertThat(roomId).isEqualTo(pusherino.generalRoom.id)
            }
        }

        it("should avoid multiple setReadCursor calls") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val cursorsReceived = mutableListOf<RoomSubscriptionEvent.NewReadCursor>()
            val pusherino = chatFor(PUSHERINO).connect().wait().assumeSuccess()
            val alice = chatFor(ALICE).connect().wait().assumeSuccess()

            val firstMessageId = pusherino.sendMessage(pusherino.generalRoom, "Hey there").wait().assumeSuccess()
            val secondMessageId = pusherino.sendMessage(pusherino.generalRoom, "How are you doing?").wait().assumeSuccess()

            val secondMessageCursor by pusherino
                .subscribeRoomFor(GENERAL) {
                    (it as? RoomSubscriptionEvent.NewReadCursor)?.takeIf { event ->
                        cursorsReceived += event
                        event.cursor.position == secondMessageId
                    }
                }

            alice.setReadCursor(alice.generalRoom, firstMessageId)
            alice.setReadCursor(alice.generalRoom, secondMessageId).wait()

            assertThat(cursorsReceived).containsExactly(secondMessageCursor)
        }

        it("should report cursor after timeout") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val cursorsReceived = mutableListOf<Cursor>()
            val pusherino = chatFor(PUSHERINO).connect().wait().assumeSuccess()
            val alice = chatFor(ALICE).connect().wait().assumeSuccess()

            val firstMessageId = pusherino.sendMessage(pusherino.generalRoom, "Hey there").wait().assumeSuccess()
            val secondMessageId = pusherino.sendMessage(pusherino.generalRoom, "How are you doing?").wait().assumeSuccess()
            val thirdMessageId = pusherino.sendMessage(pusherino.generalRoom, "Are you there?").wait().assumeSuccess()

            val thirdMessageCursor by pusherino
                .subscribeRoomFor(GENERAL) {
                    (it as? RoomSubscriptionEvent.NewReadCursor)?.takeIf { event ->
                        cursorsReceived += event.cursor
                        event.cursor.position == thirdMessageId
                    }
                }

                alice.setReadCursor(alice.generalRoom, firstMessageId)
                alice.setReadCursor(alice.generalRoom, secondMessageId).wait()
                Thread.sleep(500)
                alice.setReadCursor(alice.generalRoom, thirdMessageId).wait()

            checkNotNull(thirdMessageCursor)

            assertThat(cursorsReceived.map { it.position }).containsExactly(secondMessageId, thirdMessageId)
        }

        it("should read $ALICE's cursor") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().wait().assumeSuccess()
            val alice = chatFor(ALICE).connect().wait().assumeSuccess()

            val messageId = pusherino.sendMessage(pusherino.generalRoom, "Hey there").wait().assumeSuccess()

            alice.setReadCursor(alice.generalRoom, messageId).wait().assumeSuccess()

            val cursor = alice.getReadCursor(alice.generalRoom).wait().assumeSuccess()

            assertThat(cursor.position).isEqualTo(messageId)
        }

    }

})
