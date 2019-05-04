package gr.blackswamp.myshows.ui.model

interface ShowVO {
    val id: Int
    val thumbnail: String?
    val title: String
    val rating: String
    val release: String
    val isMovie : Boolean
}
