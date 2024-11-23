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

class SingleMonthGraphFragment : Fragment() {

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
        val view = inflater.inflate(R.layout.fragment_single_month_graph, container, false)

        graphView = view.findViewById(R.id.columnChartView)
        summaryText = view.findViewById(R.id.monthGraphSummaryText)
        dateRangeText = view.findViewById(R.id.monthGraphDateRangeText)

        val database = FitnessDatabase.getDatabase(requireContext())
        dataProcessor = WeekGraphDataProcessor(database.fitnessDao())

        startDate = arguments?.getString("startDate") ?: ""
        endDate = arguments?.getString("endDate") ?: ""
        dataType = arguments?.getString("dataType") ?: "steps"

        loadMonthData()
        return view
    }

    private fun loadMonthData() {
        CoroutineScope(Dispatchers.Main).launch {
            val monthData = dataProcessor.getMonthlyDataForRange(startDate, endDate, dataType)
            summaryText.text = monthData.summaryText
            dateRangeText.text = monthData.dateRange
            updateGraph(monthData.aggregatedData)
        }
    }

    private fun updateGraph(data: List<Any>) {
        if (dataType == "steps") {
            val columns = data.mapIndexed { index, item ->
                val steps = item as Pair<String, Number>
                val subcolumn = SubcolumnValue(steps.second.toFloat(), resources.getColor(android.R.color.holo_blue_light, null))
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
                val minPulseValue = SubcolumnValue(pulseData.minPulse, resources.getColor(android.R.color.holo_blue_dark, null))
                val maxPulseValue = SubcolumnValue(pulseData.maxPulse - pulseData.minPulse, resources.getColor(android.R.color.holo_red_light, null))
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
    }

    companion object {
        fun newInstance(startDate: String, endDate: String, dataType: String): SingleMonthGraphFragment {
            return SingleMonthGraphFragment().apply {
                arguments = Bundle().apply {
                    putString("startDate", startDate)
                    putString("endDate", endDate)
                    putString("dataType", dataType)
                }
            }
        }
    }
}
