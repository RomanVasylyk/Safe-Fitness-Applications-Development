package com.example.safefitness.utils

import android.content.Context
import lecho.lib.hellocharts.formatter.SimpleAxisValueFormatter
import lecho.lib.hellocharts.gesture.ContainerScrollType
import lecho.lib.hellocharts.gesture.ZoomType
import lecho.lib.hellocharts.listener.LineChartOnValueSelectListener
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

        val axisValues = mutableListOf<AxisValue>()
        val points = data.mapIndexed { index, pair ->
            val parts = pair.first.split(" ")
            val label = if (parts.size > 1) parts[1] else parts[0]
            axisValues.add(AxisValue(index.toFloat()).setLabel(label))
            PointValue(index.toFloat(), pair.second.toFloat())
        }

        val line = Line(points).apply {
            color = context.getColor(android.R.color.holo_blue_dark)
            isCubic = true
            setHasLabels(false)
            setHasLabelsOnlyForSelected(true)
            setHasLines(true)
            setHasPoints(true)
            pointRadius = 4
        }

        val chartData = LineChartData(listOf(line)).apply {
            axisXBottom = Axis(axisValues).apply {
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
                formatter = SimpleAxisValueFormatter().setDecimalDigitsNumber(0)
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
                right = points.size - 1 + 0.5f
            }
        }
        graph.maximumViewport = viewport
        graph.currentViewport = viewport
        graph.isInteractive = true
        graph.zoomType = ZoomType.HORIZONTAL_AND_VERTICAL
        graph.isZoomEnabled = true
        graph.setContainerScrollEnabled(true, ContainerScrollType.HORIZONTAL)
        graph.setOnValueTouchListener(object : LineChartOnValueSelectListener {
            override fun onValueSelected(lineIndex: Int, pointIndex: Int, value: PointValue) {}
            override fun onValueDeselected() {}
        })
    }

    private inner class ValueTouchListener : LineChartOnValueSelectListener {
        override fun onValueSelected(lineIndex: Int, pointIndex: Int, value: PointValue) {
        }

        override fun onValueDeselected() {
        }
    }
}