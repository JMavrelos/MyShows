package gr.blackswamp.myshows.ui.fragments

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.*
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
import gr.blackswamp.myshows.R
import gr.blackswamp.myshows.ui.model.ShowDetailVO
import gr.blackswamp.myshows.ui.viewmodel.MainViewModel

class DisplayFragment : Fragment() {
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
    //endregion

    lateinit var viewModel: ShowViewModel

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

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return super.onOptionsItemSelected(item)
    }

    private fun updateAction(watchLater: Boolean?) {
        if (watchLater == true) {
            this.watchLater.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context!!, R.color.inWatchlistColor))
        } else {
            this.watchLater.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context!!, R.color.secondaryColor))
        }
        this.watchLater.setImageResource(if (watchLater == true) R.drawable.ic_favourite else R.drawable.ic_favourite_border)
    }

    private fun showDetail(show: ShowDetailVO?) {
        if (show == null) return
        if (show.image != null) {
            Glide.with(this)
                .load(show.image)
                .error(R.drawable.ic_broken_image)
                .placeholder(R.drawable.ic_sync)
                .into(poster)
        } else {
            poster.setImageResource(R.drawable.im)
        }
        genre.text = show.genre
        toolbar.title = show.title
        summary.text = show.summary
    }

    interface ShowViewModel {
        fun toggleFavourite()
        fun exitShows()

        val showInWatchlist: LiveData<Boolean>
        val show: LiveData<ShowDetailVO>

    }
}