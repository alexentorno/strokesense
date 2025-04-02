package com.alpekh.strokesense.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
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
        val btnClearAll = findViewById<Button>(R.id.btnClearAll)

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

        // Load trainings
        viewModel.getTrainings { sessions ->
            adapter.submitList(sessions)
            btnClearAll.isEnabled = sessions.isNotEmpty()
        }

        // Set up Clear All button
        btnClearAll.setOnClickListener {
            showClearAllConfirmationDialog()
        }
    }

    private fun showDeleteConfirmationDialog(session: TrainingSession) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_training))
            .setMessage(getString(R.string.confirm_delete_training))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                viewModel.deleteTraining(session) {
                    viewModel.getTrainings { sessions ->
                        adapter.submitList(sessions)
                        updateClearAllButtonState(sessions)
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showClearAllConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.clear_all_trainings))
            .setMessage(getString(R.string.confirm_clear_all_trainings))
            .setPositiveButton(getString(R.string.clear_all)) { _, _ ->
                viewModel.deleteAllTrainings {
                    viewModel.getTrainings { sessions ->
                        adapter.submitList(sessions)
                        updateClearAllButtonState(sessions)
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun updateClearAllButtonState(sessions: List<TrainingSession>) {
        findViewById<Button>(R.id.btnClearAll).isEnabled = sessions.isNotEmpty()
    }
}

