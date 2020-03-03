package com.pusher.chatkit.util

import java.text.SimpleDateFormat
import java.util.Locale

internal val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)

internal object DateUtil {
    internal fun parseApiDateToEpoch(input: String): Long {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.ENGLISH)
        return sdf.parse(input).time
    }
}
