@file:Suppress("unused") // TODO: remove when no longer just a sketch (unused)

package com.pusher.chatkit

sealed class PaginationState {
    object PartiallyPopulated: PaginationState()
    object FullyPopulated: PaginationState()
    object Fetching: PaginationState()
}