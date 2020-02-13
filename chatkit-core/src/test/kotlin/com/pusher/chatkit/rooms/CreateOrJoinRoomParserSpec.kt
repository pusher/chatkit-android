package com.pusher.chatkit.rooms

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.TestFileReader
import com.pusher.chatkit.rooms.api.CreateRoomResponse
import com.pusher.chatkit.rooms.api.JoinRoomResponse
import com.pusher.chatkit.rooms.api.JoinedRoomApiType
import com.pusher.chatkit.rooms.api.RoomMembershipApiType
import com.pusher.chatkit.util.parseAs
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object CreateOrJoinRoomParserSpec : Spek({
    val testFileReader = TestFileReader("/json/rooms")

    val expectedRoom = JoinedRoomApiType(
            id = "ac43dfef",
            createdById = "alice",
            name = "Chatkit chat",
            pushNotificationTitleOverride = null,
            private = false,
            customData = mapOf("highlight_color" to "blue"),
            lastMessageAt = "2020-01-08T14:55:10Z",
            createdAt = "2017-03-23T11:36:42Z",
            updatedAt = "2017-03-23T11:36:42Z",
            deletedAt = null
    )
    val expectedMembership = RoomMembershipApiType("ac43dfef", listOf("alice", "carol"))

    describe("when create room response from the docs is parsed") {
        val result = testFileReader.readTestFile("create_or_join_room_response-docs.json")
                .parseAs<CreateRoomResponse>().successOrThrow()

        it("then the result has expected room") {
            assertThat(result.room).isEqualTo(expectedRoom)
        }
        it("then the result has expected membership") {
            assertThat(result.membership).isEqualTo(expectedMembership)
        }
    }

    describe("when join room response from the docs is parsed") {
        val result = testFileReader.readTestFile("create_or_join_room_response-docs.json")
                .parseAs<JoinRoomResponse>().successOrThrow()

        it("then the result has expected room") {
            assertThat(result.room).isEqualTo(expectedRoom)
        }
        it("then the result has expected membership") {
            assertThat(result.membership).isEqualTo(expectedMembership)
        }
    }
})
