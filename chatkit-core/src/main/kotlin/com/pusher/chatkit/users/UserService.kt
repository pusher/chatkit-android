package com.pusher.chatkit.users

import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.presence.PresenceService
import com.pusher.chatkit.util.toJson
import com.pusher.util.Result
import com.pusher.util.collect
import com.pusher.util.orElse
import elements.Error
import elements.Errors
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

            client.doGet<List<User>>("/users_by_ids?$missingUserIdsQs")
                    .map { loadedUsers ->
                        loadedUsers.map { user ->
                            knownUsers[user.id] = user
                        }
                    }
        }

        userIds.forEach {
            presenceService.subscribeToUser(it)
        }

        return userIds
                .map { userId ->
            knownUsers[userId].orElse {
                userNotFound(userId)
            }.map { user ->
                userId to user
            }
        }.collect().map { pairs ->
            pairs.toMap()
        }.mapFailure { errors ->
            Errors.compose(errors)
        }
    }

    fun fetchUserBy(userId: String): Result<User, Error> =
        fetchUsersBy(setOf(userId)).flatMap { users ->
                users.values.firstOrNull().orElse { userNotFound(userId) }
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

    private fun userNotFound(id: String): Error =
            Errors.other("Could not load user with id: $id")
}
