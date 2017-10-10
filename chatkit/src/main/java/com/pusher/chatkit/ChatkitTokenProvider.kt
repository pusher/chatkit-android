package com.pusher.chatkit

import com.pusher.platform.Cancelable
import com.pusher.platform.tokenProvider.TokenProvider
import elements.Error

class ChatkitTokenProvider: TokenProvider {


    override fun fetchToken(tokenParams: Any?, onSuccess: (String) -> Unit, onFailure: (Error) -> Unit): Cancelable {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun clearToken(token: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}