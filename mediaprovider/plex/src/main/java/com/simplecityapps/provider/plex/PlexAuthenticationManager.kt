package com.simplecityapps.provider.plex

import android.net.Uri
import com.simplecityapps.networking.retrofit.NetworkResult
import com.simplecityapps.networking.retrofit.error.HttpStatusCode
import com.simplecityapps.networking.retrofit.error.RemoteServiceHttpError
import com.simplecityapps.provider.plex.http.*
import timber.log.Timber
import java.net.URLEncoder

class PlexAuthenticationManager(
    private val userService: UserService,
    private val credentialStore: CredentialStore
) {

    fun getLoginCredentials(): LoginCredentials? {
        return credentialStore.loginCredentials
    }

    fun setLoginCredentials(loginCredentials: LoginCredentials?) {
        credentialStore.loginCredentials = loginCredentials
    }

    fun getAuthenticatedCredentials(): AuthenticatedCredentials? {
        return credentialStore.authenticatedCredentials
    }

    fun setAddress(host: String, port: Int) {
        credentialStore.host = host
        credentialStore.port = port
    }

    fun getHost(): String? {
        return credentialStore.host
    }

    fun getPort(): Int? {
        return credentialStore.port
    }

    fun getAddress(): String? {
        return getHost()?.let { host ->
            getPort()?.toString()?.let { port ->
                "$host:$port"
            }
        }
    }

    suspend fun authenticate(address: String, loginCredentials: LoginCredentials): Result<AuthenticatedCredentials> {
        Timber.d("authenticate(address: $address)")
        val authenticationResult = userService.authenticate(
            username = loginCredentials.username,
            password = loginCredentials.password
        )

        return when (authenticationResult) {
            is NetworkResult.Success<AuthenticationResult> -> {
                val authenticatedCredentials = AuthenticatedCredentials(authenticationResult.body.user.authToken, authenticationResult.body.user.id)
                credentialStore.authenticatedCredentials = authenticatedCredentials
                Result.success(authenticatedCredentials)
            }
            is NetworkResult.Failure -> {
                (authenticationResult.error as? RemoteServiceHttpError)?.let { error ->
                    if (error.httpStatusCode == HttpStatusCode.Unauthorized) {
                        credentialStore.authenticatedCredentials = null
                    }
                }
                Result.failure(authenticationResult.error)
            }
        }
    }

    fun buildPlexPath(path: Uri, authenticatedCredentials: AuthenticatedCredentials): String? {
        if (credentialStore.host == null || credentialStore.port == null) {
            Timber.w("Invalid plex address")
            return null
        }

        return "${credentialStore.host}:${credentialStore.port}" +
                "/music/:/transcode/universal/start.m3u8" +
                "?path=${URLEncoder.encode(path.path, Charsets.UTF_8.name())}" +
                "?directStreamAudio=true" +
                "&protocol=hls" +
                "&directPlay=0" +
                "&hasMDE=1" +
                "&download=1" +
                "&X-Plex-Token=${authenticatedCredentials.accessToken}" +
                "&X-Plex-Client-Identifier=s2-music-payer" +
                "&X-Plex-Device=Android" +
                "&X-Plex-Session-Identifier=715b12da-8ac9-11eb-8dcd-0242ac130003" +
                "&X-Plex-Client-Profile-Extra=${URLEncoder.encode("add-transcode-target(type=musicProfile&context=streaming&protocol=hls&container=mpegts&audioCodec=mp3)", Charsets.UTF_8.name())}"
    }
}