package com.example.safefitness.utils.date

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun getDayDate(position: Int, totalItems: Int): Pair<String, String> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -(totalItems - 1 - position))
        val date = dateFormat.format(calendar.time)
        return date to date
    }

    fun getWeekDate(position: Int, totalItems: Int): Pair<String, String> {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.add(Calendar.WEEK_OF_YEAR, -(totalItems - 1 - position))
        val startDate = dateFormat.format(calendar.time)
        calendar.add(Calendar.DAY_OF_YEAR, 6)
        val endDate = dateFormat.format(calendar.time)
        return startDate to endDate
    }

    fun getMonthDate(position: Int, totalItems: Int): Pair<String, String> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.add(Calendar.MONTH, -(totalItems - 1 - position))
        val startDate = dateFormat.format(calendar.time)
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        val endDate = dateFormat.format(calendar.time)
        return startDate to endDate
    }

    fun getYearDate(position: Int, totalItems: Int): Pair<String, String> {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val year = currentYear - (totalItems - 1 - position)
        return "$year-01-01" to "$year-12-31"
    }
}
