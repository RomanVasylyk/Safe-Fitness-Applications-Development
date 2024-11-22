package com.example.safefitness

import com.example.safefitness.data.FitnessDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class WeekGraphDataProcessor(private val fitnessDao: FitnessDao) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("d MMM yyyy", Locale.ENGLISH)

    suspend fun getWeeklyData(dataType: String): WeekGraphData {
        return withContext(Dispatchers.IO) {
            val calendar = Calendar.getInstance()
            calendar.firstDayOfWeek = Calendar.MONDAY
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

            val startDate = dateFormat.format(calendar.time)
            calendar.add(Calendar.DAY_OF_YEAR, 6)
            val endDate = dateFormat.format(calendar.time)

            if (dataType == "steps") {
                calculateStepsData(startDate, endDate)
            } else {
                calculateHeartRateData(startDate, endDate)
            }
        }
    }

    private suspend fun calculateStepsData(startDate: String, endDate: String): WeekGraphData {
        val stepData = mutableListOf<Pair<String, Number>>()
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        for (i in 0..6) {
            val currentDate = dateFormat.format(calendar.time)
            val dayLabel = getDayLabel(i)
            val dailySteps = fitnessDao.getDataForCurrentDay(currentDate).mapNotNull { it.steps }.sum()
            if (dailySteps > 0) {
                stepData.add(dayLabel to dailySteps)
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val averageSteps = if (stepData.isNotEmpty()) stepData.sumBy { it.second.toInt() } / stepData.size else 0
        val dateRange = "${displayDateFormat.format(dateFormat.parse(startDate))} - ${displayDateFormat.format(dateFormat.parse(endDate))}"

        return WeekGraphData(
            aggregatedData = stepData,
            xLabels = stepData.map { it.first },
            summaryText = "Average Steps per Day: $averageSteps",
            dateRange = dateRange
        )
    }

    private suspend fun calculateHeartRateData(startDate: String, endDate: String): WeekGraphData {
        val pulseData = mutableListOf<DayPulseData>()
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        for (i in 0..6) {
            val currentDate = dateFormat.format(calendar.time)
            val dayLabel = getDayLabel(i)
            val dailyHeartRates = fitnessDao.getDataForCurrentDay(currentDate).mapNotNull { it.heartRate }
            if (dailyHeartRates.isNotEmpty()) {
                val minPulse = dailyHeartRates.minOrNull()?.toFloat() ?: 0f
                val maxPulse = dailyHeartRates.maxOrNull()?.toFloat() ?: 0f
                pulseData.add(DayPulseData(dayLabel, minPulse, maxPulse))
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val dateRange = "${displayDateFormat.format(dateFormat.parse(startDate))} - ${displayDateFormat.format(dateFormat.parse(endDate))}"

        return WeekGraphData(
            aggregatedData = pulseData,
            xLabels = pulseData.map { it.label },
            summaryText = "",
            dateRange = dateRange
        )
    }

    private fun getDayLabel(dayIndex: Int): String {
        return when (dayIndex) {
            0 -> "Mon"
            1 -> "Tue"
            2 -> "Wed"
            3 -> "Thu"
            4 -> "Fri"
            5 -> "Sat"
            6 -> "Sun"
            else -> ""
        }
    }

    data class WeekGraphData(
        val aggregatedData: List<Any>,
        val xLabels: List<String>,
        val summaryText: String,
        val dateRange: String
    )

    data class DayPulseData(
        val label: String,
        val minPulse: Float,
        val maxPulse: Float
    )
}
