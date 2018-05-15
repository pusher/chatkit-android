package com.pusher.chatkit

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.Users.ALICE
import com.pusher.chatkit.Users.PUSHERINO
import com.pusher.chatkit.files.AttachmentType.IMAGE
import com.pusher.chatkit.files.DataAttachment
import com.pusher.chatkit.files.LinkAttachment
import com.pusher.chatkit.messages.Direction
import com.pusher.chatkit.messages.Message
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

            val sentMessages = (0..4).map { "message $it" }

            alice.generalRoom.let { room ->
                sentMessages.forEach { message ->
                    alice.sendMessage(room, message).wait().assumeSuccess()
                }
            }

            val messages = pusherino.fetchMessages(pusherino.generalRoom.id).wait().assumeSuccess()

            assertThat(messages.map { it.text }).containsAllIn(sentMessages)
        }

        it("retrieves old messages reversed") {
            setUpInstanceWith(newUsers(PUSHERINO, ALICE), newRoom(Rooms.GENERAL, PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().wait().assumeSuccess()
            val alice = chatFor(ALICE).connect().wait().assumeSuccess()

            val sentMessages = (0..4).map { "message $it" }

            alice.generalRoom.let { room ->
                sentMessages.forEach { message ->
                    alice.sendMessage(room, message).wait().assumeSuccess()
                }
            }

            val messages = pusherino.fetchMessages(
                roomId = pusherino.generalRoom.id,
                direction = Direction.NEWER_FIRST
            ).wait().assumeSuccess()

            assertThat(messages).isOrdered( Comparator { a: Message, b: Message -> a.createdAt.compareTo(b.createdAt) } )
        }

        it("sends message with attachment") {
            setUpInstanceWith(newUsers(PUSHERINO, ALICE), newRoom(Rooms.GENERAL, PUSHERINO, ALICE))

            val classLoader = javaClass.classLoader
            val file = File(classLoader.getResource("bill_murray.jpg").file)

            val pusherino = chatFor(PUSHERINO).connect().wait().assumeSuccess()
            val alice = chatFor(ALICE).connect().wait().assumeSuccess()

            alice.sendMessage(
                room = alice.generalRoom,
                messageText = "Cats and dogs, living together",
                attachment = DataAttachment(file)
            ).wait().assumeSuccess()

            val (firstMessage) = pusherino.fetchMessages(pusherino.generalRoom.id).wait().assumeSuccess()

            val fetchedAttachment = alice.fetchAttachment(firstMessage.attachment!!.link).wait().assumeSuccess()

            assertThat(fetchedAttachment.file).isNotNull()
        }

        it("sends message with link attachment") {
            setUpInstanceWith(newUsers(PUSHERINO, ALICE), newRoom(Rooms.GENERAL, PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().wait().assumeSuccess()
            val alice = chatFor(ALICE).connect().wait().assumeSuccess()

            alice.sendMessage(
                room = alice.generalRoom,
                messageText = "Cats and dogs, living together",
                attachment = LinkAttachment("https://www.fillmurray.com/284/196", IMAGE)
            ).wait().assumeSuccess()

            val (firstMessage) = pusherino.fetchMessages(pusherino.generalRoom.id).wait().assumeSuccess()

            assertThat(firstMessage.attachment?.link).isNotNull()
        }

    }

})
