package com.pusher.chatkit.util

import java.text.SimpleDateFormat
import java.util.Locale

internal val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.ENGLISH)

internal class DateApiTypeMapper {
    fun mapToEpochTime(input: String): Long {
        return dateFormat.parse(input).time
    }
}
