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

    fun isFingerprintEnabled(): Boolean = prefs.getBoolean(KEY_FINGERPRINT_ENABLED, false)
    fun setFingerprintEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_FINGERPRINT_ENABLED, value).apply()
        syncLegacyBiometricsFlag()
    }

    fun isFaceEnabled(): Boolean = prefs.getBoolean(KEY_FACE_ENABLED, false)
    fun setFaceEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_FACE_ENABLED, value).apply()
        syncLegacyBiometricsFlag()
    }

    fun isBiometricsEnabled(): Boolean = isFingerprintEnabled() || isFaceEnabled()

    fun setBiometricsEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRICS_ENABLED, value).apply()
        if (value) {
            prefs.edit()
                .putBoolean(KEY_FINGERPRINT_ENABLED, true)
                .putBoolean(KEY_FACE_ENABLED, true)
                .apply()
        } else {
            prefs.edit()
                .putBoolean(KEY_FINGERPRINT_ENABLED, false)
                .putBoolean(KEY_FACE_ENABLED, false)
                .apply()
        }
    }

    private fun syncLegacyBiometricsFlag() {
        prefs.edit()
            .putBoolean(KEY_BIOMETRICS_ENABLED, isFingerprintEnabled() || isFaceEnabled())
            .apply()
    }

    companion object {
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"

        private const val KEY_BIOMETRICS_ENABLED = "biometrics_enabled" // legacy
        private const val KEY_FINGERPRINT_ENABLED = "fingerprint_enabled"
        private const val KEY_FACE_ENABLED = "face_enabled"
    }
}