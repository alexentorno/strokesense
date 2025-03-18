package com.alpekh.strokesense.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alpekh.strokesense.R
import com.alpekh.strokesense.model.TrainingAdapter
import com.alpekh.strokesense.model.TrainingSession
import com.alpekh.strokesense.viewmodel.TrainingViewModel

class TrainingHistoryActivity : AppCompatActivity() {

    private val viewModel: TrainingViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TrainingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training_history)

        recyclerView = findViewById(R.id.recyclerView)
        adapter = TrainingAdapter(
            onClick = { session ->
                val intent = Intent(this, TrainingDetailActivity::class.java)
                intent.putExtra("sessionId", session.id)
                startActivity(intent)
            },
            onDelete = { session ->
                showDeleteConfirmationDialog(session)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        viewModel.getTrainings { sessions ->
            adapter.submitList(sessions)
            Log.d("TrainingHistoryActivity", "Trainings loaded: ${sessions.size}")
        }
    }

    private fun showDeleteConfirmationDialog(session: TrainingSession) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_training))
            .setMessage(getString(R.string.confirm_delete_training))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                viewModel.deleteTraining(session) {
                    viewModel.getTrainings { sessions ->
                        adapter.submitList(sessions) // Обновляем список
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
}


