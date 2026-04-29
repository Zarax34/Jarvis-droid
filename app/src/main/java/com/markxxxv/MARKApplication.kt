package com.markxxxv

import android.app.Application

class MARKApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
    
    companion object {
        lateinit var instance: MARKApplication
            private set
    }
}