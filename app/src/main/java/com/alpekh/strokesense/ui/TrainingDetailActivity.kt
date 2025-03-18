package com.alpekh.strokesense.ui

import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.alpekh.strokesense.R
import com.alpekh.strokesense.viewmodel.TrainingViewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.util.Locale

class TrainingDetailActivity : AppCompatActivity() {

    private val viewModel: TrainingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training_detail)

        val sessionId = intent.getIntExtra("sessionId", -1)
        val speedChart = findViewById<LineChart>(R.id.speedChart)
        val accelerationChart = findViewById<LineChart>(R.id.accelerationChart)
        val tiltChart = findViewById<LineChart>(R.id.tiltChart)

        // Настройка графиков
        setupChart(speedChart)
        setupChart(accelerationChart)
        setupChart(tiltChart)

        viewModel.getTrainings { sessions ->
            val session = sessions.find { it.id == sessionId }
            session?.let {
                findViewById<TextView>(R.id.textViewMaxSpeed).text = "Max Speed: ${it.maxSpeed} km/h"
                findViewById<TextView>(R.id.textViewAvgSpeed).text = "Avg Speed: ${it.avgSpeed} km/h"
                findViewById<TextView>(R.id.textViewSPM).text = "Max Stroke Rate: ${it.maxSPM}"
                findViewById<TextView>(R.id.textViewTilt).text = "Avg Tilt Angle: ${it.avgTilt}°"

                // Заполнение графиков данными
                setupChartData(speedChart, it.speedChart, "Speed (km/h)")
                setupChartData(accelerationChart, it.SPMChart, "Stroke Rate (strokes/min)")
                setupChartData(tiltChart, it.tiltChart, "Tilt Angle (°)")
            }
        }
    }

    private fun setupChart(chart: LineChart) {
        chart.description.isEnabled = false
        chart.setTouchEnabled(true)
        chart.setPinchZoom(true)
        chart.xAxis.setDrawGridLines(false)
        chart.axisLeft.setDrawGridLines(false)
        chart.axisRight.isEnabled = false
        chart.legend.isEnabled = true
        chart.legend.textSize = 12f
        chart.legend.formSize = 10f

        chart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val seconds = value.toLong()
                val minutes = seconds / 60
                val remainingSeconds = seconds % 60
                return String.format(Locale.US, "%02d:%02d", minutes, remainingSeconds)
            }
        }
    }

    private fun setupChartData(chart: LineChart, data: List<Float>, label: String) {
        val entries = data.mapIndexed { index, value -> Entry(index.toFloat(), value) }
        val dataSet = LineDataSet(entries, label).apply {
            color = getColor(R.color.light_blue)
            valueTextColor = getColor(R.color.light_blue)
            lineWidth = 2f
            setDrawCircles(false)
        }
        chart.data = LineData(dataSet)
        chart.invalidate()
    }
}