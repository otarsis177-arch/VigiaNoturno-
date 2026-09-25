package com.example.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.VigiaApplication
import com.example.data.RecordingEntity
import com.example.data.RecordingRepository
import com.example.data.VigiaPreferences
import com.example.service.VigiaService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as VigiaApplication
    val repository: RecordingRepository = app.repository
    val preferences: VigiaPreferences = app.preferences

    val recordings: StateFlow<List<RecordingEntity>> = repository.allRecordings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val thresholdDb: StateFlow<Float> = preferences.thresholdDb
    val silenceTimeoutSec: StateFlow<Int> = preferences.silenceTimeoutSec
    val lensFacing: StateFlow<Int> = preferences.lensFacing
    val screenSaverEnabled: StateFlow<Boolean> = preferences.screenSaverEnabled

    private val _isServiceBound = MutableStateFlow(false)
    val isServiceBound: StateFlow<Boolean> = _isServiceBound.asStateFlow()

    private var vigiaService: VigiaService? = null

    private val _serviceState = MutableStateFlow(VigiaService.VigiaState.IDLE)
    val serviceState: StateFlow<VigiaService.VigiaState> = _serviceState.asStateFlow()

    private val _currentDb = MutableStateFlow(0f)
    val currentDb: StateFlow<Float> = _currentDb.asStateFlow()

    private val _silenceRemainingSec = MutableStateFlow(0)
    val silenceRemainingSec: StateFlow<Int> = _silenceRemainingSec.asStateFlow()

    private val _recordingDurationSec = MutableStateFlow(0)
    val recordingDurationSec: StateFlow<Int> = _recordingDurationSec.asStateFlow()

    private val _triggerCount = MutableStateFlow(0)
    val triggerCount: StateFlow<Int> = _triggerCount.asStateFlow()

    private val _isNightScreenActive = MutableStateFlow(false)
    val isNightScreenActive: StateFlow<Boolean> = _isNightScreenActive.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? VigiaService.LocalBinder
            vigiaService = binder?.getService()
            _isServiceBound.value = true

            vigiaService?.let { s ->
                viewModelScope.launch {
                    s.serviceState.collect { _serviceState.value = it }
                }
                viewModelScope.launch {
                    s.currentDb.collect { _currentDb.value = it }
                }
                viewModelScope.launch {
                    s.silenceRemainingSec.collect { _silenceRemainingSec.value = it }
                }
                viewModelScope.launch {
                    s.currentRecordingDurationSec.collect { _recordingDurationSec.value = it }
                }
                viewModelScope.launch {
                    s.triggerCount.collect { _triggerCount.value = it }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            vigiaService = null
            _isServiceBound.value = false
        }
    }

    init {
        bindService()
    }

    fun bindService() {
        val intent = Intent(app, VigiaService::class.java)
        app.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun startVigia() {
        val intent = VigiaService.startVigiaIntent(app)
        ContextCompat.startForegroundService(app, intent)
    }

    fun stopVigia() {
        val intent = VigiaService.stopVigiaIntent(app)
        app.startService(intent)
    }

    fun startManualRecording() {
        val intent = VigiaService.startManualIntent(app)
        ContextCompat.startForegroundService(app, intent)
    }

    fun stopManualRecording() {
        val intent = VigiaService.stopManualIntent(app)
        app.startService(intent)
    }

    fun attachCameraPreview(previewView: PreviewView) {
        vigiaService?.bindCameraPreview(previewView)
    }

    fun switchCameraLens() {
        vigiaService?.switchCameraLens()
    }

    fun setThreshold(value: Float) {
        preferences.setThresholdDb(value)
    }

    fun setSilenceTimeout(seconds: Int) {
        preferences.setSilenceTimeoutSec(seconds)
    }

    fun toggleScreenSaver(enabled: Boolean) {
        preferences.setScreenSaverEnabled(enabled)
    }

    fun setNightScreenActive(active: Boolean) {
        _isNightScreenActive.value = active
    }

    fun deleteRecording(recording: RecordingEntity) {
        viewModelScope.launch {
            repository.deleteRecording(recording)
        }
    }

    fun updateNotes(recording: RecordingEntity, notes: String) {
        viewModelScope.launch {
            repository.updateRecording(recording.copy(notes = notes))
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            if (_isServiceBound.value) {
                app.unbindService(serviceConnection)
                _isServiceBound.value = false
            }
        } catch (_: Exception) {}
    }
}
