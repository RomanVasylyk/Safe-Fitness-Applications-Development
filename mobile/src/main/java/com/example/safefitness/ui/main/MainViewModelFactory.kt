package com.example.safefitness.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.safefitness.data.DataHandler
import com.example.safefitness.data.FitnessDao
import com.example.safefitness.data.FitnessRepository

class MainViewModelFactory(
    private val repository: FitnessRepository,
    private val dataHandler: DataHandler,
    private val dao: FitnessDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(repository, dataHandler, dao) as T
    }
}
