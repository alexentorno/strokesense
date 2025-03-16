package com.alpekh.strokesense.ui

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.alpekh.strokesense.R
import android.content.Intent
import android.util.Log
import androidx.activity.viewModels
import com.alpekh.strokesense.model.TrainingSession
import com.alpekh.strokesense.viewmodel.TrainingViewModel

class MainActivity : AppCompatActivity() {

    private val viewModel: TrainingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnStart = findViewById<Button>(R.id.btnStartTraining)
        btnStart.setOnClickListener {
            val intent = Intent(this, TrainingActivity::class.java)
            startActivity(intent)
        }

        val btnHistory = findViewById<Button>(R.id.btnTrainingHistory)
        btnHistory.setOnClickListener {
            val intent = Intent(this, TrainingHistoryActivity::class.java)
            startActivity(intent)
        }

        viewModel.getTrainings { sessions ->
            for (trainingSession: TrainingSession in sessions) {
                Log.d("Training", "Тренировка ${trainingSession.id}: Макс. скорость = ${trainingSession.maxSpeed} км/ч")
            }
        }
    }
}
