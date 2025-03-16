package com.alpekh.strokesense.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "training_sessions")
data class TrainingSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startTime: Long,   // Время начала
    val endTime: Long,     // Время окончания
    val maxSpeed: Float,   // Максимальная скорость (км/ч)
    val avgSpeed: Float,   // Средняя скорость (км/ч)
    val maxSPM: Int,       // Максимальный SPM
    val avgSPM: Int,       // Средний SPM
    val avgTilt: Float     // Максимальный угол наклона (градусы)
)
