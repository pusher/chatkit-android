package com.pusher.chatkitdemo.arch

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProviders
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity

inline fun <reified A : ViewModel> FragmentActivity.viewModel() =
    ViewModelProviders.of(this).get(A::class.java)

inline fun <reified A : ViewModel> Fragment.viewModel() =
    ViewModelProviders.of(this).get(A::class.java)
