package com.example.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Monitor de nível sonoro de baixo consumo para vigia noturno contínuo.
 * Utiliza AudioRecord em 8000Hz (PCM 16-bit Mono) para economizar bateria
 * enquanto o dispositivo permanece em vigia durante a madrugada.
 */
class AudioLevelMonitor {

    private val _currentDb = MutableStateFlow(0f)
    val currentDb: StateFlow<Float> = _currentDb.asStateFlow()

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var monitorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    @SuppressLint("MissingPermission")
    fun startMonitoring(onSoundThresholdExceeded: ((Float) -> Unit)? = null, thresholdProvider: (() -> Float)? = null) {
        if (_isMonitoring.value) return

        monitorJob = scope.launch {
            val sampleRate = 8000
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = (minBufferSize * 2).coerceAtLeast(1024)

            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e("AudioLevelMonitor", "Falha ao inicializar AudioRecord")
                    _isMonitoring.value = false
                    return@launch
                }

                audioRecord?.startRecording()
                _isMonitoring.value = true

                val buffer = ShortArray(bufferSize / 2)
                var smoothedDb = 30f

                while (isActive && _isMonitoring.value) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        var sumSquares = 0.0
                        for (i in 0 until read) {
                            val sample = buffer[i].toDouble()
                            sumSquares += sample * sample
                        }
                        val rms = sqrt(sumSquares / read)
                        
                        // Cálculo de decibéis calibrado para ruído ambiente (30dB a ~95dB)
                        val rawDb = if (rms > 1.0) {
                            (20.0 * log10(rms)).toFloat().coerceIn(20f, 100f)
                        } else {
                            20f
                        }

                        // Suavização rápida para estabilidade de leitura
                        smoothedDb = (smoothedDb * 0.4f) + (rawDb * 0.6f)
                        _currentDb.value = smoothedDb

                        // Verificação de disparo por limite
                        val threshold = thresholdProvider?.invoke() ?: Float.MAX_VALUE
                        if (smoothedDb >= threshold) {
                            onSoundThresholdExceeded?.invoke(smoothedDb)
                        }
                    } else {
                        delay(50)
                    }
                    delay(80) // Amostragem a cada ~80ms = baixo uso de CPU
                }
            } catch (e: Exception) {
                Log.e("AudioLevelMonitor", "Erro no monitor de áudio: ${e.message}", e)
            } finally {
                stopInternal()
            }
        }
    }

    private fun stopInternal() {
        try {
            audioRecord?.stop()
        } catch (_: Exception) {}
        try {
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
        _isMonitoring.value = false
        _currentDb.value = 0f
    }

    fun stopMonitoring() {
        _isMonitoring.value = false
        monitorJob?.cancel()
        monitorJob = null
        stopInternal()
    }
}
