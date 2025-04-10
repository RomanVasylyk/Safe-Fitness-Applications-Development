package com.example.safefitness.ui.main

import androidx.lifecycle.*
import com.example.safefitness.data.local.FitnessDao
import com.example.safefitness.data.repository.DataHandler
import com.example.safefitness.data.repository.FitnessRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(
    private val repository: FitnessRepository,
    private val dataHandler: DataHandler,
    private val fitnessDao: FitnessDao
) : ViewModel() {

    private val _aggregatedSteps = MutableLiveData<List<Pair<String, Number>>>()
    val aggregatedSteps: LiveData<List<Pair<String, Number>>> get() = _aggregatedSteps

    private val _aggregatedHeartRate = MutableLiveData<List<Pair<String, Number>>>()
    val aggregatedHeartRate: LiveData<List<Pair<String, Number>>> get() = _aggregatedHeartRate

    private val _totalSteps = MutableLiveData<Int>()
    val totalSteps: LiveData<Int> get() = _totalSteps

    private val _lastHeartRate = MutableLiveData<Float?>()
    val lastHeartRate: LiveData<Float?> get() = _lastHeartRate

    private val _avgHeartRate = MutableLiveData<Float>()
    val avgHeartRate: LiveData<Float> get() = _avgHeartRate

    private val _minHeartRate = MutableLiveData<Float?>()
    val minHeartRate: LiveData<Float?> get() = _minHeartRate

    private val _maxHeartRate = MutableLiveData<Float?>()
    val maxHeartRate: LiveData<Float?> get() = _maxHeartRate

    fun updateData(currentGoal: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val (stepsData, heartRateData) = dataHandler.getDailyAggregatedData(date)

            val total = fitnessDao.getTotalStepsForCurrentDay(date)
            val lastHr = fitnessDao.getLastHeartRateForCurrentDay(date)
            val avgHr = fitnessDao.getAverageHeartRateForCurrentDay(date)
            val minHr = fitnessDao.getMinHeartRateForCurrentDay(date)
            val maxHr = fitnessDao.getMaxHeartRateForCurrentDay(date)

            _aggregatedSteps.postValue(stepsData.sortedBy { it.first })
            _aggregatedHeartRate.postValue(heartRateData.sortedBy { it.first })
            _totalSteps.postValue(total)
            _lastHeartRate.postValue(lastHr)
            _avgHeartRate.postValue(avgHr)
            _minHeartRate.postValue(minHr)
            _maxHeartRate.postValue(maxHr)
        }
    }
}
