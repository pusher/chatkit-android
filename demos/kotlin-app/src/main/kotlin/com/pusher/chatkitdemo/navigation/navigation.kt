package com.pusher.chatkitdemo.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.UriMatcher
import android.net.Uri
import android.support.customtabs.CustomTabsIntent
import com.pusher.chatkit.Room
import com.pusher.chatkitdemo.app
import com.pusher.util.orElse

private enum class Screen(val path: String, val authority: String = "chat.pusher.com") {
    ROOM("room/#") {
        override fun asNavigationEvent(uri: Uri): NavigationEvent =
            NavigationEvent.room(uri.lastPathSegment.toInt())
    },
    MAIN("home") {
        override fun asNavigationEvent(uri: Uri): NavigationEvent =
            NavigationEvent.main(uri.getQueryParameter("userId"))
    },
    ENTRY("/", "auth") {
        override fun asNavigationEvent(uri: Uri): NavigationEvent = with(uri) {
            val token = getQueryParameter("code")
            when(token) {
                null -> NavigationEvent.missingGitHubToken()
                else -> NavigationEvent.withGitHubToken(token)
            }
        }

    };

    abstract fun asNavigationEvent(uri: Uri): NavigationEvent

    companion object {
        fun find(ordinal: Int) : Screen? =
            values().getOrNull(ordinal)
    }
}

sealed class NavigationEvent {

    companion object {
        @JvmStatic fun room(roomId: Int): NavigationEvent = Room(roomId)
        @JvmStatic fun main( userId: String): NavigationEvent = Main(userId)
        @JvmStatic fun withGitHubToken(token: String) : NavigationEvent = Entry.WithGitHubCode(token)
        @JvmStatic fun missingGitHubToken(): NavigationEvent = Entry.MissingGitHubToken
        @JvmStatic fun missingUri(): NavigationEvent = MissingUri
        @JvmStatic fun unknown(uri: Uri): NavigationEvent = Unknown(uri)
    }

    data class Room(val roomId: Int) : NavigationEvent()
    data class Main(val userId: String) : NavigationEvent()
    data class Unknown(val data: Uri) : NavigationEvent()
    object MissingUri : NavigationEvent()
    sealed class Entry : NavigationEvent() {
        data class WithGitHubCode(val code: String) : Entry()
        object MissingGitHubToken : Entry()
    }
}

private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
    Screen.values().forEach { screen -> with(screen) { addURI(authority, path, ordinal) } }
}

val Intent.navigationEvent: NavigationEvent
    get() = data.navigationEvent

val Uri?.navigationEvent: NavigationEvent
    get() = orElse { NavigationEvent.missingUri() }
        .flatMap { uri ->
            Screen.find(uriMatcher.match(uri))
                .orElse { NavigationEvent.unknown(uri) }
                .map { it.asNavigationEvent(uri) }
        }.recover { it }

fun Context.openIntent(path: String) =
    Intent(Intent.ACTION_VIEW, Uri.parse(path)).apply {
        `package` = packageName
    }

fun Context.open(path: String) =
    startActivity(openIntent(path))

fun Context.openInBrowser(path: String) =
    CustomTabsIntent.Builder().build().launchUrl(this, Uri.parse(path))

fun Context.open(room: Room) =
    open("https://chat.pusher.com/room/${room.id}")

fun Context.openMain(userId: String) =
    open("https://chat.pusher.com/home?userId=$userId")

fun Activity.failNavigation(navigationEvent: NavigationEvent) {
    app.logger.error("Failed to load navigation: $navigationEvent")
    finish()
}
