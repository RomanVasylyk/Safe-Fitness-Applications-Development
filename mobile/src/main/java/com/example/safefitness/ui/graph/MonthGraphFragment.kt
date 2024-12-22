package com.example.safefitness.ui.graph

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.safefitness.R
import com.example.safefitness.data.FitnessDatabase
import com.example.safefitness.data.FitnessRepository
import com.example.safefitness.ui.adapters.UniversalGraphPagerAdapter
import com.example.safefitness.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class MonthGraphFragment : Fragment() {

    private lateinit var viewPager: ViewPager2
    private var dataType: String = "steps"
    private lateinit var repository: FitnessRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_month_graph, container, false)

        viewPager = view.findViewById(R.id.monthGraphPager)

        val database = FitnessDatabase.getDatabase(requireContext())
        repository = FitnessRepository(database.fitnessDao())

        dataType = arguments?.getString("dataType") ?: "steps"

        viewLifecycleOwner.lifecycleScope.launch {
            val (totalMonths, currentMonthPosition) = getTotalMonthsCount()

            val adapter = UniversalGraphPagerAdapter(
                fragment = this@MonthGraphFragment,
                totalItems = totalMonths,
                dateRangeProvider = { position ->
                    DateUtils.getMonthDate(position, totalMonths)
                },
                fragmentProvider = { startDate, endDate ->
                    SingleMonthGraphFragment.newInstance(startDate, endDate, dataType)
                }
            )
            viewPager.adapter = adapter
            viewPager.setCurrentItem(currentMonthPosition, false)
        }

        return view
    }

    private suspend fun getTotalMonthsCount(): Pair<Int, Int> {
        return withContext(Dispatchers.IO) {
            val firstEntryDateString = repository.getFirstEntryDate() ?: return@withContext Pair(1, 0)
            val firstEntryDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(firstEntryDateString) ?: return@withContext Pair(1, 0)

            val calendar = Calendar.getInstance()
            calendar.time = firstEntryDate

            val startYear = calendar.get(Calendar.YEAR)
            val startMonth = calendar.get(Calendar.MONTH)

            val currentCalendar = Calendar.getInstance()
            val currentYear = currentCalendar.get(Calendar.YEAR)
            val currentMonth = currentCalendar.get(Calendar.MONTH)

            val totalMonths = (currentYear - startYear) * 12 + (currentMonth - startMonth + 1)
            Pair(totalMonths, totalMonths - 1)
        }
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
