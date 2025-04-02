package com.alpekh.strokesense.helpers

import android.graphics.Color
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

class ChartManager(private val chart: LineChart) {
    private val allEntries = mutableListOf<Entry>()
    private val displayEntries = mutableListOf<Entry>()
    private var dataSet: LineDataSet? = null

    init {
        initializeChart()
        initializeDataSet()
    }

    private fun initializeChart() {
        chart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setPinchZoom(true)
            xAxis.setDrawGridLines(false)
            axisLeft.setDrawGridLines(false)
            axisRight.isEnabled = false
            legend.isEnabled = true
            setNoDataText("Loading data...")
            setNoDataTextColor(Color.GRAY)
        }
    }

    private fun initializeDataSet() {
        dataSet = LineDataSet(null, "Data").apply {
            color = Color.BLUE
            lineWidth = 2f
            setDrawCircles(true)
            circleRadius = 2.5f
            circleHoleRadius = 2f
            setCircleColor(Color.BLUE)
            circleHoleColor = Color.WHITE
            setDrawValues(false)
            mode = LineDataSet.Mode.LINEAR
        }
        chart.data = LineData(dataSet)
    }

    fun updateChart(value: Float, elapsedTimeMs: Long) {
        try {
            val elapsedSeconds = elapsedTimeMs / 1000f
            val newEntry = Entry(displayEntries.size.toFloat(), value) // Use index as x-value

            allEntries.add(newEntry)

            // Manage display entries
            displayEntries.add(newEntry)
            if (displayEntries.size > 200) {
                displayEntries.removeAt(0)
                // Reindex remaining entries
                displayEntries.forEachIndexed { index, entry ->
                    entry.x = index.toFloat()
                }
            }

            // Safely update dataset
            dataSet?.let { ds ->
                ds.values = ArrayList(displayEntries)

                // Calculate visible range
                val visibleRange = 200f
                val maxX = ds.entryCount.toFloat()
                val minX = maxOf(0f, maxX - visibleRange)

                // Move view to show latest data
                chart.moveViewToX(maxX)
                chart.setVisibleXRange(minX, maxX)

                // Refresh chart
                chart.data?.notifyDataChanged()
                chart.notifyDataSetChanged()
                chart.invalidate()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Reset chart on error
            resetChart()
        }
    }

    private fun resetChart() {
        displayEntries.clear()
        dataSet?.clear()
        initializeDataSet()
    }

    fun getAllEntries(): List<Entry> = allEntries.toList()
}