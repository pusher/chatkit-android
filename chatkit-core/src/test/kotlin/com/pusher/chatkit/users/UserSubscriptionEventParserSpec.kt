package com.pusher.chatkit.users

import com.google.common.truth.Truth.assertThat
import com.pusher.chatkit.users.UserSubscriptionEvent.AddedToRoomEvent
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object UserSubscriptionEventParserSpec : Spek({
    describe("parsing added to room example event from the docs") {

        val result = UserSubscriptionEventParser(ADDED_TO_ROOM_EVENT_DOCS_JSON)

        it("room will have unread count relayed from read state") {
            val addedToRoomEvent = result.successOrThrow() as AddedToRoomEvent
            assertThat(addedToRoomEvent.room.unreadCount).isEqualTo(15)
        }

    }
})

private const val ADDED_TO_ROOM_EVENT_DOCS_JSON = """
    {
      "event_name": "added_to_room",
      "data": {
        "room": {
          "id": "cool-room-2",
          "created_by_id": "ham",
          "name": "mycoolroom",
          "push_notification_title_override": null,
          "private": false,
          "custom_data": {
            "something": "interesting"
          },
          "last_message_at": "2017-04-14T14:00:42Z",
          "created_at": "2017-03-23T11:36:42Z",
          "updated_at": "2017-03-23T11:36:42Z",
          "deleted_at": null
        },
        "memberships": [
          {
            "room_id": "cool-room-1",
            "user_ids": ["jean", "ham"]
          },
          {
            "room_id": "cool-room-2",
            "user_ids": ["ham"]
          },
          {
            "room_id": "party-room",
            "user_ids": ["viv", "ham"]
          }
        ],
        "read_state": {
          "room_id": "cool-room-2",
          "unread_count": 15,
          "cursor": {
            "room_id": "cool-room-2",
            "user_id": "viv",
            "cursor_type": 0,
            "position": 123654,
            "updated_at": "2017-04-13T14:10:04Z"
          }
        }
      },
      "timestamp": "2017-04-14T14:00:42Z"
    }
"""