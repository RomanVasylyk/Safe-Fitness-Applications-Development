package com.example.safefitness.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.safefitness.data.local.entity.FitnessEntity

@Dao
interface FitnessDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertData(entity: FitnessEntity): Long

    @Query("UPDATE fitness_data SET steps = :steps WHERE date = :time")
    suspend fun updateStepsByTime(time: String, steps: Int)

    @Query("UPDATE fitness_data SET heartRate = :heartRate WHERE date = :date")
    suspend fun updateHeartRateByTime(date: String, heartRate: Float)

    @Query("DELETE FROM fitness_data WHERE date < :sevenDaysAgo AND isSynced = 1")
    suspend fun deleteOldData(sevenDaysAgo: String)

    @Query("SELECT * FROM fitness_data WHERE date = :date LIMIT 1")
    suspend fun getEntryByDate(date: String): FitnessEntity?

    @Query("SELECT * FROM fitness_data WHERE isSynced = 0 ORDER BY date ASC")
    suspend fun getUnsyncedData(): List<FitnessEntity>

    @Query("UPDATE fitness_data SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markDataAsSynced(ids: List<Int>): Int

    @Query("""
        UPDATE fitness_data 
        SET isSynced = :isSynced 
        WHERE strftime('%Y-%m-%d %H:%M:%S', date) = (
            SELECT strftime('%Y-%m-%d %H:%M:%S', date) 
            FROM fitness_data 
            WHERE id = :id LIMIT 1
        )
    """)
    suspend fun markDataAsSyncedForSameTime(id: Int, isSynced: Int = 1)

    @Query("SELECT SUM(steps) FROM fitness_data WHERE date LIKE :currentDate || '%'")
    suspend fun getStepsForCurrentDay(currentDate: String): Int?

    @Query("SELECT AVG(heartRate) FROM fitness_data WHERE date LIKE :currentDate || '%' AND heartRate IS NOT NULL")
    suspend fun getAverageHeartRateForCurrentDay(currentDate: String): Float?

    @Query("SELECT heartRate FROM fitness_data WHERE heartRate IS NOT NULL ORDER BY date DESC LIMIT 1")
    suspend fun getLastRecordedHeartRate(): Float?

    @Query("SELECT heartRate FROM fitness_data WHERE date LIKE :currentDate || '%' AND heartRate IS NOT NULL ORDER BY date DESC LIMIT 1")
    suspend fun getLastHeartRateForCurrentDay(currentDate: String): Float?

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
