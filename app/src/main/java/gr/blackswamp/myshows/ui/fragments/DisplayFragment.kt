package gr.blackswamp.myshows.ui.fragments

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.youtube.player.YouTubeInitializationResult
import com.google.android.youtube.player.YouTubePlayer
import com.google.android.youtube.player.YouTubePlayerSupportFragment
import gr.blackswamp.myshows.App
import gr.blackswamp.myshows.BuildConfig
import gr.blackswamp.myshows.R
import gr.blackswamp.myshows.ui.model.ShowDetailVO
import gr.blackswamp.myshows.ui.viewmodel.MainViewModel

class DisplayFragment : Fragment(), YouTubePlayer.OnInitializedListener {
    companion object {
        const val TAG = "DisplayFragment"
        fun newInstance(): DisplayFragment = DisplayFragment()
    }

    //region bindings
    private lateinit var toolbar: Toolbar
    private lateinit var poster: AppCompatImageView
    private lateinit var genre: TextView
    private lateinit var summary: TextView
    private lateinit var watchLater: FloatingActionButton
    private lateinit var trailer: YouTubePlayerSupportFragment
    //endregion

    private lateinit var viewModel: ShowViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = inflater.inflate(R.layout.fragment_display, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "view created")
        toolbar = view.findViewById(R.id.toolbar)
        poster = view.findViewById(R.id.poster)
        genre = view.findViewById(R.id.genre)
        summary = view.findViewById(R.id.summary)
        watchLater = view.findViewById(R.id.watch_later)
        summary.movementMethod = ScrollingMovementMethod()

        @Suppress("CAST_NEVER_SUCCEEDS")
        if (savedInstanceState == null) {
            trailer = YouTubePlayerSupportFragment.newInstance()
            childFragmentManager.beginTransaction().replace(R.id.trailer, trailer as Fragment).commit()
        } else {
            trailer = childFragmentManager.findFragmentById(R.id.trailer) as YouTubePlayerSupportFragment
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(activity!!).get(MainViewModel::class.java)
//        trailer = (activity!!).fragmentManager.findFragmentById(R.id.trailer) as YouTubePlayerFragment
        setUpObservers()
        setUpListeners()
    }

    private fun setUpListeners() {
        watchLater.setOnClickListener { viewModel.toggleFavourite() }
        toolbar.setNavigationOnClickListener { viewModel.exitDisplay() }
    }

    private fun setUpObservers() {
        viewModel.show.observe(this, Observer { showDetail(it) })
        viewModel.showWatchListed.observe(this, Observer { updateAction(it) })
    }

    private fun updateAction(watchLater: Boolean?) {
        if (watchLater == true) {
            this.watchLater.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context!!, R.color.inWatchlistColor))
        } else {
            this.watchLater.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context!!, R.color.secondaryColor))
        }
    }

    private fun showDetail(show: ShowDetailVO?) {
        if (show == null) return
        if (show.image != null) {
            Glide.with(this)
                .load(show.image)
                .error(R.drawable.ic_broken_image)
                .placeholder(R.drawable.ic_sync)
                .fallback(R.drawable.ic_image)
                .into(poster)
        } else {
            poster.setImageResource(R.drawable.ic_image)
            poster.imageTintList = ContextCompat.getColorStateList(App.context, R.color.secondaryColor)
        }

        genre.text = show.genre
        toolbar.title = show.title
        summary.text = show.summary


        if (show.trailer != null) {
            trailer.initialize(BuildConfig.YoutubeApiKey, this)
        }
    }

    override fun onInitializationSuccess(provider: YouTubePlayer.Provider?, player: YouTubePlayer?, wasRestored: Boolean) {
        if (!wasRestored) {
            val show = viewModel.show.value ?: return
            if (show.trailer != null) {
                player?.cueVideo(show.trailer)
            }
        }

    }

    override fun onInitializationFailure(p0: YouTubePlayer.Provider?, p1: YouTubeInitializationResult?) {
        //do nothing
    }

    interface ShowViewModel {
        fun toggleFavourite()
        fun exitDisplay()

        val showWatchListed: LiveData<Boolean>
        val show: LiveData<ShowDetailVO>

    }
}