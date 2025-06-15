package com.alpekh.strokesense.ui

import android.os.Bundle
import android.util.TypedValue
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

    private val chartLineColor by lazy {
        val typedValue = TypedValue()
        theme.resolveAttribute(R.attr.chartLineColor, typedValue, true)
        typedValue.data
    }

    private val chartTextColor by lazy {
        val typedValue = TypedValue()
        theme.resolveAttribute(R.attr.chartTextColor, typedValue, true)
        typedValue.data
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training_detail)

        val sessionId = intent.getIntExtra("sessionId", -1)
        val trainingDurationTextView = findViewById<TextView>(R.id.textViewDuration)
        val speedChart = findViewById<LineChart>(R.id.speedChart)
        val accelerationChart = findViewById<LineChart>(R.id.accelerationChart)
        val tiltChart = findViewById<LineChart>(R.id.tiltChart)

        // Настройка графиков
        setupChart(speedChart)
        setupChart(accelerationChart)
        setupChart(tiltChart)

        viewModel.getTrainings { sessions ->
            val session = sessions.find { it.id == sessionId }
            session?.let { sessionEntity ->
                viewModel.getTrainingDetails(sessionId) { details ->
                    if (details != null) {
                        val seconds = (sessionEntity.endTime / 1000) % 60
                        val minutes = (sessionEntity.endTime / (1000 * 60)) % 60
                        val hours = (sessionEntity.endTime / (1000 * 60 * 60))
                        val formattedDuration =
                            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)

                        trainingDurationTextView.text =
                            getString(R.string.training_duration, formattedDuration)

                        findViewById<TextView>(R.id.textViewDistance).text =
                            getString(R.string.distance_text, sessionEntity.distance / 1000)

                        findViewById<TextView>(R.id.textViewMaxSpeed).text =
                            getString(R.string.max_speed_text, sessionEntity.maxSpeed)
                        findViewById<TextView>(R.id.textViewAvgSpeed).text =
                            getString(R.string.avg_speed_text, sessionEntity.avgSpeed)
                        findViewById<TextView>(R.id.textViewSPM).text =
                            getString(R.string.max_stroke_rate_text, sessionEntity.maxSPM)
                        findViewById<TextView>(R.id.textViewTilt).text =
                            getString(R.string.avg_tilt_text, sessionEntity.avgTilt)

                        // Отрисовываем графики
                        setupChartData(
                            speedChart,
                            details.speedTimestamps,
                            details.speedChart,
                            "Speed (km/h)"
                        )
                        setupChartData(
                            accelerationChart,
                            details.strokeRateTimestamps,
                            details.strokeRateChart,
                            "Stroke Rate (strokes/min)"
                        )
                        setupChartData(
                            tiltChart,
                            details.tiltTimestamps,
                            details.tiltChart,
                            "Tilt Angle (°)"
                        )
                    }
                }
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

    private fun setupChartData(chart: LineChart, timestamps: List<Long>, values: List<Float>, label: String) {
        if (timestamps.isEmpty() || values.isEmpty()) return

        val dataSize = minOf(timestamps.size, values.size)
        val startTime = timestamps.first()

        val entries = (0 until dataSize).map { index ->
            val elapsedTime = (timestamps[index] - startTime) / 1000f
            Entry(elapsedTime, values[index])
        }

        val dataSet = LineDataSet(entries, label).apply {
            color = chartLineColor
            valueTextColor = chartTextColor
            lineWidth = 2f
            setDrawCircles(false)
            mode = LineDataSet.Mode.LINEAR

            setCircleColor(chartLineColor)
        }

        chart.data = LineData(dataSet).apply {
            setValueTextColor(chartTextColor)
        }

        // Configure axis colors
        chart.xAxis.textColor = chartTextColor
        chart.axisLeft.textColor = chartTextColor
        chart.legend.textColor = chartTextColor

        chart.invalidate()
    }

}