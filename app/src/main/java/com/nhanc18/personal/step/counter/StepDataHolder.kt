package com.nhanc18.personal.step.counter

import android.content.Context
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf

object StepData {
    private val _steps = mutableIntStateOf(0)
    private val _initialSteps = mutableIntStateOf(0)
    private val _error = mutableStateOf<String?>(null)
    private var _loaded = false

    private const val KEY = "StepPrefs"
    private const val KEY_LAST_TIMESTAMP = "last_timestamp"
    private const val KEY_INITIAL_STEPS = "initial_steps"

    // Getter cho Compose
    val steps: State<Int> = _steps
    val initialSteps: State<Int> = _initialSteps
    val error: State<String?> = _error

    fun updateSteps(newSteps: Int) {
        _steps.intValue = newSteps
    }

    fun addOneStep() {
        _steps.value += 1
    }

    // Getter cho logic
    fun getStepsValue(): Int = _steps.intValue
    fun setError(message: String?) {
        _error.value = message
    }

    fun getErrorValue(): String? = _error.value

    fun saveSession(context: Context) {
        if (!_loaded) {
            return
        }
        val prefs = context.getSharedPreferences(KEY, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putLong(KEY_LAST_TIMESTAMP, System.currentTimeMillis())
            putInt(KEY_INITIAL_STEPS, _steps.intValue + _initialSteps.intValue)
            apply()
        }
        Log.d(
            "step-counter",
            "Saved session with steps: ${_steps.intValue + _initialSteps.intValue}"
        )
    }

    fun loadInitialSteps(context: Context) {
        if (_loaded) {
            return
        }
        val prefs = context.getSharedPreferences(KEY, Context.MODE_PRIVATE)
        val num = prefs.getInt(KEY_INITIAL_STEPS, 0)
        Log.d("step-counter", "Loaded initial steps: $num")
        _initialSteps.intValue = num
        _loaded = true
    }
}