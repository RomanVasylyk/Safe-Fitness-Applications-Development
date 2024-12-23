package com.example.safefitness.utils.aggregation

import com.example.safefitness.data.FitnessEntity
import com.example.safefitness.data.FitnessRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class GraphDataProcessor(private val repository: FitnessRepository) {

    suspend fun aggregateData(
        startDate: String,
        endDate: String,
        dataType: String,
        period: AggregationPeriod
    ): AggregationResult = withContext(Dispatchers.IO) {
        val result = mutableListOf<Any>()
        val xLabels = mutableListOf<String>()
        var summaryText = ""
        when (period) {
            AggregationPeriod.DAY -> {
                val dailyData = aggregateDailyData(startDate, dataType)
                result.addAll(dailyData.aggregatedData)
                xLabels.addAll(dailyData.xLabels)
                summaryText = dailyData.summaryText
            }
            AggregationPeriod.WEEK -> {
                val weekData = calculateWeeklyOrMonthlyData(startDate, endDate, dataType)
                result.addAll(weekData.aggregatedData)
                xLabels.addAll(weekData.xLabels)
                summaryText = weekData.summaryText
            }
            AggregationPeriod.MONTH -> {
                val monthData = calculateWeeklyOrMonthlyData(startDate, endDate, dataType)
                result.addAll(monthData.aggregatedData)
                xLabels.addAll(monthData.xLabels)
                summaryText = monthData.summaryText
            }
            AggregationPeriod.YEAR -> {
                val yearData = calculateYearlyData(startDate, endDate, dataType)
                result.addAll(yearData.aggregatedData)
                xLabels.addAll(yearData.xLabels)
                summaryText = yearData.summaryText
            }
        }
        AggregationResult(result, xLabels, summaryText, "$startDate - $endDate")
    }

    private suspend fun aggregateDailyData(date: String, dataType: String): AggregationResult {
        val dataList = repository.getDataForCurrentDay(date)
        return if (dataType == "steps") {
            aggregateDailySteps(dataList, date)
        } else {
            aggregateDailyHeartRate(dataList, date)
        }
    }

    private fun aggregateDailySteps(dataList: List<FitnessEntity>, date: String): AggregationResult {
        val map = mutableMapOf<String, Int>()
        for (item in dataList) {
            val time = item.date.substring(11, 13)
            val steps = item.steps ?: 0
            map[time] = (map[time] ?: 0) + steps
        }
        val aggregatedData = map.map { (key, value) -> key to value }
        val xLabels = aggregatedData.map { it.first }
        val summaryText = "Total Steps: ${aggregatedData.sumBy { it.second.toInt() }}"
        return AggregationResult(aggregatedData, xLabels, summaryText, date)
    }

    private fun aggregateDailyHeartRate(dataList: List<FitnessEntity>, date: String): AggregationResult {
        val hrMap = mutableMapOf<String, MutableList<Float>>()
        for (item in dataList) {
            if (item.heartRate != null) {
                val hhmm = item.date.substring(11, 16)
                val hh = hhmm.substring(0, 2)
                val mm = hhmm.substring(3, 5).toIntOrNull() ?: 0
                val min5 = (mm / 5) * 5
                val min5Str = if (min5 < 10) "0$min5" else "$min5"
                val label = "$hh:$min5Str"
                val list = hrMap.getOrPut(label) { mutableListOf() }
                list.add(item.heartRate)
            }
        }
        val aggregatedData = mutableListOf<Pair<String, Number>>()
        for ((key, list) in hrMap) {
            val avg = list.sum() / list.size
            aggregatedData.add(key to avg)
        }
        val xLabels = aggregatedData.map { it.first }
        val allRates = dataList.mapNotNull { it.heartRate }
        val summaryText =
            if (allRates.isEmpty()) "No data" else "Avg Heart Rate: ${(allRates.average()).toInt()} bpm"
        return AggregationResult(aggregatedData, xLabels, summaryText, date)
    }

    private suspend fun calculateWeeklyOrMonthlyData(startDate: String, endDate: String, dataType: String): AggregationResult {
        val resultData = mutableListOf<Any>()
        val xLabels = mutableListOf<String>()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.time = sdf.parse(startDate)
        val end = sdf.parse(endDate)
        var totalSteps = 0
        val pulseList = mutableListOf<Float>()
        while (!calendar.time.after(end)) {
            val dayString = sdf.format(calendar.time)
            if (dataType == "steps") {
                val dayList = repository.getDataForCurrentDay(dayString)
                val steps = dayList.sumBy { it.steps ?: 0 }
                resultData.add(dayString to steps)
                totalSteps += steps
            } else {
                val rates = repository.getDataForCurrentDay(dayString).mapNotNull { it.heartRate }
                if (rates.isNotEmpty()) {
                    val min = rates.minOrNull() ?: 0f
                    val max = rates.maxOrNull() ?: 0f
                    resultData.add(DayPulseData(dayString, min, max))
                    pulseList.addAll(rates)
                } else {
                    resultData.add(DayPulseData(dayString, 0f, 0f))
                }
            }
            xLabels.add(dayString)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        val summaryText = if (dataType == "steps") {
            "Total Steps: $totalSteps"
        } else {
            if (pulseList.isEmpty()) "No data" else "Average Heart Rate: ${(pulseList.average()).toInt()} bpm"
        }
        return AggregationResult(resultData, xLabels, summaryText, "$startDate - $endDate")
    }

    private suspend fun calculateYearlyData(startDate: String, endDate: String, dataType: String): AggregationResult {
        val resultData = mutableListOf<Any>()
        val xLabels = mutableListOf<String>()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.time = sdf.parse(startDate)
        val end = sdf.parse(endDate)
        var totalSteps = 0
        val allRates = mutableListOf<Float>()
        while (!calendar.time.after(end)) {
            val monthStart = calendar.clone() as Calendar
            monthStart.set(Calendar.DAY_OF_MONTH, 1)
            val monthEnd = calendar.clone() as Calendar
            monthEnd.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            val monthStartString = sdf.format(monthStart.time)
            val monthEndString = sdf.format(monthEnd.time)
            val dataRange = repository.getDataForRange(monthStartString, monthEndString)
            if (dataType == "steps") {
                val steps = dataRange.sumBy { it.steps ?: 0 }
                val label = monthStart.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.ENGLISH)
                resultData.add(label to steps)
                xLabels.add(label)
                totalSteps += steps
            } else {
                val rates = dataRange.mapNotNull { it.heartRate }
                val label = monthStart.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.ENGLISH)
                if (rates.isNotEmpty()) {
                    val min = rates.minOrNull() ?: 0f
                    val max = rates.maxOrNull() ?: 0f
                    resultData.add(DayPulseData(label, min, max))
                    xLabels.add(label)
                    allRates.addAll(rates)
                } else {
                    resultData.add(DayPulseData(label, 0f, 0f))
                    xLabels.add(label)
                }
            }
            calendar.add(Calendar.MONTH, 1)
        }
        val summaryText = if (dataType == "steps") {
            "Total Steps: $totalSteps"
        } else {
            if (allRates.isEmpty()) "No data" else "Average Heart Rate: ${(allRates.average()).toInt()} bpm"
        }
        return AggregationResult(resultData, xLabels, summaryText, "$startDate - $endDate")
    }
}