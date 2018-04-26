package com.pusher.chatkit.test

import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

val trustyCertificate: X509TrustManager = object : X509TrustManager {
    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    override fun checkClientTrusted(vararg certs: X509Certificate, authType: String) = Unit
    override fun checkServerTrusted(vararg certs: X509Certificate, authType: String) = Unit
}
val trustAllCerts = arrayOf<TrustManager>(trustyCertificate)

val sslContext: SSLContext = SSLContext.getInstance("SSL").apply {
    init(null, trustAllCerts, SecureRandom())
}

val insecureOkHttpClient: OkHttpClient = OkHttpClient.Builder().apply {
    sslSocketFactory(sslContext.socketFactory, trustyCertificate)
}.build()
