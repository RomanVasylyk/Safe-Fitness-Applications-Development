package com.example.safefitness.ui.graph

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.safefitness.ui.adapters.DayGraphPagerAdapter
import com.example.safefitness.R
import com.example.safefitness.data.FitnessDao
import com.example.safefitness.data.FitnessDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

class DayGraphFragment : Fragment() {

    private lateinit var viewPager: ViewPager2
    private lateinit var dateText: TextView
    private lateinit var summaryText: TextView
    private lateinit var fitnessDao: FitnessDao
    private lateinit var adapter: DayGraphPagerAdapter

    private var dataType: String = "steps"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_day_graph, container, false)

        viewPager = view.findViewById(R.id.dayGraphPager)
        dateText = view.findViewById(R.id.dayGraphDateText)
        summaryText = view.findViewById(R.id.dayGraphSummaryText)

        val database = FitnessDatabase.getDatabase(requireContext())
        fitnessDao = database.fitnessDao()

        dataType = arguments?.getString("dataType") ?: "steps"

        val availableDaysCount = runBlocking { fitnessDao.getAvailableDaysCount() }
        adapter = DayGraphPagerAdapter(this, fitnessDao, availableDaysCount, dataType)
        viewPager.adapter = adapter

        viewPager.setCurrentItem(availableDaysCount - 1, false)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateSummary(position, availableDaysCount)
            }
        })

        updateSummary(availableDaysCount - 1, availableDaysCount)

        return view
    }

    private fun updateSummary(position: Int, availableDaysCount: Int) {
        val date = getDateForPosition(position, availableDaysCount)
        dateText.text = "Date: $date"

        CoroutineScope(Dispatchers.IO).launch {
            val summary = if (dataType == "steps") {
                fitnessDao.getTotalStepsForCurrentDay(date)
            } else {
                fitnessDao.getDataForCurrentDay(date).mapNotNull { it.heartRate }.average().toInt()
            }

            val summaryLabel = if (dataType == "steps") {
                "Total Steps: $summary"
            } else {
                "Average Heart Rate: $summary BPM"
            }

            CoroutineScope(Dispatchers.Main).launch {
                summaryText.text = summaryLabel
            }
        }
    }

    private fun getDateForPosition(position: Int, availableDaysCount: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -(availableDaysCount - 1 - position))
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    }

    companion object {
        fun newInstance(dataType: String): DayGraphFragment {
            return DayGraphFragment().apply {
                arguments = Bundle().apply {
                    putString("dataType", dataType)
                }
            }
        }
    }
}
