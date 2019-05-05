package gr.blackswamp.myshows


import android.database.SQLException
import android.database.sqlite.SQLiteException
import gr.blackswamp.myshows.data.api.*
import gr.blackswamp.myshows.data.db.LocalDatabase
import gr.blackswamp.myshows.data.db.ShowDO
import gr.blackswamp.myshows.logic.MainLogic
import gr.blackswamp.myshows.logic.model.Show
import gr.blackswamp.myshows.ui.model.ViewState
import gr.blackswamp.myshows.ui.viewmodel.IMainViewModel
import io.reactivex.Observable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import java.util.*
import kotlin.random.Random
import org.mockito.Mockito.`when` as whenever

class LogicTest {

    private lateinit var service: MovieDBService
    private lateinit var vm: IMainViewModel
    private lateinit var db: LocalDatabase
    private lateinit var logic: MainLogic
    private val rnd = Random(System.currentTimeMillis())

    @Before
    fun setUp() {
        service = mock(MovieDBService::class.java)
        db = mock(LocalDatabase::class.java)
        vm = mock(IMainViewModel::class.java)
        logic = MainLogic(vm, service, db, TestSchedulers)
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
        val response = buildApiShows(10)
        val expected = ViewState(shows = response.map { Show(it) }, hasMore = true, filter = expectedFilter)

        whenever(service.getShows(expectedFilter, 1))
            .thenReturn(Observable.just(ShowListAO(1, 1, response, 4)))
        logic.searchShows(expectedFilter)

        verify(vm).updateState(expected)
        assertEquals(expectedFilter, logic.showFilter)
        assertEquals(1, logic.page)
        assertEquals(4, logic.maxPages)
        assertEquals(true, logic.inShows)

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
        val expected = ViewState(shows = all.asSequence().filter { it.media_type != "person" }.map { Show(it) }.toList(), hasMore = false, filter = expectedFilter)
        whenever(service.getShows(expectedFilter, 1))
            .thenReturn(Observable.just(ShowListAO(1, 1, all, 1)))

        logic.searchShows(expectedFilter)

        verify(vm).updateState(expected)
        assertEquals(expectedFilter, logic.showFilter)
        assertEquals(1, logic.page)
        verify(vm).showLoading(true)
        verify(vm).showLoading(false)
    }

    @Test
    fun checkThatNewShowsAreAdded() {
        val expectedFilter = "12jhj3k123"
        val all = buildApiShows(100)
        val expected = ViewState(shows = all.subList(0, 20).map { Show(it) }, hasMore = true, filter = expectedFilter)

        whenever(service.getShows(expectedFilter, 1))
            .thenReturn(Observable.just(ShowListAO(1, 1, all.subList(0, 10), 10)))
        whenever(service.getShows(expectedFilter, 2))
            .thenReturn(Observable.just(ShowListAO(1, 2, all.subList(10, 20), 10)))
        logic.searchShows(expectedFilter)
        logic.loadNextShows()

        verify(vm).updateState(expected)
        assertEquals(expectedFilter, logic.showFilter)
        assertEquals(2, logic.page)
        verify(vm, times(2)).showLoading(true)
        verify(vm, times(2)).showLoading(false)
    }

