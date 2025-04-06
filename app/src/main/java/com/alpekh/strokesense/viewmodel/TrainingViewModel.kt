package com.alpekh.strokesense.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alpekh.strokesense.model.TrainingSession
import com.alpekh.strokesense.repository.AppDatabase
import kotlinx.coroutines.launch

class TrainingViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val trainingDao = db.trainingSessionDao()
    var isPaused = false

    fun saveTraining(session: TrainingSession) {
        viewModelScope.launch {
            trainingDao.insertSession(session)
            Log.d("TrainingViewModel", "Training saved: $session")
        }
    }

    fun getTrainings(callback: (List<TrainingSession>) -> Unit) {
        viewModelScope.launch {
            val sessions = trainingDao.getAllSessions()
            callback(sessions)
        }
    }

    fun deleteTraining(session: TrainingSession, callback: () -> Unit) {
        viewModelScope.launch {
            trainingDao.deleteSession(session.id)
            callback() // Обновл UI после удаления
        }
    }

    fun deleteAllTrainings(callback: () -> Unit) {
        viewModelScope.launch {
            trainingDao.deleteAllTrainings()
            callback()
        }
    }
}
