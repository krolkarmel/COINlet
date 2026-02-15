package com.coinlet.applock

import android.content.Context

class AppLockPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("applock_prefs", Context.MODE_PRIVATE)

    fun isPinSet(): Boolean = getPinHash() != null

    fun savePinHashAndSalt(hashB64: String, saltB64: String) {
        prefs.edit()
            .putString(KEY_PIN_HASH, hashB64)
            .putString(KEY_PIN_SALT, saltB64)
            .apply()
    }

    fun clearPin() {
        prefs.edit()
            .remove(KEY_PIN_HASH)
            .remove(KEY_PIN_SALT)
            .apply()
    }

    fun getPinHash(): String? = prefs.getString(KEY_PIN_HASH, null)
    fun getPinSalt(): String? = prefs.getString(KEY_PIN_SALT, null)

    fun isBiometricsEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRICS_ENABLED, false)
    fun setBiometricsEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRICS_ENABLED, value).apply()
    }

    companion object {
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_BIOMETRICS_ENABLED = "biometrics_enabled"
    }
}

