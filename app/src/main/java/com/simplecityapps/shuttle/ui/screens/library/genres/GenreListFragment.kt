package com.simplecityapps.shuttle.ui.screens.library.genres

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.simplecityapps.adapter.RecyclerAdapter
import com.simplecityapps.adapter.RecyclerListener
import com.simplecityapps.adapter.ViewBinder
import com.simplecityapps.mediaprovider.model.Genre
import com.simplecityapps.mediaprovider.model.Song
import com.simplecityapps.shuttle.R
import com.simplecityapps.shuttle.dagger.Injectable
import com.simplecityapps.shuttle.ui.common.TagEditorMenuSanitiser
import com.simplecityapps.shuttle.ui.common.autoCleared
import com.simplecityapps.shuttle.ui.common.dialog.TagEditorAlertDialog
import com.simplecityapps.shuttle.ui.common.error.userDescription
import com.simplecityapps.shuttle.ui.common.recyclerview.SectionedAdapter
import com.simplecityapps.shuttle.ui.common.view.CircularLoadingView
import com.simplecityapps.shuttle.ui.common.view.HorizontalLoadingView
import com.simplecityapps.shuttle.ui.common.view.findToolbarHost
import com.simplecityapps.shuttle.ui.screens.library.genres.detail.GenreDetailFragmentArgs
import com.simplecityapps.shuttle.ui.screens.playlistmenu.CreatePlaylistDialogFragment
import com.simplecityapps.shuttle.ui.screens.playlistmenu.PlaylistData
import com.simplecityapps.shuttle.ui.screens.playlistmenu.PlaylistMenuPresenter
import com.simplecityapps.shuttle.ui.screens.playlistmenu.PlaylistMenuView
import javax.inject.Inject

