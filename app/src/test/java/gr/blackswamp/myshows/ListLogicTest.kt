package gr.blackswamp.myshows


import android.database.SQLException
import android.database.sqlite.SQLiteException
import gr.blackswamp.myshows.data.api.*
import gr.blackswamp.myshows.data.db.LocalDatabase
import gr.blackswamp.myshows.data.db.ShowDO
import gr.blackswamp.myshows.logic.ListLogic
import gr.blackswamp.myshows.logic.model.Show
import gr.blackswamp.myshows.logic.model.ShowDetails
import gr.blackswamp.myshows.ui.viewmodel.IMainViewModel
import io.reactivex.Observable
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.*
import java.util.*
import kotlin.random.Random
import org.mockito.Mockito.`when` as whenever

class ListLogicTest {

    private lateinit var service: MovieDBService
    private lateinit var vm: IMainViewModel
    private lateinit var db: LocalDatabase
    private lateinit var logic: ListLogic
    private val rnd = Random(System.currentTimeMillis())

    @Before
    fun setUp() {
        service = mock(MovieDBService::class.java)
        db = mock(LocalDatabase::class.java)
        vm = mock(IMainViewModel::class.java)
        logic = ListLogic(vm, service, db, TestSchedulers)
        reset(db) //because init will be called
        reset(vm)
    }

    @Test
    fun getMoviesWithInvalidSearch() {
        logic.searchShows("")
        verify(vm).showError(R.string.error_invalid_filter)
    }

    @Test
    fun checkMoviesAreRetrievedCorrectly() {
        val expectedFilter = "12jhj3k123"
        val expected = buildApiShows(10)
        whenever(service.getShows(expectedFilter, 1))
            .thenReturn(Observable.just(ShowListAO(1, 1, expected, 4)))
        logic.searchShows(expectedFilter)
        verify(vm).setShows(expected.map { Show(it) }, true, expectedFilter)

        assertEquals(expectedFilter, logic.showFilter)
        assertEquals(1, logic.page)
        assertEquals(4, logic.maxPages)
        verify(vm).showLoading(true)
        verify(vm).showLoading(false)
    }

    @Test
    fun whenThereAreNoResultsShowMessage() {
        val expectedFilter = "12jhj3k123"
        whenever(service.getShows(expectedFilter, 1))
            .thenReturn(Observable.just(ShowListAO(1, 1, listOf(), 1)))

        logic.searchShows(expectedFilter)
        verify(vm).showError(R.string.error_no_results)
        verify(vm).showLoading(true)
        verify(vm).showLoading(false)
    }

    @Test
    fun checkPeopleAreFilteredOut() {
        val expectedFilter = "12jhj3k123"
        val all = buildApiShows(100, true)
        val expected = all.filter { it.media_type != "person" }
        whenever(service.getShows(expectedFilter, 1))
            .thenReturn(Observable.just(ShowListAO(1, 1, all, 1)))
        logic.searchShows(expectedFilter)
        verify(vm).setShows(expected.map { Show(it) }, false, expectedFilter)
        assertEquals(expectedFilter, logic.showFilter)
        assertEquals(1, logic.page)
        verify(vm).showLoading(true)
        verify(vm).showLoading(false)
    }

    @Test
    fun checkThatNewShowsAreAdded() {
        val expectedFilter = "12jhj3k123"
        val all = buildApiShows(100)
        whenever(service.getShows(expectedFilter, 1))
            .thenReturn(Observable.just(ShowListAO(1, 1, all.subList(0, 10), 10)))
        whenever(service.getShows(expectedFilter, 2))
            .thenReturn(Observable.just(ShowListAO(1, 2, all.subList(10, 20), 10)))
        logic.searchShows(expectedFilter)
        logic.loadNextShows()

        verify(vm).setShows(all.subList(0, 20).map { Show(it) }, true, expectedFilter)
        assertEquals(expectedFilter, logic.showFilter)
        assertEquals(2, logic.page)
        verify(vm, times(2)).showLoading(true)
        verify(vm, times(2)).showLoading(false)
    }

