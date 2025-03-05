package com.nhanc18.personal.step.counter

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class StepCounterService : Service(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var initialSteps = -1f

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val attributionContext = createAttributionContext("stepCounter")
            sensorManager = attributionContext.getSystemService(SENSOR_SERVICE) as SensorManager
        } else {
            sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        }
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)

        // Khôi phục initialSteps từ SharedPreferences nếu có
        val savedInitialSteps = StepData.loadInitialSteps(this)
        StepData.setInitialSteps(savedInitialSteps)

        // Tạo kênh thông báo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "step_channel",
                "Step Counter",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Channel cho ứng dụng đếm bước chân"
            }
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, "step_channel")
            .setContentTitle("Đang đếm bước chân")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
        } else {
            startForeground(1, notification)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            // Chỉ reset initialSteps nếu chưa có giá trị hợp lệ (ví dụ sau reboot)
            if (StepData.getInitialSteps() < 0) {
                StepData.setInitialSteps(it.values[0])
            }
            val currentSteps = (it.values[0] - StepData.getInitialSteps()).toInt()
            StepData.updateSteps(currentSteps)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        StepData.saveSession(this)
        sensorManager.unregisterListener(this)
        super.onDestroy()
    }
}