package com.example.safefitness.ui.graph

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.safefitness.R
import com.example.safefitness.data.FitnessDao
import com.example.safefitness.data.FitnessDatabase
import com.example.safefitness.ui.adapters.UniversalGraphPagerAdapter
import com.example.safefitness.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DayGraphFragment : Fragment() {

    private lateinit var viewPager: ViewPager2
    private lateinit var dateText: TextView
    private lateinit var summaryText: TextView
    private lateinit var fitnessDao: FitnessDao

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

        viewLifecycleOwner.lifecycleScope.launch {
            val availableDaysCount = withContext(Dispatchers.IO) {
                fitnessDao.getAvailableDaysCount()
            }

            setupViewPager(availableDaysCount)
            updateSummary(availableDaysCount - 1, availableDaysCount)
        }

        return view
    }

    private suspend fun setupViewPager(availableDaysCount: Int) {
        val adapter = UniversalGraphPagerAdapter(
            fragment = this,
            totalItems = availableDaysCount,
            dateRangeProvider = { position ->
                DateUtils.getDayDate(position, availableDaysCount)
            },
            fragmentProvider = { startDate, _ ->
                SingleDayGraphFragment.newInstance(startDate, dataType)
            }
        )
        withContext(Dispatchers.Main) {
            viewPager.adapter = adapter
            viewPager.setCurrentItem(availableDaysCount - 1, false)
            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    updateSummary(position, availableDaysCount)
                }
            })
        }
    }

    private fun updateSummary(position: Int, availableDaysCount: Int) {
        val date = DateUtils.getDayDate(position, availableDaysCount).first
        dateText.text = "Date: $date"

        viewLifecycleOwner.lifecycleScope.launch {
            val summary = withContext(Dispatchers.IO) {
                if (dataType == "steps") {
                    fitnessDao.getTotalStepsForCurrentDay(date)
                } else {
                    fitnessDao.getDataForCurrentDay(date).mapNotNull { it.heartRate }.average().toInt()
                }
            }

            val summaryLabel = if (dataType == "steps") {
                "Total Steps: $summary"
            } else {
                "Average Heart Rate: $summary BPM"
            }

            summaryText.text = summaryLabel
        }
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