package com.example.safefitness

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface FitnessDao {
    @Insert
    suspend fun insertData(data: FitnessEntity)

    @Query("SELECT * FROM fitness_data WHERE date LIKE :currentDate || '%'")
    suspend fun getDataForCurrentDay(currentDate: String): List<FitnessEntity>

    @Query("SELECT SUM(steps) FROM fitness_data WHERE date LIKE :currentDate || '%'")
    suspend fun getTotalStepsForCurrentDay(currentDate: String): Int

    @Query("SELECT heartRate FROM fitness_data WHERE date LIKE :currentDate || '%' AND heartRate IS NOT NULL ORDER BY date DESC LIMIT 1")
    suspend fun getLastHeartRateForCurrentDay(currentDate: String): Float?

    @Query("""
    SELECT COUNT(*) 
    FROM fitness_data 
    WHERE date = :date 
      AND (steps = :steps OR (steps IS NULL AND :steps IS NULL))
      AND (heartRate = :heartRate OR (heartRate IS NULL AND :heartRate IS NULL))
""")
    suspend fun dataExists(date: String, steps: Int?, heartRate: Float?): Int

    @Query("DELETE FROM fitness_data")
    suspend fun clearDatabase()
}
