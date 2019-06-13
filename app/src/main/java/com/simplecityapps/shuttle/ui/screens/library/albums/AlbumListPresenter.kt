package com.simplecityapps.shuttle.ui.screens.library.albums

import com.simplecityapps.mediaprovider.repository.AlbumRepository
import com.simplecityapps.shuttle.ui.common.mvp.BasePresenter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.text.Collator
import javax.inject.Inject

class AlbumListPresenter @Inject constructor(
    private val albumArtistRepository: AlbumRepository
) : AlbumListContract.Presenter,
    BasePresenter<AlbumListContract.View>() {

    override fun loadAlbums() {
        addDisposable(
            albumArtistRepository.getAlbums()
                .map { album -> album.sortedWith(Comparator { a, b -> Collator.getInstance().compare(a.sortKey, b.sortKey) }) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onNext = { albumArtists -> view?.setAlbums(albumArtists) },
                    onError = { error -> Timber.e(error, "Failed to retrieve albums") })
        )
    }
}