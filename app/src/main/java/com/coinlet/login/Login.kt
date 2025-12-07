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
        }

        binding.btnConfirmLogin.setOnClickListener {
            val email = binding.textInputEmail.text.toString()
            val password = binding.textInputPassword.text.toString()
            if (email.isNotEmpty() && password.isNotEmpty()) {
                SplashScreen.auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            val userId = FirebaseAuth.getInstance().currentUser!!.uid

                            val db = FirebaseFirestore.getInstance()
                            db.collection("users")
                                .document(userId)
                                .get()
                                .addOnSuccessListener { doc ->
                                    if (doc.exists()) {
                                        phoneNumberFromDb =
                                            doc.getString("phoneNumber") ?: ""
                                        nationalityFromDb =
                                            doc.getString("nationality") ?: ""

                                        Toast.makeText(this, "PHONE='$phoneNumberFromDb'", Toast.LENGTH_LONG).show()

                                        val options = PhoneAuthOptions.newBuilder(auth)
                                            .setPhoneNumber(phoneNumberFromDb) // Phone number to verify
                                            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                                            .setActivity(this) // Activity (for callback binding)
                                            .setCallbacks(callbacks) // OnVerificationStateChangedCallbacks
                                            .build()
                                        PhoneAuthProvider.verifyPhoneNumber(options)

                                    }
                                }.addOnFailureListener {
                                    Toast.makeText(this, it.localizedMessage, Toast.LENGTH_LONG)
                                        .show()
                                }
                        }
                    }

            }
        }
    }


    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // This callback will be invoked in two situations:
            // 1 - Instant verification. In some cases the phone number can be instantly
            //     verified without needing to send or enter a verification code.
            // 2 - Auto-retrieval. On some devices Google Play services can automatically
            //     detect the incoming verification SMS and perform verification without
            //     user action.
        }

        override fun onVerificationFailed(e: FirebaseException) {
            // This callback is invoked in an invalid request for verification is made,
            // for instance if the the phone number format is not valid.

            if (e is FirebaseAuthInvalidCredentialsException) {
                // Invalid request
            } else if (e is FirebaseTooManyRequestsException) {
                // The SMS quota for the project has been exceeded
            } else if (e is FirebaseAuthMissingActivityForRecaptchaException) {
                // reCAPTCHA verification attempted with null Activity
            }

            // Show a message and update the UI
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken,
        ) {
                        val intent = Intent(this@Login, ConfirmCodeRegisterStep::class.java)
                        intent.putExtra("OTP", verificationId)
                        intent.putExtra("resendToken", token)
                        intent.putExtra("nationality", nationalityFromDb)
                        intent.putExtra("phoneNumber", phoneNumberFromDb)
                        intent.putExtra("mode", "login")

                        startActivity(intent)
                    }
                }
        }

