package com.alpekh.strokesense.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.alpekh.strokesense.repository.Converters

@Entity(tableName = "training_sessions")
@TypeConverters(Converters::class)  // Конвертер для хранения списков в БД
data class TrainingSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startTime: Long,
    val endTime: Long,
    val distance: Float,  // (м)
    val maxSpeed: Float,   // (км/ч)
    val avgSpeed: Float,   // (км/ч)
    val maxSPM: Float,
    val avgTilt: Float,

    val speedChart: List<Float>,
    val strokeRateChart: List<Float>,
//    val tiltChart: List<Float>,

    val speedTimestamps: List<Long>,
    val strokeRateTimestamps: List<Long>,
//    val tiltTimestamps: List<Long>
)
