package com.pusher.chatkitdemo

/**
 * Simplified way to get the name of a type
 */
inline fun <reified T> nameOf(): String = nameOf(T::class.java)

fun <T> nameOf(type: Class<T>) : String = type.simpleName
