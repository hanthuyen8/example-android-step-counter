package com.nhanc18.personal.step.counter

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity

class PermissionHelper(private val activity: ComponentActivity) {

    private val permissionRationales = mutableMapOf<String, String>()
    private val requestCounts = mutableMapOf<String, Int>()
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var settingsLauncher: ActivityResultLauncher<Intent>
    private var onResultCallback: ((PermissionResult) -> Unit)? = null
    private var pendingPermissions: List<PermissionRequired>? = null
    private val MAX_REQUEST_ATTEMPTS = 2

    data class PermissionResult(
        val allGranted: Boolean,
        val deniedPermissions: List<String>,
        val permanentlyDeniedPermissions: List<String>
    )

    init {
        requestPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            handlePermissionResult(permissions)
        }

        settingsLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            pendingPermissions?.let { permissions ->
                val result = checkPermissions(permissions)
                onResultCallback?.invoke(result)
                pendingPermissions = null
            }
        }
    }

    fun checkPermissions(permissions: List<PermissionRequired>): PermissionResult {
        val applicablePermissions = permissions.filter { Build.VERSION.SDK_INT >= it.minApi }
        val denied = applicablePermissions.filter {
            ContextCompat.checkSelfPermission(
                activity,
                it.permission
            ) != PackageManager.PERMISSION_GRANTED
        }
        val permanentlyDenied = denied.filter {
            !activity.shouldShowRequestPermissionRationale(it.permission)
        }
        return PermissionResult(
            allGranted = denied.isEmpty(),
            deniedPermissions = denied.map { it.permission },
            permanentlyDeniedPermissions = permanentlyDenied.map { it.permission }
        )
    }

    fun requestPermissions(
        permissions: List<PermissionRequired>,
        onResult: (PermissionResult) -> Unit
    ) {
        val applicablePermissions =
            permissions.filter { Build.VERSION.SDK_INT >= it.minApi }
        applicablePermissions.forEach {
            permissionRationales[it.permission] = it.desc
            requestCounts[it.permission] = requestCounts[it.permission] ?: 0
        }

        onResultCallback = onResult
        pendingPermissions = applicablePermissions

        val permissionsToRequest = applicablePermissions.filter {
            Build.VERSION.SDK_INT >= it.minApi &&
                    ContextCompat.checkSelfPermission(
                        activity,
                        it.permission
                    ) != PackageManager.PERMISSION_GRANTED &&
                    (requestCounts[it.permission] ?: 0) < MAX_REQUEST_ATTEMPTS
        }

        when {
            permissionsToRequest.isEmpty() -> {
                val result = checkPermissions(applicablePermissions)
                onResultCallback?.invoke(result)
            }

            permissionsToRequest.any { activity.shouldShowRequestPermissionRationale(it.permission) } -> {
                showRationaleDialog(permissionsToRequest)
            }

            else -> {
                permissionsToRequest.forEach {
                    requestCounts[it.permission] = (requestCounts[it.permission] ?: 0) + 1
                }
                requestPermissionLauncher.launch(permissionsToRequest.map { it.permission }
                    .toTypedArray())
            }
        }
    }

    private fun handlePermissionResult(permissions: Map<String, Boolean>) {
        val denied = permissions.filter { !it.value }.keys
        val permanentlyDenied = denied.filter {
            !activity.shouldShowRequestPermissionRationale(it)
        }

        val result = PermissionResult(
            allGranted = denied.isEmpty(),
            deniedPermissions = denied.toList(),
            permanentlyDeniedPermissions = permanentlyDenied
        )

        when {
            result.allGranted -> {
                onResultCallback?.invoke(result)
            }

            permanentlyDenied.isNotEmpty() -> {
                showSettingsDialog()
            }

            else -> {
                val deniedPermissions = permissions.filter { !it.value }.keys.map { permission ->
                    PermissionRequired(
                        permission,
                        permissionRationales[permission]
                            ?: activity.getString(R.string.permission_general),
                        0
                    )
                }
                showRationaleDialog(deniedPermissions)
            }
        }
    }

    private fun showRationaleDialog(permissions: List<PermissionRequired>) {
        AlertDialog.Builder(activity)
            .setTitle(R.string.request_permission_title)
            .setMessage(permissions.joinToString("\n") { it.desc })
            .setPositiveButton(R.string.positive_button) { _, _ ->
                permissions.forEach {
                    requestCounts[it.permission] = (requestCounts[it.permission] ?: 0) + 1
                }
                requestPermissionLauncher.launch(permissions.map { it.permission }.toTypedArray())
            }
            .setNegativeButton(R.string.negative_button) { dialog, _ ->
                dialog.dismiss()
                onResultCallback?.invoke(checkPermissions(permissions))
            }
            .setCancelable(false)
            .show()
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(activity)
            .setTitle(R.string.request_permission_title)
            .setMessage(R.string.settings_dialog_message)
            .setPositiveButton(R.string.open_settings) { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", activity.packageName, null)
                }
                settingsLauncher.launch(intent)
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
                pendingPermissions?.let { perms ->
                    onResultCallback?.invoke(checkPermissions(perms))
                }
            }
            .setCancelable(false)
            .show()
    }
}

data class PermissionRequired(
    val permission: String,
    val desc: String,
    val minApi: Int = 0
)