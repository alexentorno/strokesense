package com.alpekh.strokesense.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.alpekh.strokesense.R
import android.location.Location
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import com.alpekh.strokesense.model.TrainingSession
import com.alpekh.strokesense.viewmodel.TrainingViewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Locale


class TrainingActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private lateinit var textSpeed: TextView
    private lateinit var textAvgSpeed: TextView
    private lateinit var textMaxSpeed: TextView
    private lateinit var textStrokeRate: TextView
    private lateinit var textMaxStrokeRate: TextView
    private lateinit var textTilt: TextView
    private lateinit var textAvgTilt: TextView

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null

    private lateinit var speedChart: LineChart
    private lateinit var strokeRateChart: LineChart
    private lateinit var tiltChart: LineChart

    private var maxSpeed = 0f
    private var totalSpeed = 0f
    private var avgSpeed = 0f
    private var speedCount = 0

    private val speedHistory = mutableListOf<Float>()

    private var startTime: Long = 0L
    private var strokeCount = 0
    private var minValidStrokes = 3 // Минимальное количество гребков перед началом подсчёта

    private val strokeTimestamps = mutableListOf<Long>()
    private var maxSPM = 0f
    private var smoothedSPM = 0.0
    private val strokeWindow = 10000
    private val accelerationBuffer = ArrayDeque<Float>(5) // Буфер последних значений

    private var tiltAngle = 0f // Текущий угол наклона (Roll)
    private var avgTiltAngle = 0f // Средний угол наклона

    private var lastGyroTime = System.currentTimeMillis()

    private var totalTiltSum = 0f // Сумма всех измеренных углов
    private var tiltMeasurements = 0 // Количество измерений угла

    private val speedTimestamps = mutableListOf<Long>()
    private val strokeRateTimestamps = mutableListOf<Long>()
    private val tiltTimestamps = mutableListOf<Long>()

    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                when (it.sensor.type) {
                    Sensor.TYPE_GYROSCOPE -> processGyroscopeData(it.values)
                    Sensor.TYPE_ACCELEROMETER -> processAccelerometerData(it.values)
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training)

        speedChart = findViewById(R.id.speedChart)
        tiltChart = findViewById(R.id.tiltChart)
        strokeRateChart = findViewById(R.id.strokeRateChart)

        textSpeed = findViewById(R.id.textSpeed)
        textAvgSpeed = findViewById(R.id.textAvgSpeed)
        textMaxSpeed = findViewById(R.id.textMaxSpeed)
        textStrokeRate = findViewById(R.id.textStrokeRate)
        textMaxStrokeRate = findViewById(R.id.textMaxStrokeRate)
        textTilt = findViewById(R.id.textTilt)
        textAvgTilt = findViewById(R.id.textAvgTilt)

        listOf(strokeRateChart, speedChart, tiltChart).forEach { setupChart(it) }

        findViewById<Button>(R.id.btnStopTraining).setOnClickListener { stopTraining() }

        if (checkLocationPermission()) {
            startTraining()
            startTracking()
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }

    private fun setupChart(chart: LineChart) {
        chart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setPinchZoom(true)
            xAxis.setDrawGridLines(false)
            axisLeft.setDrawGridLines(false)
            axisRight.isEnabled = false
            legend.apply {
                isEnabled = true
                textSize = 12f
                formSize = 10f
            }
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val minutes = (value / 60).toInt()
                    val seconds = (value % 60).toInt()
                    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::fusedLocationClient.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let { sensorManager.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_UI) }
        gyroscope?.let { sensorManager.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(sensorEventListener)
    }

    private fun checkLocationPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            false
        }
    }

    private fun startTraining() {
        strokeCount = 0
        startTime = System.currentTimeMillis()
        totalTiltSum = 0f
        tiltMeasurements = 0
        avgTiltAngle = 0f
        strokeTimestamps.clear()
        maxSPM = 0f
    }

    private fun startTracking() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    updateSpeed(location)
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    private fun stopTraining() {
        val viewModel: TrainingViewModel by viewModels()

        val session = TrainingSession(
            startTime = System.currentTimeMillis(),
            maxSpeed = maxSpeed,
            avgSpeed = avgSpeed,
            maxSPM = maxSPM,
            avgTilt = avgTiltAngle,
            speedChart = extractChartData(speedChart),
            strokeRateChart = extractChartData(strokeRateChart),
            tiltChart = extractChartData(tiltChart),
            speedTimestamps = speedTimestamps,
            strokeRateTimestamps = strokeRateTimestamps,
            tiltTimestamps = tiltTimestamps
        )

        viewModel.saveTraining(session)
        finish() // Закрывает текущую активность и возвращает на предыдущую
    }

    private fun updateSpeed(location: Location) {
        val speedKmh = location.speed * 3.6f // м/с перевод к км/ч
        val currentTime = System.currentTimeMillis()

        // Фильтрация выбросов: игнорируем, если скорость изменилась слишком резко
        if (speedHistory.isNotEmpty()
            && kotlin.math.abs(speedKmh - speedHistory.last()) > 10) return

        speedTimestamps.add(currentTime)

        // Ограничиваем историю до 10 последних значений (скользящее среднее)
        if (speedHistory.size >= 10) speedHistory.removeAt(0)
        speedHistory.add(speedKmh)

        // Рассчитываем среднюю скорость
        totalSpeed += speedKmh
        speedCount++
        avgSpeed = totalSpeed / speedCount
        maxSpeed = maxSpeed.coerceAtLeast(speedKmh)

        // Обновляем UI
        findViewById<TextView>(R.id.textSpeed).text = getString(R.string.speed_text, speedKmh)
        findViewById<TextView>(R.id.textAvgSpeed).text = getString(R.string.avg_speed_text, avgSpeed)
        findViewById<TextView>(R.id.textMaxSpeed).text = getString(R.string.max_speed_text, maxSpeed)

        // Обновляем график скорости
        updateChart(speedChart, speedKmh)
    }

    private fun processAccelerometerData(values: FloatArray) {
        val forwardAcceleration = values[1]

        // Обновляем буфер
        if (accelerationBuffer.size >= 5) {
            accelerationBuffer.removeFirst()
        }
        accelerationBuffer.add(forwardAcceleration)

        // Усредняем
        val avgAcceleration = accelerationBuffer.average().toFloat()

        val threshold = 2f //TODO: Подобрать оптимальное значение
        val minInterval = 300 // 300 мс между гребками (200 ударов в минуту)

        val currentTime = System.currentTimeMillis()

        if (kotlin.math.abs(avgAcceleration) > threshold) {
            if (strokeTimestamps.isEmpty() || (currentTime - strokeTimestamps.last()) > minInterval) {
                strokeTimestamps.add(currentTime)
                strokeTimestamps.removeAll { it < currentTime - strokeWindow }

                strokeRateTimestamps.add(currentTime)

                if (strokeTimestamps.size >= minValidStrokes) {
                    val strokeRate = calculateSPM()
                    if (strokeRate > maxSPM) {
                        maxSPM = strokeRate
                    }
                    findViewById<TextView>(R.id.textStrokeRate).text = getString(R.string.stroke_rate_text, strokeRate)
                    findViewById<TextView>(R.id.textMaxStrokeRate).text = getString(R.string.max_stroke_rate_text, maxSPM)
                    updateChart(strokeRateChart, strokeRate)
                }
            }
        }
    }


    private fun calculateSPM(): Float {
        val strokesMade = strokeTimestamps.size
        val avgStrokeTime = if (strokesMade > 1) {
            (strokeTimestamps.last() - strokeTimestamps.first()) / (strokesMade - 1).toDouble()
        } else {
            strokeWindow.toDouble()
        }

        val strokeRate = (60_000 / avgStrokeTime) //удары/мин
        val smoothingFactor = 0.9 // Чем меньше, тем сильнее сглаживание

        // Сглаживание
        smoothedSPM = (strokeRate * smoothingFactor) + (smoothedSPM * (1 - smoothingFactor))

        return smoothedSPM.toFloat()
    }

    private fun processGyroscopeData(values: FloatArray) {
        val gyroX = values[1] // Вращение вокруг горизонтальной оси
        val currentTime = System.currentTimeMillis()
        val deltaTime = (currentTime - lastGyroTime) / 1000f // Время в секундах
        lastGyroTime = currentTime

        // Интегрируем угловую скорость, чтобы получить угол
        tiltAngle += gyroX * deltaTime * 57.3f // Перевод радиан в градусы

        // Фильтруем мелкие колебания
        tiltAngle = (tiltAngle * 0.97f) + (gyroX * deltaTime * 0.1f) //TODO Подобрать оптимальное значение

        // Обновляем средний угол
        totalTiltSum += tiltAngle
        tiltMeasurements++
        avgTiltAngle = totalTiltSum / tiltMeasurements

        tiltTimestamps.add(currentTime)

        // Обновляем UI
        findViewById<TextView>(R.id.textTilt).text = getString(R.string.tilt_text, tiltAngle)
        findViewById<TextView>(R.id.textAvgTilt).text = getString(R.string.avg_tilt_text, avgTiltAngle)

        updateChart(tiltChart, tiltAngle) // Обновляем график
    }

    private fun updateChart(chart: LineChart, value: Float) {
        val elapsedTime = (System.currentTimeMillis() - startTime) / 1000f

        val dataSet = chart.data?.getDataSetByIndex(0) as? LineDataSet ?: LineDataSet(null, "Data").apply {
            color = Color.BLUE
            valueTextColor = Color.BLACK
            chart.data = LineData(this)
        }

        dataSet.addEntry(Entry(elapsedTime, value))
        chart.data.notifyDataChanged()
        chart.notifyDataSetChanged()
        chart.invalidate()
    }

    private fun extractChartData(chart: LineChart): List<Float> {
        val dataSet = chart.data?.getDataSetByIndex(0) as? LineDataSet
        return dataSet?.values?.map { it.y } ?: emptyList()
    }
}
