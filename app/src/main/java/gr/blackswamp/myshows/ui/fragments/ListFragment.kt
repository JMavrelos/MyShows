package gr.blackswamp.myshows.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import gr.blackswamp.myshows.R
import gr.blackswamp.myshows.ui.adapters.ShowAdapter
import gr.blackswamp.myshows.ui.adapters.ShowAdapterCallback
import gr.blackswamp.myshows.ui.model.ShowVO
import gr.blackswamp.myshows.ui.viewmodel.MainViewModel
import gr.blackswamp.myshows.util.MediatorPairLiveData

class ListFragment : Fragment(), SearchView.OnQueryTextListener {

    companion object {
        const val TAG = "ListFragment"
        @JvmStatic
        fun newInstance() = ListFragment()
    }

    //region bindings
    private lateinit var list: RecyclerView
    private lateinit var adapter: ShowAdapter
    private lateinit var refresh: SwipeRefreshLayout
    private lateinit var initialMessage: TextView
    private var search: MenuItem? = null
    private var watchList: MenuItem? = null
    private var shows: MenuItem? = null
    private lateinit var loadMore: FloatingActionButton
    //endregion
    private lateinit var movedToLast: MutableLiveData<Boolean>
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
        list = view.findViewById(R.id.shows)
        refresh = view.findViewById(R.id.refresh)
        loadMore = view.findViewById(R.id.load_more)
        movedToLast = MutableLiveData()
        initialMessage = view.findViewById(R.id.initial_message)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "view created")
        adapter = ShowAdapter(this) { show, select ->
            if (select) viewModel.select(show) else viewModel.delete(show)
        }
        list.adapter = adapter
        ItemTouchHelper(ShowAdapterCallback(adapter)).attachToRecyclerView(list)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        Log.d(TAG, "activity created")
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(activity!!).get(MainViewModel::class.java)
        (activity as AppCompatActivity).setSupportActionBar(view!!.findViewById(R.id.toolbar))
        setUpObservers()
        setUpListeners()
    }

    private fun setUpObservers() {
        Log.d(TAG, "set up observers")
        viewModel.showList.observe(this, Observer {
            adapter.setShows(it.first, it.second)
            refresh.isRefreshing = false
        })
        viewModel.displayingShowList.observe(this, Observer {
            adapter.allowSwipe = !it
            refresh.isEnabled = it
        })
        viewModel.loading.observe(this, Observer { if (!it) refresh.isRefreshing = false })
        viewModel.listTitle.observe(this, Observer { (activity as AppCompatActivity).title = it })
        viewModel.adapterFilter.observe(this, Observer {
            adapter.setFilter(it)
        })
        viewModel.canGoToShows.observe(this, Observer { shows?.isVisible = it })
        viewModel.canGoToWatchlist.observe(this, Observer { watchList?.isVisible = it })
        viewModel.showInitialMessage.observe(this, Observer { initialMessage.visibility = if (it) View.VISIBLE else View.GONE })
        MediatorPairLiveData<Boolean, Boolean, Boolean>(viewModel.canLoadMore, movedToLast) { c, m ->
            c ?: false && m ?: false
        }.observe(
            this, Observer {
                Log.d(TAG, "scroll value changed")
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
        list.addOnScrollListener(ScrollListener(movedToLast))
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        Log.d(TAG, "create options menu")
        inflater.inflate(R.menu.list, menu)
        shows = menu.findItem(R.id.shows)
        search = menu.findItem(R.id.search)
        watchList = menu.findItem(R.id.watchlist)
        (search!!.actionView as SearchView).let {
            it.setOnQueryTextListener(this)
            it.setOnSearchClickListener { _ -> it.setQuery(viewModel.searchFilter, false) }
        }
        shows!!.isVisible = viewModel.canGoToShows.value ?: false
        watchList!!.isVisible = viewModel.canGoToWatchlist.value ?: false
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        Log.d(TAG, "menu item selectionChanged ${item?.itemId}")
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

    override fun onDestroy() {
        super.onDestroy()
        list.clearOnScrollListeners()
    }


    override fun onQueryTextSubmit(query: String): Boolean {
        Log.d(TAG, "submitted $query")
        viewModel.searchItems(query)
        search?.collapseActionView()
        return true
    }

    override fun onQueryTextChange(newText: String): Boolean = false
//    override fun onQueryTextChange(query: String): Boolean {
//        Log.d(TAG, "changing $query")
//        viewModel.searchItems(query)
//        return true
//    }

    class ScrollListener(private val observable: MutableLiveData<Boolean>) : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            scrolled(recyclerView, observable)
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            scrolled(recyclerView, observable)
        }

        private fun scrolled(recyclerView: RecyclerView, event: MutableLiveData<Boolean>) {
            val lastVisible = ((recyclerView.layoutManager as? LinearLayoutManager)?.findLastCompletelyVisibleItemPosition() ?: -2)
            val itemCount = (recyclerView.adapter?.itemCount ?: 0) - 1
            val newValue = lastVisible == itemCount
            Log.d(TAG, "Old Value ${event.value} new Value $newValue last visible $lastVisible item count $itemCount")
            if (newValue != event.value)
                event.postValue(newValue)
        }
    }


    interface ListViewModel {
        val showList: LiveData<Pair<List<ShowVO>, String?>>
        val canGoToShows: LiveData<Boolean>
        val canGoToWatchlist: LiveData<Boolean>
        val canLoadMore: LiveData<Boolean>
        val displayingShowList: LiveData<Boolean>
        val listTitle: LiveData<String>
        val searchFilter: String
        val adapterFilter: LiveData<String>
        val loading: LiveData<Boolean>
        val showInitialMessage: LiveData<Boolean>

        fun select(show: ShowVO)
        fun delete(show: ShowVO)
        fun refresh()
        fun displayWatchList()
        fun displayShowList()
        fun searchItems(query: String)
        fun loadNext()
    }
}