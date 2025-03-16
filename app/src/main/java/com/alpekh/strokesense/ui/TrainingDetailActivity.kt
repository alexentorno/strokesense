package com.alpekh.strokesense.ui

import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.alpekh.strokesense.R
import com.alpekh.strokesense.viewmodel.TrainingViewModel

class TrainingDetailActivity : AppCompatActivity() {

    private val viewModel: TrainingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training_detail)

        val sessionId = intent.getIntExtra("sessionId", -1)

        viewModel.getTrainings { sessions ->
            val session = sessions.find { it.id == sessionId }
            session?.let {
                findViewById<TextView>(R.id.textViewSpeed).text = "Max Speed: ${it.maxSpeed} km/h"
                findViewById<TextView>(R.id.textViewSPM).text = "Max Stroke Rate: ${it.maxSPM}"
                findViewById<TextView>(R.id.textViewTilt).text = "Avg Tilt Angle: ${it.avgTilt}°"
            }
        }
    }
}
