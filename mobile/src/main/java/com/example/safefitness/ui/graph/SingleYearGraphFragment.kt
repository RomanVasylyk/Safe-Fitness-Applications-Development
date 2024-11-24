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
import java.util.Calendar

class SingleYearGraphFragment : Fragment() {

    private lateinit var graphView: ColumnChartView
    private lateinit var summaryText: TextView
    private lateinit var dateRangeText: TextView
    private lateinit var dataProcessor: WeekGraphDataProcessor
    private var year: Int = 0
    private var dataType: String = "steps"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_single_year_graph, container, false)

        graphView = view.findViewById(R.id.yearColumnChartView)
        summaryText = view.findViewById(R.id.yearGraphSummaryText)
        dateRangeText = view.findViewById(R.id.yearGraphDateRangeText)

        val database = FitnessDatabase.getDatabase(requireContext())
        dataProcessor = WeekGraphDataProcessor(database.fitnessDao())

        year = arguments?.getInt("year") ?: Calendar.getInstance().get(Calendar.YEAR)
        dataType = arguments?.getString("dataType") ?: "steps"

        loadYearData()
        return view
    }


    private fun loadYearData() {
        CoroutineScope(Dispatchers.Main).launch {
            val yearData = dataProcessor.getYearlyData(year, dataType)
            summaryText.text = yearData.summaryText
            dateRangeText.text = yearData.dateRange
            updateGraph(yearData.aggregatedData)
        }
    }

    private fun updateGraph(data: List<Any>) {
        var maxYValue = 0f
        val axisValues = mutableListOf<AxisValue>()

        val columns = data.mapIndexed { index, item ->
            if (dataType == "steps") {
                val steps = item as Pair<String, Number>
                val value = steps.second.toFloat()
                if (value > maxYValue) maxYValue = value
                axisValues.add(AxisValue(index.toFloat()).setLabel(steps.first))
                val subcolumn = SubcolumnValue(value, resources.getColor(android.R.color.holo_blue_light, null))
                Column(listOf(subcolumn)).apply { setHasLabels(true) }
            } else {
                val pulseData = item as WeekGraphDataProcessor.DayPulseData
                if (pulseData.maxPulse > maxYValue) maxYValue = pulseData.maxPulse
                axisValues.add(AxisValue(index.toFloat()).setLabel(pulseData.label))
                val minPulseValue = SubcolumnValue(pulseData.minPulse, resources.getColor(android.R.color.holo_blue_dark, null))
                val maxPulseValue = SubcolumnValue(pulseData.maxPulse - pulseData.minPulse, resources.getColor(android.R.color.holo_red_light, null))
                Column(listOf(minPulseValue, maxPulseValue)).apply { setHasLabels(true) }
            }
        }

        val columnChartData = ColumnChartData(columns).apply {
            axisXBottom = Axis(axisValues).apply {
                name = "Month"
                textSize = 12
            }
            axisYLeft = Axis().apply {
                name = if (dataType == "steps") "Steps" else "BPM"
                textSize = 12
            }
        }

        graphView.columnChartData = columnChartData

        val viewport = Viewport(graphView.maximumViewport).apply {
            top = maxYValue * 1.1f
        }
        graphView.maximumViewport = viewport
        graphView.currentViewport = viewport
    }

    companion object {
        fun newInstance(year: Int, dataType: String): SingleYearGraphFragment {
            return SingleYearGraphFragment().apply {
                arguments = Bundle().apply {
                    putInt("year", year)
                    putString("dataType", dataType)
                }
            }
        }
    }
}
