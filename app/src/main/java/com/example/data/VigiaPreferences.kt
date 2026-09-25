package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VigiaPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("vigia_noturno_prefs", Context.MODE_PRIVATE)

    private val _thresholdDb = MutableStateFlow(prefs.getFloat(KEY_THRESHOLD_DB, 65f))
    val thresholdDb: StateFlow<Float> = _thresholdDb.asStateFlow()

    private val _silenceTimeoutSec = MutableStateFlow(prefs.getInt(KEY_SILENCE_TIMEOUT_SEC, 15))
    val silenceTimeoutSec: StateFlow<Int> = _silenceTimeoutSec.asStateFlow()

    private val _lensFacing = MutableStateFlow(prefs.getInt(KEY_LENS_FACING, 1)) // 1 = BACK, 0 = FRONT
    val lensFacing: StateFlow<Int> = _lensFacing.asStateFlow()

    private val _screenSaverEnabled = MutableStateFlow(prefs.getBoolean(KEY_SCREEN_SAVER, true))
    val screenSaverEnabled: StateFlow<Boolean> = _screenSaverEnabled.asStateFlow()

    fun setThresholdDb(value: Float) {
        val clamped = value.coerceIn(35f, 95f)
        prefs.edit().putFloat(KEY_THRESHOLD_DB, clamped).apply()
        _thresholdDb.value = clamped
    }

    fun setSilenceTimeoutSec(value: Int) {
        val clamped = value.coerceIn(5, 60)
        prefs.edit().putInt(KEY_SILENCE_TIMEOUT_SEC, clamped).apply()
        _silenceTimeoutSec.value = clamped
    }

    fun setLensFacing(value: Int) {
        prefs.edit().putInt(KEY_LENS_FACING, value).apply()
        _lensFacing.value = value
    }

    fun setScreenSaverEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_SCREEN_SAVER, value).apply()
        _screenSaverEnabled.value = value
    }

    companion object {
        private const val KEY_THRESHOLD_DB = "threshold_db"
        private const val KEY_SILENCE_TIMEOUT_SEC = "silence_timeout_sec"
        private const val KEY_LENS_FACING = "lens_facing"
        private const val KEY_SCREEN_SAVER = "screen_saver"
    }
}
