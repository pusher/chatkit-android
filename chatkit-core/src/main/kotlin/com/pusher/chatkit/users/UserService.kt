package com.pusher.chatkit.users

import com.pusher.chatkit.PlatformClient
import com.pusher.chatkit.rooms.RoomService
import com.pusher.chatkit.util.toJson
import com.pusher.platform.logger.Logger
import com.pusher.platform.network.Futures
import com.pusher.platform.network.map
import com.pusher.platform.network.toFuture
import com.pusher.util.Result
import com.pusher.util.asSuccess
import com.pusher.util.flatMapFutureResult
import com.pusher.util.orElse
import elements.Error
import elements.Errors
import java.net.URLEncoder
import java.util.concurrent.Future

internal class UserService(
        userId: String,
        consumeEvent: (UserSubscriptionEvent) -> Unit,
        private val client: PlatformClient,
        roomService: RoomService,
        logger: Logger
) {
    private val userStore = UserStore()
    private val userSubscription = UserSubscription(
            client,
            roomService,
            consumeEvent,
            logger
    )

    fun subscribe() = userSubscription.connect()
    fun unsubscribe() = userSubscription.unsubscribe()

    fun fetchUsersBy(userIds: Set<String>): Future<Result<List<User>, Error>> {
        val users = userIds.map { id -> getLocalUser(id).orElse { id } }
        val missingUserIds = Result.failuresOf(users).map { it }
        val localUsers = Result.successesOf(users)
        val missingUserIdsQs = missingUserIds.map {
                userId -> "id=${URLEncoder.encode(userId, "UTF-8")}"
            }.joinToString(separator = "&")

        return when {
            missingUserIds.isEmpty() -> Futures.now(localUsers.asSuccess())
            else -> client.doGet<List<User>>("/users_by_ids?$missingUserIdsQs")
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
                client.doPut<Unit>("/rooms/$roomId/users/add", body)
            }

    fun removeUsersFromRoom(roomId: Int, userIds: List<String>) =
        UserIdsWrapper(userIds).toJson().toFuture()
            .flatMapFutureResult { body ->
                client.doPut<Unit>("/rooms/$roomId/users/remove", body)
            }

    internal data class UserIdsWrapper(val userIds: List<String>)

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
