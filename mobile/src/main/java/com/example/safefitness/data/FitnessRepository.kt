package com.example.safefitness.data

class FitnessRepository(private val fitnessDao: FitnessDao) {

    suspend fun getDataForRange(startDate: String, endDate: String): List<FitnessEntity> {
        return fitnessDao.getDataForRange(startDate, endDate)
    }

    suspend fun insertData(data: FitnessEntity) {
        fitnessDao.insertData(data)
    }

    suspend fun dataExists(date: String, steps: Int?, heartRate: Float?): Int {
        return fitnessDao.dataExists(date, steps, heartRate)
    }

    suspend fun getFirstEntryDate(): String? {
        return fitnessDao.getFirstEntryDate()
    }

    suspend fun getLastHeartRateForCurrentDay(currentDate: String): Float? {
        return fitnessDao.getLastHeartRateForCurrentDay(currentDate)
    }

    suspend fun getAverageHeartRateForCurrentDay(currentDate: String): Float {
        return fitnessDao.getAverageHeartRateForCurrentDay(currentDate)
    }

    suspend fun getMinHeartRateForCurrentDay(currentDate: String): Float? {
        return fitnessDao.getMinHeartRateForCurrentDay(currentDate)
    }

    suspend fun getMaxHeartRateForCurrentDay(currentDate: String): Float? {
        return fitnessDao.getMaxHeartRateForCurrentDay(currentDate)
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
