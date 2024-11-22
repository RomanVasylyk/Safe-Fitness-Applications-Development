package com.example.safefitness.ui.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.safefitness.data.FitnessDatabase
import com.example.safefitness.ui.graph.SingleWeekGraphFragment
import java.text.SimpleDateFormat
import java.util.*

class WeekGraphPagerAdapter(
    fragment: Fragment,
    private val fitnessDatabase: FitnessDatabase,
    private val totalWeeks: Int,
    private val dataType: String
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = totalWeeks

    override fun createFragment(position: Int): Fragment {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY

        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.add(Calendar.WEEK_OF_YEAR, -(totalWeeks - 1 - position))

        val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        calendar.add(Calendar.DAY_OF_YEAR, 6)
        val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        return SingleWeekGraphFragment.newInstance(startDate, endDate, dataType)
    }
}
