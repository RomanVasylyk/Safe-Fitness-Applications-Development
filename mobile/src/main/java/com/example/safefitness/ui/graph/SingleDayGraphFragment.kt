package com.example.safefitness.ui.graph

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.safefitness.R
import com.example.safefitness.data.FitnessDatabase
import com.example.safefitness.data.FitnessRepository
import com.example.safefitness.utils.aggregation.AggregationPeriod
import com.example.safefitness.utils.aggregation.GraphDataProcessor
import com.example.safefitness.utils.chart.ColumnChartHelper
import kotlinx.coroutines.launch
import lecho.lib.hellocharts.view.ColumnChartView

class SingleDayGraphFragment : Fragment() {
    private lateinit var chartView: ColumnChartView
    private lateinit var dataProcessor: GraphDataProcessor
    private var dataType: String = "steps"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_single_day_graph, container, false)
        chartView = view.findViewById(R.id.graphView)
        val database = FitnessDatabase.getDatabase(requireContext())
        val repository = FitnessRepository(database.fitnessDao())
        dataProcessor = GraphDataProcessor(repository, requireContext())
        val date = arguments?.getString("date") ?: ""
        dataType = arguments?.getString("dataType") ?: "steps"
        loadDayData(date)
        return view
    }

    private fun loadDayData(day: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = dataProcessor.aggregateData(day, day, dataType, AggregationPeriod.DAY)
            ColumnChartHelper.buildColumnChart(
                chartView,
                result.aggregatedData,
                dataType,
                getString(R.string.time),
                if (dataType == "steps") getString(R.string.steps) else getString(R.string.bpm),
                requireContext()
            )
        }
    }

    companion object {
        fun newInstance(date: String, dataType: String): SingleDayGraphFragment {
            val fragment = SingleDayGraphFragment()
            val args = Bundle()
            args.putString("date", date)
            args.putString("dataType", dataType)
            fragment.arguments = args
            return fragment
        }
    }
}
