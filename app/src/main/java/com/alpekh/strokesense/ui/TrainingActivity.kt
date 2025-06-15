package com.alpekh.strokesense.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import com.alpekh.strokesense.helpers.ChartManager
import com.alpekh.strokesense.helpers.SensorService
import com.alpekh.strokesense.model.TrainingDetailsEntity
import com.alpekh.strokesense.model.TrainingSessionEntity
import com.alpekh.strokesense.viewmodel.TrainingViewModel
import com.github.mikephil.charting.charts.LineChart
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.round

class TrainingActivity : AppCompatActivity() {

    // Views
    private lateinit var textTrainingTimer: TextView
    private lateinit var textDistance: TextView
    private lateinit var textSpeed: TextView
    private lateinit var textAvgSpeed: TextView
    private lateinit var textMaxSpeed: TextView
    private lateinit var textStrokeRate: TextView
    private lateinit var textMaxStrokeRate: TextView
    private lateinit var textTilt: TextView
    private lateinit var textAvgTilt: TextView
    private lateinit var btnPauseTraining: Button

    // Charts
    private lateinit var speedChart: LineChart
    private lateinit var strokeRateChart: LineChart
    private lateinit var tiltChart: LineChart
    private lateinit var speedChartManager: ChartManager
    private lateinit var strokeRateChartManager: ChartManager
    private lateinit var tiltChartManager: ChartManager

    // Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var lastLocation: Location? = null
    private var lastKnownLocation: Location? = null
    private var lastKnownSpeed = 0f

    // Sensors
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var sensitivityAccel = 2f
    private var sensitivityGyro = 2f

    // Timing
    private var timerHandler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null
    private var trainingDuration = 0L
    private var startTime = 0L
    private var lastSpeedUpdateTime = 0L
    private val speedUpdateInterval = 1000L
    private var elapsedTimeBeforePause = 0L

    // Stats
    private var maxSpeed = 0f
    private var totalSpeed = 0f
    private var avgSpeed = 0f
    private var speedCount = 0
    private var totalDistance = 0f
    private var strokeCount = 0
    private val minValidStrokes = 3
    private val strokeTimestamps = mutableListOf<Long>()
    private var maxSPM = 0f
    private var smoothedSPM = 0.0
    private val strokeWindow = 10000
    private val accelerationBuffer = ArrayDeque<Float>(5)
    private var tiltAngle = 0f
    private var avgTiltAngle = 0f
    private var lastGyroTime = System.currentTimeMillis()
    private var totalTiltSum = 0f
    private var tiltMeasurements = 0
    private val speedTimestamps = mutableListOf<Long>()
    private val strokeRateTimestamps = mutableListOf<Long>()
    private val tiltTimestamps = mutableListOf<Long>()

    private val viewModel: TrainingViewModel by viewModels()

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
        viewModel.isPaused = savedInstanceState?.getBoolean("isPaused", false) ?: false

