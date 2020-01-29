package com.pusher.chatkit.rooms

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.TestFileReader
import com.pusher.chatkit.rooms.api.*
import com.pusher.chatkit.util.parseAs
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object FetchRoomsParserSpec : Spek({
    val testFileReader = TestFileReader("/json/rooms")

    describe("when fetching joinable rooms") {

        val result = testFileReader.readTestFile("get_joinable_rooms_response-docs.json")
                .parseAs<JoinableRoomsResponse>().successOrThrow()

        it("then the result has the expected rooms") {
            val expectedRoom = NotJoinedRoomApiType(
                    "ac43dfef",
                    "alice",
                    "Chatkit chat",
                    null,
                    mapOf("highlight_color" to "blue"),
                    "2017-03-23T11:36:42Z",
                    "2017-03-23T11:36:42Z",
                    null)

            assertThat(result.rooms).containsExactly(expectedRoom)
        }
    }
})
