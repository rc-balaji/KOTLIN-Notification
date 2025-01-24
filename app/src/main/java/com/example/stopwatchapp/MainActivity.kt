package com.example.stopwatchapp

import android.content.*
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.Observer
import com.example.stopwatchapp.ui.theme.StopwatchAppTheme

class MainActivity : ComponentActivity() {

    private var stopwatchService: StopwatchService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as StopwatchService.LocalBinder
            stopwatchService = binder.getService()
            isBound = true
            // Observe LiveData for time updates
            stopwatchService?.timeLiveData?.observe(this@MainActivity, Observer { time ->
                // Update timeText in state when the service sends new time
                stopwatchTime.value = time
            })
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    // Use mutableStateOf to hold the time state
    private var stopwatchTime = mutableStateOf("00:00:00")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startStopwatchService()

        // Use `setContent` to set up the UI
        setContent {
            StopwatchAppTheme {
                // Make sure the StopwatchScreen composable is called inside `setContent`
                StopwatchScreen(
                    onStart = { stopwatchService?.startStopwatch() },
                    onStop = { stopwatchService?.stopStopwatch() },
                    onReset = { stopwatchService?.resetStopwatch() },
                    onExit = {
                        stopwatchService?.stopForegroundService()
                        finish()
                    },
                    time = stopwatchTime.value // Pass the time to the screen composable
                )
            }
        }
    }

    private fun startStopwatchService() {
        val intent = Intent(this, StopwatchService::class.java)
        startService(intent)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }
}