        initViews()
        initSensors()
        setupButtons()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            println("Creating notification channel")
            startForegroundService(Intent(this, SensorService::class.java))
        } else {
            startService(Intent(this, SensorService::class.java))
        }

        if (checkLocationPermission()) {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                showGpsDisabledAlert()
            } else {
                startTraining()
                startTracking()
            }
        }
    }

    private fun initViews() {
        speedChart = findViewById(R.id.speedChart)
        tiltChart = findViewById(R.id.tiltChart)
        strokeRateChart = findViewById(R.id.strokeRateChart)
        speedChartManager = ChartManager(speedChart)
        strokeRateChartManager = ChartManager(strokeRateChart)
        tiltChartManager = ChartManager(tiltChart)

        textDistance = findViewById(R.id.textDistance)
        textTrainingTimer = findViewById(R.id.textTrainingTimer)
        textSpeed = findViewById(R.id.textSpeed)
        textAvgSpeed = findViewById(R.id.textAvgSpeed)
        textMaxSpeed = findViewById(R.id.textMaxSpeed)
        textStrokeRate = findViewById(R.id.textStrokeRate)
        textMaxStrokeRate = findViewById(R.id.textMaxStrokeRate)
        textTilt = findViewById(R.id.textTilt)
        textAvgTilt = findViewById(R.id.textAvgTilt)
        btnPauseTraining = findViewById(R.id.btnPauseTraining)
        btnPauseTraining.text = if (viewModel.isPaused) getString(R.string.resume) else getString(R.string.pause)

        val sharedPrefs = getSharedPreferences("StrokeSensePrefs", Context.MODE_PRIVATE)
        sensitivityAccel = 1.0f + sharedPrefs.getInt("sensitivity_accel", 5) * 0.2f
        sensitivityGyro = 0.7f + sharedPrefs.getInt("sensitivity_gyro", 2) * 0.01f
    }

    private fun initSensors() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }

    private fun setupButtons() {
        btnPauseTraining.setOnClickListener {
            viewModel.isPaused = !viewModel.isPaused
            if (viewModel.isPaused) pauseTraining().also { btnPauseTraining.text = getString(R.string.resume) }
            else resumeTraining().also { btnPauseTraining.text = getString(R.string.pause) }
        }
        onBackPressedDispatcher.addCallback(this) { showCancelTrainingDialog() }
        findViewById<Button>(R.id.btnStopTraining).setOnClickListener { stopTraining() }
    }

    // Timer functions
    private fun startTimer() {
        timerRunnable = object : Runnable {
            override fun run() {
                updateTimerText()
                timerHandler.postDelayed(this, 1000)
            }
        }.also { timerHandler.post(it) }
    }

    private fun stopTimer() = timerRunnable?.let { timerHandler.removeCallbacks(it) }

    private fun updateTimerText() {
        val currentTime = System.currentTimeMillis()
        trainingDuration = elapsedTimeBeforePause + (currentTime - startTime)

        val seconds = (trainingDuration / 1000) % 60
        val minutes = (trainingDuration / (1000 * 60)) % 60
        val hours = (trainingDuration / (1000 * 60 * 60))
        textTrainingTimer.text = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    // Training control
    private fun startTraining() {
        strokeCount = 0
        startTime = System.currentTimeMillis()
        totalTiltSum = 0f
        tiltMeasurements = 0
        avgTiltAngle = 0f
        strokeTimestamps.clear()
        maxSPM = 0f
        startTimer()
    }

    private fun pauseTraining() {
        stopTimer()
        elapsedTimeBeforePause += System.currentTimeMillis() - startTime
        sensorManager.unregisterListener(sensorEventListener)
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun resumeTraining() {
        startTime = System.currentTimeMillis()
        startTimer()
        accelerometer?.let { sensorManager.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_UI) }
        gyroscope?.let { sensorManager.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_UI) }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    private fun stopTraining() {
        stopTimer()
        val sessionEntity = TrainingSessionEntity(
            startTime = startTime,
            endTime = trainingDuration,
            distance = totalDistance,
            maxSpeed = maxSpeed,
            avgSpeed = avgSpeed,
            maxSPM = maxSPM,
            avgTilt = avgTiltAngle
        )

        val detailsEntity = TrainingDetailsEntity(
            sessionId = 0, // будет заменён в DAO
            speedChart = speedChartManager.getAllEntries().map { it.y },
            strokeRateChart = strokeRateChartManager.getAllEntries().map { it.y },
            tiltChart = tiltChartManager.getAllEntries().map { it.y },
            speedTimestamps = speedTimestamps,
            strokeRateTimestamps = strokeRateTimestamps,
            tiltTimestamps = tiltTimestamps
        )

        viewModel.saveTraining(sessionEntity, detailsEntity)
        finish()
    }

    // Location tracking
    private fun startTracking() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showGpsDisabledAlert()
            return
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setMinUpdateIntervalMillis(500).setWaitForAccurateLocation(true)
            .setMinUpdateDistanceMeters(0f).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.locations.forEach { processLocationUpdate(it) }
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }

        timerHandler.post(object : Runnable {
            override fun run() {
                if (!viewModel.isPaused && System.currentTimeMillis() - lastSpeedUpdateTime >= speedUpdateInterval) {
                    updateSpeedDisplay().also { lastSpeedUpdateTime = System.currentTimeMillis() }
                }
                timerHandler.postDelayed(this, 100)
            }
        })
    }

    private fun processLocationUpdate(location: Location) {
        if (location.accuracy > 15f) return
        lastKnownLocation = location
        lastKnownSpeed = (location.speed * 3.6f).let { if (it < 0.5f) 0f else it }
    }

    private fun updateSpeedDisplay() {
        totalSpeed += lastKnownSpeed
        speedCount++
        avgSpeed = totalSpeed / speedCount
        maxSpeed = max(maxSpeed, lastKnownSpeed)

        lastKnownLocation?.let { currentLoc ->
            lastLocation?.let { prevLoc -> totalDistance += max(0f, prevLoc.distanceTo(currentLoc)) }
            lastLocation = currentLoc
        }

        runOnUiThread {
            textSpeed.text = getString(R.string.speed_text, lastKnownSpeed.roundTo(1))
            textAvgSpeed.text = getString(R.string.avg_speed_text, avgSpeed.roundTo(1))
            textMaxSpeed.text = getString(R.string.max_speed_text, maxSpeed.roundTo(1))
            textDistance.text = getString(R.string.distance_text, (totalDistance / 1000).roundTo(3))
            speedChartManager.updateChart(lastKnownSpeed)
        }
        speedTimestamps.add(System.currentTimeMillis())
    }

    // Sensor processing
    private fun processAccelerometerData(values: FloatArray) {
        if (accelerationBuffer.size >= 5) accelerationBuffer.removeFirst()
        accelerationBuffer.add(values[1])
        val avgAcceleration = accelerationBuffer.average().toFloat()

        if (abs(avgAcceleration) > sensitivityAccel) {
            val currentTime = System.currentTimeMillis()
            if (strokeTimestamps.isEmpty() || currentTime - strokeTimestamps.last() > 300) {
                strokeTimestamps.add(currentTime).also { strokeTimestamps.removeAll { it < currentTime - strokeWindow } }
                strokeRateTimestamps.add(currentTime)
                if (strokeTimestamps.size >= minValidStrokes) {
                    calculateSPM().let {
                        maxSPM = max(maxSPM, it)
                        findViewById<TextView>(R.id.textStrokeRate).text = getString(R.string.stroke_rate_text, it)
                        findViewById<TextView>(R.id.textMaxStrokeRate).text = getString(R.string.max_stroke_rate_text, maxSPM)
                        strokeRateChartManager.updateChart(it)
                    }
                }
            }
        }
    }

    private fun calculateSPM(): Float {
        val strokesMade = strokeTimestamps.size
        val avgStrokeTime = if (strokesMade > 1) (strokeTimestamps.last() - strokeTimestamps.first()) / (strokesMade - 1).toDouble()
        else strokeWindow.toDouble()
        val strokeRate = 60_000 / avgStrokeTime
        smoothedSPM = (strokeRate * 0.9) + (smoothedSPM * 0.1) // Smoothing factor 0.9
        return smoothedSPM.toFloat()
    }

    private fun processGyroscopeData(values: FloatArray) {
        val currentTime = System.currentTimeMillis()
        val deltaTime = (currentTime - lastGyroTime) / 1000f
        lastGyroTime = currentTime
        tiltAngle = (tiltAngle * sensitivityGyro) + (values[1] * deltaTime * 57.3f)
        totalTiltSum += tiltAngle
        avgTiltAngle = totalTiltSum / ++tiltMeasurements
        tiltTimestamps.add(currentTime)
        runOnUiThread {
            textTilt.text = getString(R.string.tilt_text, tiltAngle)
            textAvgTilt.text = getString(R.string.avg_tilt_text, avgTiltAngle)
            tiltChartManager.updateChart(tiltAngle)
        }
    }

    // Helper functions
    private fun Float.roundTo(decimals: Int): Float {
        var multiplier = 1f
        repeat(decimals) { multiplier *= 10f }
        return round(this * multiplier) / multiplier
    }

    private fun checkLocationPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) true
        else ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1).let { false }
    }

    private fun showGpsDisabledAlert() {
        AlertDialog.Builder(this).setTitle("GPS turned off")
            .setMessage("For precise location and speed measurements it is vital to have GPS turned on. Enable GPS?")
            .setPositiveButton("Yes") { _, _ -> startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
            .setNegativeButton("No", null).show()
    }

    private fun showCancelTrainingDialog() {
        AlertDialog.Builder(this).setTitle("Cancel Training")
            .setMessage("Do you really want to cancel the current training?")
            .setPositiveButton("Yes") { _, _ -> finish() }
            .setNegativeButton("No", null).setCancelable(true).show()
    }

    override fun onSaveInstanceState(outState: Bundle) = super.onSaveInstanceState(outState.apply {
        putBoolean("isPaused", viewModel.isPaused)
    })

    override fun onDestroy() = super.onDestroy().also {
        if (::fusedLocationClient.isInitialized) fusedLocationClient.removeLocationUpdates(locationCallback)
        stopTimer()
    }

    override fun onResume() = super.onResume().also {
        if (!viewModel.isPaused) {
            accelerometer?.let { sensorManager.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_UI) }
            gyroscope?.let { sensorManager.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_UI) }
        }
    }

    override fun onPause() = super.onPause().also {
        if (viewModel.isPaused) sensorManager.unregisterListener(sensorEventListener)
    }
}
