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
import com.example.safefitness.utils.ColumnChartHelper
import com.example.safefitness.utils.DayPulseData
import com.example.safefitness.utils.GraphDataProcessor
import kotlinx.coroutines.launch
import lecho.lib.hellocharts.model.*
import lecho.lib.hellocharts.view.ColumnChartView

class SingleWeekGraphFragment : Fragment() {

    private lateinit var graphView: ColumnChartView
    private lateinit var summaryText: TextView
    private lateinit var dateRangeText: TextView
    private lateinit var dataProcessor: GraphDataProcessor
    private var startDate: String = ""
    private var endDate: String = ""
    private var dataType: String = "steps"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_single_week_graph, container, false)
        graphView = view.findViewById(R.id.columnChartView)
        summaryText = view.findViewById(R.id.weekGraphSummaryText)
        dateRangeText = view.findViewById(R.id.weekGraphDateRangeText)

        val database = FitnessDatabase.getDatabase(requireContext())
        val repository = FitnessRepository(database.fitnessDao())
        dataProcessor = GraphDataProcessor(repository)

        startDate = arguments?.getString("startDate") ?: ""
        endDate = arguments?.getString("endDate") ?: ""
        dataType = arguments?.getString("dataType") ?: "steps"
        loadWeekData()
        return view
    }

    private fun loadWeekData() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = dataProcessor.aggregateData(startDate, endDate, dataType, AggregationPeriod.WEEK)
            summaryText.text = result.summaryText
            dateRangeText.text = result.dateRange
            updateGraph(result.aggregatedData)
        }
    }

    private fun updateGraph(data: List<Any>) {
        val xAxisName = "Day"
        val yAxisName = if (dataType == "steps") "Steps" else "BPM"
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
        fun newInstance(startDate: String, endDate: String, dataType: String): SingleWeekGraphFragment {
            val fragment = SingleWeekGraphFragment()
            val args = Bundle()
            args.putString("startDate", startDate)
            args.putString("endDate", endDate)
            args.putString("dataType", dataType)
            fragment.arguments = args
            return fragment
        }
    }
}
