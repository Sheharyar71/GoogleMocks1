package com.example.sip

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.AppRepository

class SipService : Service() {

    private val tag = "SipService"
    private val binder = SipServiceBinder()
    
    private lateinit var repository: AppRepository
    lateinit var sipManager: SipManager
        private set

    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "SIP background service initialized")
        val database = AppDatabase.getDatabase(this)
        repository = AppRepository(database)
        sipManager = SipManager.getInstance(this, repository)
        sipManager.startSipServer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(tag, "SIP background service started command")
        // Keep the service running even if the system kills it temporarily
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    inner class SipServiceBinder : Binder() {
        fun getService(): SipService = this@SipService
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(tag, "SIP service destroyed")
    }
}
