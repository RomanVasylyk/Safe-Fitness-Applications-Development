package com.example.safefitness

import android.content.Context
import lecho.lib.hellocharts.gesture.ContainerScrollType
import lecho.lib.hellocharts.gesture.ZoomType
import lecho.lib.hellocharts.model.*
import lecho.lib.hellocharts.view.LineChartView

class GraphManager {

    fun updateGraph(
        graph: LineChartView,
        data: List<Pair<String, Number>>,
        title: String,
        xAxisName: String,
        yAxisName: String,
        context: Context
    ) {
        if (data.isEmpty()) return

        val timeLabels = data.mapIndexed { index, pair ->
            val time = pair.first.split(" ")[1]
            AxisValue(index.toFloat()).setLabel(time)
        }

        val points = data.mapIndexed { index, pair ->
            PointValue(index.toFloat(), pair.second.toFloat())
        }

        val line = Line(points).apply {
            color = context.getColor(android.R.color.holo_blue_dark)
            isCubic = true
            setHasLabels(true)
            setHasLines(true)
            setHasPoints(true)
            pointRadius = 4
        }

        val chartData = LineChartData(listOf(line)).apply {
            axisXBottom = Axis(timeLabels).apply {
                name = xAxisName
                textColor = context.getColor(android.R.color.black)
                textSize = 12
                setHasLines(true)
            }
            axisYLeft = Axis().apply {
                name = yAxisName
                textColor = context.getColor(android.R.color.black)
                textSize = 12
                setHasLines(true)
            }
        }

        graph.lineChartData = chartData

        val minY = points.minOf { it.y }
        val maxY = points.maxOf { it.y }

        val viewport = if (points.size == 1) {
            Viewport().apply {
                top = maxY + 10f
                bottom = minY - 10f
                left = -0.5f
                right = 1.5f
            }
        } else {
            Viewport().apply {
                top = maxY + (maxY - minY).coerceAtLeast(1f) * 0.1f
                bottom = minY - (maxY - minY).coerceAtLeast(1f) * 0.1f
                left = -0.5f
                right = points.size.toFloat() - 1 + 0.5f
            }
        }

        graph.apply {
            maximumViewport = viewport
            currentViewport = viewport
            isInteractive = true
            zoomType = ZoomType.HORIZONTAL_AND_VERTICAL
            setContainerScrollEnabled(true, ContainerScrollType.HORIZONTAL)
        }
    }
}
