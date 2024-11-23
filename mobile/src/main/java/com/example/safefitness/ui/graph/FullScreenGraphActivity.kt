package com.example.safefitness.ui.graph

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.safefitness.R
import com.google.android.material.tabs.TabLayout

class FullScreenGraphActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private var dataType: String = "steps"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen_graph)

        dataType = intent.getStringExtra("dataType") ?: "steps"

        tabLayout = findViewById(R.id.tabLayout)
        tabLayout.addTab(tabLayout.newTab().setText("Day"))
        tabLayout.addTab(tabLayout.newTab().setText("Week"))
        tabLayout.addTab(tabLayout.newTab().setText("Month"))

        replaceFragment(DayGraphFragment.newInstance(dataType))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> replaceFragment(DayGraphFragment.newInstance(dataType))
                    1 -> replaceFragment(WeekGraphFragment.newInstance(dataType))
                    2 -> replaceFragment(MonthGraphFragment.newInstance(dataType))
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.graphContainer, fragment)
            .commit()
    }

    companion object {
        fun start(activity: AppCompatActivity, dataType: String) {
            val intent = Intent(activity, FullScreenGraphActivity::class.java).apply {
                putExtra("dataType", dataType)
            }
            activity.startActivity(intent)
        }
    }
}
