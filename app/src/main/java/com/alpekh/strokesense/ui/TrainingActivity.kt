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
import android.os.DropBoxManager
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlin.math.atan2


class TrainingActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null

    private lateinit var speedChart: LineChart
    private lateinit var accelerationChart: LineChart
    private lateinit var strokeRateChart: LineChart

    private lateinit var tiltChart: LineChart

    private var maxSpeed = 0f

    private val speedHistory = mutableListOf<Float>() // История скоростей

    private var startTime: Long = 0L
    private var strokeCount = 0
    private var minValidStrokes = 3 // Минимальное количество гребков перед началом подсчёта SPM

    private val strokeTimestamps = mutableListOf<Long>() // История ударов
    private var maxSPM = 0 // Максимальный SPM
    private var smoothedSPM = 0.0
    private val smoothingFactor = 0.9 // Чем меньше, тем сильнее сглаживание
    private val strokeWindow = 10000 // 10 секунд в миллисекундах
    private val accelerationBuffer = ArrayDeque<Float>(5) // Буфер последних значений

    private var tiltAngle = 0f // Текущий угол наклона (Roll)
    private var lastGyroTime = System.currentTimeMillis()

    private var totalTiltSum = 0f // Сумма всех измеренных углов
    private var tiltMeasurements = 0 // Количество измерений угла


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
        accelerationChart = findViewById(R.id.accelerationChart)
        tiltChart = findViewById(R.id.tiltChart)
        strokeRateChart = findViewById(R.id.strokeRateChart)

        setupChart(strokeRateChart)
        setupChart(speedChart)
        setupChart(accelerationChart)
        setupChart(tiltChart)

        findViewById<Button>(R.id.btnStopTraining).setOnClickListener {
            stopTraining()
        }

        if (checkLocationPermission()) {
            startTraining()
            startTracking()
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

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
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
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

        // Обнуляем средний наклон
        totalTiltSum = 0f
        tiltMeasurements = 0

        strokeTimestamps.clear()
        maxSPM = 0
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
        finish() // Закрывает текущую активность и возвращает на предыдущую
    }


    private fun updateSpeed(location: Location) {
        val speedKmh = location.speed * 3.6f // м/с → км/ч

        // Фильтрация выбросов: игнорируем, если скорость изменилась слишком резко
        if (speedHistory.isNotEmpty() && kotlin.math.abs(speedKmh - speedHistory.last()) > 10) {
            return
        }

        // Ограничиваем историю до 5 последних значений (скользящее среднее)
        if (speedHistory.size >= 5) {
            speedHistory.removeAt(0)
        }
        speedHistory.add(speedKmh)

        // Рассчитываем среднюю скорость
        val avgSpeed = speedHistory.average().toFloat()

        // Обновляем максимальную скорость
        if (avgSpeed > maxSpeed) {
            maxSpeed = avgSpeed
        }

        // Обновляем UI
        findViewById<TextView>(R.id.textSpeed).text = "Speed: %.1f km/h".format(avgSpeed)
        findViewById<TextView>(R.id.textMaxSpeed).text = "Max Speed: %.1f km/h".format(maxSpeed)

        // Обновляем график скорости
        updateChart(speedChart, avgSpeed)
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

        val threshold = 3f
        val minInterval = 300 // 300 мс между гребками (200 ударов в минуту)

        val currentTime = System.currentTimeMillis()

        if (kotlin.math.abs(avgAcceleration) > threshold) {
            if (strokeTimestamps.isEmpty() || (currentTime - strokeTimestamps.last()) > minInterval) {
                strokeTimestamps.add(currentTime)
                strokeTimestamps.removeAll { it < currentTime - strokeWindow }

                if (strokeTimestamps.size >= minValidStrokes) {
                    val strokeRate = calculateSPM()
                    findViewById<TextView>(R.id.textStrokeRate).text = "Stroke Rate: %.1f spm".format(strokeRate)
                    updateChart(strokeRateChart, strokeRate.toFloat())
                }
            }
        }
    }


    private fun calculateSPM(): Double {
        val strokesMade = strokeTimestamps.size
        val avgStrokeTime = if (strokesMade > 1) {
            (strokeTimestamps.last() - strokeTimestamps.first()) / (strokesMade - 1).toDouble()
        } else {
            strokeWindow.toDouble()
        }

        val strokeRate = (60_000 / avgStrokeTime) // Пересчет в удары/мин

        // Сглаживание
        smoothedSPM = (strokeRate * smoothingFactor) + (smoothedSPM * (1 - smoothingFactor))

        return smoothedSPM
    }



    private fun processGyroscopeData(values: FloatArray) {
        val currentTime = System.currentTimeMillis()
        val deltaTime = (currentTime - lastGyroTime) / 1000.0f // Время между измерениями (в секундах)
        lastGyroTime = currentTime

        val angularVelocityX = values[1] // Гироскоп вокруг X (Roll)

        // Интегрируем угловую скорость, чтобы получить угол
        tiltAngle += angularVelocityX * deltaTime * (180f / Math.PI.toFloat()) // В градусы
        tiltAngle = tiltAngle.coerceIn(-45f, 45f) // Ограничиваем диапазон

        // Обновляем средний угол
        totalTiltSum += tiltAngle
        tiltMeasurements++
        val avgTiltAngle = totalTiltSum / tiltMeasurements

        // Обновляем UI
        findViewById<TextView>(R.id.textTilt).text = "Tilt: %.1f°".format(tiltAngle)
        findViewById<TextView>(R.id.textAvgTilt).text = "Avg Tilt: %.1f°".format(avgTiltAngle)

        updateChart(tiltChart, tiltAngle) // Обновляем график
    }

    private fun updateChart(chart: LineChart, value: Float) {
        if (chart.data == null) {
            chart.data = LineData()
        }
        val dataSet = if (chart.data.dataSetCount > 0) {
            chart.data.getDataSetByIndex(0) as LineDataSet
        } else {
            val newDataSet = LineDataSet(null, "Data").apply {
                color = Color.BLUE
                valueTextColor = Color.BLACK
            }
            chart.data.addDataSet(newDataSet)
            newDataSet
        }
        chart.data.addEntry(Entry(chart.data.entryCount.toFloat(), value), 0)
        chart.data.notifyDataChanged()
        chart.notifyDataSetChanged()
        chart.invalidate()
    }

}
