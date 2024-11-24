package com.example.safefitness.ui.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.safefitness.data.FitnessDatabase
import com.example.safefitness.ui.graph.SingleYearGraphFragment
import java.util.Calendar

class YearGraphPagerAdapter(
    fragment: Fragment,
    private val fitnessDatabase: FitnessDatabase,
    private val totalYears: Int,
    private val dataType: String
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = totalYears

    override fun createFragment(position: Int): Fragment {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val year = currentYear - (totalYears - 1 - position)
        return SingleYearGraphFragment.newInstance(year, dataType)
    }
}

