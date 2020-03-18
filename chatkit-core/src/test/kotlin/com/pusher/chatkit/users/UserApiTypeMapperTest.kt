package com.pusher.chatkit.users

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.pusher.chatkit.users.api.UserApiType
import com.pusher.chatkit.users.api.UserApiTypeMapper
import com.pusher.chatkit.util.DateApiTypeMapper
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class UserApiTypeMapperTest : Spek({
    describe("given a complete UserApiType") {
        val userApiType = UserApiType(
            id = "alice",
            createdAt = "2020-03-05T15:31:21Z",
            name = "alice",
            customData = mapOf("item" to "data"),
            avatarUrl = "http://placekitten.com/200/200",
            updatedAt = "2020-03-05T15:31:42Z"
        )
        describe("when mapped to a UserInternalType") {
            val dateApiTypeMapper = DateApiTypeMapper()
            val userInternalType = UserApiTypeMapper(dateApiTypeMapper)
                .toUserInternalType(userApiType)

            it("then id is correct") {
                assertThat(userInternalType.id).isEqualTo(userApiType.id)
            }

            it("then name is correct") {
                assertThat(userInternalType.name).isEqualTo(userApiType.name)
            }

            it("then customData is correct") {
                assertThat(userInternalType.customData).isEqualTo(userApiType.customData)
            }

            it("then avatarUrl is correct") {
                assertThat(userInternalType.avatarUrl).isEqualTo(userApiType.avatarUrl)
            }

            it("then created at is correct") {
                assertThat(userInternalType.createdAt).isEqualTo(1583422281000L)
            }

            it("then updated at is correct") {
                assertThat(userInternalType.updatedAt).isEqualTo(1583422302000L)
            }
        }
    }

    describe("given a UserApiType with absent optionals") {
        val userApiType = UserApiType(
            id = "alice",
            createdAt = "2020-03-05T15:31:21Z",
            name = "alice",
            customData = null,
            avatarUrl = null,
            updatedAt = "2020-03-05T15:31:42Z"
        )
        describe("when mapped to a UserInternalType") {
            val dateApiTypeMapper = DateApiTypeMapper()
            val userInternalType = UserApiTypeMapper(dateApiTypeMapper)
                .toUserInternalType(userApiType)

            it("then customData will be null") {
                assertThat(userInternalType.customData).isNull()
            }

            it("then avatarUrl will be null") {
                assertThat(userInternalType.avatarUrl).isNull()
            }
        }
    }
})
