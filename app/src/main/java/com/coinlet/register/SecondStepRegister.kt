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
    private lateinit var btnSendCode : Button
    private lateinit var phoneNumberInputText : EditText
    private lateinit var auth : FirebaseAuth
    private lateinit var phoneNumber : String






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

        val spinner = findViewById<Spinner>(R.id.numberSpinner)

        val arrayAdapter = ArrayAdapter.createFromResource(
            this@SecondStepRegister,
            R.array.numberPrefix,
            android.R.layout.simple_spinner_dropdown_item
        )
        spinner.adapter = arrayAdapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                p0: AdapterView<*>?,
                p1: View?,
                p2: Int,
                p3: Long
            ) {
                //
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                //
            }
        }


        init()

        btnSendCode.setOnClickListener{

            phoneNumber = phoneNumberInputText.text.trim().toString()
            if (phoneNumber.isNotEmpty()){
                if(phoneNumber.length in 7..12){
                    val selected = binding.numberSpinner.selectedItem.toString()
                    val parts = selected.split(" ")
                    val numberPrefix = parts.last()
                    phoneNumber = numberPrefix + phoneNumber

                    val options = PhoneAuthOptions.newBuilder(auth)
                        .setPhoneNumber(phoneNumber) // Phone number to verify
                        .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                        .setActivity(this) // Activity (for callback binding)
                        .setCallbacks(callbacks) // OnVerificationStateChangedCallbacks
                        .build()
                    PhoneAuthProvider.verifyPhoneNumber(options)

                }else{
                    Toast.makeText(this, "Podaj właściwy numer telefonu!", Toast.LENGTH_SHORT).show()
                }
            }else{
                Toast.makeText(this, "Podaj numer telefonu!", Toast.LENGTH_SHORT).show()
            }
        }


//        binding.btnSendCode.setOnClickListener {
//            val nationality = intent.getStringExtra("nationality") ?: ""
//            val phoneNumber = binding.phoneNumberInputText.text.toString()
//            val intent = Intent(this, ConfirmCodeRegisterStep::class.java)
//            intent.putExtra("nationality", nationality)
//            intent.putExtra("phoneNumber", phoneNumber)
//            startActivity(intent)
//        }


    }
    private fun init(){
        btnSendCode = findViewById(R.id.btnSendCode)
        auth = FirebaseAuth.getInstance()
        phoneNumberInputText = binding.phoneNumberInputText
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
            val nationality = intent.getStringExtra("nationality") ?: ""
            val intent = Intent(this@SecondStepRegister, ConfirmCodeRegisterStep::class.java)
            intent.putExtra("OTP", verificationId)
            intent.putExtra("resendToken", token)
            intent.putExtra("nationality", nationality)
            intent.putExtra("phoneNumber", phoneNumber)
            startActivity(intent)
        }
    }



    //funkcja dla logowania
//    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
//        auth.signInWithCredential(credential)
//            .addOnCompleteListener(this) { task ->
//                if (task.isSuccessful) {
//                    // Sign in success, update UI with the signed-in user's information
//
//                    val user = task.result?.user
//                } else {
//                    // Sign in failed, display a message and update the UI
//                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
//                        // The verification code entered was invalid
//                    }
//                    // Update UI
//                }
//            }
//    }


}