    @Test
    fun checkThatStateDoesNotChangeWhenNoResultQueryIsSent(){
        val expectedFilter ="111"
        val expectedPage = 1
        val expectedMax = 10
        val incorrectFilter = "12jhj3k123"
        whenever(service.getShows(expectedFilter,1))
            .thenReturn(Observable.just(ShowListAO(1,expectedPage,buildApiShows(30),expectedMax)))

        whenever(service.getShows(incorrectFilter, 1))
            .thenReturn(Observable.just(ShowListAO(1, -3, listOf(), 1)))

        logic.searchShows(expectedFilter)
        verify(vm,never()).showError(anyInt(),any())
        logic.searchShows(incorrectFilter)
        verify(vm).showError(R.string.error_no_results)
        assertEquals(expectedFilter,logic.showFilter)
        assertEquals(expectedPage,logic.page)
        assertEquals(expectedMax,logic.maxPages)

        verify(vm,times(2)).showLoading(true)
        verify(vm,times(2)).showLoading(false)
    }


    @Test
    fun whenListIsRetrievedShowMessage() {
        val expectedFilter = "12jhj3k123"
        val all = buildApiShows(30)
        whenever(service.getShows(expectedFilter, 1))
            .thenReturn(Observable.just(ShowListAO(1, 1, all.subList(0, 10), 3)))
        whenever(service.getShows(expectedFilter, 2))
            .thenReturn(Observable.just(ShowListAO(1, 2, all.subList(0, 10), 3)))
        whenever(service.getShows(expectedFilter, 3))
            .thenReturn(Observable.just(ShowListAO(1, 3, all.subList(0, 10), 3)))
        whenever(service.getShows(expectedFilter, 4))
            .thenReturn(Observable.just(ShowListAO(1, 4, listOf(), 3)))

        logic.searchShows(expectedFilter)
        logic.loadNextShows()
        logic.loadNextShows()
        logic.loadNextShows()
        logic.loadNextShows()

        verify(vm, times(2)).showError(R.string.error_no_more_shows)

        assertEquals(expectedFilter, logic.showFilter)
        assertEquals(3, logic.maxPages)
        assertEquals(3, logic.page)
        verify(vm, times(5)).showLoading(true)
        verify(vm, times(5)).showLoading(false)
    }

    @Test
    fun whenInitialSearchIsEmptyNextPageIsRetrieved() {
        val expectedFilter = "12jhj3k123"
        val all = mutableListOf<ShowAO>()
        all.addAll((0 until 20).map { buildApiShow(it, "person") })
        all.addAll(buildApiShows(10, startId = 20))
        val expected = all.filter { it.media_type != "person" }

        whenever(service.getShows(expectedFilter, 1))
            .thenReturn(Observable.just(ShowListAO(1, 1, all.subList(0, 10), 3)))
        whenever(service.getShows(expectedFilter, 2))
            .thenReturn(Observable.just(ShowListAO(1, 2, all.subList(10, 20), 3)))
        whenever(service.getShows(expectedFilter, 3))
            .thenReturn(Observable.just(ShowListAO(1, 3, all.subList(20, 30), 3)))

        logic.searchShows(expectedFilter)

        verify(vm).setShows(expected.map { Show(it) }, false, expectedFilter)
        assertEquals(expectedFilter, logic.showFilter)
        assertEquals(3, logic.page)
        verify(vm).showLoading(true)
        verify(vm).showLoading(false)
    }

