package com.coinlet.app

import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.coinlet.R
import com.coinlet.applock.AppLockPrefs
import com.coinlet.applock.PinCrypto
import com.coinlet.databinding.ActivityChangePinBinding

class ChangePinActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangePinBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityChangePinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupInputs()

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSavePin.setOnClickListener {
            changePin()
        }
    }

    private fun setupInputs() {
        val filters = arrayOf(InputFilter.LengthFilter(4))

        binding.etOldPin.inputType =
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        binding.etNewPin.inputType =
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        binding.etConfirmNewPin.inputType =
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD

        binding.etOldPin.filters = filters
        binding.etNewPin.filters = filters
        binding.etConfirmNewPin.filters = filters
    }

    private fun changePin() {
        clearErrors()

        val oldPin = binding.etOldPin.text.toString().trim()
        val newPin = binding.etNewPin.text.toString().trim()
        val confirmPin = binding.etConfirmNewPin.text.toString().trim()

        var isValid = true

        if (oldPin.length != 4) {
            binding.etOldPin.error = "Stary PIN musi mieć 4 cyfry"
            isValid = false
        }

        if (newPin.length != 4) {
            binding.etNewPin.error = "Nowy PIN musi mieć 4 cyfry"
            isValid = false
        }

        if (confirmPin.length != 4) {
            binding.etConfirmNewPin.error = "Potwierdzenie PIN-u musi mieć 4 cyfry"
            isValid = false
        }

        if (!isValid) return

        if (!isPinConfigured()) {
            Toast.makeText(this, "PIN nie jest jeszcze ustawiony", Toast.LENGTH_SHORT).show()
            return
        }

        if (!verifyPinWithStoredHash(oldPin)) {
            binding.etOldPin.error = "Nieprawidłowy stary PIN"
            return
        }

        if (newPin == oldPin) {
            binding.etNewPin.error = "Nowy PIN musi być inny niż stary"
            return
        }

        if (newPin != confirmPin) {
            binding.etConfirmNewPin.error = "PIN-y nie są takie same"
            return
        }

        saveNewPin(newPin)

        Toast.makeText(this, "PIN został zmieniony", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun clearErrors() {
        binding.etOldPin.error = null
        binding.etNewPin.error = null
        binding.etConfirmNewPin.error = null
    }

    private fun isPinConfigured(): Boolean {
        val prefs = AppLockPrefs(this)
        return prefs.getPinHash() != null && prefs.getPinSalt() != null
    }

    private fun verifyPinWithStoredHash(enteredPin: String): Boolean {
        val prefs = AppLockPrefs(this)

        val storedHashB64 = prefs.getPinHash() ?: return false
        val storedSaltB64 = prefs.getPinSalt() ?: return false

        val salt = PinCrypto.fromB64(storedSaltB64)
        val storedHash = PinCrypto.fromB64(storedHashB64)
        val typedHash = PinCrypto.hashPin(enteredPin, salt)

        return PinCrypto.constantTimeEquals(typedHash, storedHash)
    }

    private fun saveNewPin(newPin: String) {
        val prefs = AppLockPrefs(this)

        val salt = PinCrypto.generateSalt()
        val hash = PinCrypto.hashPin(newPin, salt)

        prefs.savePinHashAndSalt(
            PinCrypto.toB64(hash),
            PinCrypto.toB64(salt)
        )
    }
}