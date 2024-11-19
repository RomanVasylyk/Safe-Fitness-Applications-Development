package com.example.safefitness.data

import org.json.JSONArray

class DataHandler(val fitnessDao: FitnessDao) {

    suspend fun saveData(jsonData: String) {
        val jsonArray = JSONArray(jsonData)
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val date = jsonObject.getString("date")
            val steps = jsonObject.optInt("steps", -1).takeIf { it >= 0 }
            val heartRate = jsonObject.optDouble("heartRate", -1.0).takeIf { it >= 0 }?.toFloat()

            if (fitnessDao.dataExists(date, steps, heartRate) == 0) {
                fitnessDao.insertData(FitnessEntity(date = date, steps = steps, heartRate = heartRate))
            }
        }
    }

    suspend fun getDailyAggregatedData(currentDate: String): Pair<List<Pair<String, Number>>, List<Pair<String, Number>>> {
        val stepsList = fitnessDao.getDataForCurrentDay(currentDate).mapNotNull { it.steps?.let { steps -> it.date to steps as Number } }
        val heartRateList = fitnessDao.getDataForCurrentDay(currentDate).mapNotNull { it.heartRate?.let { heartRate -> it.date to heartRate as Number } }
        return Pair(aggregateDataByHour(stepsList), aggregateHeartRateBy5Minutes(heartRateList))
    }

    private fun aggregateDataByHour(data: List<Pair<String, Number>>): List<Pair<String, Number>> {
        val aggregatedData = mutableMapOf<String, MutableList<Number>>()

        data.forEach { (time, value) ->
            val hour = time.split(":")[0]
            aggregatedData.getOrPut(hour) { mutableListOf() }.add(value)
        }

        return aggregatedData.map { (hour, values) ->
            val label = "$hour:00"
            val aggregatedValue = if (values.first() is Int) {
                values.sumBy { it.toInt() }
            } else {
                values.map { it.toFloat() }.average().toFloat()
            }
            label to aggregatedValue
        }
    }

    private fun aggregateHeartRateBy5Minutes(data: List<Pair<String, Number>>): List<Pair<String, Number>> {
        val aggregatedData = mutableMapOf<String, MutableList<Float>>()

        data.forEach { (time, value) ->
            val heartRate = value.toFloat()
            if (heartRate > 0) {
                val hour = time.split(":")[0]
                val minutes = time.split(":")[1].toInt()
                val roundedMinutes = (minutes / 5) * 5
                val intervalKey = "$hour:${if (roundedMinutes < 10) "0$roundedMinutes" else roundedMinutes}"
                aggregatedData.getOrPut(intervalKey) { mutableListOf() }.add(heartRate)
            }
        }

        return aggregatedData.mapNotNull { (interval, values) ->
            if (values.isNotEmpty()) {
                interval to values.average().toFloat()
            } else {
                null
            }
        }.sortedBy { it.first }
    }
}
