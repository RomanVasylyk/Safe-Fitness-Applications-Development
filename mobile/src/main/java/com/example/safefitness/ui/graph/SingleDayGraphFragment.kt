package com.example.safefitness.ui.graph

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.safefitness.utils.GraphManager
import com.example.safefitness.R
import com.example.safefitness.data.FitnessDao
import com.example.safefitness.data.FitnessDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import lecho.lib.hellocharts.view.LineChartView
import java.util.TreeMap

class SingleDayGraphFragment : Fragment() {

    private lateinit var graphView: LineChartView
    private lateinit var fitnessDao: FitnessDao
    private lateinit var graphManager: GraphManager

    private var dataType: String = "steps"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_single_day_graph, container, false)
        graphView = view.findViewById(R.id.graphView)

        val database = FitnessDatabase.getDatabase(requireContext())
        fitnessDao = database.fitnessDao()
        graphManager = GraphManager()

        val date = arguments?.getString("date") ?: return view
        dataType = arguments?.getString("dataType") ?: "steps"

        loadDayData(date)
        return view
    }

    private fun loadDayData(date: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val rawData = withContext(Dispatchers.IO) {
                if (dataType == "steps") {
                    fitnessDao.getDataForCurrentDay(date)
                        .mapNotNull { it.steps?.let { steps -> it.date to steps as Number } }
                } else {
                    fitnessDao.getDataForCurrentDay(date)
                        .mapNotNull { it.heartRate?.let { heartRate -> it.date to heartRate as Number } }
                }
            }

            val aggregatedData = if (dataType == "steps") {
                aggregateDataByHour(rawData)
            } else {
                aggregateHeartRateBy5Minutes(rawData)
            }

            graphManager.updateGraph(
                graph = graphView,
                data = aggregatedData,
                title = if (dataType == "steps") "Steps for $date" else "Heart Rate for $date",
                xAxisName = "Time",
                yAxisName = if (dataType == "steps") "Steps" else "BPM",
                context = requireContext()
            )
        }
    }

    private fun aggregateDataByHour(data: List<Pair<String, Number>>): List<Pair<String, Number>> {
        val aggregatedData = TreeMap<String, MutableList<Number>>() // Використання TreeMap

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

    companion object {
        fun newInstance(date: String, dataType: String): SingleDayGraphFragment {
            return SingleDayGraphFragment().apply {
                arguments = Bundle().apply {
                    putString("date", date)
                    putString("dataType", dataType)
                }
            }
        }
    }
}
