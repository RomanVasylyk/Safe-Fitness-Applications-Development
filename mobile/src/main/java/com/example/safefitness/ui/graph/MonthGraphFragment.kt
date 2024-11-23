package com.example.safefitness.ui.graph

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.safefitness.R
import com.example.safefitness.data.FitnessDatabase
import com.example.safefitness.ui.adapters.MonthGraphPagerAdapter
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

class MonthGraphFragment : Fragment() {

    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: MonthGraphPagerAdapter
    private var dataType: String = "steps"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_month_graph, container, false)

        viewPager = view.findViewById(R.id.monthGraphPager)

        val database = FitnessDatabase.getDatabase(requireContext())

        dataType = arguments?.getString("dataType") ?: "steps"

        val (totalMonths, currentMonthPosition) = runBlocking { getTotalMonthsCount(database) }
        adapter = MonthGraphPagerAdapter(this, database, totalMonths, dataType)
        viewPager.adapter = adapter

        viewPager.setCurrentItem(currentMonthPosition, false)

        return view
    }

    private suspend fun getTotalMonthsCount(database: FitnessDatabase): Pair<Int, Int> {
        val firstEntryDateString = database.fitnessDao().getFirstEntryDate() ?: return Pair(1, 0)
        val firstEntryDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(firstEntryDateString) ?: return Pair(1, 0)

        val calendar = Calendar.getInstance()
        calendar.time = firstEntryDate

        val startYear = calendar.get(Calendar.YEAR)
        val startMonth = calendar.get(Calendar.MONTH)

        val currentCalendar = Calendar.getInstance()
        val currentYear = currentCalendar.get(Calendar.YEAR)
        val currentMonth = currentCalendar.get(Calendar.MONTH)

        val totalMonths = (currentYear - startYear) * 12 + (currentMonth - startMonth + 1)
        return Pair(totalMonths, totalMonths - 1)
    }

    companion object {
        fun newInstance(dataType: String): MonthGraphFragment {
            return MonthGraphFragment().apply {
                arguments = Bundle().apply {
                    putString("dataType", dataType)
                }
            }
        }
    }
}
