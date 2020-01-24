package com.pusher.chatkit.users

import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.presence.PresenceService
import com.pusher.chatkit.util.toJson
import com.pusher.util.Result
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

        val fetchResult: Result<List<User>, Error> =
                if (missingUserIds.isNotEmpty()) {
                    val missingUserIdsQueryParams = missingUserIds.joinToString(separator = "&") {
                        "id=${URLEncoder.encode(it, "UTF-8")}"
                    }

                    client.doGet("/users_by_ids?$missingUserIdsQueryParams")
                } else {
                    listOf<User>().asSuccess()
                }

        return fetchResult.map { fetchedUsers ->
            fetchedUsers.forEach { fetchedUser ->
                knownUsers[fetchedUser.id] = fetchedUser
            }

            userIds.forEach {
                presenceService.subscribeToUser(it)
            }

            userIds.map { userId ->
                userId to knownUsers[userId]!!
            }.toMap()
        }
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