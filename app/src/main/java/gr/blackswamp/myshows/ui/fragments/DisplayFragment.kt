package gr.blackswamp.myshows.ui.fragments

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
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
import gr.blackswamp.myshows.App
import gr.blackswamp.myshows.R
import gr.blackswamp.myshows.ui.model.ShowDetailVO
import gr.blackswamp.myshows.ui.viewmodel.MainViewModel

class DisplayFragment : Fragment() {
    companion object {
        const val TAG = "DisplayFragment"
        fun newInstance(): DisplayFragment = DisplayFragment()
        const val NO_VIDEO_HTML = "<!doctype html>\n" +
                "<html lang=\"en\">\n" +
                "<body>\n" +
                "<header style=\"position: absolute; left: 50%; top: 50%; transform: translate(-50%, -50%);\"> No trailer exists for this Show</header>\n" +
                "</body>\n" +
                "</html>\n"
    }

    //region bindings
    private lateinit var toolbar: Toolbar
    private lateinit var poster: AppCompatImageView
    private lateinit var genre: TextView
    private lateinit var summary: TextView
    private lateinit var watchLater: FloatingActionButton
    private lateinit var trailer: WebView
    //endregion

    private lateinit var viewModel: ShowViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = inflater.inflate(R.layout.fragment_display, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        toolbar = view.findViewById(R.id.toolbar)
        poster = view.findViewById(R.id.poster)
        genre = view.findViewById(R.id.genre)
        summary = view.findViewById(R.id.summary)
        watchLater = view.findViewById(R.id.watch_later)
        trailer = view.findViewById(R.id.trailer)
        summary.movementMethod = ScrollingMovementMethod()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(activity!!).get(MainViewModel::class.java)
        setUpObservers()
        setUpListeners()
    }

    private fun setUpListeners() {
        watchLater.setOnClickListener { viewModel.toggleFavourite() }
        toolbar.setNavigationOnClickListener { viewModel.exitShows() }
    }

    private fun setUpObservers() {
        viewModel.show.observe(this, Observer { showDetail(it) })
        viewModel.showInWatchlist.observe(this, Observer { updateAction(it) })
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


        if (show.trailer == null) {
            trailer.loadData(NO_VIDEO_HTML,null,null)
        } else {

        }
    }

    interface ShowViewModel {
        fun toggleFavourite()
        fun exitShows()

        val showInWatchlist: LiveData<Boolean>
        val show: LiveData<ShowDetailVO>

    }
}