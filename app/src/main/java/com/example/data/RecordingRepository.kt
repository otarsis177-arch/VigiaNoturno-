package com.example.data

import kotlinx.coroutines.flow.Flow
import java.io.File

class RecordingRepository(private val recordingDao: RecordingDao) {

    val allRecordings: Flow<List<RecordingEntity>> = recordingDao.getAllRecordings()
    val recordingCount: Flow<Int> = recordingDao.getCount()

    suspend fun insertRecording(recording: RecordingEntity): Long {
        return recordingDao.insert(recording)
    }

    suspend fun deleteRecording(recording: RecordingEntity) {
        // Also delete file from disk if present
        try {
            val file = File(recording.filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) { }
        recordingDao.delete(recording)
    }

    suspend fun deleteById(id: Long) {
        val recording = recordingDao.getById(id)
        if (recording != null) {
            deleteRecording(recording)
        }
    }

    suspend fun updateRecording(recording: RecordingEntity) {
        recordingDao.update(recording)
    }
}
