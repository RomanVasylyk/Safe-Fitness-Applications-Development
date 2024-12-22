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
import com.example.safefitness.utils.AggregationPeriod
import com.example.safefitness.utils.DayPulseData
import com.example.safefitness.utils.GraphDataProcessor
import com.example.safefitness.utils.GraphManager
import kotlinx.coroutines.launch
import lecho.lib.hellocharts.view.LineChartView

class SingleDayGraphFragment : Fragment() {

    private lateinit var graphView: LineChartView
    private lateinit var graphManager: GraphManager
    private lateinit var dataProcessor: GraphDataProcessor
    private var dataType: String = "steps"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_single_day_graph, container, false)
        graphView = view.findViewById(R.id.graphView)

        val database = FitnessDatabase.getDatabase(requireContext())
        val repository = FitnessRepository(database.fitnessDao())
        dataProcessor = GraphDataProcessor(repository)

        graphManager = GraphManager()
        val date = arguments?.getString("date") ?: ""
        dataType = arguments?.getString("dataType") ?: "steps"

        loadDayData(date)
        return view
    }

    private fun loadDayData(day: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = dataProcessor.aggregateData(day, day, dataType, AggregationPeriod.DAY)
            graphManager.updateGraph(
                graphView,
                convertToPairs(result.aggregatedData),
                if (dataType == "steps") "Steps for $day" else "Heart Rate for $day",
                "Time",
                if (dataType == "steps") "Steps" else "BPM",
                requireContext()
            )
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
