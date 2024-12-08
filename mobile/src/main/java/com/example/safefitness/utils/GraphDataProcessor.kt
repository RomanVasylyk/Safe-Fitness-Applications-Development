package com.example.safefitness.utils

import com.example.safefitness.data.FitnessDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class GraphDataProcessor(private val fitnessDao: FitnessDao) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("d MMM yyyy", Locale.ENGLISH)
    private val shortDateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    suspend fun getWeeklyDataForRange(startDate: String, endDate: String, dataType: String): WeekGraphData {
        return withContext(Dispatchers.IO) {
            if (dataType == "steps") calculateStepsData(startDate, endDate)
            else calculateHeartRateData(startDate, endDate)
        }
    }

    suspend fun getMonthlyDataForRange(startDate: String, endDate: String, dataType: String): WeekGraphData {
        return withContext(Dispatchers.IO) {
            if (dataType == "steps") calculateStepsMonthlyData(startDate, endDate)
            else calculateHeartRateMonthlyData(startDate, endDate)
        }
    }

    suspend fun getYearlyData(year: Int, dataType: String): WeekGraphData {
        return withContext(Dispatchers.IO) {
            val startDate = dateFormat.format(Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, Calendar.JANUARY)
                set(Calendar.DAY_OF_MONTH, 1)
            }.time)

            val endDate = dateFormat.format(Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, Calendar.DECEMBER)
                set(Calendar.DAY_OF_MONTH, 31)
            }.time)

            if (dataType == "steps") calculateStepsYearlyData(startDate, endDate)
            else calculateHeartRateYearlyData(startDate, endDate)
        }
    }

    private suspend fun calculateStepsData(startDate: String, endDate: String): WeekGraphData {
        val stepData = mutableListOf<Pair<String, Number>>()
        val calendar = Calendar.getInstance().apply { time = dateFormat.parse(startDate)!! }

        while (calendar.time <= dateFormat.parse(endDate)!!) {
            val dailySteps = fitnessDao.getDataForCurrentDay(dateFormat.format(calendar.time))
                .mapNotNull { it.steps }.sum()

            stepData.add(getDayLabel(calendar.get(Calendar.DAY_OF_WEEK)) to dailySteps)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val averageSteps = stepData.sumBy { it.second.toInt() } / stepData.size.coerceAtLeast(1)
        return WeekGraphData(
            aggregatedData = stepData,
            xLabels = (1..7).map { getDayLabel(it) }, // Всі дні тижня
            summaryText = "Average Steps per Day: $averageSteps",
            dateRange = createDateRange(startDate, endDate)
        )
    }


    private suspend fun calculateHeartRateData(startDate: String, endDate: String): WeekGraphData {
        val pulseData = mutableListOf<DayPulseData>()
        val calendar = Calendar.getInstance().apply { time = dateFormat.parse(startDate)!! }

        while (calendar.time <= dateFormat.parse(endDate)!!) {
            val dailyHeartRates = fitnessDao.getDataForCurrentDay(dateFormat.format(calendar.time))
                .mapNotNull { it.heartRate }
            if (dailyHeartRates.isNotEmpty()) {
                pulseData.add(
                    DayPulseData(
                        label = getDayLabel(calendar.get(Calendar.DAY_OF_WEEK)),
                        minPulse = dailyHeartRates.minOrNull()?.toFloat() ?: 0f,
                        maxPulse = dailyHeartRates.maxOrNull()?.toFloat() ?: 0f
                    )
                )
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return WeekGraphData(
            aggregatedData = pulseData,
            xLabels = pulseData.map { it.label },
            summaryText = "",
            dateRange = createDateRange(startDate, endDate)
        )
    }

    private suspend fun calculateStepsMonthlyData(startDate: String, endDate: String): WeekGraphData {
        val stepData = mutableListOf<Pair<String, Number>>()
        val calendar = Calendar.getInstance().apply { time = dateFormat.parse(startDate)!! }

        while (calendar.time <= dateFormat.parse(endDate)!!) {
            val dailySteps = fitnessDao.getDataForCurrentDay(dateFormat.format(calendar.time))
                .mapNotNull { it.steps }.sum()
            if (dailySteps > 0) {
                stepData.add(shortDateFormat.format(calendar.time) to dailySteps)
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val totalSteps = stepData.sumBy { it.second.toInt() }
        return WeekGraphData(
            aggregatedData = stepData,
            xLabels = stepData.map { it.first },
            summaryText = "Total Steps: $totalSteps",
            dateRange = createDateRange(startDate, endDate)
        )
    }

    private suspend fun calculateHeartRateMonthlyData(startDate: String, endDate: String): WeekGraphData {
        val pulseData = mutableListOf<DayPulseData>()
        val calendar = Calendar.getInstance().apply { time = dateFormat.parse(startDate)!! }

        while (calendar.time <= dateFormat.parse(endDate)!!) {
            val dailyHeartRates = fitnessDao.getDataForCurrentDay(dateFormat.format(calendar.time))
                .mapNotNull { it.heartRate }
            if (dailyHeartRates.isNotEmpty()) {
                pulseData.add(
                    DayPulseData(
                        label = shortDateFormat.format(calendar.time),
                        minPulse = dailyHeartRates.minOrNull()?.toFloat() ?: 0f,
                        maxPulse = dailyHeartRates.maxOrNull()?.toFloat() ?: 0f
                    )
                )
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return WeekGraphData(
            aggregatedData = pulseData,
            xLabels = pulseData.map { it.label },
            summaryText = "",
            dateRange = createDateRange(startDate, endDate)
        )
    }

    private suspend fun calculateStepsYearlyData(startDate: String, endDate: String): WeekGraphData {
        val stepData = mutableListOf<Pair<String, Number>>()
        val calendar = Calendar.getInstance().apply { time = dateFormat.parse(startDate)!! }

        while (calendar.time <= dateFormat.parse(endDate)!!) {
            val startOfMonth = calendar.clone() as Calendar
            val endOfMonth = calendar.clone() as Calendar
            startOfMonth.set(Calendar.DAY_OF_MONTH, 1)
            endOfMonth.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))

            val monthlySteps = fitnessDao.getDataForRange(
                dateFormat.format(startOfMonth.time), dateFormat.format(endOfMonth.time)
            ).mapNotNull { it.steps }.sum()

            stepData.add(startOfMonth.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.ENGLISH) to monthlySteps)
            calendar.add(Calendar.MONTH, 1)
        }

        val totalSteps = stepData.sumBy { it.second.toInt() }
        return WeekGraphData(
            aggregatedData = stepData,
            xLabels = stepData.map { it.first },
            summaryText = "Total Steps: $totalSteps",
            dateRange = createDateRange(startDate, endDate)
        )
    }

    private suspend fun calculateHeartRateYearlyData(startDate: String, endDate: String): WeekGraphData {
        val pulseData = mutableListOf<DayPulseData>()
        val calendar = Calendar.getInstance().apply { time = dateFormat.parse(startDate)!! }

        while (calendar.time <= dateFormat.parse(endDate)!!) {
            val startOfMonth = calendar.clone() as Calendar
            val endOfMonth = calendar.clone() as Calendar
            startOfMonth.set(Calendar.DAY_OF_MONTH, 1)
            endOfMonth.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))

            val monthlyHeartRates = fitnessDao.getDataForRange(
                dateFormat.format(startOfMonth.time), dateFormat.format(endOfMonth.time)
            ).mapNotNull { it.heartRate }

            if (monthlyHeartRates.isNotEmpty()) {
                pulseData.add(
                    DayPulseData(
                        label = startOfMonth.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.ENGLISH),
                        minPulse = monthlyHeartRates.minOrNull()?.toFloat() ?: 0f,
                        maxPulse = monthlyHeartRates.maxOrNull()?.toFloat() ?: 0f
                    )
                )
            }
            calendar.add(Calendar.MONTH, 1)
        }

        return WeekGraphData(
            aggregatedData = pulseData,
            xLabels = pulseData.map { it.label },
            summaryText = "",
            dateRange = createDateRange(startDate, endDate)
        )
    }

    private fun getDayLabel(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.MONDAY -> "Mon"
            Calendar.TUESDAY -> "Tue"
            Calendar.WEDNESDAY -> "Wed"
            Calendar.THURSDAY -> "Thu"
            Calendar.FRIDAY -> "Fri"
            Calendar.SATURDAY -> "Sat"
            Calendar.SUNDAY -> "Sun"
            else -> ""
        }
    }

    private fun createDateRange(startDate: String, endDate: String): String {
        return "${displayDateFormat.format(dateFormat.parse(startDate))} - ${displayDateFormat.format(dateFormat.parse(endDate))}"
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