    @Test
    fun checkThatStateDoesNotChangeWhenNoResultQueryIsSent() {
        val expectedFilter = "111"
        val expectedPage = 1
        val expectedMax = 10
        val incorrectFilter = "12jhj3k123"
        whenever(service.getShows(expectedFilter, 1))
            .thenReturn(Observable.just(ShowListAO(1, expectedPage, buildApiShows(30), expectedMax)))

        whenever(service.getShows(incorrectFilter, 1))
            .thenReturn(Observable.just(ShowListAO(1, -3, listOf(), 1)))

        logic.searchShows(expectedFilter)
        verify(vm, never()).showError(anyInt(), any())
        logic.searchShows(incorrectFilter)
        verify(vm).showError(R.string.error_no_results)
        assertEquals(expectedFilter, logic.showFilter)
        assertEquals(expectedPage, logic.page)
        assertEquals(expectedMax, logic.maxPages)

        verify(vm, times(2)).showLoading(true)
        verify(vm, times(2)).showLoading(false)
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
        val expected = ViewState(shows = all.filter { it.media_type != "person" }.map { Show(it) }, hasMore = false, filter = expectedFilter)

        whenever(service.getShows(expectedFilter, 1))
            .thenReturn(Observable.just(ShowListAO(1, 1, all.subList(0, 10), 3)))
        whenever(service.getShows(expectedFilter, 2))
            .thenReturn(Observable.just(ShowListAO(1, 2, all.subList(10, 20), 3)))
        whenever(service.getShows(expectedFilter, 3))
            .thenReturn(Observable.just(ShowListAO(1, 3, all.subList(20, 30), 3)))

        logic.searchShows(expectedFilter)

        verify(vm).updateState(expected)
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
        val expected = ViewState(shows = all.asSequence().filter { it.media_type != "person" }.map { Show(it) }.toList(), hasMore = false, filter = expectedFilter)

        whenever(service.getShows(expectedFilter, 1))
            .thenReturn(Observable.just(ShowListAO(1, 1, all.subList(0, 10), 3)))
        whenever(service.getShows(expectedFilter, 2))
            .thenReturn(Observable.just(ShowListAO(1, 2, all.subList(10, 20), 3)))
        whenever(service.getShows(expectedFilter, 3))
            .thenReturn(Observable.just(ShowListAO(1, 3, all.subList(20, 22), 3)))

        logic.searchShows(expectedFilter)
        logic.loadNextShows()

        verify(vm).updateState(expected)

        assertEquals(expectedFilter, logic.showFilter)
        assertEquals(3, logic.page)
        verify(vm, times(2)).showLoading(true)
        verify(vm, times(2)).showLoading(false)
    }

    @Test
    fun whenUserSelectsShowFromShowListDisplayDetails() {
        val filter = "111"
        val all = buildApiShows(10, false, rnd.nextInt(100))
        val selected = all[3]
        val id = selected.id
        val selectedDetails = ShowDetailAO(selected.id, randomString(100), listOf(), VideosAO(listOf()))
        val expected = ViewState(selectionChanged = true, show = Show(Show(selected), selectedDetails), watchListed = false)

        whenever(service.getShows(filter, 1))
            .thenReturn(Observable.just(ShowListAO(1, 1, all, 1)))

        if (selected.media_type == "movie") {
            whenever(service.getMovieDetails(id)).thenReturn(Observable.just(selectedDetails))
        } else {
            whenever(service.getTvDetails(id)).thenReturn(Observable.just(selectedDetails))
        }

        logic.searchShows(filter)
        logic.showSelected(id, true)

        verify(vm).updateState(expected)
        verify(vm, times(2)).showLoading(true)
        verify(vm, times(2)).showLoading(false)
    }

    @Test
    fun whenUserSelectsShowFromShowListWhichIsNotLoaded() {
        val filter = "111"
        val all = buildApiShows(10, false, rnd.nextInt(100))
        val id = 1111
        whenever(service.getShows(filter, 1))
            .thenReturn(Observable.just(ShowListAO(1, 1, all, 1)))
        logic.searchShows(filter)
        verify(vm).showLoading(true)
        verify(vm).showLoading(false)

        reset(vm)

        logic.showSelected(id, true)

        verify(vm, never()).updateState(anyNotNull())
        verify(vm).showError(R.string.error_show_not_found)
        verify(vm).showLoading(true)
        verify(vm).showLoading(false)
    }

    @Test
    fun whenUserSelectsShowFromWatchListDisplayDetails() {
        logic.watchList.clear()
        logic.watchList.addAll(buildDbShows(10, rnd.nextInt(100)).map { Show(it) })
        val selected = logic.watchList[3]
        val id = selected.id
        val expected = ViewState(selectionChanged = true, show = selected, watchListed = true)

        logic.showSelected(id, false)

        verify(vm).updateState(expected)
        verify(vm).showLoading(true)
        verify(vm).showLoading(false)
        assertEquals(selected, logic.show)
    }

