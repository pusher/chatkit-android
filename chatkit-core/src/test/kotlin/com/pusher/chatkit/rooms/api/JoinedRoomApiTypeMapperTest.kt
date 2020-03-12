package com.pusher.chatkit.rooms.api

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.pusher.chatkit.util.DateApiTypeMapper
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object JoinedRoomApiTypeMapperTest : Spek({

    describe("given a complete JoinedRoomApiType") {
        val joinedRoomApiType = JoinedRoomApiType(
            id = "1",
            createdById = "ham",
            name = "mycoolroom",
            pushNotificationTitleOverride = "pushNotificationOverride",
            private = false,
            customData = mapOf("background-colour" to "red"),
            lastMessageAt = "2017-04-14T14:00:42Z",
            createdAt = "2017-04-13T14:10:38Z",
            updatedAt = "2017-04-13T14:10:38Z",
            deletedAt = null
        )

        describe("when mapped to a JoinedRoomInternalType") {
            val dateApiTypeMapper = DateApiTypeMapper()
            val joinedRoomInternalType = JoinedRoomApiTypeMapper(dateApiTypeMapper)
                .toRoomInternalType(joinedRoomApiType)

            it("then id matches") {
                assertThat(joinedRoomInternalType.id).isEqualTo(joinedRoomApiType.id)
            }

            it("then name matches") {
                assertThat(joinedRoomInternalType.name).isEqualTo(joinedRoomApiType.name)
            }

            it("then isPrivate matches") {
                assertThat(joinedRoomInternalType.isPrivate).isEqualTo(joinedRoomApiType.private)
            }

            it("then customData matches") {
                assertThat(joinedRoomInternalType.customData).isEqualTo(joinedRoomApiType.customData)
            }

            it("then createdAt is correct") {
                assertThat(joinedRoomInternalType.createdAt).isEqualTo(1492092638000L)
            }

            it("then updatedAt is correct") {
                assertThat(joinedRoomInternalType.updatedAt).isEqualTo(1492092638000L)
            }

            it("then lastMessageAt is correct") {
                assertThat(joinedRoomInternalType.lastMessageAt).isEqualTo(1492178442000L)
            }

            it("then pushNotificationTitleOverride matches") {
                assertThat(joinedRoomInternalType.pushNotificationTitleOverride)
                    .isEqualTo(joinedRoomApiType.pushNotificationTitleOverride)
            }
        }
    }

    describe("given a JoinedRoomApiType with absent optionals") {
        val joinedRoomApiType = JoinedRoomApiType(
            id = "1",
            createdById = "ham",
            name = "mycoolroom",
            pushNotificationTitleOverride = null,
            private = false,
            customData = null,
            updatedAt = "2017-04-14T14:00:42Z",
            lastMessageAt = null,
            createdAt = "2017-04-13T14:10:38Z",
            deletedAt = null
        )

        describe("when mapped to JoinedRoomInternalType") {
            val dateApiTypeMapper = DateApiTypeMapper()
            val joinedRoomInternalType = JoinedRoomApiTypeMapper(dateApiTypeMapper)
                .toRoomInternalType(joinedRoomApiType)

            it("then lastMessageAt is null") {
                assertThat(joinedRoomInternalType.lastMessageAt).isNull()
            }

            it("then customData is null") {
                assertThat(joinedRoomInternalType.customData).isNull()
            }

            it("then pushNotificationTitleOverride is null") {
                assertThat(joinedRoomInternalType.pushNotificationTitleOverride).isNull()
            }
        }
    }
})
