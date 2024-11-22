package com.example.safefitness.ui.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.safefitness.data.FitnessDao
import com.example.safefitness.ui.graph.SingleDayGraphFragment
import java.text.SimpleDateFormat
import java.util.*

class DayGraphPagerAdapter(
    fragment: Fragment,
    private val fitnessDao: FitnessDao,
    private val availableDaysCount: Int,
    private val dataType: String
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = availableDaysCount

    override fun createFragment(position: Int): Fragment {
        return SingleDayGraphFragment.newInstance(getDateForPosition(position), dataType)
    }

    private fun getDateForPosition(position: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -(availableDaysCount - 1 - position))
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    }
}

