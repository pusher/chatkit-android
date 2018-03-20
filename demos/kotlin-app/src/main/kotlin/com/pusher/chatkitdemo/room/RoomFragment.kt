package com.pusher.chatkitdemo.room

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import android.os.Bundle
import android.support.annotation.UiThread
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pusher.chatkit.Message
import com.pusher.chatkit.Room
import com.pusher.chatkit.rooms.NoRoomMembershipError
import com.pusher.chatkitdemo.R
import com.pusher.chatkitdemo.app
import com.pusher.chatkitdemo.recyclerview.dataAdapterFor
import com.pusher.chatkitdemo.room.RoomFragment.State.*
import com.pusher.chatkitdemo.room.RoomFragment.State.Loaded.WithRoom
import com.pusher.chatkitdemo.showOnly
import com.pusher.platform.network.await
import com.pusher.util.Result
import com.pusher.util.asFailure
import com.pusher.util.asSuccess
import elements.Error
import kotlinx.android.synthetic.main.fragment_room.*
import kotlinx.android.synthetic.main.fragment_room_loaded.view.*
import kotlinx.android.synthetic.main.include_error.*
import kotlinx.android.synthetic.main.item_message.*
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlin.properties.Delegates

class RoomFragment : Fragment() {

    private val views by lazy { arrayOf(idleLayout, loadedLayout, errorLayout) }

    private val adapter = dataAdapterFor<Message>(R.layout.item_message) { message ->
        userNameView.setText(R.string.loading)
        app.users()
            .userFor(message)
            .onReady { userNameView.text = it.map { it.name }.recover { "???" } }
        messageView.text = message.text
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_room, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(view) {
            messageList.adapter = adapter
            messageList.layoutManager = LinearLayoutManager(activity).apply {
                reverseLayout = true
            }
            sendButton.setOnClickListener {
                val message = messageInput.text.toString()
                messageInput.text.clear()
                launch {
                    sendMessage(message)
                }
            }
        }
    }

    fun bind(roomId: Int) {
        state = Idle(roomId)
    }

    private var state: State by Delegates.observable<State>(Idle(-1)) { _, _, newState ->
        newState.render()
    }

    private fun State.render() = when (this) {
        is Idle -> {
            loadRoom()
            launchOnUi { renderIdle() }
        }
        is Loaded.WithRoom -> launchOnUi { renderLoadedRoom(room) }
        is Loaded.WithoutRoomMembership -> launchOnUi { renderNoMembership(room) }
        is Loaded.Complete -> launchOnUi {
            renderLoadedCompletely(room, messages)
            listenToMessages()
        }
        is Failed -> launchOnUi { renderFailed(error) }
    }

    @UiThread
    private fun renderIdle() {
        views.showOnly(idleLayout)
        adapter.data = emptyList()
    }

    private fun renderLoadedRoom(room: Room) {
        views.showOnly(loadedLayout)
        activity?.title = room.coolName
        loadMessages(room)
    }

    private fun renderLoadedCompletely(room: Room, messages: List<Message>) {
        views.showOnly(loadedLayout)
        activity?.title = room.coolName
        adapter.data = messages
    }

    private fun renderFailed(error: Error) {
        views.showOnly(errorLayout)
        errorMessageView.text = error.reason
        retryButton.visibility = View.GONE // TODO: Retry policy
    }

    private fun renderNoMembership(room: Room) {
        renderIdle()
        joinRoom(room)
    }

    private fun loadRoom() = launch {
        state = state.room().fold(::forError, ::WithRoom)
    }

    private fun loadMessages(room: Room) = launch {
        state = app.messageServiceFor(room)
            .flatMap { it.fetchMessages(10).await() }
            .map<State> { messages -> Loaded.Complete(room, messages) }
            .recover(::forError)
    }

    private fun joinRoom(room: Room) = launch {
        state = app.rooms()
            .flatMap<State> {
                it.joinRoom(room).await()
                    .map { Loaded.WithRoom(room) }
            }
            .recover(::forError)
    }

    private fun sendMessage(message: String) = launch {
        state.room()
            .flatMap { room -> app.messageServiceFor(room) }
            .flatMap { it.sendMessage(message).await() }
        // TODO report message sent or not
    }

    private fun listenToMessages() = launch {
        state.room().flatMap { room -> app.messageServiceFor(room) }
            .map {
                it.messageEvents {
                    when (it) {
                        is Result.Success -> adapter += it.value
                    }
                }
            }
    }

    private fun forError(error: Error): State = when (error) {
        is NoRoomMembershipError -> Loaded.WithoutRoomMembership(error.room)
        else -> Failed(error)
    }

    sealed class State {
        sealed class Loaded : State() {
            abstract val room: Room

            data class WithoutRoomMembership(override val room: Room) : Loaded()
            data class WithRoom(override val room: Room) : Loaded()
            data class Complete(override val room: Room, val messages: List<Message>) : Loaded()
        }

        data class Failed(val error: Error) : State()
        data class Idle(val roomId: Int) : State()
    }

    private suspend fun State.room(): Result<Room, Error> = when (this) {
        is Loaded -> room.asSuccess()
        is Idle -> app.rooms().flatMap { it.fetchRoomBy(roomId).await() }
        is Failed -> error.asFailure()
    }

}

private fun LifecycleOwner.launchOnUi(block: suspend CoroutineScope.() -> Unit): Job =
    launch(UI) {
        when {
            lifecycle.currentState != Lifecycle.State.DESTROYED -> block()
            else -> Log.d("Boo", "Unexpected lifecycle state: ${lifecycle.currentState}")
        }
    }
