package com.pusher.chatkit.users

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.pusher.chatkit.users.api.UserApiTyepMapper
import com.pusher.chatkit.users.api.UserApiType
import com.pusher.chatkit.util.DateApiTypeMapper
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class UserApiTypeMapperTest : Spek({
    describe("given a complete UserApiType object") {
        val userApiType = UserApiType(
            id = "danielle",
            name = "daniellevass",
            customData = mapOf("item" to "data"),
            avatarUrl = "http://placekitten.com/200/200",
            online = true,
            createdAt = "2020-03-05T15:31:21Z",
            updatedAt = "2020-03-05T15:31:42Z"
        )
        describe("when mapped to a UserInternalType object") {
            val dateApiTypeMapper = DateApiTypeMapper()
            val userInternalType = UserApiTyepMapper(dateApiTypeMapper)
                .toUserInternalType(userApiType)

            it("then the id matches") {
                assertThat(userInternalType.id).isEqualTo(userApiType.id)
            }

            it("then the name matches") {
                assertThat(userInternalType.name).isEqualTo(userApiType.name)
            }

            it("then the custom data matches") {
                assertThat(userInternalType.customData).isEqualTo(userApiType.customData)
            }

            it("then the avatar url matches") {
                assertThat(userInternalType.avatarUrl).isEqualTo(userApiType.avatarUrl)
            }

            it("then the online matches") {
                assertThat(userInternalType.online).isEqualTo(userApiType.online)
            }

            it("then the created at matches") {
                assertThat(userInternalType.createdAt).isEqualTo(1583422281000L)
            }

            it("then the updated at matches") {
                assertThat(userInternalType.updatedAt).isEqualTo(1583422302000L)
            }
        }
    }

    describe("given a partially complete UserApiType object") {
        val userApiType = UserApiType(
            id = "danielle",
            name = "daniellevass",
            customData = null,
            avatarUrl = null,
            createdAt = "2020-03-05T15:31:21Z",
            updatedAt = "2020-03-05T15:31:42Z"
        )
        describe("when mapped to a UserInternalType object") {
            val dateApiTypeMapper = DateApiTypeMapper()
            val userInternalType = UserApiTyepMapper(dateApiTypeMapper)
                .toUserInternalType(userApiType)

            it("then the custom data will be null") {
                assertThat(userInternalType.customData).isNull()
            }

            it("then the avat url will be null") {
                assertThat(userInternalType.avatarUrl).isNull()
            }

            it("then the online status will be offline") {
                assertThat(userInternalType.online).isEqualTo(false)
            }
        }
    }
})
