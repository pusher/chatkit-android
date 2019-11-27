package com.pusher.chatkit.cursors

import com.google.gson.annotations.SerializedName
import com.pusher.chatkit.util.dateFormat
import java.util.*

data class Cursor(
        val userId: String,
        val roomId: String,
        val position: Int,
        val updatedAt: String = dateFormat.format(Date()),
        @SerializedName("cursor_type") val type: Int = 0
) {
    fun matches(other : Cursor) = userId == other.userId && roomId == other.roomId
}
