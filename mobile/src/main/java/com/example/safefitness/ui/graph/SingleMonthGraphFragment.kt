package com.example.safefitness.ui.graph

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.safefitness.R
import com.example.safefitness.data.local.FitnessDatabase
import com.example.safefitness.data.repository.FitnessRepository
import com.example.safefitness.utils.aggregation.AggregationPeriod
import com.example.safefitness.utils.chart.ColumnChartHelper
import com.example.safefitness.utils.aggregation.GraphDataProcessor
import kotlinx.coroutines.launch
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
        val repository = FitnessRepository(database.fitnessDao())
        dataProcessor = GraphDataProcessor(repository, requireContext())

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
        val xAxisName = getString(R.string.tab_day)
        val yAxisName = if (dataType == "steps")
            getString(R.string.steps)
        else
            getString(R.string.bpm)

        ColumnChartHelper.buildColumnChart(
            chartView = graphView,
            data = data,
            dataType = dataType,
            xAxisName = xAxisName,
            yAxisName = yAxisName,
            context = requireContext()
        )
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
