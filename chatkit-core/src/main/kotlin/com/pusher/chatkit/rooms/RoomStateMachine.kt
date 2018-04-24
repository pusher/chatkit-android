package com.pusher.chatkit.rooms

import com.pusher.chatkit.ChatManager
import com.pusher.chatkit.CurrentUser
import com.pusher.chatkit.Message
import com.pusher.chatkit.Room
import com.pusher.chatkit.rooms.RoomState.Item
import com.pusher.chatkit.rooms.RoomState.Type.*
import com.pusher.platform.Cancelable
import com.pusher.platform.Scheduler
import com.pusher.platform.network.Promise
import com.pusher.util.Result
import com.pusher.util.flatMapResult
import com.pusher.util.mapResult
import com.pusher.util.recover
import elements.Error
import elements.Errors
import elements.Subscription
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.LinkedBlockingDeque

class RoomStateMachine internal constructor(
    private val backgroundScheduler: Scheduler,
    internal val chatManager: ChatManager
) {

    private val listeners = ConcurrentLinkedQueue<(RoomState) -> Unit>()

    private val updates: BlockingQueue<(RoomState) -> RoomState> = LinkedBlockingDeque()
    private var active = true

    private var _state: RoomState = RoomState.Initial(::handleAction)
    val state: RoomState
        get() = _state

    init {
        backgroundScheduler.schedule {
            while (active) {
                val update = updates.take()
                _state = update(state)
                chatManager.logger.verbose("Room state updated to $state")
                listeners.forEach { report -> report(state) }
            }
        }
    }

    internal fun handleAction(action: RoomAction): RoomTask = when (action) {
        is RoomAction.LoadRoom -> LoadRoomTask(this, action.roomId)
        is RoomAction.LoadMessages -> LoadMessagesTask(this, action.room)
        is RoomAction.Join -> JoinTask(this)
        is RoomAction.AddMessage -> AddMessageTask(this, action.room, action.message)
        is RoomAction.Retry -> RetryTask(this)
        is RoomAction.LoadMore -> LoadMoreTask(this, action.messageCount)
    }.also { task: InternalRoomTask ->
        chatManager.logger.verbose("Room action triggered state updated to $action")
        backgroundScheduler.schedule(task)
    }

    internal fun update(stateUpdate: (RoomState) -> RoomState) {
        updates += stateUpdate
    }

    fun onStateChanged(listener: (RoomState) -> Unit) {
        listeners += listener
        listener(state)
    }

    fun cancel() {
        active = false
    }

    internal fun whenLoaded(block: RoomState.Ready.() -> RoomState) = { state: RoomState ->
        when (state) {
            is RoomState.Ready -> block(state)
        // TODO: replace with soft failure
            else -> RoomState.Failed(::handleAction, Errors.other("Can't add message with state: $state"))
        }
    }

}

interface RoomTask : Cancelable

private sealed class InternalRoomTask(
    val machine: RoomStateMachine
) : RoomTask, () -> Unit {

    private var _active = true
    protected val active = _active

    override fun cancel() {
        _active = false
        onCancel()
    }

    abstract fun onCancel()

}

private class LoadRoomTask(
    machine: RoomStateMachine,
    val roomId: Int
) : InternalRoomTask(machine) {

    var loadRoomPromise: Promise<RoomState>? = null

    override fun invoke() {
        loadRoomPromise = machine.chatManager.currentUser
            .flatMapResult { user -> loadRoom(user, roomId) }
            .recover { RoomState.Failed(machine::handleAction, it) }
            .onReady { state -> machine.update { state } }
    }

    private fun loadRoom(user: CurrentUser, roomId: Int): Promise<Result<RoomState, Error>> =
        machine.chatManager.roomService().fetchRoomBy(user.id, roomId)
            .mapResult { room -> validateMembership(room, user.id) }

    private fun validateMembership(room: Room, userId: String) = when {
        room.memberUserIds.contains(userId) -> RoomState.RoomLoaded(machine::handleAction, room)
        else -> RoomState.NoMembership(machine::handleAction, room)
    }

    override fun onCancel() {
        loadRoomPromise?.cancel()
    }

}

