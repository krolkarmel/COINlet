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
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

class SecondStepRegister : AppCompatActivity() {

    private lateinit var binding: ActivitySecondStepRegisterBinding
    private lateinit var btnSendCode: Button
    private lateinit var phoneNumberInputText: EditText
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
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
            checkPhoneAndStartVerification()
        }
    }

    private fun init() {
        btnSendCode = findViewById(R.id.btnSendCode)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
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

    private fun checkPhoneAndStartVerification() {
        val localNumber = phoneNumberInputText.text.trim().toString()

        if (localNumber.isEmpty()) {
            phoneNumberInputText.error = "Wpisz numer telefonu"
            return
        } else {
            phoneNumberInputText.error = null
        }

        if (localNumber.length !in 7..12) {
            phoneNumberInputText.error = "Niepoprawna długość numeru"
            return
        } else {
            phoneNumberInputText.error = null
        }

        val selected = binding.numberSpinner.selectedItem.toString()
        val parts = selected.split(" ")
        val numberPrefix = parts.last()
        phoneNumber = numberPrefix + localNumber

        btnSendCode.isEnabled = false
        btnSendCode.alpha = 0.5f

        db.collection("users")
            .whereEqualTo("phoneNumber", phoneNumber)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    btnSendCode.isEnabled = true
                    btnSendCode.alpha = 1.0f
                    phoneNumberInputText.error = "Ten numer telefonu jest już zarejestrowany"
                    Toast.makeText(
                        this,
                        "Ten numer telefonu jest już przypisany do konta.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }

                phoneNumberInputText.error = null
                startPhoneVerification()
            }
            .addOnFailureListener { e ->
                btnSendCode.isEnabled = true
                btnSendCode.alpha = 1.0f
                Toast.makeText(
                    this,
                    e.message ?: "Błąd sprawdzania numeru telefonu",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun startPhoneVerification() {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
        }

        override fun onVerificationFailed(e: FirebaseException) {
            btnSendCode.isEnabled = true
            btnSendCode.alpha = 1.0f

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
            btnSendCode.isEnabled = true
            btnSendCode.alpha = 1.0f

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