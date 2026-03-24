package com.coinlet.register

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
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
import com.coinlet.app.SplashScreen
import com.coinlet.databinding.ActivityConfirmCodeRegisterStepBinding
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class ConfirmCodeRegisterStep : AppCompatActivity() {

    private lateinit var binding: ActivityConfirmCodeRegisterStepBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var btnConfirmCode: Button
    private lateinit var btnResendCode: Button

    private lateinit var code1: EditText
    private lateinit var code2: EditText
    private lateinit var code3: EditText
    private lateinit var code4: EditText
    private lateinit var code5: EditText
    private lateinit var code6: EditText

    private var otp: String = ""
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var phoneNumber: String = ""
    private var nationality: String = ""
    private var mode: String = "register"

    private var resendCountdown: CountDownTimer? = null
    private val resendCooldownMs = 30_000L

    private val phoneCallbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            verifyCode(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            setResendButtonEnabled(true)
            Toast.makeText(
                this@ConfirmCodeRegisterStep,
                "Nie udało się wysłać kodu: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            otp = verificationId
            resendToken = token
            clearCodeFields()
            startResendCooldown()

            Toast.makeText(
                this@ConfirmCodeRegisterStep,
                "Wysłano nowy kod SMS.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

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

        otp = intent.getStringExtra("OTP").orEmpty()
        resendToken = getResendTokenFromIntent()
        phoneNumber = intent.getStringExtra("phoneNumber").orEmpty()
        nationality = intent.getStringExtra("nationality").orEmpty()
        mode = intent.getStringExtra("mode") ?: "register"

        init()
        addTextChangeListener()
        startResendCooldown()

        binding.btnConfirmCode.setOnClickListener {
            val typedOtp = getTypedOtp()

            if (typedOtp.isEmpty()) {
                Toast.makeText(this, "Wprowadź kod", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (typedOtp.length != 6) {
                Toast.makeText(this, "Niepoprawnie wprowadzony kod", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (otp.isBlank()) {
                Toast.makeText(this, "Brak aktywnego kodu weryfikacyjnego", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val credential = PhoneAuthProvider.getCredential(otp, typedOtp)
            verifyCode(credential)
        }

        binding.btnResendCode.setOnClickListener {
            resendVerificationCode()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        resendCountdown?.cancel()
    }

    private fun init() {
        auth = FirebaseAuth.getInstance()
        btnConfirmCode = findViewById(R.id.btnConfirmCode)
        btnResendCode = findViewById(R.id.btnResendCode)

        code1 = binding.code1
        code2 = binding.code2
        code3 = binding.code3
        code4 = binding.code4
        code5 = binding.code5
        code6 = binding.code6
    }

    private fun addTextChangeListener() {
        code1.addTextChangedListener(EditTextWatcher(code1))
        code2.addTextChangedListener(EditTextWatcher(code2))
        code3.addTextChangedListener(EditTextWatcher(code3))
        code4.addTextChangedListener(EditTextWatcher(code4))
        code5.addTextChangedListener(EditTextWatcher(code5))
        code6.addTextChangedListener(EditTextWatcher(code6))
    }

    private fun getTypedOtp(): String {
        return code1.text.toString() +
                code2.text.toString() +
                code3.text.toString() +
                code4.text.toString() +
                code5.text.toString() +
                code6.text.toString()
    }

    private fun clearCodeFields() {
        code1.text?.clear()
        code2.text?.clear()
        code3.text?.clear()
        code4.text?.clear()
        code5.text?.clear()
        code6.text?.clear()
        code1.requestFocus()
    }

    private fun startResendCooldown() {
        resendCountdown?.cancel()

        setResendButtonEnabled(false)

        resendCountdown = object : CountDownTimer(resendCooldownMs, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000L
                btnResendCode.text = "Wyślij ponownie za ${seconds}s"
            }

            override fun onFinish() {
                btnResendCode.text = "Wyślij kod ponownie"
                setResendButtonEnabled(true)
            }
        }.start()
    }

    private fun setResendButtonEnabled(enabled: Boolean) {
        btnResendCode.isEnabled = enabled
        btnResendCode.alpha = if (enabled) 1.0f else 0.5f
    }

    private fun resendVerificationCode() {
        if (phoneNumber.isBlank()) {
            Toast.makeText(
                this,
                "Brak numeru telefonu do ponownej wysyłki kodu.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        setResendButtonEnabled(false)
        btnResendCode.text = "Wysyłanie..."

        val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(phoneCallbacks)

        resendToken?.let {
            optionsBuilder.setForceResendingToken(it)
        }

        PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
    }

    private fun verifyCode(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Błąd weryfikacji, kod niepoprawny.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnCompleteListener
                }

                if (mode == "login") {
                    startActivity(Intent(this, SplashScreen::class.java))
                    finish()
                    return@addOnCompleteListener
                }

                val i = Intent(this, ThirdRegisterStep::class.java).apply {
                    putExtra("nationality", nationality)
                    putExtra("phoneNumber", phoneNumber)
                }
                startActivity(i)
                finish()
            }
    }

    private fun getResendTokenFromIntent(): PhoneAuthProvider.ForceResendingToken? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(
                "resendToken",
                PhoneAuthProvider.ForceResendingToken::class.java
            )
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("resendToken")
        }
    }

    inner class EditTextWatcher(private val view: View) : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            val text = s.toString()
            when (view.id) {
                R.id.code1 -> if (text.length == 1) code2.requestFocus()
                R.id.code2 -> if (text.length == 1) code3.requestFocus() else if (text.isEmpty()) code1.requestFocus()
                R.id.code3 -> if (text.length == 1) code4.requestFocus() else if (text.isEmpty()) code2.requestFocus()
                R.id.code4 -> if (text.length == 1) code5.requestFocus() else if (text.isEmpty()) code3.requestFocus()
                R.id.code5 -> if (text.length == 1) code6.requestFocus() else if (text.isEmpty()) code4.requestFocus()
                R.id.code6 -> if (text.isEmpty()) code5.requestFocus()
            }
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }
}