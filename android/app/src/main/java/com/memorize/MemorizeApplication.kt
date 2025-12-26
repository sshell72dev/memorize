package com.memorize

import android.app.Application
import android.util.Log

class MemorizeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("Memorize", "MemorizeApplication.onCreate called")
    }
    
    init {
        Log.d("Memorize", "MemorizeApplication class loaded")
    }
}

