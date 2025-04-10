package com.example.safefitness.ui.graph

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.safefitness.R
import com.example.safefitness.data.local.FitnessDatabase
import com.example.safefitness.data.repository.FitnessRepository
import com.example.safefitness.ui.adapters.UniversalGraphPagerAdapter
import com.example.safefitness.utils.date.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class YearGraphFragment : Fragment() {

    private lateinit var viewPager: ViewPager2
    private lateinit var database: FitnessDatabase
    private var dataType: String = "steps"
    private lateinit var repository: FitnessRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_year_graph, container, false)
        viewPager = view.findViewById(R.id.yearGraphPager)

        database = FitnessDatabase.getDatabase(requireContext())
        repository = FitnessRepository(database.fitnessDao())

        dataType = arguments?.getString("dataType") ?: "steps"

        viewLifecycleOwner.lifecycleScope.launch {
            setupAdapter()
        }

        return view
    }

    private suspend fun setupAdapter() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val earliestYear = withContext(Dispatchers.IO) {
            repository.getFirstEntryDate()?.let { getYearFromDate(it) } ?: currentYear
        }

        val totalYears = (currentYear - earliestYear + 1).coerceAtLeast(1)

        val adapter = UniversalGraphPagerAdapter(
            fragment = this,
            totalItems = totalYears,
            dateRangeProvider = { position ->
                DateUtils.getYearDate(position, totalYears)
            },
            fragmentProvider = { startDate, _ ->
                SingleYearGraphFragment.newInstance(startDate.substring(0, 4).toInt(), dataType)
            }
        )

        withContext(Dispatchers.Main) {
            viewPager.adapter = adapter
            viewPager.setCurrentItem(totalYears - 1, false)
        }
    }

    private fun getYearFromDate(date: String): Int {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.time = format.parse(date)!!
        return calendar.get(Calendar.YEAR)
    }

    companion object {
        fun newInstance(dataType: String): YearGraphFragment {
            return YearGraphFragment().apply {
                arguments = Bundle().apply {
                    putString("dataType", dataType)
                }
            }
        }
    }
}
