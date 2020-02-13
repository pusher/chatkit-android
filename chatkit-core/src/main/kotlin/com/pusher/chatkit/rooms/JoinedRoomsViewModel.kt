@file:Suppress("unused")  // public api

package com.pusher.chatkit.rooms

import elements.Error
import java.text.DateFormat
import java.text.DateFormat.LONG
import java.text.DateFormat.SHORT
import java.util.Date

data class RoomViewType(val source: Room) { // JoinedRoom?

    /**
     * Identifier intended to be used for navigation (to create ViewModels in consecutive screens).
     */
    val id: CharSequence get() = source.id

    val name: CharSequence get() = source.name
    val isPrivate: Boolean get() = source.isPrivate

    val unreadCount: Int? get() = source.unreadCount

    val lastMessageAt: CharSequence? by lazy { format(dateFormat, source.lastMessageAt) }
    val lastMessageAtShort: CharSequence? by lazy { format(dateFormatShort, source.lastMessageAt) }
    val lastMessageAtLong: CharSequence? by lazy { format(dateFormatLong, source.lastMessageAt) }

    // TODO: follow the same pattern as for lastMessageAt
//    val createdAt: CharSequence,
//    val updatedAt: CharSequence,
//    val deletedAt: CharSequence?

    private fun format(dateFormat: DateFormat, millis: Long?): String? =
            millis?.let { dateFormat.format(Date(millis)) }

    // TODO: use android.text.format.DateFormat for appropriate localization, probably best
    //  retrieve via reflection (once in the entry point and falling back to java.text.DateFormat)
    //  to not impose mandatory dependency on Android or not to require the client code to pass it
    //  (it would be good to have it but as an option — for custom client-provided DateFormat's,
    //  not as a requirement)
    private val dateFormat by lazy { DateFormat.getDateTimeInstance() }
    private val dateFormatShort by lazy { DateFormat.getDateTimeInstance(SHORT, SHORT) }
    private val dateFormatLong by lazy { DateFormat.getDateTimeInstance(LONG, LONG) }
}

sealed class JoinedRoomsViewModelState {

    data class Initializing(val error: Error?): JoinedRoomsViewModelState()

    data class Connected(
            val rooms: List<RoomViewType>,
            val changeReason: ChangeReason?
    ): JoinedRoomsViewModelState()

    class Degraded(
            val rooms: List<RoomViewType>,
            val changeReason: ChangeReason?,
            // TODO: model degrade details
            val error: Error
    ): JoinedRoomsViewModelState()

    object Closed: JoinedRoomsViewModelState()

    sealed class ChangeReason {
        data class ItemInserted(val position: Int): ChangeReason()
        data class ItemMoved(val fromPosition: Int, val toPosition: Int): ChangeReason()
        data class ItemChanged(val position: Int, val previousValue: RoomViewType): ChangeReason()
        data class ItemRemoved(val position: Int, val previousValue: RoomViewType): ChangeReason()
    }
}

// TODO: Fix ViewModel/LiveData dependency. Is because of non-Android module?
class JoinedRoomsViewModel(private val providerFactory: Any/*JoinedRoomsProviderFactory*/) : Any()/*ViewModel*/ {

    val state: Lazy/*LiveData*/<JoinedRoomsViewModelState> = TODO()

}