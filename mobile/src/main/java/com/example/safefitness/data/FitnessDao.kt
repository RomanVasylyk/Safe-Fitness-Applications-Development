package com.example.safefitness.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FitnessDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertData(data: FitnessEntity)

    @Query("SELECT * FROM fitness_data WHERE date(date) LIKE :currentDate || '%'")
    suspend fun getDataForCurrentDay(currentDate: String): List<FitnessEntity>

    @Query("SELECT SUM(steps) FROM fitness_data WHERE date(date) LIKE :currentDate || '%'")
    suspend fun getTotalStepsForCurrentDay(currentDate: String): Int

    @Query("SELECT heartRate FROM fitness_data WHERE date LIKE :currentDate || '%' AND heartRate IS NOT NULL ORDER BY date DESC LIMIT 1")
    suspend fun getLastHeartRateForCurrentDay(currentDate: String): Float?

    @Query(
        """
    SELECT COUNT(*) 
    FROM fitness_data 
    WHERE date = :date AND 
          (steps = :steps OR (steps IS NULL AND :steps IS NULL)) AND 
          (heartRate = :heartRate OR (heartRate IS NULL AND :heartRate IS NULL))
    """
    )
    suspend fun dataExists(date: String, steps: Int?, heartRate: Float?): Int

    @Query("DELETE FROM fitness_data")
    suspend fun clearDatabase()

    @Query("SELECT COUNT(DISTINCT date(date)) FROM fitness_data")
    suspend fun getAvailableDaysCount(): Int

    @Query("SELECT AVG(heartRate) FROM fitness_data WHERE date(date) LIKE :currentDate || '%' AND heartRate IS NOT NULL")
    suspend fun getAverageHeartRateForCurrentDay(currentDate: String): Float

    @Query("SELECT MIN(date) FROM fitness_data")
    suspend fun getFirstEntryDate(): String?

    @Query("SELECT * FROM fitness_data WHERE date(date) BETWEEN :startDate AND :endDate")
    suspend fun getDataForRange(startDate: String, endDate: String): List<FitnessEntity>

    @Query("SELECT MIN(heartRate) FROM fitness_data WHERE date(date) LIKE :currentDate || '%' AND heartRate IS NOT NULL")
    suspend fun getMinHeartRateForCurrentDay(currentDate: String): Float?

    @Query("SELECT MAX(heartRate) FROM fitness_data WHERE date(date) LIKE :currentDate || '%' AND heartRate IS NOT NULL")
    suspend fun getMaxHeartRateForCurrentDay(currentDate: String): Float?

}
