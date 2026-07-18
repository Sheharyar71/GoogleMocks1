package com.example

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.sip.SipService
import com.example.ui.MainScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val tag = "MainActivity"
    private val viewModel: MainViewModel by viewModels()

    private var sipService: SipService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? SipService.SipServiceBinder
            if (binder != null) {
                val serviceInstance = binder.getService()
                sipService = serviceInstance
                isBound = true
                Log.d(tag, "SipService bound successfully to Activity")
                
                // Supply SipManager to ViewModel
                viewModel.attachSipManager(serviceInstance.sipManager)
                
                // Process any incoming intent extras on startup
                handleLaunchIntent(intent)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            sipService = null
            isBound = false
            Log.d(tag, "SipService disconnected from Activity")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Start and bind the SIP listening background service
        val serviceIntent = Intent(this, SipService::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent(intent)
    }

    /**
     * Checks if the activity was launched via a Secure incoming call notification.
     */
    private fun handleLaunchIntent(launchIntent: Intent?) {
        if (launchIntent == null) return
        val launchTab = launchIntent.getStringExtra("LAUNCH_TAB")
        if (launchTab == "incoming_call") {
            val caller = launchIntent.getStringExtra("CALLER_NAME") ?: "Secure Coworker"
            Log.d(tag, "Activity launched via incoming secure call notification for $caller")
            // SipManager in service is already in RINGING state since it triggered the notification
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }
}
