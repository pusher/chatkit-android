package com.pusher.chatkit.users.state

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.pusher.chatkit.state.ChatState
import com.pusher.chatkit.state.CurrentUserReceived
import com.pusher.chatkit.state.State
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class CurrentUserReceivedTest : Spek({

    val simpleUser = UserInternalType(
        id = "danielle",
        createdAt = 1583422281000L,
        updatedAt = 1583422302000L,
        name = "daniellevass",
        avatarUrl = null,
        customData = null
    )

    describe("given initial state") {
        val initialState = State.initial()

        describe("when a current user is received") {
            val currentUserReceived = CurrentUserReceived(simpleUser)
            val newState = currentUserReceivedReducer(initialState, currentUserReceived)

            it("then the state contains the current user") {
                assertThat(newState.chatState.currentUser).isEqualTo(simpleUser)
            }
        }
    }

    describe("given a current user") {

        val initialState = State.initial().with(
            ChatState.initial().with(
                currentUser = simpleUser
            )
        )

        describe("when a current user is received") {
            val currentUserInternalType = UserInternalType(
                id = "danielle2",
                createdAt = 1583422281000L,
                updatedAt = 1583422302000L,
                name = "daniellevass",
                avatarUrl = null,
                customData = null
            )
            val currentUserReceived = CurrentUserReceived(currentUserInternalType)
            val newState = currentUserReceivedReducer(initialState, currentUserReceived)

            it("then the state contains the current user") {
                assertThat(newState.chatState.currentUser).isEqualTo(currentUserInternalType)
            }
        }
    }
})
