package com.example.safefitness.ui.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class UniversalGraphPagerAdapter(
    fragment: Fragment,
    private val totalItems: Int,
    private val dateRangeProvider: (Int) -> Pair<String, String>,
    private val fragmentProvider: (String, String) -> Fragment
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = totalItems

    override fun createFragment(position: Int): Fragment {
        val (startDate, endDate) = dateRangeProvider(position)
        return fragmentProvider(startDate, endDate)
    }
}
