package com.alpekh.strokesense.repository

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.alpekh.strokesense.model.TrainingSession

@Database(entities = [TrainingSession::class], version = 4)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trainingSessionDao(): TrainingSessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "stroke_sense_db"
                ).fallbackToDestructiveMigration()  // Удаляет старую БД при изменении схемы
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

