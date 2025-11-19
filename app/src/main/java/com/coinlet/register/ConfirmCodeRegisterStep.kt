package com.coinlet.register

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.coinlet.R
import com.coinlet.databinding.ActivityConfirmCodeRegisterStepBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider

class ConfirmCodeRegisterStep : AppCompatActivity() {
    private lateinit var binding: ActivityConfirmCodeRegisterStepBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var btnConfirmCode: Button
//    private lateinit var resendTV: TextView
    private lateinit var code1: EditText
    private lateinit var code2: EditText
    private lateinit var code3: EditText
    private lateinit var code4: EditText
    private lateinit var code5: EditText
    private lateinit var code6: EditText

    private lateinit var OTP : String
    private lateinit var resendToken : PhoneAuthProvider.ForceResendingToken
    private lateinit var phoneNumber : String
    private lateinit var nationality : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityConfirmCodeRegisterStepBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        OTP = intent.getStringExtra("OTP").toString()
        resendToken = intent.getParcelableExtra("resendToken")!!
        phoneNumber = intent.getStringExtra("phoneNumber")!!
        nationality = intent.getStringExtra("nationality")!!


        init()

        addTextChangeListener()
        binding.btnConfirmCode.setOnClickListener {
            val typedOTP = (code1.text.toString() + code2.text.toString() + code3.text.toString()
                    + code4.text.toString() + code5.text.toString() + code6.text.toString())
            if (typedOTP.isEmpty()) {
                Toast.makeText(this, "Wprowadź kod", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (typedOTP.length != 6) {
                Toast.makeText(this, "Wprowadź poprawny kod", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val credential: PhoneAuthCredential =
                PhoneAuthProvider.getCredential(OTP, typedOTP)

            verifyCode(credential)


//        binding.btnConfirmCode.setOnClickListener {
//            val nationality = intent.getStringExtra("nationality") ?: ""
//            val phoneNumber = intent.getStringExtra("phoneNumber") ?: ""
//            val intent = Intent(this, ThirdRegisterStep::class.java)
//            intent.putExtra("nationality", nationality)
//            intent.putExtra("phoneNumber", phoneNumber)
//            startActivity(intent)
        }
    }

    private fun addTextChangeListener(){
        code1.addTextChangedListener(EditTextWatcher(code1))
        code2.addTextChangedListener(EditTextWatcher(code2))
        code3.addTextChangedListener(EditTextWatcher(code3))
        code4.addTextChangedListener(EditTextWatcher(code4))
        code5.addTextChangedListener(EditTextWatcher(code5))
        code6.addTextChangedListener(EditTextWatcher(code6)) }
    private fun init(){
        auth = FirebaseAuth.getInstance()
        btnConfirmCode = findViewById(R.id.btnConfirmCode)
        code1 = binding.code1
        code2 = binding.code2
        code3 = binding.code3
        code4 = binding.code4
        code5 = binding.code5
        code6 = binding.code6
    } // resendCode = findViewById() code1 = findViewById(R.id.code1) code2 = findViewById(R.id.code2) code3 = findViewById(R.id.code3) code4 = findViewById(R.id.code4) code5 = findViewById(R.id.code5) code6 = findViewById(R.id.code6) }

    inner class EditTextWatcher(private val view : View) : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                when(view.id){
                    R.id.code1 -> if(text.length == 1) code2.requestFocus()
                    R.id.code2 -> if(text.length == 1) code3.requestFocus() else if (text.isEmpty()) code1.requestFocus()
                    R.id.code3 -> if(text.length == 1) code4.requestFocus() else if (text.isEmpty()) code2.requestFocus()
                    R.id.code4 -> if(text.length == 1) code5.requestFocus() else if (text.isEmpty()) code3.requestFocus()
                    R.id.code5 -> if(text.length == 1) code6.requestFocus() else if (text.isEmpty()) code4.requestFocus()
                    R.id.code6 -> if(text.isEmpty()) code5.requestFocus() } }

        override fun beforeTextChanged(
            s: CharSequence?,
            start: Int, count: Int,
            after: Int ) {

        }

        override fun onTextChanged(
            s: CharSequence?, start: Int,
            before: Int,
            count: Int ) {

        }

    }
    private fun verifyCode(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Numer telefonu został zweryfikowany",
                        Toast.LENGTH_SHORT
                    ).show()

                    val intent = Intent(this, ThirdRegisterStep::class.java).apply {
                        putExtra("nationality", nationality)
                        putExtra("phoneNumber", phoneNumber)
                    }
                    startActivity(intent)
                    finish()
                } else {
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        Toast.makeText(
                            this,
                            "Nieprawidłowy kod weryfikacyjny",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this,
                            "Błąd weryfikacji: ${task.exception?.localizedMessage}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
    }

}