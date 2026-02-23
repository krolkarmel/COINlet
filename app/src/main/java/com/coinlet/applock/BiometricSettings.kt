package com.coinlet.applock

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.coinlet.R
import com.coinlet.facelogin.FaceEnrollActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import java.io.File
import java.util.concurrent.Executor

class BiometricSettings : AppCompatActivity() {

    private lateinit var swFingerprint: SwitchMaterial
    private lateinit var swFace: SwitchMaterial

    private val lockPrefs by lazy { AppLockPrefs(this) }
    private val executor: Executor by lazy { ContextCompat.getMainExecutor(this) }

    private val enrollLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    private var ignoreChanges = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_biometric_settings)

        swFingerprint = findViewById(R.id.swFingerprint)
        swFace = findViewById(R.id.swFace)

        ignoreChanges = true
        swFingerprint.isChecked = lockPrefs.isFingerprintEnabled()
        swFace.isChecked = lockPrefs.isFaceEnabled()
        ignoreChanges = false

        swFingerprint.setOnCheckedChangeListener { _, isChecked ->
            if (ignoreChanges) return@setOnCheckedChangeListener

            if (!isChecked) {
                lockPrefs.setFingerprintEnabled(false)
                Toast.makeText(this, "Odcisk palca wyłączony", Toast.LENGTH_SHORT).show()
                return@setOnCheckedChangeListener
            }

            if (!canAuthenticate()) {
                ignoreChanges = true
                swFingerprint.isChecked = false
                ignoreChanges = false
                openSecurityOrEnroll()
                return@setOnCheckedChangeListener
            }

            showBiometricPrompt(
                onSuccess = {
                    lockPrefs.setFingerprintEnabled(true)
                    Toast.makeText(this, "Odcisk palca włączony", Toast.LENGTH_SHORT).show()
                },
                onFail = {
                    ignoreChanges = true
                    swFingerprint.isChecked = false
                    ignoreChanges = false
                    lockPrefs.setFingerprintEnabled(false)
                }
            )
        }

        swFace.setOnCheckedChangeListener { _, isChecked ->
            if (ignoreChanges) return@setOnCheckedChangeListener

            if (!isChecked) {
                lockPrefs.setFaceEnabled(false)
                Toast.makeText(this, "Rozpoznawanie twarzy wyłączone", Toast.LENGTH_SHORT).show()
                return@setOnCheckedChangeListener
            }

            if (!isDeviceSecure()) {
                ignoreChanges = true
                swFace.isChecked = false
                ignoreChanges = false
                lockPrefs.setFaceEnabled(false)
                openSecurityOrEnroll()
                return@setOnCheckedChangeListener
            }

            if (!canAuthenticate()) {
                ignoreChanges = true
                swFace.isChecked = false
                ignoreChanges = false
                lockPrefs.setFaceEnabled(false)
                openSecurityOrEnroll()
                return@setOnCheckedChangeListener
            }

            showBiometricPrompt(
                onSuccess = {
                    lockPrefs.setFaceEnabled(true)
                    Toast.makeText(this, "Rozpoznawanie twarzy włączone", Toast.LENGTH_SHORT).show()

                    // jeśli brak wzorca -> od razu rejestracja
                    if (!hasFaceModel()) {
                        Toast.makeText(this, "Zarejestruj twarz, aby dokończyć konfigurację.", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, FaceEnrollActivity::class.java))
                    }
                },
                onFail = {
                    ignoreChanges = true
                    swFace.isChecked = false
                    ignoreChanges = false
                    lockPrefs.setFaceEnabled(false)
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()

        if (lockPrefs.isFaceEnabled() && !hasFaceModel()) {
            ignoreChanges = true
            swFace.isChecked = false
            ignoreChanges = false

            lockPrefs.setFaceEnabled(false)
            Toast.makeText(this, "Nie zarejestrowano twarzy – logowanie twarzą wyłączone.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hasFaceModel(): Boolean = File(filesDir, "lbph_model.yml").exists()

    private fun canAuthenticate(): Boolean {
        val bm = BiometricManager.from(this)
        val result = bm.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        return result == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun showBiometricPrompt(onSuccess: () -> Unit, onFail: () -> Unit) {
        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onFail()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFail()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Potwierdź biometrię")
            .setSubtitle("Włączanie zabezpieczenia biometrycznego")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun isDeviceSecure(): Boolean {
        val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return km.isDeviceSecure
    }

    private fun openSecurityOrEnroll() {
        Toast.makeText(
            this,
            "Dodaj blokadę ekranu lub biometrię w ustawieniach telefonu.",
            Toast.LENGTH_LONG
        ).show()

        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                putExtra(
                    Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                            BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }

        enrollLauncher.launch(intent)
    }
}