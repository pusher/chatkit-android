package com.pusher.chatkit.rooms

import elements.Error

sealed class JoinedRoomsViewModelState {
    data class Initializing(val error: Error?): JoinedRoomsViewModelState()

    data class Connected(
            val rooms: List<Room>,
            val update: JoinedRoomsViewModelStateUpdate?
    ): JoinedRoomsViewModelState()

    class Degraded(
            val rooms: List<Room>,
            val update: JoinedRoomsViewModelStateUpdate?,
            val error: Error
    ): JoinedRoomsViewModelState()

    object Closed: JoinedRoomsViewModelState()
}

sealed class JoinedRoomsViewModelStateUpdate {

    data class ItemInserted(
            val position: Int
    ): JoinedRoomsViewModelStateUpdate()

    data class ItemMoved(
            val fromPosition: Int,
            val toPosition: Int
    ): JoinedRoomsViewModelStateUpdate()

    data class ItemChanged(
            val position: Int,
            val previousValue: Room
    ): JoinedRoomsViewModelStateUpdate()

    data class ItemRemoved(
            val position: Int,
            val previousValue: Room
    ): JoinedRoomsViewModelStateUpdate()

}

class JoinedRoomsViewModel(private val providerFactory: JoinedRoomsProviderFactory) : ViewModel {

    val state: LiveData<JoinedRoomsViewModelState> = TODO()

}