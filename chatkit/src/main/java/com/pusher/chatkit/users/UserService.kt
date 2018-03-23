package com.pusher.chatkit.users

import com.pusher.chatkit.ChatManager
import com.pusher.chatkit.User
import com.pusher.chatkit.network.parseResponseWhenReady
import com.pusher.platform.network.Promise
import com.pusher.platform.network.asPromise
import com.pusher.util.*
import elements.Error

typealias UserResult = Result<User, Error>
typealias UserListResult = Result<List<User>, Error>
typealias UserListResultPromise = Promise<UserListResult>
typealias UserPromiseResult = Promise<UserResult>

class UserService(
    private val chatManager: ChatManager
) {

    fun fetchUsersBy(userIds: Set<String>): UserListResultPromise {
        val users = userIds.map { id -> getLocalUser(id).orElse { MissingLocalUser(id) } }
        val missingUserIds = Result.failuresOf(users).map { it.id }
        val localUsers = Result.successesOf(users)

        return when {
            missingUserIds.isEmpty() -> localUsers.asSuccess<List<User>, Error>().asPromise()
            else -> chatManager.doGet("/users_by_ids?user_ids=${missingUserIds.joinToString(separator = ",")}")
                .parseResponseWhenReady<List<User>>()
                .onReady { it.map { chatManager.userStore += it } }
                .mapResult { fetchedUsers -> localUsers + fetchedUsers }
        }
    }

    fun fetchUserBy(userId: String): UserPromiseResult =
        fetchUsersBy(setOf(userId)).flatMapResult {
            it.firstOrNull().orElse { userNotFound(userId) }.asPromise()
        }

    internal fun populateUserStore(userIds: Set<String>) {
        fetchUsersBy(userIds)
    }

    data class MissingLocalUser(val id: String) : Error {
        override val reason = "Missing user locally: $id"
    }

    private fun userNotFound(id: String): Error =
        UserNotFound(id)

    data class UserNotFound internal constructor(val id: String) : Error {
        override val reason: String = "Could not load user with id: $id"
    }

    private fun getLocalUser(id: String) =
        chatManager.userStore[id]

}
