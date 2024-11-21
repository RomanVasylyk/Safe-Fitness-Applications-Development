package com.example.safefitness

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.viewpager.widget.PagerAdapter
import lecho.lib.hellocharts.view.LineChartView
import com.example.safefitness.data.FitnessDao
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

class GraphPagerAdapter(
    private val context: Context,
    private val fitnessDao: FitnessDao,
    private val graphManager: GraphManager,
    private val dataType: String
) : PagerAdapter() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int {
        return runBlocking {
            fitnessDao.getAvailableDaysCount()
        }
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = inflater.inflate(R.layout.page_graph, container, false)
        val graphView = view.findViewById<LineChartView>(R.id.graphView)
        val dateText = view.findViewById<TextView>(R.id.dateText)
        val summaryText = view.findViewById<TextView>(R.id.summaryText)

        val date = getDateForPosition(position)
        dateText.text = "Date: $date"

        val data = runBlocking { fitnessDao.getDataForSpecificDay(date) }

        val aggregatedData = if (dataType == "steps") {
            aggregateDataByHour(
                data.mapNotNull { it.steps?.let { steps -> it.date to steps as Number } }
            )
        } else {
            aggregateHeartRateBy5Minutes(
                data.mapNotNull { it.heartRate?.let { heartRate -> it.date to heartRate as Number } }
            )
        }

        graphManager.updateGraph(
            graph = graphView,
            data = aggregatedData,
            title = if (dataType == "steps") "Steps for $date" else "Heart Rate for $date",
            xAxisName = "Time",
            yAxisName = if (dataType == "steps") "Steps" else "BPM",
            context = context
        )

        runBlocking {
            if (dataType == "steps") {
                val totalSteps = fitnessDao.getTotalStepsForCurrentDay(date)
                summaryText.text = "Steps: $totalSteps"
            } else {
                val lastHeartRate = fitnessDao.getLastHeartRateForCurrentDay(date)
                summaryText.text = "Last HR: ${lastHeartRate?.toInt() ?: "N/A"}"
            }
        }

        container.addView(view)
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    private fun getDateForPosition(position: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -(count - 1 - position))
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    }

    private fun aggregateDataByHour(data: List<Pair<String, Number>>): List<Pair<String, Number>> {
        val aggregatedData = mutableMapOf<String, MutableList<Number>>()

        data.forEach { (time, value) ->
            val hour = time.split(":")[0]
            aggregatedData.getOrPut(hour) { mutableListOf() }.add(value)
        }

        return aggregatedData.map { (hour, values) ->
            val label = "$hour:00"
            val aggregatedValue = values.sumBy { it.toInt() }
            label to aggregatedValue
        }
    }

    private fun aggregateHeartRateBy5Minutes(data: List<Pair<String, Number>>): List<Pair<String, Number>> {
        val aggregatedData = mutableMapOf<String, MutableList<Float>>()

        data.forEach { (time, value) ->
            val heartRate = value.toFloat()
            val hour = time.split(":")[0]
            val minutes = time.split(":")[1].toInt()
            val roundedMinutes = (minutes / 5) * 5
            val intervalKey = "$hour:${if (roundedMinutes < 10) "0$roundedMinutes" else roundedMinutes}"
            aggregatedData.getOrPut(intervalKey) { mutableListOf() }.add(heartRate)
        }

        return aggregatedData.map { (interval, values) ->
            interval to values.average().toFloat()
        }.sortedBy { it.first }
    }
}