    @Test
    fun whenLoadNextIsEmptyNextPageIsRetrieved() {
        val expectedFilter = "12jhj3k123"
        val all = mutableListOf<ShowAO>()
        all.addAll(buildApiShows(10))
        all.addAll((10 until 20).map { buildApiShow(it, "person") })
        all.addAll(buildApiShows(2, startId = 20))

        whenever(service.getShows(expectedFilter, 1))
            .thenReturn(Observable.just(ShowListAO(1, 1, all.subList(0, 10), 3)))
        whenever(service.getShows(expectedFilter, 2))
            .thenReturn(Observable.just(ShowListAO(1, 2, all.subList(10, 20), 3)))
        whenever(service.getShows(expectedFilter, 3))
            .thenReturn(Observable.just(ShowListAO(1, 3, all.subList(20, 22), 3)))

        logic.searchShows(expectedFilter)
        logic.loadNextShows()

        verify(vm).setShows(all.asSequence().filter { it.media_type != "person" }.map { Show(it) }.toList(), false, expectedFilter)
        assertEquals(expectedFilter, logic.showFilter)
        assertEquals(3, logic.page)
        verify(vm, times(2)).showLoading(true)
        verify(vm, times(2)).showLoading(false)
    }

    @Test
    fun whenUserSelectsShowDisplayDetails() {
        val id = 123
        val expected = ShowDetailAO(id, null, randomString(10), randomString(10), randomString(100), listOf(), VideosAO(listOf()))
        whenever(service.getMovieDetails(id)).thenReturn(Observable.just(expected))

        logic.showSelected(id, true)

        verify(vm).showDetails(ShowDetails(expected, true))
        verify(vm).showLoading(true)
        verify(vm).showLoading(false)
    }

    @Test
    fun onRefreshListIsReloaded() {
        val expectedFilter = "12jhj3k123"
        val all = buildApiShows(100)
        whenever(service.getShows(expectedFilter, 1))
            .thenReturn(Observable.just(ShowListAO(1, 1, all.subList(0, 10), 4)))
        whenever(service.getShows(expectedFilter, 2))
            .thenReturn(Observable.just(ShowListAO(1, 2, all.subList(10, 20), 4)))
        whenever(service.getShows(expectedFilter, 3))
            .thenReturn(Observable.just(ShowListAO(1, 3, all.subList(20, 30), 4)))
        whenever(service.getShows(expectedFilter, 4))
            .thenReturn(Observable.just(ShowListAO(1, 4, all.subList(30, 40), 4)))

        logic.searchShows(expectedFilter)
        verify(vm).setShows(all.subList(0, 10).map { Show(it) }, true, expectedFilter)
        logic.loadNextShows()
        verify(vm).setShows(all.subList(0, 20).map { Show(it) }, true, expectedFilter)
        logic.loadNextShows()
        verify(vm).setShows(all.subList(0, 30).map { Show(it) }, true, expectedFilter)
        reset(vm)

        logic.refreshData()
        verify(vm).setShows(all.subList(0, 10).map { Show(it) }, true, expectedFilter)

        assertEquals(expectedFilter, logic.showFilter)
        assertEquals(1, logic.page)
        assertEquals(4, logic.maxPages)
        verify(vm).showLoading(true)
        verify(vm).showLoading(false)
    }

    @Test
    fun onRefreshWithoutInputErrorIsShown() {
        logic.refreshData()
        verify(vm).showError(R.string.error_invalid_filter)
    }

    @Test
    fun whenUserSelectsShowsSignalIsSentAndListIsUpdated() {
        val expectedFilter = "12jhj3k123"
        val all = buildApiShows(100)
        whenever(service.getShows(expectedFilter, 1))
            .thenReturn(Observable.just(ShowListAO(1, 1, all, 1)))
        logic.searchShows(expectedFilter)
        reset(vm)

        logic.displayShowList()

        verify(vm).showList(true, all.map { Show(it) }, false, expectedFilter)
        assertEquals(expectedFilter, logic.showFilter)
        assertEquals(1, logic.page)
        assertEquals(1, logic.maxPages)
    }

