package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val filePath: String,
    val timestamp: Long = System.currentTimeMillis(),
    val durationMs: Long = 0L,
    val fileSizeBytes: Long = 0L,
    val peakDecibels: Float = 0f,
    val triggerMode: String = "MANUAL", // "MANUAL" ou "AUTOMÁTICO"
    val notes: String = ""
)
