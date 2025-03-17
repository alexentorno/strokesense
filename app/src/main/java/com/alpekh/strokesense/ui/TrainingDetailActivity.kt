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

class TrainingDetailActivity : AppCompatActivity() {

    private val viewModel: TrainingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training_detail)

        val sessionId = intent.getIntExtra("sessionId", -1)
        val speedChart = findViewById<LineChart>(R.id.speedChart)
        val accelerationChart = findViewById<LineChart>(R.id.accelerationChart)
        val tiltChart = findViewById<LineChart>(R.id.tiltChart)

        viewModel.getTrainings { sessions ->
            val session = sessions.find { it.id == sessionId }
            session?.let {
                findViewById<TextView>(R.id.textViewSpeed).text = "Max Speed: ${it.maxSpeed} km/h"
                findViewById<TextView>(R.id.textViewSPM).text = "Max Stroke Rate: ${it.maxSPM}"
                findViewById<TextView>(R.id.textViewTilt).text = "Avg Tilt Angle: ${it.avgTilt}°"

                setupChart(speedChart, it.speedGraph, "Speed (km/h)")
                setupChart(accelerationChart, it.accelerationGraph, "Acceleration (m/s²)")
                setupChart(tiltChart, it.tiltGraph, "Tilt Angle (°)")
            }
        }
    }

    private fun setupChart(chart: LineChart, data: List<Float>, label: String) {
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
