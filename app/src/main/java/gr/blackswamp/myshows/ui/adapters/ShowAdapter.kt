package gr.blackswamp.myshows.ui.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import gr.blackswamp.myshows.R
import gr.blackswamp.myshows.ui.model.ShowVO
import java.lang.Math.min

class ShowAdapter(private val fragment: Fragment, private val listener: (ShowVO, Boolean) -> Unit) : RecyclerView.Adapter<ShowAdapter.ShowViewHolder>() {
    companion object {
        const val TAG = "ShowAdapter"
    }
    private val _shows = mutableListOf<ShowVO>()
    val shows: List<ShowVO>
        get() = _shows.filter { it.title.toLowerCase().contains(filter.toLowerCase()) }
    var allowSwipe: Boolean = false
    private var filter: String = ""
    fun setFilter(newFilter: String?) {
        if (newFilter ?: "" != filter) {
            Log.d(TAG,"filter changed")
            notifyChange {
                filter = newFilter ?: ""
            }
        }
    }

    override fun getItemCount(): Int = shows.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShowViewHolder =
        ShowViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.list_item_show
                , parent
                , false))

    override fun onBindViewHolder(holder: ShowViewHolder, position: Int) = holder.update(position, shows[position])

    fun setShows(shows: List<ShowVO>, newFilter: String?) {
        Log.d(TAG,"source changed")
        notifyChange {
            _shows.clear()
            _shows.addAll(shows)
            filter = newFilter ?: ""
        }
    }

    private fun notifyChange(action: () -> Unit) {
        val oldCount = itemCount
        action.invoke()
        if (oldCount > itemCount) {
            notifyItemRangeRemoved(itemCount, oldCount - itemCount)
        } else if (oldCount < itemCount) {
            notifyItemRangeInserted(oldCount, itemCount - oldCount)
        }
        notifyItemRangeChanged(0, min(oldCount, itemCount))
    }


    private fun itemClicked(position: Int) =
        listener.invoke(shows[position], true)


    fun deleteItem(position: Int) =
        listener.invoke(shows[position], false)


    inner class ShowViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        private val thumbnail: AppCompatImageView = view.findViewById(R.id.thumbnail)
        private val title: TextView = view.findViewById(R.id.title)
        private val release: TextView = view.findViewById(R.id.release)
        private val rating: TextView = view.findViewById(R.id.rating)
        private var pos: Int = 0

        init {
            view.setOnClickListener { this@ShowAdapter.itemClicked(pos) }
        }

        fun update(position: Int, show: ShowVO) {
            pos = position
            title.text = show.title
            release.text = show.release
            rating.text = show.rating
            if (show.thumbnail == null) {
                Glide.with(fragment).clear(thumbnail)
                thumbnail.setImageResource(R.drawable.ic_image)
            } else {
                Glide.with(fragment)
                    .load(show.thumbnail)
                    .error(R.drawable.ic_broken_image)
                    .placeholder(R.drawable.ic_sync)
                    .into(thumbnail)
            }

        }
    }
}