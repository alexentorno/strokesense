package com.alpekh.strokesense.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.alpekh.strokesense.model.TrainingSession

@Dao
interface TrainingSessionDao {

    @Insert
    suspend fun insertSession(session: TrainingSession)

    @Query("SELECT * FROM training_sessions ORDER BY startTime DESC")
    suspend fun getAllSessions(): List<TrainingSession>

    @Query("SELECT * FROM training_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Int): TrainingSession?

    @Query("DELETE FROM training_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Int)

    @Query("DELETE FROM training_sessions")
    suspend fun deleteAllTrainings()
}
