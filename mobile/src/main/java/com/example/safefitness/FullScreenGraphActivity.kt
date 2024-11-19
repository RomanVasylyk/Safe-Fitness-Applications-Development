package com.example.safefitness

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import lecho.lib.hellocharts.view.LineChartView

class FullScreenGraphActivity : AppCompatActivity() {

    private lateinit var fullScreenGraph: LineChartView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen_graph)

        fullScreenGraph = findViewById(R.id.fullScreenGraph)

        val graphData = intent.getSerializableExtra("graphData") as? List<Pair<String, Number>>
        val title = intent.getStringExtra("title")
        val xAxisName = intent.getStringExtra("xAxisName")
        val yAxisName = intent.getStringExtra("yAxisName")

        if (graphData != null && title != null && xAxisName != null && yAxisName != null) {
            val graphManager = GraphManager()
            graphManager.updateGraph(
                fullScreenGraph,
                graphData,
                title,
                xAxisName,
                yAxisName,
                this
            )
        }
    }
}
