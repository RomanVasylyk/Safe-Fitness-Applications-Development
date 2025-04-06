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
import com.example.safefitness.utils.date.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class WeekGraphFragment : Fragment() {

    private lateinit var viewPager: ViewPager2
    private var dataType: String = "steps"
    private lateinit var repository: FitnessRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_week_graph, container, false)

        viewPager = view.findViewById(R.id.weekGraphPager)

        val database = FitnessDatabase.getDatabase(requireContext())
        repository = FitnessRepository(database.fitnessDao())

        dataType = arguments?.getString("dataType") ?: "steps"

        viewLifecycleOwner.lifecycleScope.launch {
            val (totalWeeks, currentWeekPosition) = withContext(Dispatchers.IO) {
                getTotalWeeksCount()
            }

            val adapter = UniversalGraphPagerAdapter(
                fragment = this@WeekGraphFragment,
                totalItems = totalWeeks,
                dateRangeProvider = { position ->
                    DateUtils.getWeekDate(position, totalWeeks)
                },
                fragmentProvider = { startDate, endDate ->
                    SingleWeekGraphFragment.newInstance(startDate, endDate, dataType)
                }
            )
            viewPager.adapter = adapter
            viewPager.setCurrentItem(currentWeekPosition, false)
        }

        return view
    }

    private suspend fun getTotalWeeksCount(): Pair<Int, Int> {
        val firstEntryDateString = repository.getFirstEntryDate() ?: return Pair(1, 0)
        val firstEntryDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(firstEntryDateString) ?: return Pair(1, 0)

        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val currentMonday = calendar.time

        val diffInMillis = currentMonday.time - firstEntryDate.time
        val totalWeeks = (diffInMillis / (1000 * 60 * 60 * 24 * 7)).toInt() + 1

        return Pair(totalWeeks, totalWeeks - 1)
    }

    companion object {
        fun newInstance(dataType: String): WeekGraphFragment {
            return WeekGraphFragment().apply {
                arguments = Bundle().apply {
                    putString("dataType", dataType)
                }
            }
        }
    }
}
