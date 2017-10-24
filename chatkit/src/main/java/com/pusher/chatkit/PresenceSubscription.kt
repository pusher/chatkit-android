package com.pusher.chatkit

//TODO()
/*
*   func setupPresenceSubscription(delegate: PCChatManagerDelegate) {
        let path = "/users/\(self.id)/presence"

        let subscribeRequest = PPRequestOptions(method: HTTPMethod.SUBSCRIBE.rawValue, path: path)

        var resumableSub = PPResumableSubscription(
            instance: self.instance,
            requestOptions: subscribeRequest
        )

        self.presenceSubscription = PCPresenceSubscription(
            instance: self.instance,
            resumableSubscription: resumableSub,
            userStore: self.userStore,
            roomStore: self.roomStore,
            delegate: delegate
        )

        self.instance.subscribeWithResume(
            with: &resumableSub,
            using: subscribeRequest,
            onEvent: self.presenceSubscription!.handleEvent
        )
    }

* */