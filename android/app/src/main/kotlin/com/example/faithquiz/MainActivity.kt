package com.example.faithquiz

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val feedbackChannel = "com.example.faithquiz/answer_feedback"
    private val tones by lazy { ToneGenerator(AudioManager.STREAM_MUSIC, 85) }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, feedbackChannel)
            .setMethodCallHandler { call, result ->
                if (call.method != "play") {
                    result.notImplemented()
                    return@setMethodCallHandler
                }
                when (call.argument<String>("result")) {
                    "correct" -> playCorrectFeedback()
                    "incorrect" -> playIncorrectFeedback()
                    else -> {
                        result.error("invalid_feedback", "Unknown answer feedback type.", null)
                        return@setMethodCallHandler
                    }
                }
                result.success(null)
            }
    }

    private fun playCorrectFeedback() {
        tones.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        vibrate(longArrayOf(0, 45, 55, 85))
    }

    private fun playIncorrectFeedback() {
        tones.startTone(ToneGenerator.TONE_PROP_NACK, 180)
        vibrate(longArrayOf(0, 180))
    }

    private fun vibrate(pattern: LongArray) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return

        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    override fun onDestroy() {
        tones.release()
        super.onDestroy()
    }
}
