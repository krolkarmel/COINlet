package com.coinlet.applock

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.compose.material3.DatePickerState
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.coinlet.R
import com.coinlet.app.Dashboard
import com.coinlet.databinding.ActivityBiometricSetupBinding
import com.coinlet.databinding.ActivityConfirmPinBinding
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.Executor
import kotlin.jvm.java

class BiometricSetupActivity : AppCompatActivity() {
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: androidx.biometric.BiometricPrompt.PromptInfo
    private lateinit var binding: ActivityBiometricSetupBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBiometricSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.biometricSetupRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        AppLockPrefs(this).setBiometricsEnabled(false)

        executor = ContextCompat.getMainExecutor(this)

        biometricPrompt = BiometricPrompt(
            this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext, "Błąd: $errString", Toast.LENGTH_SHORT)
                        .show()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    if (FirebaseAuth.getInstance().currentUser != null) {
                        val intent = Intent(this@BiometricSetupActivity, Dashboard::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        AppLockPrefs(this@BiometricSetupActivity).setBiometricsEnabled(true)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(
                            applicationContext,
                            "Zaloguj się email/hasło przynajmniej raz, aby włączyć biometrię.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Nieudana autoryzacja", Toast.LENGTH_SHORT)
                        .show()
                }
            })


        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Odblokowanie biometryczne")
            .setSubtitle("Użyj odcisku palca / twarz")
            .setNegativeButtonText("Anuluj")
            .build()

        binding.btnSkip.setOnClickListener {
            val intent = Intent(this, Dashboard::class.java)
            AppLockPrefs(this).setBiometricsEnabled(false)
            startActivity(intent)
            finish()
        }

        binding.btnContinue.setOnClickListener {
            if(binding.checkboxEnableBiometrics.isChecked){
                biometricPrompt.authenticate(promptInfo)
            }else{
                AppLockPrefs(this).setBiometricsEnabled(false)
                startActivity(Intent(this, Dashboard::class.java))
                finish()
            }
        }


    }
}