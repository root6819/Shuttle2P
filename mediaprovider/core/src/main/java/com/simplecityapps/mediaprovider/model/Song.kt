package com.simplecityapps.mediaprovider.model

import android.os.Parcelable
import androidx.annotation.Keep
import com.simplecityapps.mediaprovider.MediaProvider
import kotlinx.parcelize.Parcelize
import java.util.*

@Keep
@Parcelize
data class Song(
    val id: Long,
    val name: String?,
    val albumArtist: String?,
    val artists: List<String>,
    val album: String?,
    val track: Int?,
    val disc: Int?,
    val duration: Int,
    val year: Int?,
    val genres: List<String>,
    val path: String,
    val size: Long,
    val mimeType: String,
    val lastModified: Date,
    val lastPlayed: Date?,
    val lastCompleted: Date?,
    val playCount: Int,
    var playbackPosition: Int,
    val blacklisted: Boolean,
    var mediaStoreId: Long? = null,
    var mediaProvider: MediaProvider.Type,
    val replayGainTrack: Double? = null,
    val replayGainAlbum: Double? = null
) : Parcelable {

    val type: Type
        get() {
            return when {
                path.contains("audiobook", true) || path.endsWith("m4b", true) -> Type.Audiobook
                path.contains("podcast", true) -> Type.Podcast
                else -> Type.Audio
            }
        }

    val artistGroupKey: ArtistGroupKey
        get() = ArtistGroupKey(albumArtist?.toLowerCase(Locale.getDefault())?.removeArticles() ?: when (artists.size) {
            0 -> "Unknown"
            else -> artists.joinToString(", ") { it.toLowerCase(Locale.getDefault()).removeArticles() }.ifEmpty { "Unknown" }
        })

    val albumGroupKey: AlbumGroupKey
        get() = AlbumGroupKey(album?.toLowerCase(Locale.getDefault())?.removeArticles(), artistGroupKey)


    override fun toString(): String {
        return "Song(id=$id, name='$name', albumArtist='$albumArtist', artist='$artists', album='$album', track=$track, disc=$disc, duration=$duration, year=$year, genres=$genres, path='$path', size=$size, mimeType='$mimeType', lastModified=$lastModified, lastPlayed=$lastPlayed, lastCompleted=$lastCompleted, playCount=$playCount, playbackPosition=$playbackPosition, blacklisted=$blacklisted, mediaStoreId=$mediaStoreId, mediaProvider=$mediaProvider, replayGainTrack=$replayGainTrack, replayGainAlbum=$replayGainAlbum)"
    }

    enum class Type {
        Audio, Audiobook, Podcast
    }
}

val Song.friendlyArtistName: String?
    get() {
        return if (artists.isEmpty()) {
            albumArtist
        } else {
            artists.groupBy { it.toLowerCase(Locale.getDefault()).removeArticles() }
                .map { map -> map.value.maxByOrNull { it.length } }
                .joinToString(", ")
                .ifEmpty { "Unknown" }
        }
    }