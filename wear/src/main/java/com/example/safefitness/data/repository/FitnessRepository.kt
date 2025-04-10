package com.example.safefitness.data.repository

import com.example.safefitness.data.local.entity.FitnessEntity

interface FitnessRepository {
    suspend fun insertOrUpdateData(entity: FitnessEntity)
    suspend fun getStepsForCurrentDay(date: String): Int?
    suspend fun getLastHeartRateForCurrentDay(date: String): Float?
    suspend fun getUnsyncedData(): List<FitnessEntity>
    suspend fun deleteOldData(cutoff: String)
    suspend fun markDataAsSynced(ids: List<Int>)
    suspend fun syncDataWithPhone()
}
