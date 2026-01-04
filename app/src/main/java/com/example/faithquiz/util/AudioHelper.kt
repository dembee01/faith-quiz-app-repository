package com.example.faithquiz.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object AudioHelper {
    private var toneGenerator: ToneGenerator? = null
    
    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playCorrect() {
        // High pitch happy tone
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
    }

    fun playWrong() {
        // Low pitch error tone
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 200)
    }

    fun playSelect() {
        // Short click
        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_PIP, 50)
    }
    
    fun playLevelComplete() {
         toneGenerator?.startTone(ToneGenerator.TONE_DTMF_0, 100) // Placeholder melody
    }

    fun vibrateSuccess(context: Context) {
        try {
            val vibrator = getVibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 50, 50, 100), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(200)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun vibrateError(context: Context) {
        try {
            val vibrator = getVibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(300)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun vibrateClick(context: Context) {
        try {
            val vibrator = getVibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(20, 50))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(20)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getVibrator(context: Context): Vibrator? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private var mediaPlayer: android.media.MediaPlayer? = null
    private var currentAmbienceId: Int? = null

    fun playAmbience(context: Context, resourceId: Int) {
        if (currentAmbienceId == resourceId && mediaPlayer?.isPlaying == true) return
        
        stopAmbience()
        
        try {
            mediaPlayer = android.media.MediaPlayer.create(context, resourceId)
            mediaPlayer?.isLooping = true
            mediaPlayer?.setVolume(0.3f, 0.3f) // Lower volume for background
            mediaPlayer?.start()
            currentAmbienceId = resourceId
        } catch (e: Exception) {
            e.printStackTrace()
            // Resource might not exist
        }
    }
    
    fun stopAmbience() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            currentAmbienceId = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
        stopAmbience()
    }
}
