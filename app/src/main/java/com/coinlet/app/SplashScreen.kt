package com.coinlet.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.coinlet.R
import com.coinlet.databinding.ActivitySplashScreenBinding
import com.coinlet.login.Login
import com.coinlet.register.FirstRegisterStep
import com.google.firebase.auth.FirebaseAuth


class SplashScreen : AppCompatActivity() {
    private lateinit var binding: ActivitySplashScreenBinding

    companion object {
        lateinit var auth: FirebaseAuth
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        auth = FirebaseAuth.getInstance()


        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnRegister.setOnClickListener {
            startActivity(Intent(this, FirstRegisterStep::class.java))
        }

        binding.btnLogin.setOnClickListener {
            startActivity(Intent(this, Login::class.java))
        }






    }
}