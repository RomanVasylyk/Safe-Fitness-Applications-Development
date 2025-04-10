package com.example.safefitness.data.repository

import com.example.safefitness.data.local.FitnessDao
import com.example.safefitness.data.local.FitnessEntity

class FitnessRepository(private val fitnessDao: FitnessDao) {

    suspend fun getDataForRange(startDate: String, endDate: String): List<FitnessEntity> {
        return fitnessDao.getDataForRange(startDate, endDate)
    }

    suspend fun getFirstEntryDate(): String? {
        return fitnessDao.getFirstEntryDate()
    }

    suspend fun getAvailableDaysCount(): Int {
        return fitnessDao.getAvailableDaysCount()
    }

    suspend fun getTotalStepsForCurrentDay(date: String): Int {
        return fitnessDao.getTotalStepsForCurrentDay(date)
    }

    suspend fun getDataForCurrentDay(date: String): List<FitnessEntity> {
        return fitnessDao.getDataForCurrentDay(date)
    }
}