    @Test
    fun whenUserSelectsWatchlistAndThereIsNoneSendError() {
        val expectedFilter = "12jhj3k123"
        val all = buildApiShows(100)
        whenever(service.getShows(expectedFilter, 1))
            .thenReturn(Observable.just(ShowListAO(1, 1, all, 1)))
        logic.searchShows(expectedFilter)
        reset(vm)

        logic.displayWatchList()

        verify(vm, never()).showList(anyBoolean(), anyList(), anyBoolean(), anyString())
        verify(vm).showError(R.string.error_no_watchlist)

        assertEquals(expectedFilter, logic.showFilter)
        assertEquals(1, logic.page)
        assertEquals(1, logic.maxPages)
    }


    @Test
    fun logicInstantiation() {
        val all = buildDbShows(100)
        whenever(db.loadWatchlistMatching(""))
            .thenReturn(all)

        logic = ListLogic(vm, service, db, TestSchedulers)
        verify(vm).setHasWatchlist(true)
        assertEquals("", logic.watchFilter)
        verify(vm).showLoading(true)
        verify(vm).showLoading(false)
    }

    @Test
    fun logicInstantiationWithNoData() {
        whenever(db.loadWatchlistMatching(""))
            .thenReturn(listOf())

        logic = ListLogic(vm, service, db, TestSchedulers)
        verify(vm).setHasWatchlist(false)
        assertEquals("", logic.watchFilter)
        verify(vm).showLoading(true)
        verify(vm).showLoading(false)
    }

    @Test
    fun logicInstantiationWithError() {
        whenever(db.loadWatchlistMatching("")).thenThrow(SQLException::class.java)

        logic = ListLogic(vm, service, db, TestSchedulers)

        verify(vm).setHasWatchlist(false)
        verify(vm).showError(R.string.error_loading_data)
        verify(vm).showLoading(true)
        verify(vm).showLoading(false)
    }

    @Test
    fun filterThroughWatchlist() {
        val expectedFilter = "ovi"
        val all = buildDbShows(100)
        val expected = all.filter { it.title.contains(expectedFilter) }
        whenever(db.loadWatchlistMatching(expectedFilter)).thenReturn(expected)

        logic.searchWatchlist(expectedFilter)
        verify(vm).setShows(expected.map { Show(it) }, false, expectedFilter)
        assertEquals(expectedFilter, logic.watchFilter)
        verify(vm).showLoading(true)
        verify(vm).showLoading(false)
    }

    @Test
    fun filterThroughWatchlistAndGetNoResults() {
        val noResultFilter = "ovi123123"
        val all = buildDbShows(100)
        whenever(db.loadWatchlistMatching(noResultFilter))
            .thenReturn(all.filter { it.title.contains(noResultFilter) })

        logic.searchWatchlist(noResultFilter)
        verify(vm).showError(R.string.error_no_results)
        assertEquals("", logic.watchFilter)
        verify(vm).showLoading(true)
        verify(vm).showLoading(false)
    }


    @Test
    fun deleteFromLogicAndStillItemsLeft() {
        val filter = "11123123"
        val all = buildDbShows(10)
        val toDelete = all[rnd.nextInt(10)]
        val remaining = all.filter { it.id != toDelete.id }

        whenever(db.loadWatchlistMatching(anyString())).thenReturn(all)
        logic = ListLogic(vm, service, db, TestSchedulers)

        logic.displayWatchList()
        logic.searchWatchlist(filter)
        verify(vm, never()).showError(anyInt(), any())

        reset(vm)
        reset(db)

        whenever(db.deleteWatchlistItem(toDelete.id))
            .thenReturn(1)
        whenever(db.loadWatchlistMatching(filter))
            .thenReturn(all.filter { it != toDelete })

        logic.deleteItem(toDelete.id)
        verify(vm).setShows(remaining.map { Show(it) }, false, filter)
        assertEquals(filter, logic.watchFilter)
        verify(vm).showLoading(true)
        verify(vm).showLoading(false)
    }

