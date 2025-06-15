package com.alpekh.strokesense.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.alpekh.strokesense.repository.Converters

@Entity(
    tableName = "training_details",
    foreignKeys = [ForeignKey(
        entity = TrainingSessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["sessionId"])]
)
@TypeConverters(Converters::class)
data class TrainingDetailsEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int,

    val speedChart: List<Float>,
    val strokeRateChart: List<Float>,
    val tiltChart: List<Float>,

    val speedTimestamps: List<Long>,
    val strokeRateTimestamps: List<Long>,
    val tiltTimestamps: List<Long>
)
