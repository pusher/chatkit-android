package com.pusher.chatkit

import com.pusher.chatkit.files.AttachmentBody
import com.pusher.chatkit.files.DataAttachment
import com.pusher.chatkit.util.parseAs
import com.pusher.platform.Instance
import com.pusher.platform.RequestOptions
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.network.DataParser
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.Result
import elements.Error
import java.util.concurrent.Future

class PlatformClient(
        private val platformInstance: Instance,
        private val tokenProvider: TokenProvider
) {

    internal inline fun <reified A> doPost(
            path: String,
            body: String = "",
            noinline responseParser: DataParser<A> = { it.parseAs() }
    ): Future<Result<A, Error>> =
            doRequest("POST", path, body, responseParser)

    internal inline fun <reified A> doPut(
            path: String,
            body: String = "",
            noinline responseParser: DataParser<A> = { it.parseAs() }
    ): Future<Result<A, Error>> =
            doRequest("PUT", path, body, responseParser)

    internal inline fun <reified A> doGet(
            path: String,
            noinline responseParser: DataParser<A> = { it.parseAs() }
    ): Future<Result<A, Error>> =
            doRequest("GET", path, null, responseParser)

    internal inline fun <reified A> doDelete(
            path: String,
            noinline responseParser: DataParser<A> = { it.parseAs() }
    ): Future<Result<A, Error>> =
            doRequest("DELETE", path, null, responseParser)

    private fun <A> doRequest(
            method: String,
            path: String,
            body: String?,
            responseParser: DataParser<A>
    ): Future<Result<A, Error>> =
            platformInstance.request(
                    options = RequestOptions(
                            method = method,
                            path = path,
                            body = body
                    ),
                    tokenProvider = tokenProvider,
                    responseParser = responseParser
            )

    fun <A> doRequest(
            options: RequestOptions,
            responseParser: DataParser<A>
    ): Future<Result<A, Error>> =
            platformInstance.request(
                    options = options,
                    tokenProvider = tokenProvider,
                    responseParser = responseParser
            )

    @Suppress("UNCHECKED_CAST")
    internal fun upload(
            path: String,
            attachment: DataAttachment
    ): Future<Result<AttachmentBody, Error>> = platformInstance.upload(
            path = path,
            file = attachment.file,
            tokenProvider = tokenProvider,
            responseParser = { it.parseAs<AttachmentBody.Resource>() as Result<AttachmentBody, Error> }
    )

    internal fun <A> subscribeResuming(
            path: String,
            listeners: SubscriptionListeners<A>,
            messageParser: DataParser<A>
    ) = platformInstance.subscribeResuming(
            path = path,
            tokenProvider = tokenProvider,
            listeners = listeners,
            messageParser = messageParser
    )
}