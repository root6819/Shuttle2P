package com.simplecityapps.shuttle.mediaprovider.jellyfin

import com.simplecityapps.shuttle.mediaprovider.common.MediaProvider
import com.simplecityapps.shuttle.mediaprovider.jellyfin.http.data.AuthenticatedCredentials
import com.simplecityapps.shuttle.mediaprovider.jellyfin.http.data.ItemResponse
import com.simplecityapps.shuttle.mediaprovider.jellyfin.http.data.toSongData
import com.simplecityapps.shuttle.mediaprovider.jellyfin.http.service.ItemsService
import com.simplecityapps.shuttle.model.MediaProviderType
import com.simplecityapps.shuttle.model.Progress
import kotlinx.coroutines.flow.*
import kotlin.math.min

class JellyfinMediaProvider(
    private val authenticationManager: AuthenticationManager,
    private val credentialStore: CredentialStore,
    private val itemsService: ItemsService
) : MediaProvider {

    override val type: MediaProviderType
        get() = MediaProviderType.Jellyfin

    override fun findSongs(): Flow<MediaProvider.SongRetrievalState> {
        return flow {
            authenticationManager.authenticate().fold(
                { authenticatedCredentials ->
                    val address = credentialStore.getAddress().firstOrNull() ?: run {
//                        logcat { "findSongs() failed: address null" }
                        emit(MediaProvider.SongRetrievalState.Failed)
                        return@flow
                    }
                    val items = queryItems(address, authenticatedCredentials)
                        .onEach { queryResponseProgress ->
                            emit(MediaProvider.SongRetrievalState.QueryingApi(queryResponseProgress.progress))
                        }
                        .last().items
                    emit(MediaProvider.SongRetrievalState.Complete(items.map { it.toSongData() }))
                },
                { throwable ->
//                    logcat { "findSongs() failed: $throwable" }
                    emit(MediaProvider.SongRetrievalState.Failed)
                }
            )
        }
    }

    data class QueryResponseProgress(val items: List<ItemResponse>, val progress: Progress)

    private fun queryItems(
        address: String,
        credentials: AuthenticatedCredentials,
        startIndex: Int = 0,
        pageSize: Int = 500,
        items: MutableList<ItemResponse> = mutableListOf()
    ): Flow<QueryResponseProgress> {
        return flow {
            itemsService.audioItems(
                url = address,
                token = credentials.accessToken,
                userId = credentials.userId,
                limit = pageSize,
                startIndex = startIndex
            ).fold(
                onSuccess = { response ->
                    val lastIndex = startIndex + pageSize
                    emit(
                        QueryResponseProgress(
                            items = items + response.items,
                            progress = Progress(
                                progress = startIndex,
                                total = min(pageSize, response.totalRecordCount - (lastIndex))
                            )
                        )
                    )
                    if (lastIndex < response.totalRecordCount) {
                        emitAll(
                            queryItems(
                                address = address,
                                credentials = credentials,
                                startIndex = lastIndex,
                                pageSize = pageSize,
                                items = items
                            )
                        )
                    }
                },
                onFailure = { throwable ->
                    throw throwable
                }
            )
        }
    }
}