package com.coinlet.register

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.coinlet.R
import com.coinlet.databinding.ActivitySecondStepRegisterBinding
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class SecondStepRegister : AppCompatActivity() {

    private lateinit var binding: ActivitySecondStepRegisterBinding
    private lateinit var btnSendCode: Button
    private lateinit var phoneNumberInputText: EditText
    private lateinit var auth: FirebaseAuth
    private lateinit var phoneNumber: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySecondStepRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        init()
        setupSpinner()

        btnSendCode.setOnClickListener {
            startPhoneVerification()
        }
    }

    private fun init() {
        btnSendCode = findViewById(R.id.btnSendCode)
        auth = FirebaseAuth.getInstance()
        phoneNumberInputText = binding.phoneNumberInputText
    }

    private fun setupSpinner() {
        val spinner = findViewById<Spinner>(R.id.numberSpinner)
        val arrayAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.numberPrefix,
            android.R.layout.simple_spinner_dropdown_item
        )
        spinner.adapter = arrayAdapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {}
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    private fun startPhoneVerification() {
        val localNumber = phoneNumberInputText.text.trim().toString()

        if (localNumber.isEmpty()) {
            Toast.makeText(this, "Wpisz numer telefonu", Toast.LENGTH_SHORT).show()
            return
        }

        if (localNumber.length !in 7..12) {
            Toast.makeText(this, "Niepoprawna długość numeru", Toast.LENGTH_SHORT).show()
            return
        }

        val selected = binding.numberSpinner.selectedItem.toString()
        val parts = selected.split(" ")
        val numberPrefix = parts.last()
        phoneNumber = numberPrefix + localNumber

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .build()

        // ✅ BEZ signInAnonymously() – mniej problemów ze stanem sesji
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // opcjonalnie: można od razu zalogować usera i przejść dalej
        }

        override fun onVerificationFailed(e: FirebaseException) {
            val msg = when (e) {
                is FirebaseAuthInvalidCredentialsException -> "Niepoprawny numer telefonu."
                is FirebaseTooManyRequestsException -> "Przekroczono limit SMS. Spróbuj później."
                is FirebaseAuthMissingActivityForRecaptchaException -> "Błąd reCAPTCHA (brak Activity)."
                else -> e.localizedMessage ?: "Nieznany błąd weryfikacji."
            }
            Toast.makeText(this@SecondStepRegister, "Weryfikacja nieudana: $msg", Toast.LENGTH_LONG).show()
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken,
        ) {
            val nationality = intent.getStringExtra("nationality") ?: ""

            val i = Intent(this@SecondStepRegister, ConfirmCodeRegisterStep::class.java).apply {
                putExtra("OTP", verificationId)
                putExtra("resendToken", token)
                putExtra("nationality", nationality)
                putExtra("phoneNumber", phoneNumber)
                putExtra("mode", "register")
            }
            startActivity(i)
        }
    }
}
