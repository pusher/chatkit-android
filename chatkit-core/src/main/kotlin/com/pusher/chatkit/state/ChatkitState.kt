package com.pusher.chatkit.state

import com.pusher.chatkit.rooms.state.JoinedRoomsState
import com.pusher.chatkit.rooms.state.joinedRoomReducer
import com.pusher.chatkit.rooms.state.joinedRoomsReceivedReducer
import com.pusher.chatkit.rooms.state.leftRoomReducer
import com.pusher.chatkit.rooms.state.roomDeletedReducer
import com.pusher.chatkit.rooms.state.roomUpdatedReducer
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.reduxkotlin.combineReducers
import org.reduxkotlin.createStore

internal val createStoreModule: () -> Module = {
    module {
        single(named(store::subscribe.name)) { store.subscribe }
        single { store.getState }

        // TODO: uncomment when reducers branch is merged into v2 and this branch caught up
        single(named(store::dispatch.name)) { store.dispatch /*as (Action) -> Action*/ }
    }
}

private val store = createStore(
    combineReducers(
        joinedRoomsReceivedReducer,
        joinedRoomReducer,
        roomUpdatedReducer,
        leftRoomReducer,
        roomDeletedReducer
    ),
    ChatkitState.initial()
)

@Suppress("CopyWithoutNamedArguments") // auxiliaryState (legible) passes as first param many times
internal data class ChatkitState(
    val auxiliaryState: AuxiliaryState = AuxiliaryState.initial(),
    val joinedRoomsState: JoinedRoomsState?
) {

    companion object {
        fun initial() = ChatkitState(
                joinedRoomsState = null
        )
    }

    fun with(joinedRoomsState: JoinedRoomsState, auxiliaryState: AuxiliaryState) = copy(
        auxiliaryState = auxiliaryState,
        joinedRoomsState = joinedRoomsState
    )
}
