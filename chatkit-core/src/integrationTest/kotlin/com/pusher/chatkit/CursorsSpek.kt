package com.pusher.chatkit

import com.google.common.truth.Truth
import com.google.common.truth.Truth.*
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
import com.pusher.platform.network.wait
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

class CursorsSpek : Spek({

    afterEachTest(InstanceSupervisor::tearDownInstance)
    afterEachTest(::closeChatManagers)

    describe("ChatManager with cursors") {

        it("notifies when user reads messages") {
            setUpInstanceWith(newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            var receivedCursor by FutureValue<Cursor>()
            val pusherino = chatFor(PUSHERINO).connect().wait().assumeSuccess()
            val alice = chatFor(ALICE).connect().wait().assumeSuccess()

            val messageId = pusherino.sendMessage(pusherino.generalRoom, "Hey there").wait().assumeSuccess()

            pusherino.subscribeToRoom(pusherino.generalRoom) { event ->
                if (event is RoomSubscriptionEvent.NewReadCursor) receivedCursor = event.cursor
            }

            alice.setReadCursor(alice.generalRoom, messageId).wait().assumeSuccess()

            val cursor = receivedCursor
            assertThat(cursor.position).isEqualTo(messageId)
            assertThat(cursor.userId).isEqualTo(ALICE)
            assertThat(cursor.roomId).isEqualTo(pusherino.generalRoom.id)
        }

    }

})
