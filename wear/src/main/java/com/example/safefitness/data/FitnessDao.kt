package com.example.safefitness.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FitnessDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertData(fitnessEntity: FitnessEntity)

    @Query("DELETE FROM fitness_data WHERE date < :sevenDaysAgo")
    suspend fun deleteOldData(sevenDaysAgo: String)

    @Query("SELECT AVG(heartRate) FROM fitness_data WHERE date LIKE :currentDate || '%' AND heartRate IS NOT NULL")
    suspend fun getAverageHeartRateForCurrentDay(currentDate: String): Float?

    @Query("SELECT * FROM fitness_data ORDER BY date ASC")
    suspend fun getAllData(): List<FitnessEntity>

    @Query("SELECT SUM(steps) FROM fitness_data WHERE date LIKE :currentDate || '%'")
    suspend fun getStepsForCurrentDay(currentDate: String): Int?

    @Query("""
    SELECT COUNT(*) 
    FROM fitness_data 
    WHERE strftime('%Y-%m-%d %H:%M', date) = strftime('%Y-%m-%d %H:%M', :date) AND 
          (steps = :steps OR (steps IS NULL AND :steps IS NULL)) AND 
          (heartRate = :heartRate OR (heartRate IS NULL AND :heartRate IS NULL))
    """)
    suspend fun dataExists(date: String, steps: Int?, heartRate: Float?): Int

    @Query("SELECT * FROM fitness_data WHERE date = :time LIMIT 1")
    suspend fun getStepsByTime(time: String): FitnessEntity?

    @Query("UPDATE fitness_data SET steps = :steps WHERE date = :time")
    suspend fun updateStepsByTime(time: String, steps: Int)
}
