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
import com.example.safefitness.utils.AggregationPeriod
import com.example.safefitness.utils.DayPulseData
import com.example.safefitness.utils.GraphDataProcessor
import kotlinx.coroutines.launch
import lecho.lib.hellocharts.model.*
import lecho.lib.hellocharts.view.ColumnChartView

class SingleMonthGraphFragment : Fragment() {
    private lateinit var graphView: ColumnChartView
    private lateinit var summaryText: TextView
    private lateinit var dateRangeText: TextView
    private lateinit var dataProcessor: GraphDataProcessor
    private var startDate: String = ""
    private var endDate: String = ""
    private var dataType: String = "steps"
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_single_month_graph, container, false)
        graphView = view.findViewById(R.id.columnChartView)
        summaryText = view.findViewById(R.id.monthGraphSummaryText)
        dateRangeText = view.findViewById(R.id.monthGraphDateRangeText)
        val database = FitnessDatabase.getDatabase(requireContext())
        dataProcessor = GraphDataProcessor(database.fitnessDao())
        startDate = arguments?.getString("startDate") ?: ""
        endDate = arguments?.getString("endDate") ?: ""
        dataType = arguments?.getString("dataType") ?: "steps"
        loadMonthData()
        return view
    }
    private fun loadMonthData() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = dataProcessor.aggregateData(startDate, endDate, dataType, AggregationPeriod.MONTH)
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
            val columnChartData = ColumnChartData(columns)
            columnChartData.axisXBottom = Axis(axisValues).setName("Day")
            columnChartData.axisYLeft = Axis().setName("Steps")
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
            val columnChartData = ColumnChartData(columns)
            columnChartData.axisXBottom = Axis(axisValues).setName("Day")
            columnChartData.axisYLeft = Axis().setName("BPM")
            graphView.columnChartData = columnChartData
        }
        val viewport = Viewport(graphView.maximumViewport)
        viewport.top = maxYValue * 1.1f
        graphView.maximumViewport = viewport
        graphView.currentViewport = viewport
    }
    companion object {
        fun newInstance(startDate: String, endDate: String, dataType: String): SingleMonthGraphFragment {
            val fragment = SingleMonthGraphFragment()
            val args = Bundle()
            args.putString("startDate", startDate)
            args.putString("endDate", endDate)
            args.putString("dataType", dataType)
            fragment.arguments = args
            return fragment
        }
    }
}