    @Test
    fun deleteFromLogicAndStillNoItemsLeftWithFilter() {
        val filter = "11123123"
        val toDelete = buildDbShow(1, true)
        whenever(db.loadWatchlistMatching(anyString())).thenReturn(listOf(toDelete))
        logic = ListLogic(vm, service, db, TestSchedulers)
        logic.displayWatchList()
        logic.searchWatchlist(filter)
        verify(vm, never()).showError(anyInt(), any())
        reset(vm)
        reset(db)

        whenever(db.deleteWatchlistItem(toDelete.id))
            .thenReturn(1)
        whenever(db.loadWatchlistMatching(filter))
            .thenReturn(listOf())

        logic.deleteItem(toDelete.id)
        verify(vm).showError(R.string.error_no_results)
        assertEquals(filter, logic.watchFilter)
        verify(vm).showLoading(true)
        verify(vm).showLoading(false)
    }

    @Test
    fun deleteFromLogicAndStillNoItemsLeftWithoutFilter() {
        val toDelete = buildDbShow(1, true)
        whenever(db.loadWatchlistMatching(anyString())).thenReturn(listOf(toDelete))
        logic = ListLogic(vm, service, db, TestSchedulers)
        logic.displayWatchList()
        verify(vm, never()).showError(anyInt(), any())
        reset(vm)
        reset(db)

        whenever(db.deleteWatchlistItem(toDelete.id))
            .thenReturn(1)
        whenever(db.loadWatchlistMatching(""))
            .thenReturn(listOf())

        logic.deleteItem(toDelete.id)
        verify(vm).showList(true, listOf(), false, logic.showFilter)
        assertEquals("", logic.watchFilter)
        verify(vm).showLoading(true)
        verify(vm).showLoading(false)
    }

    @Test
    fun deleteFromLogicAndErrorOccurs() {
        val all = buildDbShows(10)
        val toDelete = all[rnd.nextInt(10)]

        whenever(db.loadWatchlistMatching(anyString())).thenReturn(all)
        logic = ListLogic(vm, service, db, TestSchedulers)

        logic.displayWatchList()
        verify(vm, never()).showError(anyInt(), any())

        reset(vm)
        reset(db)

        whenever(db.deleteWatchlistItem(toDelete.id))
            .thenThrow(SQLiteException::class.java)
        whenever(db.loadWatchlistMatching(anyString()))
            .thenReturn(all)

        logic.deleteItem(toDelete.id)
        verify(vm).showError(R.string.error_delete_watchlist)
        verify(vm).setShows(all.map { Show(it) }, false, "")
        assertEquals("", logic.watchFilter)
        verify(vm).showLoading(true)
        verify(vm).showLoading(false)
    }


    private fun buildApiShows(count: Int, withPersons: Boolean = false, startId: Int = 0): List<ShowAO> {
        val shows = mutableListOf<ShowAO>()
        for (idx in startId until startId + count) {
            val type: String =
                when (rnd.nextInt(if (withPersons) 3 else 2)) {
                    2 -> "person"
                    1 -> "movie"
                    else -> "tv"
                }
            shows.add(buildApiShow(idx, type))
        }
        return shows
    }

    private fun buildApiShow(id: Int, type: String) =
        ShowAO(id, null, Date(rnd.nextLong()).toString(), Date(rnd.nextLong()).toString(), type, "$type $id", "$type $id", rnd.nextDouble(10.0))

    private fun buildDbShows(count: Int, startId: Int = 0): List<ShowDO> =
        (startId until startId + count).map { buildDbShow(it, rnd.nextInt(2) == 1) }

    private fun buildDbShow(id: Int, isMovie: Boolean) =
        ShowDO(id, if (isMovie) "Movie $id" else "Tv $id", null, randomString(10), "Action/Comedy", isMovie, null, rnd.nextDouble(10.0).toString(), Date(rnd.nextLong()).toString(), null, null)

    private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

    private fun randomString(length: Int) = (0..length).map { rnd.nextInt(charPool.size) }.map { charPool[it] }.joinToString("")

}