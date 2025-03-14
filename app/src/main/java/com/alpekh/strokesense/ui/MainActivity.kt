package com.alpekh.strokesense.ui

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.alpekh.strokesense.R
import android.content.Intent


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnStart = findViewById<Button>(R.id.btnStartTraining)
        btnStart.setOnClickListener {

                    btnStart.setOnClickListener {
                        val intent = Intent(this, TrainingActivity::class.java)
                        startActivity(intent)
                    }

        }
    }
}
