package com.alpekh.strokesense.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alpekh.strokesense.model.TrainingDetailsEntity
import com.alpekh.strokesense.model.TrainingSessionEntity
import com.alpekh.strokesense.repository.AppDatabase
import kotlinx.coroutines.launch

class TrainingViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val trainingDao = db.trainingSessionDao()
    var isPaused = false

    fun saveTraining(sessionMeta: TrainingSessionEntity, details: TrainingDetailsEntity) {
        viewModelScope.launch {
            trainingDao.insertFullSession(sessionMeta, details)
        }
    }

    fun getTrainings(callback: (List<TrainingSessionEntity>) -> Unit) {
        viewModelScope.launch {
            callback(trainingDao.getAllSessions())
        }
    }

    fun getTrainingDetails(sessionId: Int, callback: (TrainingDetailsEntity?) -> Unit) {
        viewModelScope.launch {
            callback(trainingDao.getDetailsForSession(sessionId))
        }
    }


    fun deleteTraining(session: TrainingSessionEntity, callback: () -> Unit) {
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