    @Test
    fun whenUserSelectsShowFromWatchListWhichIsNotLoaded() {
        logic.watchList.clear()
        logic.watchList.addAll(buildDbShows(10, rnd.nextInt(100)).map { Show(it) })
        val id = 113

        logic.showSelected(id, false)

        verify(vm).showError(R.string.error_show_not_found)

        verify(vm, never()).updateState(anyNotNull())
        verify(vm).showLoading(true)
        verify(vm).showLoading(false)
        assertNull(logic.show)
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
        verify(vm).updateState(ViewState(shows = all.subList(0, 10).map { Show(it) }, hasMore = true, filter = expectedFilter))
        logic.loadNextShows()
        verify(vm).updateState(ViewState(shows = all.subList(0, 20).map { Show(it) }, hasMore = true, filter = expectedFilter))
        logic.loadNextShows()
        verify(vm).updateState(ViewState(shows = all.subList(0, 30).map { Show(it) }, hasMore = true, filter = expectedFilter))
        reset(vm)
        logic.refreshData()

        verify(vm).updateState(ViewState(shows = all.subList(0, 10).map { Show(it) }, hasMore = true, filter = expectedFilter))

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
        val expected = ViewState(shows = all.map { Show(it) }, hasMore = false, filter = expectedFilter, inShows = true)

        whenever(service.getShows(expectedFilter, 1))
            .thenReturn(Observable.just(ShowListAO(1, 1, all, 1)))
        logic.searchShows(expectedFilter)
        reset(vm)

        logic.displayShowList()

        verify(vm).updateState(expected)
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

        verify(vm, never()).updateState(anyNotNull())
        verify(vm).showError(R.string.error_no_watchlist)

        assertEquals(expectedFilter, logic.showFilter)
        assertEquals(1, logic.page)
        assertEquals(1, logic.maxPages)
    }

    @Test
    fun logicInstantiation() {
        val all = buildDbShows(100)
        whenever(db.loadWatchlist())
            .thenReturn(all)
        val expected = ViewState(inShows = true, hasMore = false, hasWatchlist = true)

        logic = MainLogic(vm, service, db, TestSchedulers)

        verify(vm).updateState(expected)

        assertEquals("", logic.watchFilter)
        verify(vm).showLoading(true)
        verify(vm).showLoading(false)
    }

    @Test
    fun logicInstantiationWithNoData() {
        whenever(db.loadWatchlist())
            .thenReturn(listOf())

        val expected = ViewState(inShows = true, hasMore = false, hasWatchlist = false)

        logic = MainLogic(vm, service, db, TestSchedulers)

        verify(vm).updateState(expected)
        assertEquals("", logic.watchFilter)
        verify(vm).showLoading(true)
        verify(vm).showLoading(false)
    }

    @Test
    fun logicInstantiationWithError() {
        whenever(db.loadWatchlist()).thenThrow(SQLException::class.java)

        val expected = ViewState(inShows = true, hasMore = false, hasWatchlist = false)

        logic = MainLogic(vm, service, db, TestSchedulers)

        verify(vm).updateState(expected)
        verify(vm).showError(R.string.error_loading_data)
        verify(vm).showLoading(true)
        verify(vm).showLoading(false)
    }

    @Test
    fun filterThroughWatchlist() {
        val expectedFilter = "ovi"
        logic.watchList.clear()
        logic.watchList.addAll(buildDbShows(100).map { Show(it) })
        logic.inShows = false
        val expected = ViewState(filter = expectedFilter)

        logic.searchShows(expectedFilter)

        verify(vm).updateState(expected)
        assertEquals(expectedFilter, logic.watchFilter)
        verify(vm, never()).showLoading(true)
        verify(vm, never()).showLoading(false)
    }

    @Test
    fun deleteFromLogicAndStillItemsLeft() {
        logic.watchList.clear()
        val all = buildDbShows(10)
        logic.watchList.addAll(all.map { Show(it) })
        logic.inShows = false
        val toDelete = logic.watchList[3]
        val remainingDo = all.filter { it.id != toDelete.id }
        val remaining = logic.watchList.filter { it.id != toDelete.id }

        val expected = ViewState(shows = remaining, hasMore = false)

        whenever(db.deleteWatchlistItem(toDelete.id))
            .thenReturn(remainingDo)

        logic.deleteItem(toDelete.id)
        verify(vm).updateState(expected)

        verify(vm).showLoading(true)
        verify(vm).showLoading(false)
    }

