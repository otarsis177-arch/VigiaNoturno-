package com.example.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Environment
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor

class CameraRecorder(private val context: Context) {

    private var videoCapture: VideoCapture<Recorder>? = null
    private var currentRecording: Recording? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationSec = MutableStateFlow(0)
    val recordingDurationSec: StateFlow<Int> = _recordingDurationSec.asStateFlow()

    private val mainExecutor: Executor = ContextCompat.getMainExecutor(context)

    var currentLensFacing: Int = CameraSelector.LENS_FACING_BACK

    fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView? = null,
        onReady: (() -> Unit)? = null
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(currentLensFacing)
                    .build()

                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.HD))
                    .build()

                videoCapture = VideoCapture.withOutput(recorder)

                cameraProvider?.unbindAll()

                if (previewView != null) {
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    cameraProvider?.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        videoCapture
                    )
                } else {
                    cameraProvider?.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        videoCapture
                    )
                }

                onReady?.invoke()
            } catch (e: Exception) {
                Log.e("CameraRecorder", "Erro ao vincular câmera: ${e.message}", e)
            }
        }, mainExecutor)
    }

    @SuppressLint("MissingPermission")
    fun startRecording(
        onFinished: (file: File, durationMs: Long, peakDb: Float) -> Unit,
        onError: (String) -> Unit
    ): File? {
        val capture = videoCapture ?: run {
            onError("Câmera não inicializada.")
            return null
        }

        if (currentRecording != null) {
            Log.w("CameraRecorder", "Já existe gravação em andamento")
            return null
        }

        val moviesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
        if (!moviesDir.exists()) moviesDir.mkdirs()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH'h'mm", Locale.getDefault())
        val dateString = dateFormat.format(Date())
        var videoFile = File(moviesDir, "evidencia_$dateString.mp4")

        // Se já existir no mesmo minuto, adiciona sufixo com segundos
        if (videoFile.exists()) {
            val secFormat = SimpleDateFormat("yyyy-MM-dd_HH'h'mm_ss", Locale.getDefault())
            videoFile = File(moviesDir, "evidencia_${secFormat.format(Date())}.mp4")
        }

        val outputOptions = FileOutputOptions.Builder(videoFile).build()

        var pendingRecording = capture.output.prepareRecording(context, outputOptions)

        try {
            pendingRecording = pendingRecording.withAudioEnabled()
        } catch (e: SecurityException) {
            Log.w("CameraRecorder", "Permissão de áudio não concedida para VideoCapture: ${e.message}")
        }

        var recordingStartTime = System.currentTimeMillis()

        currentRecording = pendingRecording.start(mainExecutor) { recordEvent ->
            when (recordEvent) {
                is VideoRecordEvent.Start -> {
                    _isRecording.value = true
                    _recordingDurationSec.value = 0
                    recordingStartTime = System.currentTimeMillis()
                }

                is VideoRecordEvent.Status -> {
                    val durationNanos = recordEvent.recordingStats.recordedDurationNanos
                    _recordingDurationSec.value = (durationNanos / 1_000_000_000L).toInt()
                }

                is VideoRecordEvent.Finalize -> {
                    _isRecording.value = false
                    _recordingDurationSec.value = 0
                    currentRecording = null

                    if (!recordEvent.hasError()) {
                        val durationMs = (recordEvent.recordingStats.recordedDurationNanos / 1_000_000L)
                            .coerceAtLeast(System.currentTimeMillis() - recordingStartTime)

                        // Gerar miniatura em cache
                        generateThumbnail(videoFile)

                        onFinished(videoFile, durationMs, 0f)
                    } else {
                        Log.e("CameraRecorder", "Erro na gravação: ${recordEvent.error}")
                        onError("Erro na gravação do vídeo: código ${recordEvent.error}")
                    }
                }
            }
        }

        return videoFile
    }

    fun stopRecording() {
        try {
            currentRecording?.stop()
        } catch (e: Exception) {
            Log.e("CameraRecorder", "Erro ao parar gravação: ${e.message}")
        }
    }

    private fun generateThumbnail(videoFile: File) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoFile.absolutePath)
            val bitmap = retriever.getFrameAtTime(500000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime()
            retriever.release()

            if (bitmap != null) {
                val thumbFile = File(videoFile.parentFile, "${videoFile.nameWithoutExtension}_thumb.jpg")
                FileOutputStream(thumbFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                }
            }
        } catch (e: Exception) {
            Log.w("CameraRecorder", "Não foi possível gerar miniatura: ${e.message}")
        }
    }

    fun unbind() {
        try {
            stopRecording()
            cameraProvider?.unbindAll()
        } catch (_: Exception) {}
    }
}
