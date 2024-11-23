package com.example.safefitness.ui.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.safefitness.data.FitnessDatabase
import com.example.safefitness.ui.graph.SingleMonthGraphFragment
import java.text.SimpleDateFormat
import java.util.*

class MonthGraphPagerAdapter(
    fragment: Fragment,
    private val fitnessDatabase: FitnessDatabase,
    private val totalMonths: Int,
    private val dataType: String
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = totalMonths

    override fun createFragment(position: Int): Fragment {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.add(Calendar.MONTH, -(totalMonths - 1 - position))

        val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        return SingleMonthGraphFragment.newInstance(startDate, endDate, dataType)
    }
}
