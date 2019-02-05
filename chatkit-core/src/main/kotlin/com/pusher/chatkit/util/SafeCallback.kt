package com.pusher.chatkit.util

import com.pusher.platform.logger.Logger

fun makeSafe(logger: Logger, f: () -> Unit) {
    try {
        f()
    } catch (t: Throwable) {
        logger.error("Exception raised during callback", Error(t))
    }
}
