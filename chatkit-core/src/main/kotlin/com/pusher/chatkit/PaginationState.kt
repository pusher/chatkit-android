package com.pusher.chatkit

sealed class PaginationState {
    object PartiallyPopulated: PaginationState()
    object FullyPopulated: PaginationState()
    object Fetching: PaginationState()
}