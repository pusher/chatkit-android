package com.pusher.chatkitdemo.room

import android.os.Bundle
import android.support.annotation.UiThread
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pusher.chatkit.Message
import com.pusher.chatkit.messages.MessageService
import com.pusher.chatkit.rooms.RoomResult
import com.pusher.chatkitdemo.R
import com.pusher.chatkitdemo.app
import com.pusher.chatkitdemo.recyclerview.dataAdapterFor
import com.pusher.chatkitdemo.room.RoomFragment.State.*
import com.pusher.chatkitdemo.showOnly
import com.pusher.platform.network.await
import com.pusher.util.Result
import elements.Error
import kotlinx.android.synthetic.main.activity_entry.*
import kotlinx.android.synthetic.main.fragment_room_loaded.view.*
import kotlinx.android.synthetic.main.include_error.*
import kotlinx.android.synthetic.main.item_message.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.launch
import kotlin.properties.Delegates

class RoomFragment : Fragment() {

    private val views by lazy { arrayOf(idleLayout, loadedLayout, errorLayout) }

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
            state = messageService()
                .flatMap { it.fetchMessages().await() }
                .fold(::Failed, ::Loaded)
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
        state = state.let {
            when (it) {
                is State.Loaded -> Loaded(it.messages + message)
                else -> Loaded(listOf(message))
            }
        }
    }

    fun bind(roomId: Int) {
        state = Idle
        roomIdChannel.offer(roomId)
    }

    private var state by Delegates.observable<State>(Idle) { _, _, newState ->
        newState.render()
    }

    private fun State.render() = when (this) {
        is State.Loaded -> launch(UI) { renderLoaded(messages) }
        is State.Idle -> launch(UI) { renderIdle() }
        is Failed -> launch(UI) { renderFailed(error) }
    }

    @UiThread
    private fun renderIdle() {
        views.showOnly(idleLayout)
        adapter.data = emptyList()
    }

    private fun renderLoaded(messages: List<Message>) {
        views.showOnly(loadedLayout)
        adapter.data = messages
    }

    private fun renderFailed(error: Error) {
        views.showOnly(errorLayout)
        errorMessageView.text = error.reason
    }

    sealed class State {
        data class Loaded(val messages: List<Message>) : State()
        data class Failed(val error: Error) : State()
        object Idle : State()

        fun appendMessage(message: Message) = when (this) {
            is Loaded -> Loaded(messages + message)
            else -> Loaded(listOf(message))
        }

    }

}
