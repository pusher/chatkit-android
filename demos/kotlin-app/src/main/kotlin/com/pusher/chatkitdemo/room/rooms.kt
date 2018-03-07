package com.pusher.chatkitdemo.room

import com.pusher.chatkit.Room

val Room.coolName
    get() = "#$name"
