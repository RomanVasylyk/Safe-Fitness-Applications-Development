package com.example.safefitness.utils.aggregation

data class AggregationResult(
    val aggregatedData: List<Any>,
    val xLabels: List<String>,
    val summaryText: String,
    val dateRange: String
)