    @Test
    fun deleteFromWatchlistAndNoItemsLeft() {
        logic.watchList.clear()
        logic.showList.clear()
        logic.page = 1
        logic.maxPages = 10
        logic.inShows = false
        logic.showList.addAll(buildApiShows(10).map { Show(it) })
        val toDelete = buildDbShow(123, true)
        logic.show = Show(toDelete)
        logic.watchList.add(Show(toDelete))

        val expected = ViewState(shows = logic.showList, inShows = true, filter = logic.showFilter, hasMore = true, watchListed = false)

        whenever(db.deleteWatchlistItem(toDelete.id))
            .thenReturn(listOf())

        logic.deleteItem(toDelete.id)
        verify(vm).updateState(expected)
        verify(vm).showLoading(true)
        verify(vm).showLoading(false)
    }

    @Test
    fun deleteFromWatchlistWhileInDisplay() {
        logic.watchList.clear()
        logic.showList.clear()
        logic.page = 1
        logic.maxPages = 10
        logic.inShows = true
        logic.showList.addAll(buildApiShows(10).map { Show(it) })
        val toDelete = buildDbShow(123, true)
        logic.show = Show(toDelete)
        logic.watchList.add(Show(toDelete))

        val expected = ViewState(watchListed = false)

        whenever(db.deleteWatchlistItem(toDelete.id))
            .thenReturn(listOf())

        logic.toggleItem()

        verify(vm,times(2)).updateState(expected)
        verify(vm).showLoading(true)
        verify(vm).showLoading(false)
    }


    @Test
    fun deleteFromWatchlistAndErrorOccurs() {
        val all = buildDbShows(10)
        logic.inShows = false
        logic.watchList.clear()
        logic.watchList.addAll(all.map { Show(it) })
        val toDelete = all[rnd.nextInt(10)]
        val expected = ViewState(shows = logic.watchList, hasMore = false)

        whenever(db.deleteWatchlistItem(toDelete.id))
            .thenThrow(SQLiteException::class.java)

        logic.deleteItem(toDelete.id)

        verify(vm).updateState(expected)
        verify(vm).showError(R.string.error_delete_watchlist)
        assertEquals("", logic.watchFilter)
        verify(vm).showLoading(true)
        verify(vm).showLoading(false)
    }

    @Test
    fun userExitingDisplayShowsList() {
        logic.exitDisplay()
        verify(vm).updateState(ViewState(selectionChanged = true, show = null, watchListed = false))
    }

    @Test
    fun userAddingToWatchlist() {
        logic.showList.addAll(buildApiShows(10).map { Show(it) })
        val toAddDetails = ShowDetailAO(logic.showList[3].id, randomString(100), listOf(), null)
        logic.show = Show(logic.showList[3], toAddDetails)
        logic.inShows = true
        val toAdd = ShowDO(logic.show!!)
        val expected = ViewState(watchListed = true)

        whenever(db.addWatchlistItem(toAdd))
            .thenReturn(listOf(toAdd))

        logic.toggleItem()
        verify(vm, times(2)).updateState(expected)
        verify(vm).showLoading(true)
        verify(vm).showLoading(false)
    }

    @Test
    fun userAddingToWatchlistWithError() {
        logic.showList.addAll(buildApiShows(10).map { Show(it) })
        val toAddDetails = ShowDetailAO(logic.showList[3].id, randomString(100), listOf(), null)
        logic.show = Show(logic.showList[3], toAddDetails)
        logic.inShows = true
        logic.watchFilter = ""

        val toAdd = ShowDO(logic.show!!)

        val expected1 = ViewState(watchListed = true)
        val expected2 = ViewState(watchListed = false)

        whenever(db.addWatchlistItem(toAdd))
            .thenThrow(SQLiteException::class.java)

        logic.toggleItem()

        verify(vm).showError(R.string.error_add_watchlist)
        verify(vm).updateState(expected1)
        verify(vm).updateState(expected2)
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
        ShowDO(id, if (isMovie) "Movie $id" else "Tv $id", null, randomString(10), "Action/Comedy", isMovie, rnd.nextDouble(10.0).toString(), Date(rnd.nextLong()).toString(), null)

    private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

    private fun randomString(length: Int) = (0..length).map { rnd.nextInt(charPool.size) }.map { charPool[it] }.joinToString("")

    private fun <T> anyNotNull(): T {
        any<T>()
        return uninitialized()
    }

    private fun <T> uninitialized(): T = null as T
}