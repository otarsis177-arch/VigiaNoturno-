package com.example

import android.app.Application
import com.example.data.RecordingRepository
import com.example.data.VigiaDatabase
import com.example.data.VigiaPreferences

class VigiaApplication : Application() {

    val database by lazy { VigiaDatabase.getInstance(this) }
    val repository by lazy { RecordingRepository(database.recordingDao()) }
    val preferences by lazy { VigiaPreferences(this) }

    override fun onCreate() {
        super.onCreate()
    }
}
