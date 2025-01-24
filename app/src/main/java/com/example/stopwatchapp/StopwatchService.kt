package com.example.stopwatchapp

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.os.*
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class StopwatchService : Service() {

    private val binder = LocalBinder()
    private var isRunning = false
    private var elapsedTime: Long = 0
    private var startTime: Long = 0
    private lateinit var handler: Handler
    private lateinit var notificationManager: NotificationManager

    // LiveData to update time in MainActivity
    private val _timeLiveData = MutableLiveData<String>()
    val timeLiveData: LiveData<String> get() = _timeLiveData

    inner class LocalBinder : Binder() {
        fun getService(): StopwatchService = this@StopwatchService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "STOPWATCH_CHANNEL",
                "Stopwatch Notifications",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Channel for Stopwatch Notifications"
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        handler = Handler(Looper.getMainLooper())
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        startForeground(1, createNotification(formatTime(elapsedTime)))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.getStringExtra("action")) {
            "START" -> startStopwatch()
            "STOP" -> stopStopwatch()
            "RESET" -> resetStopwatch()
            "EXIT" -> stopForegroundService()
        }
        return START_STICKY
    }

    fun startStopwatch() {
        if (!isRunning) {
            isRunning = true
            startTime = SystemClock.elapsedRealtime() - elapsedTime
            handler.post(updateRunnable)
            updateNotification(formatTime(elapsedTime)) // Update notification when started
        }
    }

    fun stopStopwatch() {
        if (isRunning) {
            isRunning = false
            elapsedTime = SystemClock.elapsedRealtime() - startTime
            handler.removeCallbacks(updateRunnable)
            updateNotification(formatTime(elapsedTime)) // Update notification when stopped
        }
    }

    fun resetStopwatch() {
        stopStopwatch()
        elapsedTime = 0
        _timeLiveData.postValue(formatTime(elapsedTime))
        updateNotification(formatTime(elapsedTime))
    }

    fun stopForegroundService() {
        stopStopwatch()
        stopForeground(true)
        stopSelf()
    }

    private val updateRunnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                elapsedTime = SystemClock.elapsedRealtime() - startTime
                _timeLiveData.postValue(formatTime(elapsedTime))  // Update LiveData
                updateNotification(formatTime(elapsedTime))
                handler.postDelayed(this, 1000)
            }
        }
    }

    @SuppressLint("NotificationPermission")
    private fun updateNotification(time: String) {
        val notification = createNotification(time)
        notificationManager.notify(1, notification)
    }

    private fun createNotification(time: String): Notification {
        // Create intents for action buttons
        val startIntent = Intent(this, StopwatchService::class.java).apply {
            putExtra("action", "START")
        }
        val stopIntent = Intent(this, StopwatchService::class.java).apply {
            putExtra("action", "STOP")
        }
        val resetIntent = Intent(this, StopwatchService::class.java).apply {
            putExtra("action", "RESET")
        }
        val exitIntent = Intent(this, StopwatchService::class.java).apply {
            putExtra("action", "EXIT")
        }

        val startPendingIntent = PendingIntent.getService(this, 1, startIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        val stopPendingIntent = PendingIntent.getService(this, 2, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        val resetPendingIntent = PendingIntent.getService(this, 3, resetIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        val exitPendingIntent = PendingIntent.getService(this, 4, exitIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)

        // Create RemoteViews for collapsed and expanded notifications
        val collapsedView = RemoteViews(packageName, R.layout.custom_notification_collapsed)
        collapsedView.setTextViewText(R.id.notification_time, time)

        val expandedView = RemoteViews(packageName, R.layout.custom_notification_expanded)
        expandedView.setTextViewText(R.id.notification_time, time)
        expandedView.setOnClickPendingIntent(R.id.button_start, startPendingIntent)
        expandedView.setOnClickPendingIntent(R.id.button_stop, stopPendingIntent)
        expandedView.setOnClickPendingIntent(R.id.button_reset, resetPendingIntent)
        expandedView.setOnClickPendingIntent(R.id.button_exit, exitPendingIntent)

        return NotificationCompat.Builder(this, "STOPWATCH_CHANNEL")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContent(collapsedView) // Set the collapsed view
            .setCustomBigContentView(expandedView) // Set the expanded view
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle()) // Use custom view style
            .build()
    }

    private fun formatTime(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val hours = (millis / (1000 * 60 * 60))
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}