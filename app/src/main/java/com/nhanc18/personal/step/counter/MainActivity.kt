package com.nhanc18.personal.step.counter

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
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
    private lateinit var permissionHelper: PermissionHelper

    private val requiredPermissions = mutableListOf<PermissionRequired>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requiredPermissions.add(
            PermissionRequired(
                android.Manifest.permission.ACTIVITY_RECOGNITION,
                getString(R.string.permission_activity_recognition),
                29
            )
        )
        requiredPermissions.add(
            PermissionRequired(
                android.Manifest.permission.POST_NOTIFICATIONS,
                getString(R.string.permission_post_notifications),
                33
            )
        )
        permissionHelper = PermissionHelper(this)

        setContent {
            MaterialTheme {
                StepCounterScreen {
                    requestPermissions()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Kiểm tra lại quyền khi Activity resumed (sau khi từ Settings về hoặc bất kỳ lúc nào)
        val result = permissionHelper.checkPermissions(requiredPermissions)
        if (!result.allGranted) {
            onNotEnoughPermission()
            requestPermissions()
        }
    }

    override fun onStop() {
        StepData.saveSession(this)
        super.onStop()
    }

    private fun onNotEnoughPermission() {
        StepData.setError("Không đủ quyền để đếm bước chân.")
    }

    private fun onAllPermissionGranted() {
        startService(Intent(this, StepCounterService::class.java))
        StepData.setError(null)
    }

    private fun requestPermissions() {
        val status = permissionHelper.checkPermissions(
            requiredPermissions
        )
        if (status.allGranted) {
            onAllPermissionGranted()
        } else {
            onNotEnoughPermission()

            permissionHelper.requestPermissions(
                requiredPermissions
            ) { result ->
                when {
                    result.allGranted -> {
                        onAllPermissionGranted()
                    }

                    result.permanentlyDeniedPermissions.isNotEmpty() -> {
                        // quyền bị denied vĩnh viễn
                        onNotEnoughPermission()
                    }

                    else -> {
                        // quyền bị denied mới 1 lần
                        onNotEnoughPermission()
                    }
                }
            }
        }
    }
}

@Composable
fun StepCounterScreen(
    onAskPermissionRequest: () -> Unit
) {
    val context = LocalContext.current
    val steps by StepData.steps
    val initialSteps by StepData.initialSteps
    val error by StepData.error

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Đang bước: $steps", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Khoảng cách: %.2f mét".format(steps * 0.6),
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Tổng số đã bước: ${initialSteps + steps}",
            style = MaterialTheme.typography.headlineSmall
        )
        error?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onAskPermissionRequest
        ) {
            Text("Cấp quyền")
        }
    }
}