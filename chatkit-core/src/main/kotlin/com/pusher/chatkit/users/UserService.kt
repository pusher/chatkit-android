package com.pusher.chatkit.users

import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.util.toJson
import com.pusher.util.Result
import com.pusher.util.asFailure
import com.pusher.util.asSuccess
import com.pusher.util.orElse
import elements.Error
import elements.Errors
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

class UserService(
        private val client: PlatformClient
) {
    private val knownUsers = ConcurrentHashMap<String, User>()

    fun fetchUsersBy(userIds: Set<String>): Result<Map<String, User>, Error> {
        val missingUserIds = userIds.filter { id -> knownUsers[id] == null }

        if (missingUserIds.isNotEmpty()) {
            val missingUserIdsQs =
                    missingUserIds.map { userId ->
                        "id=${URLEncoder.encode(userId, "UTF-8")}"
                    }.joinToString(separator = "&")

            client.doGet<List<User>>("/users_by_ids?$missingUserIdsQs")
                    .map { loadedUsers ->
                        loadedUsers.map { user ->
                            knownUsers[user.id] = user
                        }
                    }
        }

        return userIds.fold(
                mutableMapOf<String, User>().asSuccess<MutableMap<String, User>, Error>()
        ) { accumulator, userId ->
            val user = knownUsers[userId]
            if (user == null) {
                userNotFound(userId).asFailure()
            } else {
                accumulator.map { it[userId] = user; it }
            }
        }.map {
            // Return an immutable copy
            it.toMap()
        }
    }

    fun fetchUserBy(userId: String): Result<User, Error> =
        fetchUsersBy(setOf(userId)).flatMap { users ->
                users.values.firstOrNull().orElse { userNotFound(userId) }
            }

    fun addUsersToRoom(roomId: Int, userIds: List<String>) =
        UserIdsWrapper(userIds).toJson()
            .flatMap { body ->
                client.doPut<Unit>("/rooms/$roomId/users/add", body)
            }

    fun removeUsersFromRoom(roomId: Int, userIds: List<String>) =
        UserIdsWrapper(userIds).toJson()
            .flatMap { body ->
                client.doPut<Unit>("/rooms/$roomId/users/remove", body)
            }

    internal data class UserIdsWrapper(val userIds: List<String>)

    internal fun populateUserStore(userIds: Set<String>) {
        fetchUsersBy(userIds)
    }

    private fun userNotFound(id: String): Error =
            Errors.other("Could not load user with id: $id")
}

// TODO: Unused?
interface HasUser {
    val userId: String
}
