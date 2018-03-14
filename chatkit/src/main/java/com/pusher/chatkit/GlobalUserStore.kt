package com.pusher.chatkit

import com.google.gson.reflect.TypeToken
import com.pusher.platform.Instance
import com.pusher.platform.RequestOptions
import com.pusher.platform.network.Promise
import com.pusher.platform.network.asPromise
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.Result
import com.pusher.util.flatMapResult
import com.pusher.util.mapResult
import com.pusher.util.orElse
import elements.Error
import elements.Errors
import elements.OtherError
import java.util.concurrent.ConcurrentHashMap


private val listOfUsersType = object : TypeToken<List<User>>() {}.type

class GlobalUserStore(
    val apiInstance: Instance,
    val tokenProvider: TokenProvider?,
    val tokenParams: ChatkitTokenParams?) {

    val users = ConcurrentHashMap<String, User>()

    fun fetchUsersWithIds(userIds: Set<String>): Promise<Result<List<User>, Error>> = apiInstance.request(
        options = RequestOptions(
            method = "GET",
            path = "/users_by_ids?user_ids=${userIds.joinToString(separator = ",")}"
        ),
        tokenProvider = tokenProvider,
        tokenParams = tokenParams
    ).mapResult {
        ChatManager.GSON.fromJson<List<User>>(it.body()!!.charStream(), listOfUsersType).apply {
            forEach { user ->
                users[user.id] = user
            }
        }
    }

    fun findOrGetUser(id: String): Promise<Result<User, Error>> =
        users[id].orElse<User, Error> { OtherError("") }
            .asPromise()
            .flatMap { findUser(id) }

    private fun findUser(id: String): Promise<Result<User, Error>> =
        fetchUsersWithIds(setOf(id)).flatMapResult {
            it.firstOrNull().orElse { Errors.network("User not found!") }.asPromise()
        }

}