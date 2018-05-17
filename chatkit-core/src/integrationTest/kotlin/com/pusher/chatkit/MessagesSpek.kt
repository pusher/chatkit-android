package com.pusher.chatkit

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.Rooms.GENERAL
import com.pusher.chatkit.Users.ALICE
import com.pusher.chatkit.Users.PUSHERINO
import com.pusher.chatkit.files.AttachmentType.IMAGE
import com.pusher.chatkit.files.DataAttachment
import com.pusher.chatkit.files.LinkAttachment
import com.pusher.chatkit.messages.Direction
import com.pusher.chatkit.messages.Message
import com.pusher.chatkit.rooms.RoomSubscriptionEvent
import com.pusher.chatkit.test.FutureValue
import com.pusher.chatkit.test.InstanceActions.newRoom
import com.pusher.chatkit.test.InstanceActions.newUsers
import com.pusher.chatkit.test.InstanceSupervisor.setUpInstanceWith
import com.pusher.chatkit.test.InstanceSupervisor.tearDownInstance
import com.pusher.platform.network.wait
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.xit
import java.io.File

class MessagesSpek : Spek({

    afterEachTest(::tearDownInstance)
    afterEachTest(::closeChatManagers)

    describe("Messages for Chatkit") {

        it("retrieves old messages") {
            setUpInstanceWith(newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

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
            setUpInstanceWith(newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

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
            setUpInstanceWith(newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().wait().assumeSuccess()
            val alice = chatFor(ALICE).connect().wait().assumeSuccess()

            alice.sendMessage(
                room = alice.generalRoom,
                messageText = "Cats and dogs, living together",
                attachment = DataAttachment(billMurray)
            ).wait().assumeSuccess()

            val (firstMessage) = pusherino.fetchMessages(pusherino.generalRoom.id).wait().assumeSuccess()

            val fetchedAttachment = alice.fetchAttachment(firstMessage.attachment!!.link).wait().assumeSuccess()

            assertThat(fetchedAttachment.file).isNotNull()
        }

        it("sends message with link attachment") {
            setUpInstanceWith(newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

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

        it("receives message sent") {
            setUpInstanceWith(newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().wait().assumeSuccess()
            val alice = chatFor(ALICE).connect().wait().assumeSuccess()

            var receivedMessage by FutureValue<Message>()

            pusherino.subscribeToRoom(pusherino.generalRoom) { event ->
                when (event) {
                    is RoomSubscriptionEvent.NewMessage -> receivedMessage = event.message
                }
            }

            alice.sendMessage(
                room = alice.generalRoom,
                messageText = "Cats and dogs, living together"
            ).wait().assumeSuccess()

            with(receivedMessage) {
                assertThat(text).isEqualTo("Cats and dogs, living together")
                assertThat(roomId).isEqualTo(alice.generalRoom.id)
                assertThat(attachment).isNull()
            }
        }

        it("receives message with link attachment") {
            setUpInstanceWith(newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().wait().assumeSuccess()
            val alice = chatFor(ALICE).connect().wait().assumeSuccess()

            var receivedMessage by FutureValue<Message>()

            pusherino.subscribeToRoom(pusherino.generalRoom) { event ->
                when (event) {
                    is RoomSubscriptionEvent.NewMessage -> receivedMessage = event.message
                }
            }

            alice.sendMessage(
                room = alice.generalRoom,
                messageText = "Cats and dogs, living together",
                attachment = LinkAttachment("https://www.fillmurray.com/284/196", IMAGE)
            ).wait().assumeSuccess()

            with(receivedMessage) {
                assertThat(text).isEqualTo("Cats and dogs, living together")
                assertThat(roomId).isEqualTo(pusherino.generalRoom.id)
                assertThat(attachment).isNotNull()
                assertThat(attachment?.link).isEqualTo("https://www.fillmurray.com/284/196")
                assertThat(attachment?.type).isEqualTo("image")
            }
        }

        it("receives message with file attachment") {
            setUpInstanceWith(newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().wait().assumeSuccess()
            val alice = chatFor(ALICE).connect().wait().assumeSuccess()

            var receivedMessage by FutureValue<Message>()

            pusherino.subscribeToRoom(pusherino.generalRoom) { event ->
                when (event) {
                    is RoomSubscriptionEvent.NewMessage -> receivedMessage = event.message
                }
            }

            alice.sendMessage(
                room = alice.generalRoom,
                messageText = "Cats and dogs, living together",
                attachment = DataAttachment(billMurray)
            ).wait().assumeSuccess()

            with(receivedMessage) {
                assertThat(text).isEqualTo("Cats and dogs, living together")
                assertThat(roomId).isEqualTo(pusherino.generalRoom.id)
                assertThat(attachment).isNotNull()
                assertThat(attachment?.link).isNotNull()
                assertThat(attachment?.type).isEqualTo("image")
            }
        }

    }

})


val billMurray = File(MessagesSpek::class.java.classLoader.getResource("bill_murray.jpg").file)