class GenreListFragment :
    Fragment(),
    Injectable,
    GenreBinder.Listener,
    GenreListContract.View,
    CreatePlaylistDialogFragment.Listener {

    private var adapter: RecyclerAdapter by autoCleared()

    private var recyclerView: RecyclerView by autoCleared()
    private var circularLoadingView: CircularLoadingView by autoCleared()
    private var horizontalLoadingView: HorizontalLoadingView by autoCleared()

    @Inject lateinit var presenter: GenreListPresenter

    @Inject lateinit var playlistMenuPresenter: PlaylistMenuPresenter

    private lateinit var playlistMenuView: PlaylistMenuView

    private var recyclerViewState: Parcelable? = null


    // Lifecycle

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_genres, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playlistMenuView = PlaylistMenuView(requireContext(), playlistMenuPresenter, childFragmentManager)

        adapter = object : SectionedAdapter(viewLifecycleOwner.lifecycleScope) {
            override fun getSectionName(viewBinder: ViewBinder?): String? {
                return (viewBinder as? GenreBinder)?.genre?.let { genre ->
                    presenter.getFastscrollPrefix(genre)
                }
            }
        }
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.adapter = adapter
        recyclerView.setRecyclerListener(RecyclerListener())

        circularLoadingView = view.findViewById(R.id.circularLoadingView)
        horizontalLoadingView = view.findViewById(R.id.horizontalLoadingView)

        savedInstanceState?.getParcelable<Parcelable>(ARG_RECYCLER_STATE)?.let { recyclerViewState = it }

        presenter.bindView(this)
        playlistMenuPresenter.bindView(playlistMenuView)
    }

    override fun onResume() {
        super.onResume()

        presenter.loadGenres(false)

        findToolbarHost()?.toolbar?.menu?.clear()
    }

    override fun onPause() {
        super.onPause()

        findToolbarHost()?.toolbar?.let { toolbar ->
            toolbar.menu.removeItem(R.id.viewMode)
            toolbar.setOnMenuItemClickListener(null)
        }

        recyclerViewState = recyclerView.layoutManager?.onSaveInstanceState()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(ARG_RECYCLER_STATE, recyclerViewState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        presenter.unbindView()
        playlistMenuPresenter.unbindView()

        super.onDestroyView()
    }


    // GenreListContract.View Implementation

    override fun setGenres(genres: List<Genre>, resetPosition: Boolean) {
        if (resetPosition) {
            adapter.clear()
        }

        val data = genres.map { genre -> GenreBinder(genre, this) }.toMutableList<ViewBinder>()

        adapter.update(data, completion = {
            recyclerViewState?.let {
                recyclerView.layoutManager?.onRestoreInstanceState(recyclerViewState)
                recyclerViewState = null
            }
        })
    }

    override fun onAddedToQueue(genre: Genre) {
        Toast.makeText(context, "${genre.name} added to queue", Toast.LENGTH_SHORT).show()
    }

    override fun setLoadingState(state: GenreListContract.LoadingState) {
        when (state) {
            is GenreListContract.LoadingState.Scanning -> {
                horizontalLoadingView.setState(HorizontalLoadingView.State.Loading("Scanning your library"))
                circularLoadingView.setState(CircularLoadingView.State.None)
            }
            is GenreListContract.LoadingState.Loading -> {
                horizontalLoadingView.setState(HorizontalLoadingView.State.None)
                circularLoadingView.setState(CircularLoadingView.State.Loading())
            }
            is GenreListContract.LoadingState.Empty -> {
                horizontalLoadingView.setState(HorizontalLoadingView.State.None)
                circularLoadingView.setState(CircularLoadingView.State.Empty("No genres"))
            }
            is GenreListContract.LoadingState.None -> {
                horizontalLoadingView.setState(HorizontalLoadingView.State.None)
                circularLoadingView.setState(CircularLoadingView.State.None)
            }
        }
    }

    override fun setLoadingProgress(progress: Float) {
        horizontalLoadingView.setProgress(progress)
    }

    override fun showLoadError(error: Error) {
        Toast.makeText(context, error.userDescription(), Toast.LENGTH_LONG).show()
    }

    override fun showTagEditor(songs: List<Song>) {
        TagEditorAlertDialog.newInstance(songs).show(childFragmentManager)
    }


    // GenreBinder.Listener Implementation

    override fun onGenreSelected(genre: Genre, viewHolder: GenreBinder.ViewHolder) {
        if (findNavController().currentDestination?.id != R.id.genreDetailFragment) {
            findNavController().navigate(
                R.id.action_libraryFragment_to_genreDetailFragment,
                GenreDetailFragmentArgs(genre).toBundle()
            )
        }
    }

    override fun onOverflowClicked(view: View, genre: Genre) {
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.inflate(R.menu.menu_popup)
        TagEditorMenuSanitiser.sanitise(popupMenu.menu, genre.mediaProviders)

        playlistMenuView.createPlaylistMenu(popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            if (playlistMenuView.handleMenuItem(menuItem, PlaylistData.Genres(genre))) {
                return@setOnMenuItemClickListener true
            } else {
                when (menuItem.itemId) {
                    R.id.play -> {
                        presenter.play(genre)
                        return@setOnMenuItemClickListener true
                    }
                    R.id.queue -> {
                        presenter.addToQueue(genre)
                        return@setOnMenuItemClickListener true
                    }
                    R.id.playNext -> {
                        presenter.playNext(genre)
                        return@setOnMenuItemClickListener true
                    }
                    R.id.exclude -> {
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Exclude Genre")
                            .setMessage("\"${genre.name}\" will be hidden from your library.\n\nYou can view excluded songs in settings.")
                            .setPositiveButton("Exclude") { _, _ ->
                                presenter.exclude(genre)
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                        return@setOnMenuItemClickListener true
                    }
                    R.id.editTags -> {
                        presenter.editTags(genre)
                        return@setOnMenuItemClickListener true
                    }
                }
            }
            false
        }
        popupMenu.show()
    }


    // CreatePlaylistDialogFragment.Listener Implementation

    override fun onSave(text: String, playlistData: PlaylistData) {
        playlistMenuView.onSave(text, playlistData)
    }


    // Static

    companion object {

        const val TAG = "GenreListFragment"

        const val ARG_RECYCLER_STATE = "recycler_state"

        fun newInstance() = GenreListFragment()
    }
}