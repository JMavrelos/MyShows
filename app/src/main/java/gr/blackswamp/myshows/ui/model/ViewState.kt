package gr.blackswamp.myshows.ui.model

data class ViewState(
    val shows: List<ShowVO>? = null
    , val selectionChanged: Boolean = false
    , val show: ShowDetailVO? = null
    , val hasMore: Boolean? = null
    , val filter: String? = null
    , val inShows: Boolean? = null
    , val hasWatchlist: Boolean? = null
    , val watchListed: Boolean? = null
)