private class LoadMessagesTask(
    machine: RoomStateMachine,
    val room: Room
) : InternalRoomTask(machine) {

    private var subscription: Subscription? = null

    override fun invoke() {
        machine.chatManager.messageService(room)
            .messageEvents { event ->
                event.map { message -> processNewMessage(message) }
                    .map {
                        it.mapResult { item ->
                            machine.update(machine.whenLoaded { copy(items = listOf<Item>(item) + items) })
                        }
                    }
            }.onReady { result ->
                result.map { subscription = it }
                    .recover { error -> machine.update { RoomState.Failed(machine::handleAction, error) } }
            }
    }

    fun processNewMessage(message: Message): Promise<Result<Item.Loaded, Error>> =
        message.asDetails().mapResult { details -> Item.Loaded(details) }
            .onReady { result ->
                machine.update { oldState ->
                    result.map { item -> oldState + item }
                        .recover { error -> RoomState.Failed(machine::handleAction, error) }
                }

            }

    private fun Message.asDetails(): Promise<Result<Item.Details, Error>> =
        machine.chatManager.userService().userFor(this)
            .mapResult { it.name ?: "???" }
            .mapResult { userName -> Item.Details(userName, text ?: "") }

    private operator fun RoomState.plus(item: Item): RoomState = when (this) {
        is RoomState.RoomLoaded -> RoomState.Ready(machine::handleAction, room, listOf(item))
        is RoomState.Ready -> copy(items = listOf(item) + items)
        else -> RoomState.Failed(machine::handleAction, prematureMessageError())
    }

    private fun prematureMessageError(): Error =
        Errors.other("Received message before being ready. [${machine.state}]")

    override fun onCancel() {
        subscription?.unsubscribe()
    }

}

private class JoinTask(machine: RoomStateMachine) : InternalRoomTask(machine) {
    override fun invoke() = Unit
    override fun onCancel() = Unit
}

private class AddMessageTask(machine: RoomStateMachine, val room: Room, val messageText: CharSequence) : InternalRoomTask(machine) {

    var pendingPromise: Promise<Any>? = null

    override fun invoke() {
        pendingPromise = machine.chatManager.currentUser
            .mapResult { currentUser -> currentUser.name ?: "???" }
            .mapResult { userName -> Item.Pending(Item.Details(userName, messageText)) as Item }
            .recover { error -> Item.Failed(Item.Details(messageText, "Could not find name"), error) }
            .flatMap { item ->
                machine.update(machine.whenLoaded { copy(items = listOf(item) + items) })
                machine.chatManager.messageService(room).sendMessage(messageText)
                    .mapResult { machine.whenLoaded { copy(items = items - item) } }
                    .recover { error ->
                        machine.whenLoaded { copy(items = listOf(Item.Failed(item.details, error)) + items - item) }
                    }
            }
            .onReady { update -> machine.update(update) }
    }

    override fun onCancel() {
        pendingPromise?.cancel()
    }

}

private class RetryTask(machine: RoomStateMachine) : InternalRoomTask(machine) {
    override fun invoke() = Unit
    override fun onCancel() = Unit
}

private class LoadMoreTask(machine: RoomStateMachine, val messageCount: Int) : InternalRoomTask(machine) {
    override fun invoke() = Unit
    override fun onCancel() = Unit
}

internal sealed class RoomAction {
    data class LoadRoom(val roomId: Int) : RoomAction()
    data class LoadMessages(val room: Room) : RoomAction()
    data class Join(val room: Room) : RoomAction()
    data class AddMessage(val room: Room, val message: CharSequence) : RoomAction()
    data class LoadMore(val messageCount: Int) : RoomAction()
    object Retry : RoomAction()
}

private typealias Actor = (RoomAction) -> RoomTask

sealed class RoomState(val type: Type) {

    class Initial internal constructor(private val handle: Actor) : RoomState(INITIAL) {
        fun loadRoom(roomId: Int): RoomTask = handle(RoomAction.LoadRoom(roomId))
    }

    data class Idle internal constructor(val roomId: Int) : RoomState(IDLE)

    data class NoMembership internal constructor(
        private val handle: Actor,
        val room: Room
    ) : RoomState(NO_MEMBERSHIP) {
        fun join(): RoomTask = handle(RoomAction.Join(room))
    }

    data class RoomLoaded internal constructor(
        private val handle: Actor,
        val room: Room
    ) : RoomState(ROOM_LOADED) {
        fun loadMessages() = handle(RoomAction.LoadMessages(room))
    }

    data class Ready internal constructor(
        private val handle: Actor,
        val room: Room,
        val items: List<Item>
    ) : RoomState(READY) {
        fun addMessage(text: CharSequence): RoomTask = handle(RoomAction.AddMessage(room, text))
        @JvmOverloads
        fun loadMore(messageCount: Int = 10): RoomTask = handle(RoomAction.LoadMore(messageCount))
    }

    data class Failed internal constructor(private val handle: Actor, val error: Error) : RoomState(FAILED) {
        fun retry(): RoomTask = handle(RoomAction.Retry)
    }

    sealed class Item(val type: Type) {
        abstract val details: Details

        data class Loaded internal constructor(override val details: Details) : Item(Type.LOADED)
        data class Pending internal constructor(override val details: Details) : Item(Type.PENDING)
        data class Failed internal constructor(override val details: Details, val error: Error) : Item(Type.FAILED)

        enum class Type {
            LOADED, PENDING, FAILED
        }

        data class Details(val userName: CharSequence, val message: CharSequence)

    }

    enum class Type {
        INITIAL, IDLE, NO_MEMBERSHIP, ROOM_LOADED, READY, FAILED
    }

}
