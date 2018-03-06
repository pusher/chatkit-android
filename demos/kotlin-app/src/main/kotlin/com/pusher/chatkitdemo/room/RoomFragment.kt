package com.pusher.chatkitdemo.room

import android.os.Bundle
import android.support.annotation.UiThread
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import com.pusher.chatkit.Message
import com.pusher.chatkit.RoomSubscription.Event.OnError
import com.pusher.chatkit.RoomSubscription.Event.OnNewMessage
import com.pusher.chatkit.rooms.RoomResult
import com.pusher.chatkitdemo.R
import com.pusher.chatkitdemo.app
import com.pusher.chatkitdemo.parallel.onLifecycle
import com.pusher.chatkitdemo.recyclerview.dataAdapterFor
import kotlinx.android.synthetic.main.fragment_room.*
import kotlinx.android.synthetic.main.fragment_room.view.*
import kotlinx.android.synthetic.main.item_message.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.channels.*
import kotlinx.coroutines.experimental.launch
import kotlin.properties.Delegates

class RoomFragment : Fragment() {

    private val roomIdBroadcast = BroadcastChannel<Int>(Channel.CONFLATED)
    private val room
        get() = roomIdBroadcast.openSubscription()
            .flatMap { id -> app.rooms.map { it.findBy(id) } }
            .map { (it as? RoomResult.Found)?.room ?: TODO() }

    private val messageService =
        room.flatMap { r -> app.messageServiceFor(r) }

    private val messageEvents
        get() = onLifecycle { messageService.flatMap { it.messageEvents() } }

    private val adapter = dataAdapterFor<Message>(R.layout.item_message) { message ->
        userNameView.text = message.user?.name ?: "???"
        messageView.text = message.text
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launch {
            room.consumeEach { activity?.title = it.coolName }
        }
        launch {
            messageEvents.consumeEach { event ->
                when (event) {
                    is OnNewMessage -> addMessage(event.message)
                    is OnError -> TODO("${event.error}")
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_room, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(view) {
            messageList.adapter = adapter
            messageList.layoutManager = LinearLayoutManager(activity)
            sendButton.setOnClickListener {
                val message = messageInput.text.toString()
                sendMessage(message)
            }
        }
    }

    private fun sendMessage(message: String) {
        launch {
            messageService.receive().sendMessage(message) {
                Log.d("TEST", it.toString())
            }
        }
    }

    private fun addMessage(message: Message) {
        state += message
    }

    fun bind(roomId: Int) {
        state = State.Idle
        roomIdBroadcast.offer(roomId)
    }

    private var state by Delegates.observable<State>(State.Idle) { _, _, newState ->
        when (newState) {
            is State.Loaded -> renderLoaded(newState.messages)
            is State.Idle -> renderIdle()
        }
    }

    @UiThread
    private fun renderIdle() = launch(UI) {
        progress.visibility = VISIBLE
        messageList.visibility = GONE
        adapter.data = emptyList()
    }

    private fun renderLoaded(messages: List<Message>) = launch(UI) {
        progress.visibility = GONE
        messageList.visibility = VISIBLE
        adapter.data = messages
    }

    sealed class State {
        data class Loaded(val messages: List<Message>) : State()
        object Idle : State()

        operator fun plus(message: Message): State =
            when (this) {
                is Loaded -> Loaded(this.messages + message)
                is Idle -> Loaded(listOf(message))
            }

    }

}