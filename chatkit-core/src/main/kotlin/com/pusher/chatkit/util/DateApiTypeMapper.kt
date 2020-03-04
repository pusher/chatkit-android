package com.pusher.chatkit.util

internal class DateApiTypeMapper {
    fun mapToEpochTime(input: String): Long {
        return dateFormat.parse(input).time
    }
}
