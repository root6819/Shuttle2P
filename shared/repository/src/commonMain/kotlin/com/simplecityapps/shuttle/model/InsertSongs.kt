package com.simplecityapps.shuttle.model

import com.simplecityapps.shuttle.inject.Inject
import com.simplecityapps.shuttle.repository.SongRepository

class InsertSongs @Inject constructor(
    private val songRepository: SongRepository
) {
    suspend operator fun invoke(songs: List<SongData>) {
        songRepository.insertSongs(songs)
    }
}