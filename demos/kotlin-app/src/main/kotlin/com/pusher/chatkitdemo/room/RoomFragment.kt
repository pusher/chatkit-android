package com.pusher.chatkitdemo.room

import android.os.Bundle
import android.support.annotation.UiThread
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import com.pusher.chatkit.Message
import com.pusher.chatkit.messages.MessageService
import com.pusher.chatkit.rooms.RoomResult
import com.pusher.chatkitdemo.R
import com.pusher.chatkitdemo.app
import com.pusher.chatkitdemo.recyclerview.dataAdapterFor
import com.pusher.platform.network.await
import com.pusher.util.Result
import elements.Error
import kotlinx.android.synthetic.main.fragment_room.*
import kotlinx.android.synthetic.main.fragment_room.view.*
import kotlinx.android.synthetic.main.item_message.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.launch
import kotlin.properties.Delegates

class RoomFragment : Fragment() {

    private val roomIdChannel = Channel<Int>(Channel.CONFLATED)

    private suspend fun roomId(): Int =
        roomIdChannel.receive()

    private suspend fun room(): RoomResult =
        app.rooms().flatMap { it.findBy(roomId()) }

    private suspend fun messageService(): Result<MessageService, Error> =
        room().flatMap { room -> app.messageServiceFor(room) }

    private suspend fun messageEvents(): Result<Message, Error> =
        messageService().flatMap { it.messageEvents().await() }

    private val adapter = dataAdapterFor<Message>(R.layout.item_message) { message ->
        userNameView.text = message.user?.name ?: "???"
        messageView.text = message.text
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launch {
            activity?.title = room().map { it.coolName }.recover { "???" }
        }
        launch {
            messageEvents().fold(
                { TODO("$it") },
                { addMessage(it) }
            )
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
            messageService().map { it.sendMessage(message) }
                .recover { TODO("report $it") }
        }
    }

    private fun addMessage(message: Message) {
        state += message
    }

    fun bind(roomId: Int) {
        state = State.Idle
        roomIdChannel.offer(roomId)
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
