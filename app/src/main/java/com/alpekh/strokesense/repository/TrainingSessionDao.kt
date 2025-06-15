package com.alpekh.strokesense.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.alpekh.strokesense.model.TrainingDetailsEntity
import com.alpekh.strokesense.model.TrainingSessionEntity

@Dao
interface TrainingSessionDao {

    @Insert
    suspend fun insertSession(session: TrainingSessionEntity): Long

    @Insert
    suspend fun insertDetails(details: TrainingDetailsEntity)

    @Transaction
    suspend fun insertFullSession(session: TrainingSessionEntity, details: TrainingDetailsEntity) {
        val sessionId = insertSession(session)
        insertDetails(details.copy(sessionId = sessionId.toInt()))
    }

    @Query("SELECT * FROM training_sessions ORDER BY startTime DESC")
    suspend fun getAllSessions(): List<TrainingSessionEntity>

    @Query("SELECT * FROM training_details WHERE sessionId = :sessionId")
    suspend fun getDetailsForSession(sessionId: Int): TrainingDetailsEntity?

    @Query("DELETE FROM training_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Int)

    @Query("DELETE FROM training_sessions")
    suspend fun deleteAllTrainings()
}
