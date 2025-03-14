package com.nhanc18.personal.step.counter

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class StepCounterService : Service(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var isSensorRegistered = false

    companion object {
        private const val TAG = "step-counter"
        private const val ACTION_STOP_SERVICE = "com.ee.fitness.STOP_SERVICE"
    }

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val attributionContext = createAttributionContext("stepCounter")
            sensorManager = attributionContext.getSystemService(SENSOR_SERVICE) as SensorManager
        } else {
            sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        }

        StepData.loadInitialSteps(this)
        setupForegroundNotification()
        registerSensorIfPermitted()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
//            StepData.updateSteps(it.values[0].toInt())
            StepData.addOneStep()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopForeground(true)
            stopSelf()
            StepData.saveSession(this)
            Log.w(TAG, "Service stopped by user")
            return START_NOT_STICKY
        }
        registerSensorIfPermitted()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isSensorRegistered) {
            sensorManager.unregisterListener(this)
            isSensorRegistered = false
        }
        StepData.saveSession(this)
    }

    private fun setupForegroundNotification() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    "step_channel",
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = getString(R.string.notification_channel_desc)
                }
                val notificationManager =
                    getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }

            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent =
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

            val stopIntent = Intent(this, StepCounterService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            val stopPendingIntent =
                PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE)

            val notification = NotificationCompat.Builder(this, "step_channel")
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_text))
                .setSmallIcon(R.drawable.ic_notification_step_counter)
                .setContentIntent(pendingIntent)
                .addAction(
                    R.drawable.ic_stop,
                    getString(R.string.notification_stop),
                    stopPendingIntent
                )
                .setAutoCancel(false)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
            } else {
                startForeground(1, notification)
            }
            Log.d(TAG, "Foreground notification set up")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up notification: ${e.message}")
            stopSelf()
            StepData.saveSession(this)
        }
    }

    private fun registerSensorIfPermitted() {
        val isApiLowerThan29 = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
        if (isApiLowerThan29 || ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            if (stepSensor != null && !isSensorRegistered) {
                sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
                isSensorRegistered = true
                Log.d(TAG, "Step sensor registered")
            } else if (stepSensor == null) {
                Log.w(TAG, "Step sensor not available")
                stopSelf()
                StepData.saveSession(this)
            }
        } else {
            Log.w(TAG, "ACTIVITY_RECOGNITION permission not granted")
            stopForeground(true) // Dừng foreground nhưng không kill service
            stopSelf() // Dừng service để chờ khởi động lại
            StepData.saveSession(this)
        }
    }
}