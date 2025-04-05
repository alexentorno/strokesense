package com.alpekh.strokesense.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.alpekh.strokesense.R

class SettingsActivity : AppCompatActivity() {

    private lateinit var sensitivityAccelSeekBar: SeekBar
    private lateinit var sensitivityGyroSeekBar: SeekBar
    private lateinit var textSensitivityAccel: TextView
    private lateinit var textSensitivityGyro: TextView

    private lateinit var sharedPreferences: SharedPreferences

    private val accelMin = 1.0f
    private val accelStep = 0.2f

    private val gyroMin = 0.7f
    private val gyroStep = 0.01f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        sharedPreferences = getSharedPreferences("StrokeSensePrefs", Context.MODE_PRIVATE)

        sensitivityAccelSeekBar = findViewById(R.id.sensitivityAccelSeekBar)
        sensitivityGyroSeekBar = findViewById(R.id.sensitivityGyroSeekBar)
        textSensitivityAccel = findViewById(R.id.textSensitivityAccel)
        textSensitivityGyro = findViewById(R.id.textSensitivityGyro)

        sensitivityAccelSeekBar.max = 30  // (5.0 - 1.0) / 0.2
        sensitivityGyroSeekBar.max = 30    // (1.0 - 0.7) / 0.1

        // Загрузка сохранённых значений (индексы)
        val accelIndex = sharedPreferences.getInt("sensitivity_accel", 5)
        val gyroIndex = sharedPreferences.getInt("sensitivity_gyro", 27)

        sensitivityAccelSeekBar.progress = accelIndex
        sensitivityGyroSeekBar.progress = gyroIndex

        updateSensitivityText(accelIndex, gyroIndex)

        sensitivityAccelSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = accelMin + progress * accelStep
                textSensitivityAccel.text = getString(R.string.stroke_rate_sensitivity, value)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                sharedPreferences.edit().putInt("sensitivity_accel", sensitivityAccelSeekBar.progress).apply()
            }
        })

        sensitivityGyroSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = gyroMin + progress * gyroStep
                textSensitivityGyro.text = getString(R.string.tilt_sensitivity, value)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                sharedPreferences.edit().putInt("sensitivity_gyro", sensitivityGyroSeekBar.progress).apply()
            }
        })
    }

    private fun updateSensitivityText(accelIndex: Int, gyroIndex: Int) {
        val accelValue = accelMin + accelIndex * accelStep
        val gyroValue = gyroMin + gyroIndex * gyroStep

        textSensitivityAccel.text = getString(R.string.stroke_rate_sensitivity, accelValue)
        textSensitivityGyro.text = getString(R.string.tilt_sensitivity, gyroValue)
    }
}
