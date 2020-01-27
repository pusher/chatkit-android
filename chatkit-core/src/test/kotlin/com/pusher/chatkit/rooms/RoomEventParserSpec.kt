package com.pusher.chatkit.rooms

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.TestFileReader
import com.pusher.chatkit.rooms.api.CreateRoomResponse
import com.pusher.chatkit.rooms.api.JoinRoomResponse
import com.pusher.chatkit.rooms.api.JoinedRoomApiType
import com.pusher.chatkit.util.parseAs
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object RoomEventParserSpec : Spek({
    val testFileReader = TestFileReader("/json/rooms")

    describe("when create room response from the docs is parsed") {

        val result = testFileReader.readTestFile("room_created_response-docs.json")
                .parseAs<CreateRoomResponse>().successOrThrow()

        it("then the result has expected room") {
            val expectedRoom = JoinedRoomApiType(
                    "ac43dfef",
                    "alice",
                    "Chatkit chat",
                    null,
                    false,
                    mapOf("highlight_color" to "blue"),
                    "2020-01-08T14:55:10Z",
                    "2017-03-23T11:36:42Z",
                    "2017-03-23T11:36:42Z",
                    null)

            assertThat(result.room).isEqualTo(expectedRoom)
        }

        it("creates valid membership") {
            val membership = result.membership

            assertThat(membership.roomId).isEqualTo("ac43dfef")
            assertThat(membership.userIds.size).isEqualTo(2)
            assertThat(membership.userIds[0]).isEqualTo("alice")
            assertThat(membership.userIds[1]).isEqualTo("carol")
        }

    }

    describe("when joining a room example from the docs") {

        val result = testFileReader.readTestFile("room_joined_response-docs.json")
                .parseAs<JoinRoomResponse>().successOrThrow()

        it("creates a valid room") {
            val expectedRoom = JoinedRoomApiType(
                    "ac43dfef",
                    "alice",
                    "Chatkit chat",
                    null,
                    false,
                    mapOf("highlight_color" to "blue"),
                    "2020-01-08T14:55:10Z",
                    "2017-03-23T11:36:42Z",
                    "2017-03-23T11:36:42Z",
                    null)

            assertThat(result.room).isEqualTo(expectedRoom)
        }

        it("creates valid membership") {
            val membership = result.membership

            assertThat(membership.roomId).isEqualTo("ac43dfef")
            assertThat(membership.userIds.size).isEqualTo(2)
            assertThat(membership.userIds[0]).isEqualTo("alice")
            assertThat(membership.userIds[1]).isEqualTo("carol")
        }

    }

})
