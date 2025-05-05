package com.alpekh.strokesense.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.alpekh.strokesense.R
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.widget.ImageView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val imgLogo = findViewById<ImageView>(R.id.imgLogo)
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

        if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
            imgLogo.setImageResource(R.drawable.main_logo_dark)
        } else {
            imgLogo.setImageResource(R.drawable.main_logo_light)
        }

        val btnStart = findViewById<Button>(R.id.btnStartTraining)
        btnStart.setOnClickListener {
            startActivity(Intent(this, TrainingActivity::class.java))
        }

        val btnHistory = findViewById<Button>(R.id.btnTrainingHistory)
        btnHistory.setOnClickListener {
            startActivity(Intent(this, TrainingHistoryActivity::class.java))
        }

        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "sensor_channel",
                "Sensor Background Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }


    }
}
