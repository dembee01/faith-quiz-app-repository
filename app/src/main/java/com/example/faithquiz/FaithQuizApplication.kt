package com.example.faithquiz

import android.app.Application
import com.example.faithquiz.data.store.ProgressDataStore
import com.example.faithquiz.util.CrashReporter
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@HiltAndroidApp
class FaithQuizApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            ProgressDataStore.observeCrashReportingEnabled(this@FaithQuizApplication)
                .distinctUntilChanged()
                .collect { enabled ->
                    CrashReporter.applyConsent(this@FaithQuizApplication, enabled)
                }
        }
    }
}
