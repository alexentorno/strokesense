package com.alpekh.strokesense.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.alpekh.strokesense.repository.Converters

@Entity(tableName = "training_sessions")
@TypeConverters(Converters::class)
data class TrainingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startTime: Long,
    val endTime: Long,
    val distance: Float,
    val maxSpeed: Float,
    val avgSpeed: Float,
    val maxSPM: Float,
    val avgTilt: Float
)
