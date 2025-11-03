package com.coinlet.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.coinlet.R
import com.coinlet.app.Dashboard
import com.coinlet.app.SplashScreen
import com.coinlet.databinding.ActivityLoginBinding

class Login : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
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
//                            startActivity(Intent(this, ConfirmCodeRegisterStep::class.java))
                            startActivity(Intent(this, Dashboard::class.java))
                            finish()
                        }
                    }.addOnFailureListener {
                        Toast.makeText(this, it.localizedMessage, Toast.LENGTH_LONG).show()
                    }
            }
        }

    }
}