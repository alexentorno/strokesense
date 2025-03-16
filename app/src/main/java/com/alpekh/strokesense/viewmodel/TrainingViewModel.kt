package com.alpekh.strokesense.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alpekh.strokesense.model.TrainingSession
import com.alpekh.strokesense.repository.AppDatabase
import kotlinx.coroutines.launch

class TrainingViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val trainingDao = db.trainingSessionDao()

    fun saveTraining(session: TrainingSession) {
        viewModelScope.launch {
            trainingDao.insertSession(session)
        }
    }

    fun getTrainings(callback: (List<TrainingSession>) -> Unit) {
        viewModelScope.launch {
            val sessions = trainingDao.getAllSessions()
            callback(sessions)
        }
    }
}
