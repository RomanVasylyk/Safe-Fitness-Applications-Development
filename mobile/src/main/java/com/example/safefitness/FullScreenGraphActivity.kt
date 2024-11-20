package com.example.safefitness

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.example.safefitness.data.FitnessDatabase

class FullScreenGraphActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager
    private lateinit var graphPagerAdapter: GraphPagerAdapter
    private lateinit var dataType: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen_graph)

        val database = FitnessDatabase.getDatabase(this)
        val fitnessDao = database.fitnessDao()
        val graphManager = GraphManager()

        dataType = intent.getStringExtra("dataType") ?: "steps"

        viewPager = findViewById(R.id.graphViewPager)
        graphPagerAdapter = GraphPagerAdapter(this, fitnessDao, graphManager, dataType)
        viewPager.adapter = graphPagerAdapter

        viewPager.currentItem = graphPagerAdapter.count - 1
    }
}
