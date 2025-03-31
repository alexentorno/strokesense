package com.alpekh.strokesense.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.alpekh.strokesense.repository.Converters

@Entity(tableName = "training_sessions")
@TypeConverters(Converters::class)  // Конвертер для хранения списков в БД
data class TrainingSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startTime: Long,   // Время начала
    val endTime: Long,     // Время окончания
    val maxSpeed: Float,   // Максимальная скорость (км/ч)
    val avgSpeed: Float,   // Средняя скорость (км/ч)
    val maxSPM: Float,       // Максимальный SPM
    val avgTilt: Float,    // Средний угол наклона (градусы)

    val speedChart: List<Float>,    // Данные графика скорости
    val SPMChart: List<Float>, // Данные графика ускорения
    val tiltChart: List<Float> ,     // Данные графика наклона

    // Временные метки для графиков
    val speedTimestamps: List<Long>,
    val SPMTimestamps: List<Long>,
    val tiltTimestamps: List<Long>
)
