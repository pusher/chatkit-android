package com.pusher.chatkit.users

fun readTestJson(fileName: String): String = UserSubscriptionEvent::class.java.getResource(
        "/json/subscription/user/$fileName.json").readText()