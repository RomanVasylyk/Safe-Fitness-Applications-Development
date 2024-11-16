package com.example.safefitness

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.*

class FitnessDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "fitness.db"
        private const val DATABASE_VERSION = 5
        const val TABLE_FITNESS = "fitness_data"
        const val COLUMN_ID = "_id"
        const val COLUMN_DATE = "date"
        const val COLUMN_STEPS = "steps"
        const val COLUMN_HEART_RATE = "heart_rate"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_FITNESS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_DATE TEXT,
                $COLUMN_STEPS INTEGER,
                $COLUMN_HEART_RATE REAL
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FITNESS")
        onCreate(db)
    }

    fun insertData(steps: Int?, heartRate: Float?) {
        deleteOldData()

        val db = writableDatabase
        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val contentValues = ContentValues().apply {
            put(COLUMN_DATE, currentTime)
            steps?.let { put(COLUMN_STEPS, it) }
            heartRate?.let { put(COLUMN_HEART_RATE, it) }
        }
        db.insert(TABLE_FITNESS, null, contentValues)
    }

    private fun deleteOldData() {
        val db = writableDatabase
        val sevenDaysAgo = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -7)
        }.time
        val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(sevenDaysAgo)

        db.delete(TABLE_FITNESS, "$COLUMN_DATE < ?", arrayOf(formattedDate))
    }

    fun getStepsForCurrentDay(): Int {
        val db = readableDatabase
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val cursor = db.query(
            TABLE_FITNESS,
            arrayOf("SUM($COLUMN_STEPS) AS total_steps"),
            "$COLUMN_DATE LIKE ?",
            arrayOf("$currentDate%"),
            null,
            null,
            null
        )
        var steps = 0
        if (cursor.moveToFirst()) {
            steps = cursor.getInt(cursor.getColumnIndexOrThrow("total_steps"))
        }
        cursor.close()
        return steps
    }

    fun getAverageHeartRateForCurrentDay(): Float {
        val db = readableDatabase
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val cursor = db.query(
            TABLE_FITNESS,
            arrayOf("AVG($COLUMN_HEART_RATE) AS average_heart_rate"),
            "$COLUMN_DATE LIKE ? AND $COLUMN_HEART_RATE IS NOT NULL",
            arrayOf("$currentDate%"),
            null,
            null,
            null
        )
        var averageHeartRate = 0f
        if (cursor.moveToFirst()) {
            averageHeartRate = cursor.getFloat(cursor.getColumnIndexOrThrow("average_heart_rate"))
        }
        cursor.close()
        return averageHeartRate
    }

    fun getAllData(): List<Map<String, Any?>> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_FITNESS,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_DATE ASC"
        )
        val dataList = mutableListOf<Map<String, Any?>>()
        while (cursor.moveToNext()) {
            val date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE))
            val steps = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_STEPS))
            val heartRate = cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_HEART_RATE))
            dataList.add(
                mapOf(
                    COLUMN_DATE to date,
                    COLUMN_STEPS to steps,
                    COLUMN_HEART_RATE to heartRate
                )
            )
        }
        cursor.close()
        return dataList
    }
}
