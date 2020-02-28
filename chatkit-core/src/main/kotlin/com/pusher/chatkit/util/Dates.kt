package com.pusher.chatkit.util

import java.text.SimpleDateFormat
import java.util.*

internal val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)

internal object DateUtil {
    internal fun parseApiDateToEpoch(input: String): Long {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)
        sdf.timeZone = TimeZone.getTimeZone("GMT")
        return sdf.parse(input).time
    }
}
