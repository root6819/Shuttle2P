package com.simplecityapps.shuttle.ui.screens.playback

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import au.com.simplecityapps.shuttle.imageloading.ArtworkImageLoader
import au.com.simplecityapps.shuttle.imageloading.glide.GlideImageLoader
import com.google.android.gms.cast.framework.CastButtonFactory
import com.simplecityapps.adapter.RecyclerAdapter
import com.simplecityapps.adapter.RecyclerListener
import com.simplecityapps.mediaprovider.model.Song
import com.simplecityapps.playback.queue.QueueItem
import com.simplecityapps.playback.queue.QueueManager
import com.simplecityapps.shuttle.R
import com.simplecityapps.shuttle.dagger.Injectable
import com.simplecityapps.shuttle.ui.common.autoCleared
import com.simplecityapps.shuttle.ui.common.recyclerview.SnapOnScrollListener
import com.simplecityapps.shuttle.ui.common.recyclerview.SpacesItemDecoration
import com.simplecityapps.shuttle.ui.common.recyclerview.attachSnapHelperWithListener
import com.simplecityapps.shuttle.ui.common.recyclerview.clearAdapterOnDetach
import com.simplecityapps.shuttle.ui.common.utils.toHms
import com.simplecityapps.shuttle.ui.common.view.FavoriteButton
import com.simplecityapps.shuttle.ui.common.view.RepeatButton
import com.simplecityapps.shuttle.ui.common.view.SeekButton
import com.simplecityapps.shuttle.ui.common.view.ShuffleButton
import com.simplecityapps.shuttle.ui.screens.sleeptimer.SleepTimerDialogFragment
import javax.inject.Inject

