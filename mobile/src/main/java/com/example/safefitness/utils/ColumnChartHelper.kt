package com.example.safefitness.utils

import android.content.Context
import lecho.lib.hellocharts.model.Axis
import lecho.lib.hellocharts.model.AxisValue
import lecho.lib.hellocharts.model.Column
import lecho.lib.hellocharts.model.ColumnChartData
import lecho.lib.hellocharts.model.SubcolumnValue
import lecho.lib.hellocharts.model.Viewport
import lecho.lib.hellocharts.view.ColumnChartView

object ColumnChartHelper {

    fun buildColumnChart(
        chartView: ColumnChartView,
        data: List<Any>,
        dataType: String,
        xAxisName: String,
        yAxisName: String,
        context: Context
    ) {
        var maxYValue = 0f
        val columns = mutableListOf<Column>()
        val axisValues = mutableListOf<AxisValue>()

        if (dataType == "steps") {
            data.forEachIndexed { index, item ->
                if (item is Pair<*, *>) {
                    val label = item.first as? String ?: ""
                    val value = (item.second as? Number)?.toFloat() ?: 0f
                    if (value > maxYValue) maxYValue = value
                    axisValues.add(AxisValue(index.toFloat()).setLabel(label))
                    val subcolumn = SubcolumnValue(value, context.getColor(android.R.color.holo_blue_dark))
                    columns.add(Column(listOf(subcolumn)).apply { setHasLabels(true) })
                }
            }
        } else {
            data.forEachIndexed { index, item ->
                if (item is DayPulseData) {
                    val min = item.minPulse
                    val max = item.maxPulse
                    val label = item.label
                    if (max > maxYValue) maxYValue = max
                    axisValues.add(AxisValue(index.toFloat()).setLabel(label))
                    val minPulseValue = SubcolumnValue(min, context.getColor(android.R.color.holo_blue_dark))
                    val maxPulseValue = SubcolumnValue(max, context.getColor(android.R.color.holo_red_light))
                    columns.add(Column(listOf(minPulseValue, maxPulseValue)).apply { setHasLabels(true) })
                }
            }
        }

        val columnChartData = ColumnChartData(columns).apply {
            axisXBottom = Axis(axisValues).setName(xAxisName)
            axisYLeft = Axis().setName(yAxisName)
        }

        chartView.columnChartData = columnChartData

        val viewport = Viewport(chartView.maximumViewport)
        viewport.top = maxYValue * 1.1f
        chartView.maximumViewport = viewport
        chartView.currentViewport = viewport
    }
}
