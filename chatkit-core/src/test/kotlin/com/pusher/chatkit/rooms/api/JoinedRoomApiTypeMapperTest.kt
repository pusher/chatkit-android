package com.pusher.chatkit.rooms.api

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.pusher.chatkit.util.DateApiTypeMapper
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class JoinedRoomApiTypeMapperTest : Spek({

    describe("given a complete JoinedRoomApiType object") {
        val joinedRoomApiType = JoinedRoomApiType(
            id = "1",
            createdById = "ham",
            name = "mycoolroom",
            pushNotificationTitleOverride = "pushNotificationOverride",
            private = false,
            customData = mapOf("background-colour" to "red"),
            updatedAt = "2017-04-14T14:00:42Z",
            lastMessageAt = "2017-04-13T14:10:38Z",
            createdAt = "2017-04-13T14:10:38Z",
            deletedAt = null)

        describe("when mapped to a JoinedRoomInternalType object") {
            val dateApiTypeMapper = DateApiTypeMapper()
            val joinedRoomInternalType = JoinedRoomApiTypeMapper(dateApiTypeMapper)
                .toRoomInternalType(joinedRoomApiType)

            it("then the id matches") {
                assertThat(joinedRoomInternalType.id).isEqualTo(joinedRoomApiType.id)
            }

            it("then the name matches") {
                assertThat(joinedRoomInternalType.name).isEqualTo(joinedRoomApiType.name)
            }

            it("then the privacy matches") {
                assertThat(joinedRoomInternalType.isPrivate).isEqualTo(joinedRoomApiType.private)
            }

            it("then the custom data matches") {
                assertThat(joinedRoomInternalType.customData).isEqualTo(joinedRoomApiType.customData)
            }

            it("then the created at date will be in epochTime") {
                assertThat(joinedRoomInternalType.createdAt).isEqualTo(1492092638000L)
            }

            it("then the updated at date will be in epochTime") {
                assertThat(joinedRoomInternalType.updatedAt).isEqualTo(1492092638000L)
            }

            it("then the last message at date will be in epochTime") {
                assertThat(joinedRoomInternalType.lastMessageAt).isEqualTo(1492178442000L)
            }

            it("then the push notification override matches") {
                assertThat(joinedRoomInternalType.pushNotificationTitleOverride)
                    .isEqualTo(joinedRoomApiType.pushNotificationTitleOverride)
            }
        }
    }

    describe("given a partially complete JoinedRoomApiType object") {
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
            deletedAt = null)

        describe("when parsed as a JoinedRoomInternalType object") {
            val dateApiTypeMapper = DateApiTypeMapper()
            val joinedRoomInternalType = JoinedRoomApiTypeMapper(dateApiTypeMapper)
                .toRoomInternalType(joinedRoomApiType)

            it("then the last message at will be null") {
                assertThat(joinedRoomInternalType.lastMessageAt).isNull()
            }

            it("then the custom data will be null") {
                assertThat(joinedRoomInternalType.customData).isNull()
            }

            it("then the push notification override will be null") {
                assertThat(joinedRoomInternalType.pushNotificationTitleOverride).isNull()
            }
        }
    }
})
