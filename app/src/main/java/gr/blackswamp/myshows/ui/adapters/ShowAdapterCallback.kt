package gr.blackswamp.myshows.ui.adapters

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class ShowAdapterCallback(private val adapter: ShowAdapter) : ItemTouchHelper.Callback() {

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int =
        if (adapter.allowSwipe) makeMovementFlags(0, ItemTouchHelper.END) else makeMovementFlags(0,0)

    override fun isItemViewSwipeEnabled(): Boolean = adapter.allowSwipe

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = true

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        adapter.deleteItem(viewHolder.adapterPosition)
    }
}