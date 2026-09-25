package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.view.PreviewView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.example.MainActivity
import com.example.R
import com.example.VigiaApplication
import com.example.audio.AudioLevelMonitor
import com.example.camera.CameraRecorder
import com.example.data.RecordingEntity
import com.example.data.VigiaPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class VigiaService : Service(), LifecycleOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    inner class LocalBinder : Binder() {
        fun getService(): VigiaService = this@VigiaService
    }

    private val binder = LocalBinder()

    enum class VigiaState {
        IDLE,
        MONITORING,
        RECORDING_AUTO,
        RECORDING_MANUAL
    }

    private val _serviceState = MutableStateFlow(VigiaState.IDLE)
    val serviceState: StateFlow<VigiaState> = _serviceState.asStateFlow()

    private val _currentDb = MutableStateFlow(0f)
    val currentDb: StateFlow<Float> = _currentDb.asStateFlow()

    private val _silenceRemainingSec = MutableStateFlow(0)
    val silenceRemainingSec: StateFlow<Int> = _silenceRemainingSec.asStateFlow()

    private val _currentRecordingDurationSec = MutableStateFlow(0)
    val currentRecordingDurationSec: StateFlow<Int> = _currentRecordingDurationSec.asStateFlow()

    private val _lastSavedFileName = MutableStateFlow<String?>(null)
    val lastSavedFileName: StateFlow<String?> = _lastSavedFileName.asStateFlow()

    private val _triggerCount = MutableStateFlow(0)
    val triggerCount: StateFlow<Int> = _triggerCount.asStateFlow()

    private var wakeLock: PowerManager.WakeLock? = null
    private var audioMonitor: AudioLevelMonitor? = null
    private var cameraRecorder: CameraRecorder? = null
    private lateinit var preferences: VigiaPreferences

    private var silenceCheckJob: Job? = null
    private var durationJob: Job? = null
    private var peakDbInSession = 0f
    private var silenceSecondsElapsed = 0

    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        preferences = VigiaPreferences(this)
        cameraRecorder = CameraRecorder(this)
        audioMonitor = AudioLevelMonitor()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_VIGIA -> startVigiaAuto()
            ACTION_STOP_VIGIA -> stopVigia()
            ACTION_START_MANUAL -> startManualRecording()
            ACTION_STOP_MANUAL -> stopManualRecording()
        }

        return START_STICKY
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "VigiaNoturno:MonitoringWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire(12 * 60 * 60 * 1000L) // Até 12 horas (madrugada inteira)
            }
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
        wakeLock = null
    }

    fun bindCameraPreview(previewView: PreviewView) {
        val lensFacing = if (preferences.lensFacing.value == 0) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        cameraRecorder?.currentLensFacing = lensFacing
        cameraRecorder?.bindCamera(this, previewView)
    }

    fun switchCameraLens() {
        val newLens = if (preferences.lensFacing.value == 0) 1 else 0
        preferences.setLensFacing(newLens)
        val lensFacing = if (newLens == 0) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
        cameraRecorder?.currentLensFacing = lensFacing
        cameraRecorder?.bindCamera(this, null)
    }

    @SuppressLint("MissingPermission")
    fun startVigiaAuto() {
        acquireWakeLock()
        startForegroundWithType(buildNotification("Vigia noturno ativo", "Monitorando ruído ambiente..."))

        // Bind camera in background
        val lensFacing = if (preferences.lensFacing.value == 0) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        cameraRecorder?.currentLensFacing = lensFacing
        cameraRecorder?.bindCamera(this, null)

        _serviceState.value = VigiaState.MONITORING

        // Start low-power audio level monitoring
        audioMonitor?.startMonitoring(
            thresholdProvider = { preferences.thresholdDb.value },
            onSoundThresholdExceeded = { detectedDb ->
                onSoundTriggered(detectedDb)
            }
        )

        // Observe decibels
        serviceScope.launch {
            audioMonitor?.currentDb?.collect { db ->
                _currentDb.value = db
                if (_serviceState.value == VigiaState.RECORDING_AUTO) {
                    if (db > peakDbInSession) {
                        peakDbInSession = db
                    }
                    if (db >= preferences.thresholdDb.value) {
                        // Ruído contínuo detectado: zera o contador de silêncio
                        silenceSecondsElapsed = 0
                        val timeout = preferences.silenceTimeoutSec.value
                        _silenceRemainingSec.value = timeout
                    }
                }
            }
        }
    }

    private fun onSoundTriggered(detectedDb: Float) {
        if (_serviceState.value == VigiaState.MONITORING) {
            // Inicia gravação automática
            peakDbInSession = detectedDb
            silenceSecondsElapsed = 0
            val timeout = preferences.silenceTimeoutSec.value
            _silenceRemainingSec.value = timeout
            _triggerCount.value += 1

            startAutoVideoRecording()
        }
    }

    private fun startAutoVideoRecording() {
        _serviceState.value = VigiaState.RECORDING_AUTO
        updateNotification("GRAVANDO EVIDÊNCIA DE RUÍDO", "Detectado ${peakDbInSession.toInt()} dB")

        cameraRecorder?.startRecording(
            onFinished = { file, durationMs, _ ->
                saveRecordingToDatabase(file, durationMs, peakDbInSession, "AUTOMÁTICO")
                if (_serviceState.value == VigiaState.RECORDING_AUTO) {
                    _serviceState.value = VigiaState.MONITORING
                    updateNotification("Vigia noturno ativo", "Monitorando ruído ambiente...")
                }
            },
            onError = { errMsg ->
                Log.e("VigiaService", errMsg)
                if (_serviceState.value == VigiaState.RECORDING_AUTO) {
                    _serviceState.value = VigiaState.MONITORING
                    updateNotification("Vigia noturno ativo", "Monitorando ruído ambiente...")
                }
            }
        )

        startSilenceCountdown()
        startDurationTimer()
    }

    private fun startSilenceCountdown() {
        silenceCheckJob?.cancel()
        silenceCheckJob = serviceScope.launch {
            val timeout = preferences.silenceTimeoutSec.value
            _silenceRemainingSec.value = timeout
            silenceSecondsElapsed = 0

            while (isActive && _serviceState.value == VigiaState.RECORDING_AUTO) {
                delay(1000)
                silenceSecondsElapsed += 1
                val remaining = (timeout - silenceSecondsElapsed).coerceAtLeast(0)
                _silenceRemainingSec.value = remaining

                if (silenceSecondsElapsed >= timeout) {
                    // Silêncio contínuo atingido: finaliza a gravação
                    cameraRecorder?.stopRecording()
                    break
                }
            }
        }
    }

    private fun startDurationTimer() {
        durationJob?.cancel()
        durationJob = serviceScope.launch {
            var seconds = 0
            _currentRecordingDurationSec.value = 0
            while (isActive && (_serviceState.value == VigiaState.RECORDING_AUTO || _serviceState.value == VigiaState.RECORDING_MANUAL)) {
                delay(1000)
                seconds++
                _currentRecordingDurationSec.value = seconds
            }
        }
    }

    fun startManualRecording() {
        acquireWakeLock()
        startForegroundWithType(buildNotification("Gravando Evidência Manual", "Gravação em andamento..."))

        _serviceState.value = VigiaState.RECORDING_MANUAL
        peakDbInSession = _currentDb.value

        cameraRecorder?.startRecording(
            onFinished = { file, durationMs, _ ->
                saveRecordingToDatabase(file, durationMs, peakDbInSession, "MANUAL")
                _serviceState.value = VigiaState.IDLE
                releaseWakeLock()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            },
            onError = { errMsg ->
                Log.e("VigiaService", errMsg)
                _serviceState.value = VigiaState.IDLE
                releaseWakeLock()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        )

        startDurationTimer()
    }

    fun stopManualRecording() {
        cameraRecorder?.stopRecording()
    }

    fun stopVigia() {
        silenceCheckJob?.cancel()
        durationJob?.cancel()
        cameraRecorder?.stopRecording()
        audioMonitor?.stopMonitoring()
        _serviceState.value = VigiaState.IDLE
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun saveRecordingToDatabase(file: File, durationMs: Long, peakDb: Float, mode: String) {
        serviceScope.launch(Dispatchers.IO) {
            try {
                val repository = (application as VigiaApplication).repository
                val entity = RecordingEntity(
                    fileName = file.name,
                    filePath = file.absolutePath,
                    timestamp = System.currentTimeMillis(),
                    durationMs = durationMs,
                    fileSizeBytes = file.length(),
                    peakDecibels = peakDb,
                    triggerMode = mode
                )
                repository.insertRecording(entity)
                _lastSavedFileName.value = file.name
            } catch (e: Exception) {
                Log.e("VigiaService", "Erro ao salvar evidência no banco: ${e.message}", e)
            }
        }
    }

    private fun startForegroundWithType(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(title: String, content: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(title, content))
    }

    private fun buildNotification(title: String, content: String): Notification {
        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, VigiaService::class.java).apply {
            action = ACTION_STOP_VIGIA
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Encerrar Vigia", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Vigia Noturno - Câmera e Microfone",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificação persistente durante o monitoramento noturno"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        stopVigia()
        cameraRecorder?.unbind()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "vigia_noturno_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_VIGIA = "com.example.action.START_VIGIA"
        const val ACTION_STOP_VIGIA = "com.example.action.STOP_VIGIA"
        const val ACTION_START_MANUAL = "com.example.action.START_MANUAL"
        const val ACTION_STOP_MANUAL = "com.example.action.STOP_MANUAL"

        fun startVigiaIntent(context: Context): Intent =
            Intent(context, VigiaService::class.java).apply { action = ACTION_START_VIGIA }

        fun stopVigiaIntent(context: Context): Intent =
            Intent(context, VigiaService::class.java).apply { action = ACTION_STOP_VIGIA }

        fun startManualIntent(context: Context): Intent =
            Intent(context, VigiaService::class.java).apply { action = ACTION_START_MANUAL }

        fun stopManualIntent(context: Context): Intent =
            Intent(context, VigiaService::class.java).apply { action = ACTION_STOP_MANUAL }
    }
}
