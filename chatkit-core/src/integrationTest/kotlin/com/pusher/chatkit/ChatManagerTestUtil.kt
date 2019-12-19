package com.pusher.chatkit

import com.pusher.platform.tokenProvider.TokenProvider

fun createChatManager(
        instanceLocator: String = INSTANCE_LOCATOR,
        userId: String = Users.PUSHERINO,
        tokenProvider: TokenProvider) =
        ChatManager(instanceLocator, userId, TestChatkitDependencies(tokenProvider))