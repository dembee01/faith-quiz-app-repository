package com.example.faithquiz

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FaithQuizApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("FaithQuizApplication", "Application started without Hilt")
    }
}
