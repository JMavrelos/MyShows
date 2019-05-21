package gr.blackswamp.myshows.data.datasource

enum class LoadState {
    Loading,
    Completed,
    Error
}

data class State(val state: LoadState, val error: String? = null) {
    companion object {
        @JvmStatic
        val LOADING = State(LoadState.Loading)
        @JvmStatic
        val SUCCESS = State(LoadState.Completed)
        @JvmStatic
        fun error(message: String?) = State(LoadState.Error, message)
    }
}