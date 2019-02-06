package com.pusher.chatkit.messages

/**
 * Describes the direction of a query for messages.
 */
enum class Direction {
    /**
     * Shows newer messages first
     */
    NEWER_FIRST,
    /**
     * Shows older messages first.
     */
    OLDER_FIRST;

    override fun toString() = when (this) {
        NEWER_FIRST -> "newer"
        OLDER_FIRST -> "older"
    }
}
