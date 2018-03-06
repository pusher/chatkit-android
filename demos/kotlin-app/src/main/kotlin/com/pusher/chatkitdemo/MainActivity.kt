package com.pusher.chatkitdemo

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.annotation.UiThread
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.View.GONE
import android.view.View.VISIBLE
import com.pusher.chatkit.Room
import com.pusher.chatkitdemo.navigation.open
import com.pusher.chatkitdemo.recyclerview.dataAdapterFor
import com.pusher.chatkitdemo.room.coolName
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.item_room.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.map
import kotlinx.coroutines.experimental.launch
import kotlin.properties.Delegates
import kotlinx.android.synthetic.main.activity_main.room_list as roomListView

class MainActivity : AppCompatActivity() {

    private val adapter = dataAdapterFor(R.layout.item_room) { room: Room ->
        @SuppressLint("SetTextI18n")
        roomNameView.text = room.coolName
        roomItemLayout.setOnClickListener {
            open(room)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        roomListView.adapter = adapter
        roomListView.layoutManager = LinearLayoutManager(this)

        state = State.Idle

        launch {
            app.rooms
                .map { s -> s.findAll() }
                .consumeEach { rooms -> state = State.Loaded(rooms) }
        }
    }

    private var state by Delegates.observable<State>(State.Idle) { _, _, newState ->
        when (newState) {
            is State.Idle -> renderIdle()
            is State.Loaded -> renderLoaded(newState.rooms)
        }
    }

    @UiThread
    private fun renderIdle() = launch(UI) {
        progress.visibility = VISIBLE
        roomListView.visibility = GONE
    }

    @UiThread
    private fun renderLoaded(rooms: List<Room>) = launch(UI) {
        progress.visibility = GONE
        roomListView.visibility = VISIBLE
        adapter.data = rooms
    }

    sealed class State {
        object Idle : State()
        data class Loaded(val rooms: List<Room>) : State()
    }

}

