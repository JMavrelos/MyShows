package gr.blackswamp.myshows

import gr.blackswamp.myshows.data.api.MovieDBService
import gr.blackswamp.myshows.data.db.LocalDatabase
import gr.blackswamp.myshows.logic.DisplayLogic
import gr.blackswamp.myshows.ui.viewmodel.IMainViewModel
import org.junit.Before
import org.mockito.Mockito

class DisplayLogicTest {
    private lateinit var service: MovieDBService
    private lateinit var vm: IMainViewModel
    private lateinit var db: LocalDatabase
    private lateinit var logic: DisplayLogic


    @Before
    fun setUp() {
        service = Mockito.mock(MovieDBService::class.java)
        db = Mockito.mock(LocalDatabase::class.java)
        vm = Mockito.mock(IMainViewModel::class.java)
        logic = DisplayLogic(vm, db, TestSchedulers)
    }


}