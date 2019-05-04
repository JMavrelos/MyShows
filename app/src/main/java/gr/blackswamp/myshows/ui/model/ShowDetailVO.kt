package gr.blackswamp.myshows.ui.model

interface ShowDetailVO {
    val id: Int
    val title: String
    val image: String?
    val summary: String
    val genre: String
    val isMovie: Boolean
    val trailer: String?
}