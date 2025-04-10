package com.example.safefitness.ui.graph

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.safefitness.R
import com.example.safefitness.data.local.FitnessDatabase
import com.example.safefitness.data.repository.FitnessRepository
import com.example.safefitness.utils.aggregation.AggregationPeriod
import com.example.safefitness.utils.aggregation.DayPulseData
import com.example.safefitness.utils.aggregation.GraphDataProcessor
import com.example.safefitness.utils.chart.ColumnChartHelper
import com.example.safefitness.utils.chart.GraphManager
import kotlinx.coroutines.launch
import lecho.lib.hellocharts.view.ColumnChartView
import lecho.lib.hellocharts.view.LineChartView

class SingleDayGraphFragment : Fragment() {
    private lateinit var columnChartView: ColumnChartView
    private lateinit var lineChartView: LineChartView
    private lateinit var dataProcessor: GraphDataProcessor
    private var dataType: String = "steps"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_single_day_graph, container, false)
        columnChartView = view.findViewById(R.id.columnChartView)
        lineChartView = view.findViewById(R.id.lineChartView)
        val db = FitnessDatabase.getDatabase(requireContext())
        val repository = FitnessRepository(db.fitnessDao())
        dataProcessor = GraphDataProcessor(repository, requireContext())
        val date = arguments?.getString("date") ?: ""
        dataType = arguments?.getString("dataType") ?: "steps"
        loadDayData(date)
        return view
    }

    private fun loadDayData(day: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = dataProcessor.aggregateData(day, day, dataType, AggregationPeriod.DAY)
            if (dataType == "steps") {
                columnChartView.visibility = View.VISIBLE
                lineChartView.visibility = View.GONE
                ColumnChartHelper.buildColumnChart(
                    chartView = columnChartView,
                    data = result.aggregatedData,
                    dataType = dataType,
                    xAxisName = getString(R.string.time),
                    yAxisName = getString(R.string.steps),
                    context = requireContext()
                )
            } else {
                lineChartView.visibility = View.VISIBLE
                columnChartView.visibility = View.GONE
                GraphManager().updateGraph(
                    lineChartView,
                    convertToPairs(result.aggregatedData),
                    getString(R.string.heart_rate_for_day, day),
                    getString(R.string.time),
                    getString(R.string.bpm),
                    requireContext()
                )
            }
        }
    }

    private fun convertToPairs(data: List<Any>): List<Pair<String, Number>> {
        val list = mutableListOf<Pair<String, Number>>()
        for (item in data) {
            if (item is Pair<*, *>) {
                val key = item.first as? String ?: continue
                val value = item.second as? Number ?: continue
                list.add(key to value)
            }
            if (item is DayPulseData) {
                list.add(item.label to (item.minPulse + item.maxPulse) / 2)
            }
        }
        return list
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
