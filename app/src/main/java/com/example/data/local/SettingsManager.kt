package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("smart_notes_settings", Context.MODE_PRIVATE)

    // Flows for visual updates
    private val _themeFlow = MutableStateFlow(getThemeSetting())
    val themeFlow: StateFlow<String> = _themeFlow

    private val _onboardingCompletedFlow = MutableStateFlow(isOnboardingCompleted())
    val onboardingCompletedFlow: StateFlow<Boolean> = _onboardingCompletedFlow

    private val _userEmailFlow = MutableStateFlow(getUserEmail())
    val userEmailFlow: StateFlow<String?> = _userEmailFlow

    private val _pinCodeFlow = MutableStateFlow(getPinCode())
    val pinCodeFlow: StateFlow<String?> = _pinCodeFlow

    private val _isLockedFlow = MutableStateFlow(isLockEnabled())
    val isLockedFlow: StateFlow<Boolean> = _isLockedFlow

    private val _fontSizeFlow = MutableStateFlow(getFontSizeScale())
    val fontSizeFlow: StateFlow<Float> = _fontSizeFlow

    private val _lastSyncTimeFlow = MutableStateFlow(getLastSyncTime())
    val lastSyncTimeFlow: StateFlow<Long> = _lastSyncTimeFlow

    fun getThemeSetting(): String {
        return prefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM"
    }

    fun setThemeSetting(theme: String) {
        prefs.edit().putString("theme_mode", theme).apply()
        _themeFlow.value = theme
    }

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean("onboarding_completed", false)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean("onboarding_completed", completed).apply()
        _onboardingCompletedFlow.value = completed
    }

    fun getUserEmail(): String? {
        return prefs.getString("user_email", null)
    }

    fun setUserEmail(email: String?) {
        prefs.edit().putString("user_email", email).apply()
        _userEmailFlow.value = email
    }

    fun getPinCode(): String? {
        return prefs.getString("pin_code", null)
    }

    fun setPinCode(pin: String?) {
        prefs.edit().putString("pin_code", pin).apply()
        _pinCodeFlow.value = pin
    }

    fun isLockEnabled(): Boolean {
        return prefs.getBoolean("lock_enabled", false)
    }

    fun setLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("lock_enabled", enabled).apply()
        _isLockedFlow.value = enabled
    }

    fun getFontSizeScale(): Float {
        return prefs.getFloat("font_size_scale", 1.0f)
    }

    fun setFontSizeScale(scale: Float) {
        prefs.edit().putFloat("font_size_scale", scale).apply()
        _fontSizeFlow.value = scale
    }

    fun getLastSyncTime(): Long {
        return prefs.getLong("last_sync_time", 0L)
    }

    fun setLastSyncTime(time: Long) {
        prefs.edit().putLong("last_sync_time", time).apply()
        _lastSyncTimeFlow.value = time
    }
}
