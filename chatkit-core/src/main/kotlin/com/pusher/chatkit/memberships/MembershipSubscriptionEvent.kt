package com.pusher.chatkit.memberships

import elements.Error


typealias MembershipSubscriptionConsumer = (MembershipSubscriptionEvent) -> Unit

sealed class MembershipSubscriptionEvent {
    data class InitialState(val userIds: List<String>) : MembershipSubscriptionEvent()
    data class UserJoined(val userId: String) : MembershipSubscriptionEvent()
    data class UserLeft(val userId: String) : MembershipSubscriptionEvent()
    data class ErrorOccurred(val error: Error) : MembershipSubscriptionEvent()
}