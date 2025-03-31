package com.alpekh.strokesense.repository

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromFloatList(value: List<Float>): String {
        return value.joinToString(",")
    }

    @TypeConverter
    fun toFloatList(value: String): List<Float> {
        return if (value.isEmpty()) emptyList() else value.split(",").map { it.toFloat() }
    }

    @TypeConverter
    fun fromLongList(value: List<Long>?): String {
        return value?.toList()?.joinToString(",") ?: ""
    }


    @TypeConverter
    fun toLongList(value: String): List<Long> {
        return if (value.isEmpty()) emptyList() else value.split(",").map { it.toLong() }
    }
}
