package com.nhanc18.personal.step.counter

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

object StepData {
    private val _steps = mutableStateOf(0)
    private val _distance = mutableStateOf(0f)
    private val _error = mutableStateOf<String?>(null)
    private var initialSteps = -1f

    // Getter cho Compose
    val steps: State<Int> = _steps
    val distance: State<Float> = _distance
    val error: State<String?> = _error

    fun updateSteps(newSteps: Int) {
        _steps.value = newSteps
        _distance.value = newSteps * 0.762f
    }

    fun setInitialSteps(value: Float) { initialSteps = value }
    fun getInitialSteps(): Float = initialSteps

    // Getter cho logic
    fun getStepsValue(): Int = _steps.value
    fun getDistanceValue(): Float = _distance.value
    fun setError(message: String?) { _error.value = message }
    fun getErrorValue(): String? = _error.value

    fun saveSession(context: Context) {
        val prefs = context.getSharedPreferences("StepPrefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt("last_steps", _steps.value)
            putFloat("last_distance", _distance.value)
            putLong("last_timestamp", System.currentTimeMillis())
            putFloat("initial_steps", initialSteps)
            apply()
        }
    }

    fun getStepsSinceLast(context: Context): Int {
        val prefs = context.getSharedPreferences("StepPrefs", Context.MODE_PRIVATE)
        val lastSteps = prefs.getInt("last_steps", 0)
        return _steps.value - lastSteps
    }

    fun loadInitialSteps(context: Context): Float {
        val prefs = context.getSharedPreferences("StepPrefs", Context.MODE_PRIVATE)
        return prefs.getFloat("initial_steps", -1f)
    }
}