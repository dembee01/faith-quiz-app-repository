package com.example.faithquiz.util

import android.app.Application
import com.example.faithquiz.BuildConfig
import io.sentry.Sentry
import io.sentry.android.core.SentryAndroid

object CrashReporter {
    val isConfigured: Boolean
        get() = BuildConfig.SENTRY_DSN.isNotBlank()

    @Synchronized
    fun applyConsent(application: Application, enabled: Boolean) {
        if (!enabled || !isConfigured) {
            Sentry.close()
            return
        }

        if (!Sentry.isEnabled()) {
            SentryAndroid.init(application) { options ->
                options.dsn = BuildConfig.SENTRY_DSN
                options.isSendDefaultPii = false
                options.isEnableAutoSessionTracking = true
                options.tracesSampleRate = 0.0
                options.environment = if (BuildConfig.DEBUG) "debug" else "production"
            }
        }
    }
}
