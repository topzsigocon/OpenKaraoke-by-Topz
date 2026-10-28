package com.topz.karaoke

import android.app.Application

class KaraokeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashReporter.install(this)
    }
}
