package com.coinlet.register

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.coinlet.R
import com.coinlet.app.SplashScreen
import com.coinlet.databinding.ActivityFinishRegisterStepBinding
import com.coinlet.login.Login
import com.google.firebase.auth.FirebaseAuth

class FinishRegisterStep : AppCompatActivity() {
    private lateinit var binding: ActivityFinishRegisterStepBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        enableEdgeToEdge()
        binding = ActivityFinishRegisterStepBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        binding.btnGoMenu.setOnClickListener {
            startActivity(Intent(this, SplashScreen::class.java))
        }

        binding.btnLogin.setOnClickListener {
            startActivity(Intent(this, Login::class.java))
        }
    }
}