package com.example.safefitness.utils.chart

import android.content.Context
import com.example.safefitness.utils.aggregation.DayPulseData
import lecho.lib.hellocharts.model.*
import lecho.lib.hellocharts.view.ColumnChartView
import lecho.lib.hellocharts.gesture.ZoomType
import java.text.SimpleDateFormat
import java.util.Locale

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
        val sdfParse = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        data.forEachIndexed { index, item ->
            when {
                dataType == "steps" && item is Pair<*, *> -> {
                    val originalLabel = item.first as? String ?: ""
                    val value = (item.second as? Number)?.toFloat() ?: 0f
                    if (value > maxYValue) maxYValue = value
                    val label = try {
                        sdfFormat.format(sdfParse.parse(originalLabel) ?: originalLabel)
                    } catch (e: Exception) {
                        originalLabel
                    }
                    axisValues.add(AxisValue(index.toFloat()).setLabel(label))
                    columns.add(Column(listOf(SubcolumnValue(value, context.getColor(android.R.color.holo_blue_dark)))).apply { setHasLabels(true) })
                }
                dataType != "steps" && item is DayPulseData -> {
                    val min = item.minPulse
                    val max = item.maxPulse
                    if (max > maxYValue) maxYValue = max
                    val label = try {
                        sdfFormat.format(sdfParse.parse(item.label) ?: item.label)
                    } catch (e: Exception) {
                        item.label
                    }
                    axisValues.add(AxisValue(index.toFloat()).setLabel(label))
                    columns.add(Column(listOf(
                        SubcolumnValue(min, context.getColor(android.R.color.holo_blue_dark)),
                        SubcolumnValue(max, context.getColor(android.R.color.holo_red_light))
                    )).apply { setHasLabels(true) })
                }
            }
        }

        val columnChartData = ColumnChartData(columns).apply {
            fillRatio = 0.5f
            axisXBottom = Axis(axisValues).apply {
                name = xAxisName
                setHasTiltedLabels(true)
                textSize = 12
            }
            axisYLeft = Axis().apply {
                name = yAxisName
                textSize = 12
            }
        }
        chartView.columnChartData = columnChartData
        chartView.isInteractive = true
        chartView.isScrollEnabled = true
        chartView.setZoomEnabled(true)
        chartView.setZoomType(ZoomType.HORIZONTAL_AND_VERTICAL)
        val viewport = Viewport(chartView.maximumViewport)
        viewport.top = maxYValue * 1.1f
        chartView.maximumViewport = viewport
        chartView.currentViewport = viewport
    }
}
