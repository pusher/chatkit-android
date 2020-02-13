package com.pusher.chatkit.rooms

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.TestFileReader
import com.pusher.chatkit.rooms.api.JoinableRoomsResponse
import com.pusher.chatkit.rooms.api.NotJoinedRoomApiType
import com.pusher.chatkit.util.parseAs
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object GetJoinableRoomsParserSpec : Spek({
    val testFileReader = TestFileReader("/json/rooms")

    describe("when get joinable rooms response from the docs is parsed") {
        val result = testFileReader.readTestFile("get_joinable_rooms_response-docs.json")
                .parseAs<JoinableRoomsResponse>().successOrThrow()

        it("then the result has expected room") {
            val expectedRoom = NotJoinedRoomApiType(
                    id = "ac43dfef",
                    createdById = "alice",
                    name = "Chatkit chat",
                    pushNotificationTitleOverride = null,
                    customData = mapOf("highlight_color" to "blue"),
                    createdAt = "2017-03-23T11:36:42Z",
                    updatedAt = "2017-03-23T11:36:42Z",
                    deletedAt = null)

            assertThat(result.rooms).containsExactly(expectedRoom)
        }
    }
})
