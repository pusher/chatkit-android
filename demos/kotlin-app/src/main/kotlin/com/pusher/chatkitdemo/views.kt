package com.pusher.chatkitdemo

import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE

fun <A : View> Array<A>.showOnly(view: View) = forEach {
    when (it) {
        view -> it.visibility = VISIBLE
        else -> it.visibility = GONE
    }
}
