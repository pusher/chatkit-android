package com.pusher.chatkitdemo

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.View.GONE
import android.view.View.VISIBLE
import com.pusher.chatkit.Room
import com.pusher.chatkitdemo.MainActivity.State.Failed
import com.pusher.chatkitdemo.MainActivity.State.Loaded
import com.pusher.chatkitdemo.navigation.open
import com.pusher.chatkitdemo.recyclerview.dataAdapterFor
import com.pusher.chatkitdemo.room.coolName
import com.pusher.platform.network.await
import elements.Error
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.item_room.*
import kotlinx.coroutines.experimental.android.UI
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

    private var state by Delegates.observable<State>(State.Idle) { _, _, state ->
        state.render()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        roomListView.adapter = adapter
        roomListView.layoutManager = LinearLayoutManager(this)

        state = State.Idle

        launch {
            state = app.roomService()
                .flatMap { it.fetchUserRooms().await() }
                .map {rooms -> rooms.filter { it.memberUserIds.size < 100 } }
                .fold(::Failed, ::Loaded)
        }
    }

    private fun State.render() = when (this) {
        is State.Idle -> launch(UI) { renderIdle() }
        is State.Loaded -> launch(UI) { renderLoaded(rooms) }
        is State.Failed -> launch(UI) { renderFailed(error) }
    }

    private fun renderIdle() {
        progress.visibility = VISIBLE
        roomListView.visibility = GONE
        errorView.visibility = GONE
    }

    private fun renderLoaded(rooms: List<Room>) {
        progress.visibility = GONE
        roomListView.visibility = VISIBLE
        errorView.visibility = GONE
        adapter.data = rooms.filter { it.memberUserIds.size < 100 }
    }

    private fun renderFailed(error: Error) {
        progress.visibility = GONE
        roomListView.visibility = GONE
        errorView.visibility = VISIBLE
        errorView.text = error.reason
    }

    sealed class State {
        object Idle : State()
        data class Loaded(val rooms: List<Room>) : State()
        data class Failed(val error: Error) : State()
    }

}

