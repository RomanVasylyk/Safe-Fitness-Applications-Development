package com.example.safefitness

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class MainViewModelTest {

    private lateinit var database: FitnessDatabase
    private lateinit var dao: FitnessDao
    private lateinit var repository: FitnessRepository
    private lateinit var dataHandler: DataHandler
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FitnessDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.fitnessDao()
        repository = FitnessRepository(dao)
        dataHandler = DataHandler(dao)
        viewModel = MainViewModel(repository, dataHandler, dao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testUpdateData_aggregatesTotalSteps() = runBlocking {
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        dao.insertData(FitnessEntity(date = "$currentDate 10:00:00", steps = 1000, heartRate = 70f, isSynced = false))
        dao.insertData(FitnessEntity(date = "$currentDate 12:00:00", steps = 2000, heartRate = 75f, isSynced = false))
        viewModel.updateData(10000)
        val totalSteps = viewModel.totalSteps.getOrAwaitValue()
        assertEquals(3000, totalSteps)
    }
}
