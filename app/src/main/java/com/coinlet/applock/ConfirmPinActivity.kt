package com.coinlet.applock

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.coinlet.R
import com.coinlet.Validator
import com.coinlet.app.SplashScreen
import com.coinlet.databinding.ActivityConfirmPinBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class ConfirmPinActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfirmPinBinding
    private lateinit var deviceId: String
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityConfirmPinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.confirmPinRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"

        binding.btnSavePin.setOnClickListener {
            if (!validateForm()) return@setOnClickListener

            val enteredPin = intent.getStringExtra("enteredPin") ?: ""
            val confirmedPin = binding.textInputConfirmPin.text.toString()

            if (confirmedPin != enteredPin) {
                Toast.makeText(this, "Podane piny nie są takie same!", Toast.LENGTH_LONG).show()
                binding.textInputConfirmPin.text?.clear()
                return@setOnClickListener
            }

            val prefs = AppLockPrefs(this)
            val salt = PinCrypto.generateSalt()
            val hash = PinCrypto.hashPin(enteredPin, salt)
            prefs.savePinHashAndSalt(
                hashB64 = PinCrypto.toB64(hash),
                saltB64 = PinCrypto.toB64(salt)
            )

            val uid = auth.currentUser?.uid
            if (uid == null) {
                Toast.makeText(this, "Brak sesji użytkownika. Zaloguj się ponownie.", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, SplashScreen::class.java))
                finish()
                return@setOnClickListener
            }

            val updates = mapOf(
                "needsAppLockSetup" to false,
                "appLockEnabled" to true,
                "trustedDevices.$deviceId" to true
            )

            db.collection("users").document(uid)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener {
                    startActivity(Intent(this, BiometricSetupActivity::class.java))
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Nie zapisano ustawień w chmurze: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this, BiometricSetupActivity::class.java))
                    finish()
                }
        }
    }

    private fun validateForm(): Boolean {
        val pin = binding.textInputConfirmPin.text.toString()
        return if (!Validator.isPinGood(pin)) {
            binding.textInputConfirmPin.error = "PIN jest niezgodny z regulaminem!"
            false
        } else {
            binding.textInputConfirmPin.error = null
            true
        }
    }
}
