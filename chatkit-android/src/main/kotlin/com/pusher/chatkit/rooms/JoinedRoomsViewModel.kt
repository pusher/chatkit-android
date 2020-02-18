@file:Suppress("unused") // public api

package com.pusher.chatkit.rooms

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.pusher.chatkit.JoinedRoomsRepositoryFactory
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

    /**
     * Comma separated, e.g. `Alice, Bob`
     */
    val namesOfOtherMembers: CharSequence get() = TODO()

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

    // TODO: use android.text.format.DateFormat for appropriate localization (requires Context)
    private val dateFormat by lazy { DateFormat.getDateTimeInstance() }
    private val dateFormatShort by lazy { DateFormat.getDateTimeInstance(SHORT, SHORT) }
    private val dateFormatLong by lazy { DateFormat.getDateTimeInstance(LONG, LONG) }
}

sealed class JoinedRoomsViewModelState {

    data class Initializing(val error: Error?) : JoinedRoomsViewModelState()

    data class Connected(
        val rooms: List<RoomViewType>,
        val changeDescription: ChangeDescription?
    ) : JoinedRoomsViewModelState()

    class Degraded(
        val rooms: List<RoomViewType>,
        val changeDescription: ChangeDescription?,
        val error: Error
    ) : JoinedRoomsViewModelState()

    data class Closed(val error: Error? = null) : JoinedRoomsViewModelState()

    sealed class ChangeDescription {
        data class ItemInserted(val position: Int) : ChangeDescription()
        data class ItemMoved(val fromPosition: Int, val toPosition: Int) : ChangeDescription()
        data class ItemChanged(val position: Int, val previousValue: RoomViewType) : ChangeDescription()
        data class ItemRemoved(val position: Int, val previousValue: RoomViewType) : ChangeDescription()
    }
}

class JoinedRoomsViewModel(
    private val providerFactory: JoinedRoomsRepositoryFactory
) : ViewModel() {

    val state: LiveData<JoinedRoomsViewModelState> = TODO()
}
