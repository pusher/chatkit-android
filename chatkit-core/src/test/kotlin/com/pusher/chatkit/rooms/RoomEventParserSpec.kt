package com.pusher.chatkit.rooms

import com.google.common.truth.Truth
import com.pusher.chatkit.TestFileReader
import com.pusher.chatkit.rooms.api.CreateRoomResponse
import com.pusher.chatkit.rooms.api.JoinRoomResponse
import com.pusher.chatkit.util.parseAs
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object RoomEventParserSpec : Spek({
    val testFileReader = TestFileReader("/json/rooms")

    describe("when create room response from the docs is parsed") {

        val result = testFileReader.readTestFile("room_created-docs.json")
                .parseAs<CreateRoomResponse>().successOrThrow()

        it("creates a valid room") {
            val room = result.room

            Truth.assertThat(room.id).isEqualTo("ac43dfef")
            Truth.assertThat(room.name).isEqualTo("Chatkit chat")
            Truth.assertThat(room.createdById).isEqualTo("alice")
            Truth.assertThat(room.pushNotificationTitleOverride).isEqualTo(null)
            Truth.assertThat(room.private).isEqualTo(false)
            Truth.assertThat(room.lastMessageAt).isEqualTo("2020-01-08T14:55:10Z")
            Truth.assertThat(room.createdAt).isEqualTo("2017-03-23T11:36:42Z")
            Truth.assertThat(room.updatedAt).isEqualTo("2017-03-23T11:36:42Z")
            Truth.assertThat(room.customData).isEqualTo(mapOf("highlight_color" to "blue"))
        }

        it("creates valid membership") {
            val membership = result.membership

            Truth.assertThat(membership.roomId).isEqualTo("ac43dfef")
            Truth.assertThat(membership.userIds.size).isEqualTo(2)
            Truth.assertThat(membership.userIds[0]).isEqualTo("alice")
            Truth.assertThat(membership.userIds[1]).isEqualTo("carol")
        }

    }

    describe("when joining a room example from the docs") {

        val result = testFileReader.readTestFile("room_joined-docs.json")
                .parseAs<JoinRoomResponse>().successOrThrow()

        it("creates a valid room") {
            val room = result.room

            Truth.assertThat(room.id).isEqualTo("ac43dfef")
            Truth.assertThat(room.name).isEqualTo("Chatkit chat")
            Truth.assertThat(room.createdById).isEqualTo("alice")
            Truth.assertThat(room.pushNotificationTitleOverride).isEqualTo(null)
            Truth.assertThat(room.private).isEqualTo(false)
            Truth.assertThat(room.lastMessageAt).isEqualTo("2020-01-08T14:55:10Z")
            Truth.assertThat(room.createdAt).isEqualTo("2017-03-23T11:36:42Z")
            Truth.assertThat(room.updatedAt).isEqualTo("2017-03-23T11:36:42Z")
            Truth.assertThat(room.customData).isEqualTo(mapOf("highlight_color" to "blue"))
        }

        it("creates valid membership") {
            val membership = result.membership

            Truth.assertThat(membership.roomId).isEqualTo("ac43dfef")
            Truth.assertThat(membership.userIds.size).isEqualTo(2)
            Truth.assertThat(membership.userIds[0]).isEqualTo("alice")
            Truth.assertThat(membership.userIds[1]).isEqualTo("carol")
        }

    }



})
