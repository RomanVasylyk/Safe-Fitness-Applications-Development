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
        dataProcessor = GraphDataProcessor(repository, requireContext())

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
        val xAxisName = getString(R.string.tab_month)
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
