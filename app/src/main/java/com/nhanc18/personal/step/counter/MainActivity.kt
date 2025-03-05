package com.nhanc18.personal.step.counter

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startService(Intent(this, StepCounterService::class.java))
            } else {
                StepData.setError("Không đủ quyền để đếm bước chân.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                StepCounterScreen()
            }
        }

        // Kiểm tra và yêu cầu quyền sau khi UI hiển thị
        if (checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            startService(Intent(this, StepCounterService::class.java))
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                permissionLauncher.launch(android.Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }
    }

    override fun onStop() {
        StepData.saveSession(this)
        super.onStop()
    }
}

@Composable
fun StepCounterScreen() {
    val context = LocalContext.current
    val steps by StepData.steps // Đọc trực tiếp từ State
    val distance by StepData.distance
    val error by StepData.error

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Tổng số bước: $steps", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Khoảng cách: %.2f mét".format(distance),
            style = MaterialTheme.typography.headlineSmall
        )
        error?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            val stepsSinceLast = StepData.getStepsSinceLast(context)
            Toast.makeText(context, "Số bước từ lần cuối: $stepsSinceLast", Toast.LENGTH_SHORT)
                .show()
        }) {
            Text("Hiện số bước từ lần cuối")
        }
    }
}