package com.example.safefitness.ui.graph

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.safefitness.R
import com.example.safefitness.data.FitnessDatabase
import com.example.safefitness.utils.WeekGraphDataProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import lecho.lib.hellocharts.model.*
import lecho.lib.hellocharts.view.ColumnChartView

class SingleWeekGraphFragment : Fragment() {

    private lateinit var graphView: ColumnChartView
    private lateinit var summaryText: TextView
    private lateinit var dateRangeText: TextView
    private lateinit var dataProcessor: WeekGraphDataProcessor
    private var startDate: String = ""
    private var endDate: String = ""
    private var dataType: String = "steps"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_single_week_graph, container, false)

        graphView = view.findViewById(R.id.columnChartView)
        summaryText = view.findViewById(R.id.weekGraphSummaryText)
        dateRangeText = view.findViewById(R.id.weekGraphDateRangeText)

        val database = FitnessDatabase.getDatabase(requireContext())
        dataProcessor = WeekGraphDataProcessor(database.fitnessDao())

        startDate = arguments?.getString("startDate") ?: ""
        endDate = arguments?.getString("endDate") ?: ""
        dataType = arguments?.getString("dataType") ?: "steps"

        loadWeekData()
        return view
    }

    private fun loadWeekData() {
        CoroutineScope(Dispatchers.Main).launch {
            val weekData = dataProcessor.getWeeklyDataForRange(startDate, endDate, dataType)
            summaryText.text = weekData.summaryText
            dateRangeText.text = weekData.dateRange
            updateGraph(weekData.aggregatedData)
        }
    }

    private fun updateGraph(data: List<Any>) {
        var maxYValue = 0f

        if (dataType == "steps") {
            val columns = data.mapIndexed { index, item ->
                val steps = item as Pair<String, Number>
                val value = steps.second.toFloat()
                if (value > maxYValue) maxYValue = value
                val subcolumn = SubcolumnValue(value, resources.getColor(android.R.color.holo_blue_light, null))
                Column(listOf(subcolumn)).apply { setHasLabels(true) }
            }

            val columnChartData = ColumnChartData(columns).apply {
                axisXBottom = Axis(data.mapIndexed { index, item ->
                    val steps = item as Pair<String, Number>
                    AxisValue(index.toFloat()).setLabel(steps.first)
                }).apply {
                    name = "Day"
                    textSize = 12
                }
                axisYLeft = Axis().apply {
                    name = "Steps"
                    textSize = 12
                }
            }

            graphView.columnChartData = columnChartData
        } else if (dataType == "heartRate") {
            val columns = data.map { item ->
                val pulseData = item as WeekGraphDataProcessor.DayPulseData
                if (pulseData.maxPulse > maxYValue) maxYValue = pulseData.maxPulse
                val minPulseValue = SubcolumnValue(pulseData.minPulse, resources.getColor(android.R.color.holo_blue_dark, null))
                val maxPulseValue = SubcolumnValue(pulseData.maxPulse, resources.getColor(android.R.color.holo_red_light, null))
                Column(listOf(minPulseValue, maxPulseValue)).apply { setHasLabels(true) }
            }

            val columnChartData = ColumnChartData(columns).apply {
                axisXBottom = Axis(data.mapIndexed { index, item ->
                    val pulseData = item as WeekGraphDataProcessor.DayPulseData
                    AxisValue(index.toFloat()).setLabel(pulseData.label)
                }).apply {
                    name = "Day"
                    textSize = 12
                }
                axisYLeft = Axis().apply {
                    name = "BPM"
                    textSize = 12
                }
            }

            graphView.columnChartData = columnChartData
        }

        val viewport = Viewport(graphView.maximumViewport).apply {
            top = maxYValue * 1.1f
        }
        graphView.maximumViewport = viewport
        graphView.currentViewport = viewport
    }


    companion object {
        fun newInstance(startDate: String, endDate: String, dataType: String): SingleWeekGraphFragment {
            return SingleWeekGraphFragment().apply {
                arguments = Bundle().apply {
                    putString("startDate", startDate)
                    putString("endDate", endDate)
                    putString("dataType", dataType)
                }
            }
        }
    }
}
