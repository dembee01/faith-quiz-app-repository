package com.example.faithquiz

import android.app.Application

class FaithQuizApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("FaithQuizApplication", "Application started without Hilt")
    }
}
