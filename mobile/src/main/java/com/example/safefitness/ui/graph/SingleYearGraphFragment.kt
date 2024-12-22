package com.example.safefitness.ui.graph

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.safefitness.R
import com.example.safefitness.data.FitnessDatabase
import com.example.safefitness.data.FitnessRepository
import com.example.safefitness.utils.AggregationPeriod
import com.example.safefitness.utils.DayPulseData
import com.example.safefitness.utils.GraphDataProcessor
import kotlinx.coroutines.launch
import lecho.lib.hellocharts.model.*
import lecho.lib.hellocharts.view.ColumnChartView
import java.util.Calendar

class SingleYearGraphFragment : Fragment() {

    private lateinit var graphView: ColumnChartView
    private lateinit var summaryText: TextView
    private lateinit var dateRangeText: TextView
    private lateinit var dataProcessor: GraphDataProcessor
    private var year: Int = 0
    private var dataType: String = "steps"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_single_year_graph, container, false)
        graphView = view.findViewById(R.id.yearColumnChartView)
        summaryText = view.findViewById(R.id.yearGraphSummaryText)
        dateRangeText = view.findViewById(R.id.yearGraphDateRangeText)

        val database = FitnessDatabase.getDatabase(requireContext())
        val repository = FitnessRepository(database.fitnessDao())
        dataProcessor = GraphDataProcessor(repository)

        year = arguments?.getInt("year") ?: Calendar.getInstance().get(Calendar.YEAR)
        dataType = arguments?.getString("dataType") ?: "steps"

        loadYearData()
        return view
    }

    private fun loadYearData() {
        viewLifecycleOwner.lifecycleScope.launch {
            val startDate = "$year-01-01"
            val endDate = "$year-12-31"
            val result = dataProcessor.aggregateData(startDate, endDate, dataType, AggregationPeriod.YEAR)
            summaryText.text = result.summaryText
            dateRangeText.text = result.dateRange
            updateGraph(result.aggregatedData)
        }
    }

    private fun updateGraph(data: List<Any>) {
        var maxYValue = 0f
        val columns = mutableListOf<Column>()
        val axisValues = mutableListOf<AxisValue>()

        if (dataType == "steps") {
            data.forEachIndexed { index, item ->
                if (item is Pair<*, *>) {
                    val label = item.first as? String ?: ""
                    val value = (item.second as? Number)?.toFloat() ?: 0f
                    if (value > maxYValue) maxYValue = value
                    axisValues.add(AxisValue(index.toFloat()).setLabel(label))
                    val subcolumn = SubcolumnValue(value, requireContext().getColor(android.R.color.holo_blue_dark))
                    columns.add(Column(listOf(subcolumn)).apply { setHasLabels(true) })
                }
            }
            val columnChartData = ColumnChartData(columns).apply {
                axisXBottom = Axis(axisValues).setName("Month")
                axisYLeft = Axis().setName("Steps")
            }
            graphView.columnChartData = columnChartData
        } else {
            data.forEachIndexed { index, item ->
                if (item is DayPulseData) {
                    val min = item.minPulse
                    val max = item.maxPulse
                    val label = item.label
                    if (max > maxYValue) maxYValue = max
                    axisValues.add(AxisValue(index.toFloat()).setLabel(label))
                    val minPulseValue = SubcolumnValue(min, requireContext().getColor(android.R.color.holo_blue_dark))
                    val maxPulseValue = SubcolumnValue(max, requireContext().getColor(android.R.color.holo_red_light))
                    columns.add(Column(listOf(minPulseValue, maxPulseValue)).apply { setHasLabels(true) })
                }
            }
            val columnChartData = ColumnChartData(columns).apply {
                axisXBottom = Axis(axisValues).setName("Month")
                axisYLeft = Axis().setName("BPM")
            }
            graphView.columnChartData = columnChartData
        }

        val viewport = Viewport(graphView.maximumViewport)
        viewport.top = maxYValue * 1.1f
        graphView.maximumViewport = viewport
        graphView.currentViewport = viewport
    }

    companion object {
        fun newInstance(year: Int, dataType: String): SingleYearGraphFragment {
            val fragment = SingleYearGraphFragment()
            val args = Bundle()
            args.putInt("year", year)
            args.putString("dataType", dataType)
            fragment.arguments = args
            return fragment
        }
    }
}
