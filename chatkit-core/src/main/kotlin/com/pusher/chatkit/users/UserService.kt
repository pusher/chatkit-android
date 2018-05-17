package com.pusher.chatkit.users

import com.pusher.chatkit.*
import com.pusher.chatkit.util.toJson
import com.pusher.chatkit.rooms.Room
import com.pusher.platform.network.*
import com.pusher.util.*
import elements.Error
import elements.Errors
import java.util.concurrent.Future

internal class UserService(
    override val chatManager: ChatManager
) : HasChat {

    private val userStore by lazy { UserStore() }

    fun fetchUsersBy(userIds: Set<String>): Future<Result<List<User>, Error>> {
        val users = userIds.map { id -> getLocalUser(id).orElse { id } }
        val missingUserIds = Result.failuresOf(users).map { it }
        val localUsers = Result.successesOf(users)
        return when {
            missingUserIds.isEmpty() -> Futures.now(localUsers.asSuccess())
            else -> chatManager.doGet<List<User>>("/users_by_ids?user_ids=${missingUserIds.joinToString(separator = ",")}")
                .map { usersResult ->
                    usersResult.map { loadedUsers ->
                        userStore += loadedUsers
                        loadedUsers + localUsers
                    }
                }
        }
    }

    fun fetchUserBy(userId: String): Future<Result<User, Error>> =
        fetchUsersBy(setOf(userId)).map { usersResult ->
            usersResult.flatMap { users ->
                users.firstOrNull().orElse { userNotFound(userId) }
            }
        }

    fun addUsersToRoom(roomId: Int, userIds: List<String>) =
        UserIdsWrapper(userIds).toJson().toFuture()
            .flatMapFutureResult { body ->
                chatManager.doPut<Unit>("/rooms/$roomId/users/add", body)
            }

    fun removeUsersFromRoom(roomId: Int, userIds: List<String>) =
        UserIdsWrapper(userIds).toJson().toFuture()
            .flatMapFutureResult { body ->
                chatManager.doPut<Unit>("/rooms/$roomId/users/remove", body)
            }

    internal data class UserIdsWrapper(val userIds: List<String>)

    fun joinRoom(user: CurrentUser, roomId: Int): Future<Result<Room, Error>> =
        chatManager.doPost<Room>("/users/${user.id}/rooms/$roomId/join")
            .saveRoomWhenReady()

    fun userFor(userAware: HasUser): Future<Result<User, Error>> =
        fetchUserBy(userAware.userId)

    fun usersFor(userAware: List<HasUser>): Future<Result<List<User>, Error>> =
        fetchUsersBy(userAware.map { it.userId }.toSet())

    internal fun populateUserStore(userIds: Set<String>) {
        fetchUsersBy(userIds)
    }

    private fun getLocalUser(id: String) =
        userStore[id]

}

interface HasUser {
    val userId: String
}

private fun userNotFound(id: String): Error =
    Errors.other("Could not load user with id: $id")
