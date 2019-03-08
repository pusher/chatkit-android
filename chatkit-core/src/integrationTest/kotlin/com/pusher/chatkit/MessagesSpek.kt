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
import com.pusher.chatkit.messages.multipart.NewPart
import com.pusher.chatkit.messages.multipart.PartType
import com.pusher.chatkit.messages.multipart.Payload
import com.pusher.chatkit.rooms.RoomEvent
import com.pusher.chatkit.test.InstanceActions.createDefaultRole
import com.pusher.chatkit.test.InstanceActions.newRoom
import com.pusher.chatkit.test.InstanceActions.newUsers
import com.pusher.chatkit.test.InstanceSupervisor.setUpInstanceWith
import com.pusher.chatkit.test.InstanceSupervisor.tearDownInstance
import com.pusher.chatkit.util.FutureValue
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URL
import java.util.*

class MessagesSpek : Spek({
    beforeEachTest(::tearDownInstance)
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

        it("retrieves messages with attachments (v2)") {
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
                        DataAttachment(billMurray, "billmurray.jpeg")
                ).assumeSuccess()
            }

            val messages = pusherino.fetchMessages(pusherino.generalRoom.id).assumeSuccess()

            assertThat(messages[0].attachment).isNotNull()
            assertThat(messages[1].attachment).isNotNull()
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

            assertThat(messages).isOrdered(Comparator { a: Message, b: Message -> a.createdAt.compareTo(b.createdAt) })
        }

        it("sends (v2) message with attachment") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()
            val alice = chatFor(ALICE).connect().assumeSuccess()

            alice.sendMessage(
                    room = alice.generalRoom,
                    messageText = "Dogs and cats, living together",
                    attachment = DataAttachment(billMurray, "billmurray.jpeg")
            ).assumeSuccess()

            val (firstMessage) = pusherino.fetchMessages(pusherino.generalRoom.id).assumeSuccess()

            assertThat(firstMessage.attachment).isNotNull()
            with(firstMessage.attachment!!) {
                assertThat(type).isEqualTo("image")
                assertThat(name).isEqualTo("billmurray.jpeg")
                assertThat(fetch(link)).isEqualTo(billMurray.readBytes())
            }
        }

        it("sends (v2) message with link attachment") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()
            val alice = chatFor(ALICE).connect().assumeSuccess()

            alice.sendMessage(
                    room = alice.generalRoom,
                    messageText = "Dogs and cats, living together",
                    attachment = LinkAttachment("https://www.fillmurray.com/284/196", IMAGE)
            ).assumeSuccess()

            val (firstMessage) = pusherino.fetchMessages(pusherino.generalRoom.id).assumeSuccess()

            assertThat(firstMessage.attachment?.link).isEqualTo("https://www.fillmurray.com/284/196")
        }

        it("sends (v3) message with one text part, retrieves (v2)") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()
            val alice = chatFor(ALICE).connect().assumeSuccess()

            alice.sendMultipartMessage(
                    room = alice.generalRoom,
                    parts = listOf(NewPart.Inline("Dogs and cats, living together", "text/plain"))
            ).assumeSuccess()

            val (firstMessage) = pusherino.fetchMessages(pusherino.generalRoom.id).assumeSuccess()

            assertThat(firstMessage.text).isEqualTo("Dogs and cats, living together")
        }

        it("sends (v3) message with several text parts, retrieves (v3)") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()
            val alice = chatFor(ALICE).connect().assumeSuccess()

            alice.sendMultipartMessage(
                    room = alice.generalRoom,
                    parts = listOf(
                            NewPart.Inline("Fire and brimstone coming down from the skies. Rivers and seas boiling.", "text/plain"),
                            NewPart.Inline("Forty years of darkness. Earthquakes, volcanoes...", "text/plain"),
                            NewPart.Inline("The dead rising from the grave.", "text/plain"),
                            NewPart.Inline("Dogs and cats, living together!", "text/plain")
                    )
            ).assumeSuccess()

            val (firstMessage) = pusherino.fetchMultipartMessages(pusherino.generalRoom.id).assumeSuccess()

            assertThat(firstMessage.parts.map { (it.payload as Payload.Inline).content } ).containsExactly(
                    "Fire and brimstone coming down from the skies. Rivers and seas boiling.",
                    "Forty years of darkness. Earthquakes, volcanoes...",
                    "The dead rising from the grave.",
                    "Dogs and cats, living together!"
            )
        }

        it("sends (v3) multipart message with media attachment, retrieves (v3)") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val bytes = "<body>This is a test attachment</body>".toByteArray(Charsets.UTF_8)

            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()

            val alice = chatFor(ALICE).connect().assumeSuccess()
            alice.sendMultipartMessage(
                    room = alice.generalRoom,
                    parts = listOf(
                            NewPart.Inline("Here comes the media", "text/plain"),
                            NewPart.Attachment(
                                    type = "text/html",
                                    file = ByteArrayInputStream(bytes)
                            )
                    )
            ).assumeSuccess()

            val (firstMessage) = pusherino.fetchMultipartMessages(pusherino.generalRoom.id).assumeSuccess()

            with(firstMessage) {
                assertThat(parts.map { it.partType }).containsExactly(
                        PartType.Inline,
                        PartType.Attachment
                )

                with(parts[0].payload as Payload.Inline) {
                    assertThat(type).isEqualTo("text/plain")
                    assertThat(content).isEqualTo("Here comes the media")
                }

                with(parts[1].payload as Payload.Attachment) {
                    assertThat(type).isEqualTo("text/html")
                    assertThat(url().map { fetch(it) }.assumeSuccess()).isEqualTo(bytes)
                }
            }
        }

        it("sends (v3) multipart message with media attachment with name, retrieves (v3)") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val bytes = "<body>This is a test attachment</body>".toByteArray(Charsets.UTF_8)

            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()

            val alice = chatFor(ALICE).connect().assumeSuccess()
            alice.sendMultipartMessage(
                    room = alice.generalRoom,
                    parts = listOf(
                            NewPart.Attachment(
                                    type = "text/html",
                                    name = "some_blob.bin",
                                    file = ByteArrayInputStream(bytes)
                            )
                    )
            ).assumeSuccess()

            val (firstMessage) = pusherino.fetchMultipartMessages(pusherino.generalRoom.id).assumeSuccess()

            with(firstMessage) {
                assertThat(parts.map { it.partType }).containsExactly(
                        PartType.Attachment
                )

                with(parts[0].payload as Payload.Attachment) {
                    assertThat(type).isEqualTo("text/html")
                    assertThat(name).isEqualTo("some_blob.bin")
                    assertThat(url().map { fetch(it) }.assumeSuccess()).isEqualTo(bytes)
                }
            }
        }

        it("sends (v3) multipart message with media attachment with custom data, retrieves (v3)") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val bytes = "<body>This is a test attachment</body>".toByteArray(Charsets.UTF_8)

            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()

            val alice = chatFor(ALICE).connect().assumeSuccess()
            alice.sendMultipartMessage(
                    room = alice.generalRoom,
                    parts = listOf(
                            NewPart.Attachment(
                                    type = "text/html",
                                    customData = mapOf("custom" to "data"),
                                    file = ByteArrayInputStream(bytes)
                            )
                    )
            ).assumeSuccess()

            val (firstMessage) = pusherino.fetchMultipartMessages(pusherino.generalRoom.id).assumeSuccess()

            with(firstMessage) {
                assertThat(parts.map { it.partType }).containsExactly(
                        PartType.Attachment
                )

                with(parts[0].payload as Payload.Attachment) {
                    assertThat(type).isEqualTo("text/html")
                    assertThat(customData).isEqualTo(mapOf("custom" to "data"))
                    assertThat(url().map { fetch(it) }.assumeSuccess()).isEqualTo(bytes)
                }
            }
        }

        it("sends (v3) text only message using helper, retrieves (v2)") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()
            val alice = chatFor(ALICE).connect().assumeSuccess()

            alice.sendSimpleMessage(
                    room = alice.generalRoom,
                    messageText = "Cats and dogs, living together"
            ).assumeSuccess()

            val (firstMessage) = pusherino.fetchMessages(pusherino.generalRoom.id).assumeSuccess()

            assertThat(firstMessage.text).isEqualTo("Cats and dogs, living together")
        }

        it("receives (v2) message with text only (v2)") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()
            val alice = chatFor(ALICE).connect().assumeSuccess()

            var receivedMessage by FutureValue<Message>()

            pusherino.subscribeToRoom(pusherino.generalRoom) { event ->
                when (event) {
                    is RoomEvent.Message -> receivedMessage = event.message
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

        it("receives (v2) message with text and link (v2)") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()
            val alice = chatFor(ALICE).connect().assumeSuccess()

            var receivedMessage by FutureValue<Message>()

            pusherino.subscribeToRoom(pusherino.generalRoom) { event ->
                when (event) {
                    is RoomEvent.Message -> receivedMessage = event.message
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
                with(attachment!!) {
                    assertThat(link).isEqualTo("https://www.fillmurray.com/284/196")
                    assertThat(type).isEqualTo("image")
                    assertThat(name).isEqualTo("196")
                }
            }
        }

        it("receives (v2) message with text and attachment (v2)") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()
            val alice = chatFor(ALICE).connect().assumeSuccess()

            var receivedMessage by FutureValue<Message>()

            pusherino.subscribeToRoom(pusherino.generalRoom) { event ->
                when (event) {
                    is RoomEvent.Message -> receivedMessage = event.message
                }
            }

            alice.sendMessage(
                    room = alice.generalRoom,
                    messageText = "Cats and dogs, living together",
                    attachment = DataAttachment(billMurray, "billmurray.jpeg")
            ).assumeSuccess()

            with(receivedMessage) {
                assertThat(text).isEqualTo("Cats and dogs, living together")
                assertThat(roomId).isEqualTo(pusherino.generalRoom.id)
                assertThat(attachment).isNotNull()
                assertThat(attachment?.link).isNotNull()
                assertThat(attachment?.type).isEqualTo("image")
                assertThat(attachment?.name).isEqualTo("billmurray.jpeg")
            }
        }

        it("receives (v2) message sent with text only (v3)") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()
            val alice = chatFor(ALICE).connect().assumeSuccess()

            var receivedMessage by FutureValue<Message>()

            pusherino.subscribeToRoom(pusherino.generalRoom) { event ->
                when (event) {
                    is RoomEvent.Message -> receivedMessage = event.message
                }
            }

            alice.sendMultipartMessage(
                    room = alice.generalRoom,
                    parts = listOf(NewPart.Inline("Dogs and cats, living together", "text/plain"))
            ).assumeSuccess()

            with(receivedMessage) {
                assertThat(text).isEqualTo("Dogs and cats, living together")
                assertThat(roomId).isEqualTo(alice.generalRoom.id)
                assertThat(attachment).isNull()
                assertThat(user?.id).isEqualTo(alice.id)
            }
        }

        it("receives (v2) message sent with text and link (v3)") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()
            val alice = chatFor(ALICE).connect().assumeSuccess()

            var receivedMessage by FutureValue<Message>()

            pusherino.subscribeToRoom(pusherino.generalRoom) { event ->
                when (event) {
                    is RoomEvent.Message -> receivedMessage = event.message
                }
            }

            alice.sendMultipartMessage(
                    room = alice.generalRoom,
                    parts = listOf(
                            NewPart.Inline("Dogs and cats, living together", "text/plain"),
                            NewPart.Url("https://www.fillmurray.com/284/196", "image/jpeg")
                    )
            ).assumeSuccess()

            with(receivedMessage) {
                assertThat(text).isEqualTo("Dogs and cats, living together")
                assertThat(roomId).isEqualTo(alice.generalRoom.id)
                assertThat(user?.id).isEqualTo(alice.id)
                assertThat(attachment).isNotNull()
                with(attachment!!) {
                    assertThat(link).isEqualTo("https://www.fillmurray.com/284/196")
                    assertThat(type).isEqualTo("image")
                    assertThat(name).isEqualTo("196")
                }
            }
        }

        it("receives (v2) message sent with text and attachment (v3)") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()
            val alice = chatFor(ALICE).connect().assumeSuccess()
            val bytes = billMurray.readBytes()

            var receivedMessage by FutureValue<Message>()

            pusherino.subscribeToRoom(pusherino.generalRoom) { event ->
                when (event) {
                    is RoomEvent.Message -> receivedMessage = event.message
                }
            }

            alice.sendMultipartMessage(
                    room = alice.generalRoom,
                    parts = listOf(
                            NewPart.Inline("Dogs and cats, living together", "text/plain"),
                            NewPart.Attachment(
                                    type = "image/jpeg",
                                    name = "billmurray.jpeg",
                                    customData = mapOf("dogs" to "cats"),
                                    file = billMurray.inputStream()
                            )
                    )
            ).assumeSuccess()

            with(receivedMessage) {
                assertThat(text).isEqualTo("Dogs and cats, living together")
                assertThat(roomId).isEqualTo(alice.generalRoom.id)
                assertThat(user?.id).isEqualTo(alice.id)
                assertThat(attachment).isNotNull()
                with(attachment!!) {
                    assertThat(link).isNotNull()
                    assertThat(type).isEqualTo("image")
                    assertThat(name).isEqualTo("billmurray.jpeg")
                }
            }
        }

        it("receives (v3) message sent with multiple text parts and link (v3)") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()
            val alice = chatFor(ALICE).connect().assumeSuccess()

            var receivedMessage by FutureValue<com.pusher.chatkit.messages.multipart.Message>()

            pusherino.subscribeToRoomMultipart(pusherino.generalRoom) { event ->
                when (event) {
                    is RoomEvent.MultipartMessage -> receivedMessage = event.message
                }
            }

            alice.sendMultipartMessage(
                    room = alice.generalRoom,
                    parts = listOf(
                            NewPart.Inline("Fire and brimstone coming down from the skies. Rivers and seas boiling.", "text/plain"),
                            NewPart.Inline("Forty years of darkness. Earthquakes, volcanoes...", "text/plain"),
                            NewPart.Inline("The dead rising from the grave.", "text/plain"),
                            NewPart.Inline("Dogs and cats, living together!", "text/plain"),
                            NewPart.Url("https://www.rottentomatoes.com/m/ghostbusters/quotes/", "text/html")
                    )
            ).assumeSuccess()

            with(receivedMessage) {
                assertThat(room.id).isEqualTo(alice.generalRoom.id)
                assertThat(sender.id).isEqualTo(alice.id)
                assertThat(parts.map { it.partType }).containsExactly(
                        PartType.Inline,
                        PartType.Inline,
                        PartType.Inline,
                        PartType.Inline,
                        PartType.Url
                )
                assertThat(parts.map { it.payload }).containsExactly(
                        Payload.Inline("text/plain", "Fire and brimstone coming down from the skies. Rivers and seas boiling."),
                        Payload.Inline("text/plain", "Forty years of darkness. Earthquakes, volcanoes..."),
                        Payload.Inline("text/plain", "The dead rising from the grave."),
                        Payload.Inline("text/plain", "Dogs and cats, living together!"),
                        Payload.Url("text/html", URL("https://www.rottentomatoes.com/m/ghostbusters/quotes/"))
                )
            }
        }

        it("receives (v3) message sent with text and attachment (v3)") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val bytes = "This is a test attachment".toByteArray(Charsets.UTF_8)

            var receivedMessage by FutureValue<com.pusher.chatkit.messages.multipart.Message>()

            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()
            pusherino.subscribeToRoomMultipart(pusherino.generalRoom) { event ->
                when (event) {
                    is RoomEvent.MultipartMessage -> receivedMessage = event.message
                }
            }

            val alice = chatFor(ALICE).connect().assumeSuccess()
            alice.sendMultipartMessage(
                    room = alice.generalRoom,
                    parts = listOf(
                            NewPart.Inline("Here comes the media", "text/plain"),
                            NewPart.Attachment(
                                    type = "application/octet-stream",
                                    name = "some_blob.bin",
                                    customData = mapOf("key" to "value"),
                                    file = ByteArrayInputStream(bytes)
                            )
                    )
            ).assumeSuccess()

            with(receivedMessage) {
                assertThat(parts.map { it.partType }).containsExactly(
                        PartType.Inline,
                        PartType.Attachment
                )

                with(parts[0].payload as Payload.Inline) {
                    assertThat(type).isEqualTo("text/plain")
                    assertThat(content).isEqualTo("Here comes the media")
                }

                with(parts[1].payload as Payload.Attachment) {
                    assertThat(type).isEqualTo("application/octet-stream")
                    assertThat(url().map { fetch(it) }.assumeSuccess()).isEqualTo(bytes)
                }
            }
        }

        it("refreshes multipart attachment when URL expires") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val bytes = "This is a test attachment".toByteArray(Charsets.UTF_8)

            var receivedMessage by FutureValue<com.pusher.chatkit.messages.multipart.Message>()

            val pusherino = chatFor(PUSHERINO).connect().assumeSuccess()
            pusherino.subscribeToRoomMultipart(pusherino.generalRoom) { event ->
                when (event) {
                    is RoomEvent.MultipartMessage -> receivedMessage = event.message
                }
            }

            val alice = chatFor(ALICE).connect().assumeSuccess()
            alice.sendMultipartMessage(
                    room = alice.generalRoom,
                    parts = listOf(
                            NewPart.Attachment(
                                    type = "application/octet-stream",
                                    file = ByteArrayInputStream(bytes)
                            )
                    )
            ).assumeSuccess()

            with(receivedMessage) {
                assertThat(parts.map { it.partType }).containsExactly(
                        PartType.Attachment
                )

                with(parts[0].payload as Payload.Attachment) {
                    assertThat(type).isEqualTo("application/octet-stream")
                    assertThat(url().map { fetch(it) }.assumeSuccess()).isEqualTo(bytes)

                    expiration = Date() // make the expiry date now
                    assertThat(url().map { fetch(it) }.assumeSuccess()).isEqualTo(bytes)
                    assertThat(urlExpiry()).isAtLeast(Date(Date().time + 30 * 60 * 1000))
                }
            }
        }

    }
})

val billMurray = File(MessagesSpek::class.java.classLoader.getResource("bill_murray.jpg").file)

fun fetch(url: String): ByteArray =
        OkHttpClient().newCall(
                Request.Builder()
                        .url(url)
                        .build()
        ).execute().body()?.bytes()!!