class PlaybackFragment :
    Fragment(),
    Injectable,
    PlaybackContract.View,
    SeekBar.OnSeekBarChangeListener {

    @Inject lateinit var presenter: PlaybackPresenter

    private var imageLoader: ArtworkImageLoader by autoCleared()

    private var recyclerView: RecyclerView by autoCleared()

    private lateinit var adapter: RecyclerAdapter

    private var playPauseButton: ImageButton by autoCleared()
    private var skipNextButton: ImageButton by autoCleared()
    private var skipPrevButton: ImageButton by autoCleared()
    private var shuffleButton: ShuffleButton by autoCleared()
    private var repeatButton: RepeatButton by autoCleared()
    private var seekBackwardButton: SeekButton by autoCleared()
    private var seekForwardButton: SeekButton by autoCleared()
    private var seekBar: SeekBar by autoCleared()
    private var titleTextView: TextView by autoCleared()
    private var subtitleTextView: TextView by autoCleared()
    private var currentTimeTextView: TextView by autoCleared()
    private var durationTextView: TextView by autoCleared()
    private var toolbar: Toolbar by autoCleared()
    private var favoriteButton: FavoriteButton by autoCleared()


    // Lifecycle

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_playback, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)

        toolbar = view.findViewById(R.id.toolbar)

        skipNextButton = view.findViewById(R.id.skipNextButton)
        skipPrevButton = view.findViewById(R.id.skipPrevButton)
        playPauseButton = view.findViewById(R.id.playPauseButton)
        shuffleButton = view.findViewById(R.id.shuffleButton)
        repeatButton = view.findViewById(R.id.repeatButton)
        seekBackwardButton = view.findViewById(R.id.seekBackwardButton)
        seekForwardButton = view.findViewById(R.id.seekForwardButton)
        seekBar = view.findViewById(R.id.seekBar)
        titleTextView = view.findViewById(R.id.titleTextView)
        subtitleTextView = view.findViewById(R.id.subtitleTextView)
        currentTimeTextView = view.findViewById(R.id.currentTimeTextView)
        durationTextView = view.findViewById(R.id.durationTextView)

        adapter = RecyclerAdapter()
        imageLoader = GlideImageLoader(this)

        playPauseButton.setOnClickListener { presenter.togglePlayback() }
        shuffleButton.setOnClickListener { presenter.toggleShuffle() }
        repeatButton.setOnClickListener { presenter.toggleRepeat() }
        skipNextButton.setOnClickListener { presenter.skipNext() }
        skipPrevButton.setOnClickListener { presenter.skipPrev() }
        seekBackwardButton.listener = object : SeekButton.OnSeekListener {
            override fun onSeek(seekAmount: Int) {
                presenter.seekBackward(seekAmount)
            }
        }
        seekForwardButton.listener = object : SeekButton.OnSeekListener {
            override fun onSeek(seekAmount: Int) {
                presenter.seekForward(seekAmount)
            }
        }
        seekBar.setOnSeekBarChangeListener(this)

        recyclerView.adapter = adapter
        recyclerView.setRecyclerListener(RecyclerListener())
        recyclerView.clearAdapterOnDetach()

        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(recyclerView)
        recyclerView.attachSnapHelperWithListener(snapHelper, SnapOnScrollListener.Behavior.NOTIFY_ON_SCROLL_STATE_IDLE) { position ->
            presenter.skipTo(position)
        }

        recyclerView.addItemDecoration(SpacesItemDecoration(8))

        playPauseButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context!!, R.color.colorPrimary))

        toolbar.inflateMenu(R.menu.menu_playback)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.sleepTimer -> {
                    presenter.sleepTimerClicked()
                    true
                }
                else -> false
            }
        }
        favoriteButton = toolbar.menu.findItem(R.id.favorite).actionView.findViewById(R.id.favoritesButton)
        favoriteButton.setOnClickListener {
            favoriteButton.toggle()
            presenter.setFavorite(favoriteButton.isChecked)
        }

        CastButtonFactory.setUpMediaRouteButton(context!!, toolbar.menu, R.id.media_route_menu_item)

        presenter.bindView(this)
    }

    override fun onDestroyView() {
        adapter.dispose()
        presenter.unbindView()
        super.onDestroyView()
    }


    // PlaybackContract.View

    override fun setPlayState(isPlaying: Boolean) {
        when {
            isPlaying -> playPauseButton.setImageDrawable(ContextCompat.getDrawable(context!!, R.drawable.ic_pause_black_24dp))
            else -> playPauseButton.setImageDrawable(ContextCompat.getDrawable(context!!, R.drawable.ic_play_arrow_black_24dp))
        }
    }

    override fun setShuffleMode(shuffleMode: QueueManager.ShuffleMode) {
        shuffleButton.shuffleMode = shuffleMode
    }

    override fun setRepeatMode(repeatMode: QueueManager.RepeatMode) {
        repeatButton.repeatMode = repeatMode
    }

    override fun setCurrentSong(song: Song?) {
        song?.let {
            titleTextView.text = song.name
            subtitleTextView.text = "${song.albumArtistName} • ${song.albumName}"

            when (song.type) {
                Song.Type.Audiobook, Song.Type.Podcast -> {
                    seekBackwardButton.isVisible = true
                    seekForwardButton.isVisible = true
                    skipPrevButton.isVisible = false
                    skipNextButton.isVisible = false
                }
                else -> {
                    seekBackwardButton.isVisible = false
                    seekForwardButton.isVisible = false
                    skipPrevButton.isVisible = true
                    skipNextButton.isVisible = true
                }
            }
        }
    }

    override fun setQueuePosition(position: Int?, total: Int, smoothScroll: Boolean) {
        position?.let {
            if (smoothScroll) {
                recyclerView.smoothScrollToPosition(position)
            } else {
                recyclerView.scrollToPosition(position)
            }
        }
    }

    override fun setQueue(queue: List<QueueItem>, position: Int?) {
        adapter.setData(
            queue.map { queueItem -> ArtworkBinder(queueItem.song, imageLoader) },
            completion = {
                position?.let { position ->
                    recyclerView.scrollToPosition(position)
                }
            },
            animateChanges = false
        )
    }

    override fun setProgress(position: Int, duration: Int) {
        currentTimeTextView.text = position.toHms()
        durationTextView.text = duration.toHms("--:--")
        seekBar.progress = ((position.toFloat() / duration) * 1000).toInt()
    }

    override fun presentSleepTimer() {
        SleepTimerDialogFragment().show(childFragmentManager)
    }

    override fun setIsFavorite(isFavorite: Boolean) {
        favoriteButton.isChecked = isFavorite
    }


    // SeekBar.OnSeekBarChangeListener Implementation

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {

    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {

    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        presenter.seek(seekBar.progress / 1000f)
    }


    // Static

    companion object {
        fun newInstance() = PlaybackFragment()
    }
}