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
import com.pusher.chatkit.rooms.RoomEvent
import com.pusher.chatkit.test.FutureValue
import com.pusher.chatkit.test.InstanceActions.createDefaultRole
import com.pusher.chatkit.test.InstanceActions.newRoom
import com.pusher.chatkit.test.InstanceActions.newUsers
import com.pusher.chatkit.test.InstanceSupervisor.setUpInstanceWith
import com.pusher.chatkit.test.InstanceSupervisor.tearDownInstance
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import java.io.File

class MessagesSpek : Spek({
    beforeEachTest(::tearDownInstance)
    afterEachTest(::tearDownInstance)
    afterEachTest(::closeChatManagers)

    describe("Messages for Chatkit") {

        it("retrieves old messages") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()
            val alice = chatFor(ALICE).connect().assumeSuccess()

            val sentMessages = (0..4).map { "message $it" }

            alice.generalRoom.let { room ->
                for (message in sentMessages) {
                    alice.sendMessage(room, message).assumeSuccess()
                }
            }

            val messages = pusherino.fetchMessages(pusherino.generalRoom.id).assumeSuccess()

            assertThat(messages.map { it.text }).containsAllIn(sentMessages)
        }

        it("retrieves old messages and they have the user set correctly") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()
            val alice = chatFor(ALICE).connect().assumeSuccess()

            alice.generalRoom.let { room ->
                alice.sendMessage(room, "testing some stuff").assumeSuccess()
            }

            val messages = pusherino.fetchMessages(pusherino.generalRoom.id).assumeSuccess()

            assertThat(messages[0].user?.id).isEqualTo(alice.id)
        }

        it("retrieves messages with attachments and sets fetchRequired to true if appropriate") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()
            val alice = chatFor(ALICE).connect().assumeSuccess()

            alice.generalRoom.let { room ->
                alice.sendMessage(room, "message without attachment").assumeSuccess()
                alice.sendMessage(
                        room,
                        "message with no Chatkit attachment",
                        LinkAttachment("https://www.fillmurray.com/284/196", IMAGE)
                ).assumeSuccess()
                alice.sendMessage(
                        room,
                        "message with Chatkit attachment",
                        DataAttachment(billMurray)
                ).assumeSuccess()
            }

            val messages = pusherino.fetchMessages(pusherino.generalRoom.id).assumeSuccess()

            assertThat(messages[0].attachment?.fetchRequired).isTrue()
            assertThat(messages[1].attachment?.fetchRequired).isFalse()
            assertThat(messages[2].attachment).isNull()
        }

        it("retrieves old messages reversed") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()
            val alice = chatFor(ALICE).connect().assumeSuccess()

            val sentMessages = (0..4).map { "message $it" }

            alice.generalRoom.let { room ->
                for (message in sentMessages) {
                    alice.sendMessage(room, message).assumeSuccess()
                }
            }

            val messages = pusherino.fetchMessages(
                roomId = pusherino.generalRoom.id,
                direction = Direction.NEWER_FIRST
            ).assumeSuccess()

            assertThat(messages).isOrdered( Comparator { a: Message, b: Message -> a.createdAt.compareTo(b.createdAt) } )
        }

        it("sends message with attachment") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()
            val alice = chatFor(ALICE).connect().assumeSuccess()

            alice.sendMessage(
                room = alice.generalRoom,
                messageText = "Cats and dogs, living together",
                attachment = DataAttachment(billMurray)
            ).assumeSuccess()

            val (firstMessage) = pusherino.fetchMessages(pusherino.generalRoom.id).assumeSuccess()

            val fetchedAttachment = alice.fetchAttachment(firstMessage.attachment!!.link).assumeSuccess()

            assertThat(fetchedAttachment.file).isNotNull()
        }

        it("sends message with link attachment") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()
            val alice = chatFor(ALICE).connect().assumeSuccess()

            alice.sendMessage(
                room = alice.generalRoom,
                messageText = "Cats and dogs, living together",
                attachment = LinkAttachment("https://www.fillmurray.com/284/196", IMAGE)
            ).assumeSuccess()

            val (firstMessage) = pusherino.fetchMessages(pusherino.generalRoom.id).assumeSuccess()

            assertThat(firstMessage.attachment?.link).isNotNull()
        }

        it("receives message sent") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()
            val alice = chatFor(ALICE).connect().assumeSuccess()

            var receivedMessage by FutureValue<Message>()

            pusherino.subscribeToRoom(pusherino.generalRoom) { event ->
                when (event) {
                    is RoomEvent.NewMessage -> receivedMessage = event.message
                }
            }

            alice.sendMessage(
                room = alice.generalRoom,
                messageText = "Cats and dogs, living together"
            ).assumeSuccess()

            with(receivedMessage) {
                assertThat(text).isEqualTo("Cats and dogs, living together")
                assertThat(roomId).isEqualTo(alice.generalRoom.id)
                assertThat(attachment).isNull()
                assertThat(user?.id).isEqualTo(alice.id)
            }
        }

        it("receives message with link attachment") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()
            val alice = chatFor(ALICE).connect().assumeSuccess()

            var receivedMessage by FutureValue<Message>()

            pusherino.subscribeToRoom(pusherino.generalRoom) { event ->
                when (event) {
                    is RoomEvent.NewMessage -> receivedMessage = event.message
                }
            }

            alice.sendMessage(
                room = alice.generalRoom,
                messageText = "Cats and dogs, living together",
                attachment = LinkAttachment("https://www.fillmurray.com/284/196", IMAGE)
            ).assumeSuccess()

            with(receivedMessage) {
                assertThat(text).isEqualTo("Cats and dogs, living together")
                assertThat(roomId).isEqualTo(pusherino.generalRoom.id)
                assertThat(attachment).isNotNull()
                assertThat(attachment?.link).isEqualTo("https://www.fillmurray.com/284/196")
                assertThat(attachment?.type).isEqualTo("image")
            }
        }

        it("receives message with file attachment") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()
            val alice = chatFor(ALICE).connect().assumeSuccess()

            var receivedMessage by FutureValue<Message>()

            pusherino.subscribeToRoom(pusherino.generalRoom) { event ->
                when (event) {
                    is RoomEvent.NewMessage -> receivedMessage = event.message
                }
            }

            alice.sendMessage(
                room = alice.generalRoom,
                messageText = "Cats and dogs, living together",
                attachment = DataAttachment(billMurray)
            ).assumeSuccess()

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
