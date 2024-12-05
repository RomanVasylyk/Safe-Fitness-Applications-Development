package com.example.safefitness.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FitnessDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertData(fitnessEntity: FitnessEntity): Long

    @Query("DELETE FROM fitness_data WHERE date < :sevenDaysAgo")
    suspend fun deleteOldData(sevenDaysAgo: String)

    @Query("SELECT AVG(heartRate) FROM fitness_data WHERE date LIKE :currentDate || '%' AND heartRate IS NOT NULL")
    suspend fun getAverageHeartRateForCurrentDay(currentDate: String): Float?

    @Query("SELECT SUM(steps) FROM fitness_data WHERE date LIKE :currentDate || '%'")
    suspend fun getStepsForCurrentDay(currentDate: String): Int?

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

    @Query("SELECT * FROM fitness_data WHERE date = :time LIMIT 1")
    suspend fun getStepsByTime(time: String): FitnessEntity?

    @Query("UPDATE fitness_data SET steps = :steps WHERE date = :time")
    suspend fun updateStepsByTime(time: String, steps: Int)

    @Query("SELECT * FROM fitness_data WHERE date = :date LIMIT 1")
    suspend fun getEntryByDate(date: String): FitnessEntity?

    @Query("UPDATE fitness_data SET heartRate = :heartRate WHERE date = :date")
    suspend fun updateHeartRateByTime(date: String, heartRate: Float)

    @Query("UPDATE fitness_data SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markDataAsSynced(ids: List<Int>): Int

    @Query("SELECT heartRate FROM fitness_data WHERE date LIKE :currentDate || '%' AND heartRate IS NOT NULL ORDER BY date DESC LIMIT 1")
    suspend fun getLastHeartRateForCurrentDay(currentDate: String): Float?

    @Query("SELECT * FROM fitness_data WHERE isSynced = 0 ORDER BY date ASC")
    suspend fun getUnsyncedData(): List<FitnessEntity>

    suspend fun insertOrUpdateEntry(entity: FitnessEntity) {
        val existingEntry = getEntryByDate(entity.date)
        if (existingEntry != null) {
            if (entity.steps != null && existingEntry.steps != entity.steps) {
                updateStepsByTime(entity.date, entity.steps)
            }
            if (entity.heartRate != null && existingEntry.heartRate != entity.heartRate) {
                updateHeartRateByTime(entity.date, entity.heartRate)
            }
        } else {
            insertData(entity)
        }
    }
}
