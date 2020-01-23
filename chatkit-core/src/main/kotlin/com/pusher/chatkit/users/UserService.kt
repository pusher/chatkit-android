package com.pusher.chatkit.users

import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.presence.PresenceService
import com.pusher.chatkit.util.toJson
import com.pusher.util.Result
import com.pusher.util.asFailure
import com.pusher.util.asSuccess
import elements.Error
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

class UserService(
        private val client: PlatformClient,
        private val presenceService: PresenceService
) {
    private val knownUsers = ConcurrentHashMap<String, User>()

    fun fetchUsersBy(userIds: Set<String>): Result<Map<String, User>, Error> {
        val missingUserIds = userIds.filter { id -> knownUsers[id] == null }

        if (missingUserIds.isNotEmpty()) {
            val missingUserIdsQs =
                    missingUserIds.joinToString(separator = "&") {
                        "id=${URLEncoder.encode(it, "UTF-8")}"
                    }

            val fetchResult = client.doGet<List<User>>("/users_by_ids?$missingUserIdsQs")
            when (fetchResult) {
                is Result.Success -> {
                    for (fetchedUser in fetchResult.value) {
                        knownUsers[fetchedUser.id] = fetchedUser
                    }
                }
                is Result.Failure -> return fetchResult.error.asFailure()
            }
        }

        userIds.forEach {
            presenceService.subscribeToUser(it)
        }

        return userIds.map { userId ->
                    userId to knownUsers[userId]!!
                }.toMap().asSuccess()
    }

    fun fetchUserBy(userId: String): Result<User, Error> =
            fetchUsersBy(setOf(userId)).map { users ->
                users.values.first()
            }

    fun addUsersToRoom(roomId: String, userIds: List<String>) =
            UserIdsWrapper(userIds).toJson()
                    .flatMap { body ->
                        client.doPut<Unit>("/rooms/${URLEncoder.encode(roomId, "UTF-8")}/users/add", body)
                    }

    fun removeUsersFromRoom(roomId: String, userIds: List<String>) =
            UserIdsWrapper(userIds).toJson()
                    .flatMap { body ->
                        client.doPut<Unit>("/rooms/${URLEncoder.encode(roomId, "UTF-8")}/users/remove", body)
                    }

    internal data class UserIdsWrapper(val userIds: List<String>)

    internal fun populateUserStore(userIds: Set<String>) {
        fetchUsersBy(userIds)
    }

}