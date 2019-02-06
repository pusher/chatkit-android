package com.pusher.chatkit

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.Rooms.GENERAL
import com.pusher.chatkit.Users.ALICE
import com.pusher.chatkit.Users.PUSHERINO
import com.pusher.chatkit.presence.Presence
import com.pusher.chatkit.rooms.RoomEvent
import com.pusher.chatkit.test.InstanceActions.createDefaultRole
import com.pusher.chatkit.test.InstanceActions.newRoom
import com.pusher.chatkit.test.InstanceActions.newUsers
import com.pusher.chatkit.test.InstanceSupervisor.setUpInstanceWith
import com.pusher.chatkit.test.InstanceSupervisor.tearDownInstance
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

class PresenceSpek : Spek({
    beforeEachTest(::tearDownInstance)
    afterEachTest(::closeChatManagers)

    describe("Chatkit with presence") {
        it("notifies when '$ALICE' comes online in room '$GENERAL'") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val userCameOnline by chatFor(PUSHERINO)
                    .subscribeRoomFor(GENERAL) { roomEvent ->
                        (roomEvent as? RoomEvent.PresenceChange)?.takeIf { presenceEvent ->
                            presenceEvent.user.id == ALICE && presenceEvent.currentState == Presence.Online
                        }
                    }

            chatFor(ALICE).connect().assumeSuccess()

            assertThat(userCameOnline.user.id).isEqualTo(ALICE)
        }

        it("notifies when '$ALICE' goes offline in room '$GENERAL'") {
            setUpInstanceWith(createDefaultRole(), newUsers(PUSHERINO, ALICE), newRoom(GENERAL, PUSHERINO, ALICE))

            val aliceChat = chatFor(ALICE)
            aliceChat.connect().assumeSuccess()

            val userWentOffline by chatFor(PUSHERINO)
                    .subscribeRoomFor(GENERAL) { roomEvent ->
                        (roomEvent as? RoomEvent.PresenceChange)?.takeIf { presenceEvent ->
                            presenceEvent.user.id == ALICE && presenceEvent.currentState == Presence.Offline
                        }
                    }

            aliceChat.close()

            assertThat(userWentOffline.user.id).isEqualTo(ALICE)
        }
    }
})
