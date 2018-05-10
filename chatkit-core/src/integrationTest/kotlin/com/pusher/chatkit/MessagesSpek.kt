package com.pusher.chatkit

import com.google.common.truth.Truth.*
import com.pusher.chatkit.Users.ALICE
import com.pusher.chatkit.Users.PUSHERINO
import com.pusher.chatkit.files.DataAttachment
import com.pusher.chatkit.test.InstanceActions.newRoom
import com.pusher.chatkit.test.InstanceActions.newUsers
import com.pusher.chatkit.test.InstanceSupervisor.setUpInstanceWith
import com.pusher.chatkit.test.InstanceSupervisor.tearDownInstance
import com.pusher.platform.network.wait
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import java.io.File

class MessagesSpek : Spek({


    afterEachTest(::tearDownInstance)
    afterEachTest(::closeChatManagers)

    describe("Messages for Chatkit") {

        it("retrieves old messages") {
            setUpInstanceWith(newUsers(PUSHERINO, ALICE), newRoom(Rooms.GENERAL, PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().wait().assumeSuccess()
            val alice = chatFor(ALICE).connect().wait().assumeSuccess()

            val sentMessages = (0..9).map { "message $it" }

            alice.generalRoom.let { room ->
                sentMessages.forEach { message ->
                    alice.sendMessage(room, message).wait().assumeSuccess()
                }
            }

            val messages = pusherino.fetchMessages(pusherino.generalRoom.id).wait().assumeSuccess()

            assertThat(messages.map { it.text }).containsAllIn(sentMessages)
        }

        it("sends message with attachment") {
            setUpInstanceWith(newUsers(PUSHERINO, ALICE), newRoom(Rooms.GENERAL, PUSHERINO, ALICE))

            val classLoader = javaClass.classLoader
            val file = File(classLoader.getResource("bill_murray.jpg").file)

            val pusherino = chatFor(PUSHERINO).connect().wait().assumeSuccess()
            val alice = chatFor(ALICE).connect().wait().assumeSuccess()

            alice.sendMessage(
                room = alice.generalRoom,
                messageText = "Cats and dogs, libing together",
                attachment = DataAttachment(file)
            ).wait().assumeSuccess()

            val messages = pusherino.fetchMessages(pusherino.generalRoom.id).wait().assumeSuccess()

            assertThat(messages[0].attachment).isNotNull()
        }

    }

})
