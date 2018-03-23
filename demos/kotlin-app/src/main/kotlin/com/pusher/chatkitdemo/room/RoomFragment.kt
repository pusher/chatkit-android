package com.pusher.chatkitdemo.room

import android.arch.lifecycle.Lifecycle.State.STARTED
import android.arch.lifecycle.LifecycleOwner
import android.os.Bundle
import android.support.annotation.UiThread
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pusher.chatkit.Room
import com.pusher.chatkit.rooms.RoomState
import com.pusher.chatkit.rooms.RoomState.*
import com.pusher.chatkitdemo.R
import com.pusher.chatkitdemo.app
import com.pusher.chatkitdemo.nameOf
import com.pusher.chatkitdemo.recyclerview.dataAdapterFor
import com.pusher.chatkitdemo.showOnly
import elements.Error
import kotlinx.android.synthetic.main.fragment_room.*
import kotlinx.android.synthetic.main.fragment_room_loaded.view.*
import kotlinx.android.synthetic.main.include_error.*
import kotlinx.android.synthetic.main.item_message.*
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

class RoomFragment : Fragment() {

    private val stateMachine by lazy { app.chat.roomStateMachine() }

    private val views by lazy { arrayOf(idleLayout, loadedLayout, errorLayout) }

    private val adapter = dataAdapterFor<Item> {
        on<Item.Loaded>(R.layout.item_message) { (details) ->
            userNameView.text = details.userName
            messageView.text = details.message
        }
        on<Item.Pending>(R.layout.item_message_pending) { (details) ->
            userNameView.text = details.userName
            messageView.text = details.message
        }
        on<Item.Failed>(R.layout.item_message_pending) { (details, error) ->
            userNameView.text = details.userName
            messageView.text = details.message
        }
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
                val messageText = messageInput.text
                if (messageText.isNotBlank()) {
                    stateMachine.state.let { state ->
                        when(state) {
                            is Loaded -> state.addMessage(messageText)
                            else -> TODO()
                        }
                    }
                    messageInput.setText("")
                }
            }
        }
    }

    fun bind(roomId: Int) = stateMachine.onStateChanged { state ->
        state.render()
        when(state) {
            is Initial -> state.loadRoom(roomId)
            is NoMembership -> state.join()
            is RoomLoaded -> state.loadMessages()
            else -> Log.d(nameOf<RoomFragment>(), "State $state didn't trigger anything.")
        }
    }

    private fun RoomState.render(): Job = when (this) {
        is Initial -> renderIdle()
        is Idle -> renderIdle()
        is NoMembership -> renderNoMembership()
        is RoomLoaded -> renderLoadedRoom(room)
        is Loaded -> renderLoadedCompletely(room, items)
        is Failed -> renderFailed(error)
    }

    @UiThread
    private fun renderIdle() = launchOnUi {
        views.showOnly(idleLayout)
        adapter.data = emptyList()
    }

    private fun renderLoadedRoom(room: Room) = launchOnUi {
        views.showOnly(loadedLayout)
        activity?.title = room.coolName
    }

    private fun renderLoadedCompletely(room: Room, messages: List<Item>) = launchOnUi {
        views.showOnly(loadedLayout)
        activity?.title = room.coolName
        adapter.data = messages
    }

    private fun renderFailed(error: Error) = launchOnUi {
        views.showOnly(errorLayout)
        errorMessageView.text = error.reason
        retryButton.visibility = View.GONE // TODO: Retry policy
    }

    private fun renderNoMembership() =
        renderIdle()

}

private fun LifecycleOwner.launchOnUi(block: suspend CoroutineScope.() -> Unit) = when {
    lifecycle.currentState > STARTED -> launch(context = UI, block = block)
    else -> launch { Log.d("Boo", "Unexpected lifecycle state: ${lifecycle.currentState}") }
}
