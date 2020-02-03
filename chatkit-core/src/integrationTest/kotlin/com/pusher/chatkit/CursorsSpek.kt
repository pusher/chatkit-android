package com.pusher.chatkit

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.Rooms.GENERAL
import com.pusher.chatkit.Users.ALICE
import com.pusher.chatkit.Users.PUSHERINO
import com.pusher.chatkit.cursors.Cursor
import com.pusher.chatkit.rooms.RoomEvent
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
    afterEachTest(::closeChatManagers)

    describe("SynchronousChatManager with cursors") {

        it("notifies when '$ALICE' reads messages") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()
            val alice = chatFor(ALICE).connect().assumeSuccess()

            val messageId = pusherino.sendMessage(pusherino.generalRoom, "Hey there").assumeSuccess()

            val receivedCursor by pusherino
                    .subscribeRoomFor(GENERAL) { it as? RoomEvent.NewReadCursor }

            alice.setReadCursor(alice.generalRoom, messageId).wait()

            receivedCursor.cursor.apply {
                assertThat(position).isEqualTo(messageId)
                assertThat(userId).isEqualTo(ALICE)
                assertThat(roomId).isEqualTo(pusherino.generalRoom.id)
            }
        }

        it("should avoid multiple setReadCursor calls") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val cursorsReceived = mutableListOf<RoomEvent.NewReadCursor>()
            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()
            val alice = chatFor(ALICE).connect().assumeSuccess()

            val firstMessageId = pusherino.sendMessage(pusherino.generalRoom, "Hey there").assumeSuccess()
            val secondMessageId = pusherino.sendMessage(pusherino.generalRoom, "How are you doing?").assumeSuccess()

            val secondMessageCursor by pusherino
                    .subscribeRoomFor(GENERAL) {
                        (it as? RoomEvent.NewReadCursor)?.takeIf { event ->
                            cursorsReceived += event
                            event.cursor.position == secondMessageId
                        }
                    }

            alice.setReadCursor(alice.generalRoom, firstMessageId)
            alice.setReadCursor(alice.generalRoom, secondMessageId).wait().assumeSuccess()

            assertThat(cursorsReceived).containsExactly(secondMessageCursor)
        }

        it("should report cursor after timeout") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val cursorsReceived = mutableListOf<Cursor>()
            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()
            val alice = chatFor(ALICE).connect().assumeSuccess()

            val firstMessageId = pusherino.sendMessage(pusherino.generalRoom, "Hey there").assumeSuccess()
            val secondMessageId = pusherino.sendMessage(pusherino.generalRoom, "How are you doing?").assumeSuccess()
            val thirdMessageId = pusherino.sendMessage(pusherino.generalRoom, "Are you there?").assumeSuccess()

            val thirdMessageCursor by pusherino
                    .subscribeRoomFor(GENERAL) {
                        (it as? RoomEvent.NewReadCursor)?.takeIf { event ->
                            cursorsReceived += event.cursor
                            event.cursor.position == thirdMessageId
                        }
                    }

            alice.setReadCursor(alice.generalRoom, firstMessageId)
            alice.setReadCursor(alice.generalRoom, secondMessageId)
            Thread.sleep(550) // TODO: fix correctly without sleep (takes longer and still flaky)
            alice.setReadCursor(alice.generalRoom, thirdMessageId).wait().assumeSuccess()

            checkNotNull(thirdMessageCursor)

            assertThat(cursorsReceived.map { it.position }).containsExactly(secondMessageId, thirdMessageId)
        }

        it("should read $ALICE's cursor") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()
            val alice = chatFor(ALICE).connect().assumeSuccess()

            val messageId = pusherino.sendMessage(pusherino.generalRoom, "Hey there").assumeSuccess()

            alice.setReadCursor(alice.generalRoom, messageId).wait().assumeSuccess()

            val cursor = alice.getReadCursor(alice.generalRoom).assumeSuccess()

            assertThat(cursor.position).isEqualTo(messageId)
        }
    }
})
