package com.coinlet.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.coinlet.R
import com.coinlet.app.SplashScreen
import com.coinlet.databinding.ActivityLoginBinding
import com.coinlet.register.ConfirmCodeRegisterStep
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

class Login : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var phoneNumberFromDb: String
    private lateinit var nationalityFromDb: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()

        binding.btnBack.setOnClickListener {
            startActivity(Intent(this, SplashScreen::class.java))
            finish()
        }

        binding.btnConfirmLogin.setOnClickListener {
            val email = binding.textInputEmail.text.toString().trim()
            val password = binding.textInputPassword.text.toString()

            binding.textInputEmail.error = null
            binding.textInputPassword.error = null

            if (email.isEmpty()) {
                binding.textInputEmail.error = "Podaj email"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                binding.textInputPassword.error = "Podaj hasło"
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        when (task.exception) {
                            is FirebaseAuthInvalidUserException -> {
                                binding.textInputEmail.error = "Niepoprawny email"
                            }
                            is FirebaseAuthInvalidCredentialsException -> {
                                binding.textInputPassword.error = "Niepoprawne hasło"
                            }
                            else -> {
                                Toast.makeText(
                                    this,
                                    "Nie udało się zalogować. Spróbuj ponownie.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                        return@addOnCompleteListener
                    }

                    val userId = FirebaseAuth.getInstance().currentUser?.uid
                    if (userId == null) {
                        Toast.makeText(this, "Błąd logowania", Toast.LENGTH_LONG).show()
                        return@addOnCompleteListener
                    }

                    val db = FirebaseFirestore.getInstance()
                    db.collection("users")
                        .document(userId)
                        .get()
                        .addOnSuccessListener { doc ->
                            if (!doc.exists()) {
                                Toast.makeText(
                                    this,
                                    "Nie znaleziono danych użytkownika.",
                                    Toast.LENGTH_LONG
                                ).show()
                                return@addOnSuccessListener
                            }

                            phoneNumberFromDb = doc.getString("phoneNumber") ?: ""
                            nationalityFromDb = doc.getString("nationality") ?: ""

                            if (phoneNumberFromDb.isBlank()) {
                                Toast.makeText(
                                    this,
                                    "Brak numeru telefonu w profilu.",
                                    Toast.LENGTH_LONG
                                ).show()
                                return@addOnSuccessListener
                            }

                            Toast.makeText(this, "Wysłano kod SMS", Toast.LENGTH_LONG).show()

                            val options = PhoneAuthOptions.newBuilder(auth)
                                .setPhoneNumber(phoneNumberFromDb)
                                .setTimeout(60L, TimeUnit.SECONDS)
                                .setActivity(this)
                                .setCallbacks(callbacks)
                                .build()

                            PhoneAuthProvider.verifyPhoneNumber(options)
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Błąd pobierania danych użytkownika.", Toast.LENGTH_LONG).show()
                        }
                }
        }
    }

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
        }

        override fun onVerificationFailed(e: FirebaseException) {
            val msg = when (e) {
                is FirebaseAuthInvalidCredentialsException -> "Niepoprawny numer telefonu."
                is FirebaseTooManyRequestsException -> "Zbyt wiele prób. Spróbuj później."
                is FirebaseAuthMissingActivityForRecaptchaException -> "Błąd weryfikacji. Spróbuj ponownie."
                else -> "Nie udało się wysłać kodu SMS."
            }

            Toast.makeText(this@Login, msg, Toast.LENGTH_LONG).show()
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken,
        ) {
            val intent = Intent(this@Login, ConfirmCodeRegisterStep::class.java).apply {
                putExtra("OTP", verificationId)
                putExtra("resendToken", token)
                putExtra("nationality", nationalityFromDb)
                putExtra("phoneNumber", phoneNumberFromDb)
                putExtra("mode", "login")
            }
            startActivity(intent)
        }
    }
}