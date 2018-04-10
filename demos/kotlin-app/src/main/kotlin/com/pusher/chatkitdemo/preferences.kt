package com.pusher.chatkitdemo

import android.content.Context
import android.content.Context.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private const val USER_PREFERENCES_NAME = "USER_PREFERENCES_NAME"

class UserPreferences(context: Context) {

    private val preferences = SharedPreferencesExtension(context.applicationContext, USER_PREFERENCES_NAME)

    var userId by preferences.string("userId")
    var token by preferences.string("token")

}

private class SharedPreferencesExtension(context: Context, name: String) {

    private val preferences = context.getSharedPreferences(name, MODE_PRIVATE)

    fun string(key: String) = object : ReadWriteProperty<Any?, String?> {
        override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) =
            preferences.edit().putString(key, value).apply()

        override operator fun getValue(thisRef: Any?, property: KProperty<*>): String? =
            preferences.getString(key, null)
    }
}
