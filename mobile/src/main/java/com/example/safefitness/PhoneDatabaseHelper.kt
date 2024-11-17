package com.example.safefitness

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.*

class PhoneDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "phone_fitness.db"
        private const val DATABASE_VERSION = 2
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

    fun insertData(date: String, steps: Int, heartRate: Float?) {
        val db = writableDatabase
        if (!dataExists(date, steps, heartRate)) {
            val contentValues = ContentValues().apply {
                put(COLUMN_DATE, date)
                put(COLUMN_STEPS, steps)
                put(COLUMN_HEART_RATE, heartRate)
            }
            db.insert(TABLE_FITNESS, null, contentValues)
        }
    }

    private fun dataExists(date: String, steps: Int, heartRate: Float?): Boolean {
        val db = readableDatabase
        val selection = "$COLUMN_DATE = ? AND $COLUMN_STEPS = ? AND ($COLUMN_HEART_RATE = ? OR $COLUMN_HEART_RATE IS NULL AND ? IS NULL)"
        val selectionArgs = arrayOf(date, steps.toString(), heartRate?.toString(), heartRate?.toString())
        val cursor = db.query(
            TABLE_FITNESS,
            arrayOf(COLUMN_ID),
            selection,
            selectionArgs,
            null,
            null,
            null
        )
        val exists = cursor.moveToFirst()
        cursor.close()
        return exists
    }

    fun getStepsForCurrentDayWithTime(): List<Pair<String, Int>> {
        val db = readableDatabase
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val cursor = db.query(
            TABLE_FITNESS,
            arrayOf(COLUMN_DATE, COLUMN_STEPS),
            "$COLUMN_DATE LIKE ? AND $COLUMN_STEPS IS NOT NULL",
            arrayOf("$currentDate%"),
            null,
            null,
            "$COLUMN_DATE ASC"
        )
        val stepsList = mutableListOf<Pair<String, Int>>()
        while (cursor.moveToNext()) {
            val date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE))
            val steps = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_STEPS))
            stepsList.add(date to steps)
        }
        cursor.close()
        return stepsList
    }

    fun getHeartRatesForCurrentDayWithTime(): List<Pair<String, Float>> {
        val db = readableDatabase
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val cursor = db.query(
            TABLE_FITNESS,
            arrayOf(COLUMN_DATE, COLUMN_HEART_RATE),
            "$COLUMN_DATE LIKE ? AND $COLUMN_HEART_RATE IS NOT NULL",
            arrayOf("$currentDate%"),
            null,
            null,
            "$COLUMN_DATE ASC"
        )
        val heartRateList = mutableListOf<Pair<String, Float>>()
        while (cursor.moveToNext()) {
            val date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE))
            val heartRate = cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_HEART_RATE))
            heartRateList.add(date to heartRate)
        }
        cursor.close()
        return heartRateList
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

    fun getLastHeartRateForCurrentDay(): Float? {
        val db = readableDatabase
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val cursor = db.query(
            TABLE_FITNESS,
            arrayOf(COLUMN_HEART_RATE),
            "$COLUMN_DATE LIKE ? AND $COLUMN_HEART_RATE IS NOT NULL",
            arrayOf("$currentDate%"),
            null,
            null,
            "$COLUMN_DATE DESC",
            "1"
        )
        val heartRate = if (cursor.moveToFirst()) cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_HEART_RATE)) else null
        cursor.close()
        return heartRate
    }
}
