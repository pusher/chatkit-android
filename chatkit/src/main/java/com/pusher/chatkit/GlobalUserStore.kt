package com.pusher.chatkit

import com.google.gson.reflect.TypeToken
import com.pusher.platform.Instance
import com.pusher.platform.RequestOptions
import com.pusher.platform.logger.Logger
import com.pusher.platform.tokenProvider.TokenProvider
import java.util.concurrent.ConcurrentHashMap

class GlobalUserStore(
        val instance: Instance,
        val logger: Logger,
        val tokenProvider: TokenProvider?,
        val tokenParams: ChatkitTokenParams?) {

    val users = ConcurrentHashMap<String, User>()

    fun fetchUsersWithIds(userIds: Set<String>, onComplete: UsersListener, onFailure: ErrorListener){

        val path = "/users_by_ids?user_ids=${userIds.joinToString(separator = ",")}"
        val listOfUsersType = object: TypeToken<List<User>>(){}.type

        instance.request(
                options = RequestOptions(
                        method = "GET",
                        path = path
                ),
                tokenProvider = tokenProvider,
                tokenParams = tokenParams,
                onSuccess = { response ->
                    val users = ChatManager.GSON.fromJson<List<User>>(response.body()!!.charStream(), listOfUsersType)
                    users.forEach { user ->
                        this.users.put(user.id, user)
                    }
                    onComplete.onUsers(users)
                },
                onFailure = {
                    error ->  logger.debug("Failed getting list of users $error")
                    onFailure.onError(error)
                }
        )
    }

    fun findOrGetUser(id: String, userListener: UserListener, errorListener: ErrorListener){

        if(users.contains(id)) userListener.onUser(users.getValue(id))

        else{
            fetchUsersWithIds(
                    userIds = setOf(id),
                    onComplete = UsersListener { users ->
                        if(users.isNotEmpty()) userListener.onUser(users[0])
                        else errorListener.onError(elements.NetworkError("User not found!"))
                    },
                    onFailure = errorListener
            )
        }


    }

}