package com.coinlet.applock

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.coinlet.R
import com.coinlet.app.Dashboard
import com.coinlet.databinding.ActivityLockBinding
import com.coinlet.facelogin.FaceEnrollMfnActivity
import com.coinlet.facelogin.FaceLoginMfnActivity
import java.io.File
import java.util.concurrent.Executor

class LockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLockBinding
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    private lateinit var pin1: EditText
    private lateinit var pin2: EditText
    private lateinit var pin3: EditText
    private lateinit var pin4: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityLockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.lockRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        pin1 = binding.pin1
        pin2 = binding.pin2
        pin3 = binding.pin3
        pin4 = binding.pin4
        addTextChangeListener()

        executor = ContextCompat.getMainExecutor(this)

        biometricPrompt = BiometricPrompt(
            this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext, "Błąd: $errString", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    startActivity(Intent(this@LockActivity, Dashboard::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    finish()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Nieudana autoryzacja", Toast.LENGTH_SHORT).show()
                }
            }
        )

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Odblokowanie biometryczne")
            .setSubtitle("Użyj odcisku / blokady telefonu")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        val prefs = AppLockPrefs(this)

        // ====== PIN ======
        binding.btnUnlock.setOnClickListener {
            val typedPin = getTypedPin()

            if (typedPin.length != 4) {
                binding.errorTextView.visibility = View.VISIBLE
                binding.errorTextView.text = "Wpisz 4 cyfry PIN."
                return@setOnClickListener
            }

            val storedHashB64 = prefs.getPinHash()
            val storedSaltB64 = prefs.getPinSalt()

            if (storedHashB64 == null || storedSaltB64 == null) {
                binding.errorTextView.visibility = View.VISIBLE
                binding.errorTextView.text = "PIN nie jest ustawiony. Ustaw PIN ponownie."
                return@setOnClickListener
            }

            val salt = PinCrypto.fromB64(storedSaltB64)
            val storedHash = PinCrypto.fromB64(storedHashB64)
            val typedHash = PinCrypto.hashPin(typedPin, salt)

            val ok = PinCrypto.constantTimeEquals(typedHash, storedHash)

            if (ok) {
                startActivity(Intent(this, Dashboard::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            } else {
                binding.errorTextView.visibility = View.VISIBLE
                binding.errorTextView.text = "Nieprawidłowy PIN."
                pin1.text?.clear(); pin2.text?.clear(); pin3.text?.clear(); pin4.text?.clear()
                pin1.requestFocus()
            }
        }

        // ====== BTN FINGER = system biometrics ======
        binding.btnFinger.setOnClickListener {
            if (prefs.isBiometricsEnabled() && canAuth()) {
                biometricPrompt.authenticate(promptInfo)
            } else {
                binding.errorTextView.visibility = View.VISIBLE
                binding.errorTextView.text = "Biometria nie jest włączona na tym urządzeniu."
            }
        }

        // ====== BTN FACE = ML Kit + MobileFaceNet ======
        binding.btnFace.setOnClickListener {
            if (!isFaceLoginEnabled()) {
                binding.errorTextView.visibility = View.VISIBLE
                binding.errorTextView.text = "Logowanie twarzą jest wyłączone w ustawieniach."
                return@setOnClickListener
            }

            if (!hasFaceModel()) {
                Toast.makeText(this, "Brak wzorca – zarejestruj twarz.", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, FaceEnrollMfnActivity::class.java))
                return@setOnClickListener
            }

            startActivity(Intent(this, FaceLoginMfnActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }

        binding.btnBack.setOnClickListener {
            moveTaskToBack(true)
        }
    }

    private fun isFaceLoginEnabled(): Boolean {
        return AppLockPrefs(this).isFaceEnabled()
    }

    // ML Kit + MobileFaceNet: wzorzec jako embedding
    private fun hasFaceModel(): Boolean = File(filesDir, "mfn_template.bin").exists()

    private fun addTextChangeListener() {
        pin1.addTextChangedListener(EditTextWatcher(pin1))
        pin2.addTextChangedListener(EditTextWatcher(pin2))
        pin3.addTextChangedListener(EditTextWatcher(pin3))
        pin4.addTextChangedListener(EditTextWatcher(pin4))
    }

    inner class EditTextWatcher(private val view: View) : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            val text = s?.toString().orEmpty()
            when (view.id) {
                R.id.pin1 -> if (text.length == 1) pin2.requestFocus()
                R.id.pin2 -> if (text.length == 1) pin3.requestFocus() else if (text.isEmpty()) pin1.requestFocus()
                R.id.pin3 -> if (text.length == 1) pin4.requestFocus() else if (text.isEmpty()) pin2.requestFocus()
                R.id.pin4 -> if (text.isEmpty()) pin3.requestFocus()
            }
        }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

    private fun getTypedPin(): String {
        return pin1.text.toString() + pin2.text.toString() + pin3.text.toString() + pin4.text.toString()
    }

    private fun canAuth(): Boolean {
        val result = BiometricManager.from(this).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        return result == BiometricManager.BIOMETRIC_SUCCESS
    }
}