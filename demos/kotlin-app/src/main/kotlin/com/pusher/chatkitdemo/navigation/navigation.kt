package com.pusher.chatkitdemo.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.UriMatcher
import android.net.Uri
import com.pusher.chatkit.Room
import com.pusher.chatkitdemo.app

private enum class Screen(val path: String) {
    ROOM("room/#") {
        override fun asNavigationEvent(uri: Uri) =
            NavigationEvent.Room(uri.lastPathSegment.toInt())
    };

    abstract fun asNavigationEvent(uri: Uri): NavigationEvent

    companion object {
        fun find(ordinal: Int) : Screen? =
            values().getOrNull(ordinal)
    }
}

sealed class NavigationEvent {
    data class Room(val roomId: Int) : NavigationEvent()
    data class Unknown(val data: Uri) : NavigationEvent()
}

private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
    Screen.values().forEach {
        addURI("chat.pusher.com", it.path, it.ordinal)
    }
}

val Intent.navigationEvent: NavigationEvent
    get() = data.navigationEvent

val Uri.navigationEvent: NavigationEvent
    get() = uriMatcher.match(this).let { code ->
        Screen.find(code)?.asNavigationEvent(this) ?: NavigationEvent.Unknown(this)
    }

fun Context.openIntent(path: String) =
    Intent(Intent.ACTION_VIEW, Uri.parse(path)).apply {
        `package` = packageName
    }

fun Context.open(path: String) =
    startActivity(openIntent(path))

fun Context.open(room: Room) =
    open("https://chat.pusher.com/room/${room.id}")

fun Activity.failNavigation(navigationEvent: NavigationEvent) {

    app.logger.error("Failed to load navigation: $navigationEvent")
    finish()
}