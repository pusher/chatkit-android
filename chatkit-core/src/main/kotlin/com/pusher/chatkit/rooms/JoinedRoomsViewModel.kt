@file:Suppress("unused")  // public api

package com.pusher.chatkit.rooms

import elements.Error

sealed class JoinedRoomsViewModelState {

    data class Initializing(val error: Error?): JoinedRoomsViewModelState()

    data class Connected(
            val rooms: List<Room>,
            val changeReason: ChangeReason?
    ): JoinedRoomsViewModelState()

    class Degraded(
            val rooms: List<Room>,
            val changeReason: ChangeReason?,
            // TODO: model degrade details
            val error: Error
    ): JoinedRoomsViewModelState()

    object Closed: JoinedRoomsViewModelState()

    sealed class ChangeReason {
        data class ItemInserted(val position: Int): ChangeReason()
        data class ItemMoved(val fromPosition: Int, val toPosition: Int): ChangeReason()
        data class ItemChanged(val position: Int, val previousValue: Room): ChangeReason()
        data class ItemRemoved(val position: Int, val previousValue: Room): ChangeReason()
    }
}

class JoinedRoomsViewModel(private val providerFactory: JoinedRoomsProviderFactory) : ViewModel {

    val state: LiveData<JoinedRoomsViewModelState> = TODO()

}