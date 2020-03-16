package com.pusher.chatkit.state

import com.pusher.chatkit.rooms.state.joinedRoomReducer
import com.pusher.chatkit.rooms.state.joinedRoomsReceivedReducer
import com.pusher.chatkit.rooms.state.leftRoomReducer
import com.pusher.chatkit.rooms.state.roomDeletedReducer
import com.pusher.chatkit.rooms.state.roomUpdatedReducer
import org.koin.core.module.Module
import org.koin.dsl.module
import org.reduxkotlin.combineReducers
import org.reduxkotlin.createStore

internal val createStoreModule: () -> Module = {
    module {
        single { StoreSubscriber(store) }
        single { store.getState }

        single { Dispatcher(store) }
    }
}

private typealias Store = org.reduxkotlin.Store<State>

internal typealias StoreObserver = (State) -> Unit

internal class StoreSubscriber(private val store: Store) : (StoreObserver) -> StoreSubscription {

    override fun invoke(observer: StoreObserver) =
        StoreSubscription(
            store.subscribe {
                observer(store.state)
            }
        )
}

internal class StoreSubscription(unsubscribe: () -> Unit) {

    private val _unsubscribe = unsubscribe

    fun unsubscribe() {
        _unsubscribe()
    }
}

internal class Dispatcher(private val store: Store) : (Action) -> Unit {

    override fun invoke(action: Action) {
        store.dispatch(action)
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
    State.initial()
)
