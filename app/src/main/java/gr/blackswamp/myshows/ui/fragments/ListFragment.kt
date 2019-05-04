package gr.blackswamp.myshows.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import gr.blackswamp.myshows.R
import gr.blackswamp.myshows.ui.adapters.ShowAdapter
import gr.blackswamp.myshows.ui.adapters.ShowAdapterCallback
import gr.blackswamp.myshows.ui.model.ShowVO
import gr.blackswamp.myshows.ui.viewmodel.MainViewModel

class ListFragment : Fragment(), SearchView.OnQueryTextListener {

    companion object {
        const val TAG = "ListFragment"
        @JvmStatic
        fun newInstance() = ListFragment()
    }

    //region bindings
    private lateinit var toolbar: Toolbar
    private lateinit var list: RecyclerView
    private lateinit var adapter: ShowAdapter
    private lateinit var callback: ShowAdapterCallback
    private lateinit var refresh: SwipeRefreshLayout
    private var search: MenuItem? = null
    private var watchList: MenuItem? = null
    private var shows: MenuItem? = null
    private lateinit var loadMore: FloatingActionButton
    //endregion
    private lateinit var viewModel: ListViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(TAG, "creating view")
        val view = inflater.inflate(R.layout.fragment_list, container, false)
        setUpBindings(view)
        return view
    }

    private fun setUpBindings(view: View) {
        Log.d(TAG, "set up bindings")
        toolbar = view.findViewById(R.id.toolbar)
        list = view.findViewById(R.id.shows)
        refresh = view.findViewById(R.id.refresh)
        loadMore = view.findViewById(R.id.load_more)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "view created")
        adapter = ShowAdapter(this) { show, select ->
            if (select) viewModel.select(show) else viewModel.delete(show)
        }
        callback = ShowAdapterCallback(adapter)
        list.adapter = adapter
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        Log.d(TAG, "activity created")
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(activity!!).get(MainViewModel::class.java)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        setUpObservers()
        setUpListeners()
    }

    private fun setUpObservers() {
        Log.d(TAG, "set up observers")
        viewModel.showList.observe(this, Observer {
            adapter.setShows(it)
            refresh.isRefreshing = false
        })
        viewModel.displayingShowList.observe(this, Observer {
            adapter.allowSwipe = !it
            refresh.isEnabled = it
        })

        viewModel.listTitle.observe(this, Observer { toolbar.title = it })
        viewModel.canGoToShows.observe(this, Observer { shows?.isVisible = it })
        viewModel.canGoToWatchlist.observe(this, Observer { watchList?.isVisible = it })
        viewModel.searchFilter.observe(this, Observer { (search?.actionView as SearchView).setQuery(it, false) })
        viewModel.canLoadMore.observe(this, Observer {
            if (it)
                loadMore.show()
            else
                loadMore.hide()
        })
    }

    private fun setUpListeners() {
        Log.d(TAG, "set up listeners")
        refresh.setOnRefreshListener { viewModel.refresh() }
        loadMore.setOnClickListener { viewModel.loadNext() }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        Log.d(TAG, "create options menu")
        inflater.inflate(R.menu.list, menu)
        shows = menu.findItem(R.id.shows)
        search = menu.findItem(R.id.search)
        watchList = menu.findItem(R.id.watchlist)
        (search!!.actionView as SearchView).setOnQueryTextListener(this)

        shows!!.isVisible = viewModel.canGoToShows.value ?: false
        watchList!!.isVisible = viewModel.canGoToWatchlist.value ?: false
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        Log.d(TAG, "menu item selected ${item?.itemId}")
        return when (item?.itemId) {
            R.id.shows -> {
                viewModel.displayShowList()
                true
            }
            R.id.watchlist -> {
                viewModel.displayWatchList()
                true
            }
            else -> false
        }
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        Log.d(TAG, "submitted $query")
        viewModel.searchItems(query)
        search?.collapseActionView()
        return true
    }

    override fun onQueryTextChange(newText: String): Boolean = false

    interface ListViewModel {
        val showList: LiveData<List<ShowVO>>
        val canGoToShows: LiveData<Boolean>
        val canGoToWatchlist: LiveData<Boolean>
        val canLoadMore: LiveData<Boolean>
        val displayingShowList: LiveData<Boolean>
        val listTitle: LiveData<String>
        val searchFilter: LiveData<String>

        fun select(show: ShowVO)
        fun delete(show: ShowVO)
        fun refresh()
        fun displayWatchList()
        fun displayShowList()
        fun searchItems(query: String)
        fun loadNext()
    }
}