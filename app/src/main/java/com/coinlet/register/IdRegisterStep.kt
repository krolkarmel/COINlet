package com.coinlet.register

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.coinlet.R
import com.coinlet.databinding.ActivityIdRegisterStepBinding

class IdRegisterStep : AppCompatActivity() {
    private lateinit var binding: ActivityIdRegisterStepBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityIdRegisterStepBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnNext.setOnClickListener {
            val nationality = intent.getStringExtra("nationality") ?: ""
            val phoneNumber = intent.getStringExtra("phoneNumber") ?: ""
            val firstName = intent.getStringExtra("firstName") ?: ""
            val secondName = intent.getStringExtra("secondName") ?: ""
            val lastName = intent.getStringExtra("lastName") ?: ""
            val birthDate = intent.getStringExtra("birthDate") ?: ""
            val pesel = intent.getStringExtra("pesel") ?: ""
            val intent = Intent(this, IdRegisterStep::class.java)
            intent.putExtra("nationality", nationality)
            intent.putExtra("phoneNumber", phoneNumber)
            intent.putExtra("firstName", firstName)
            intent.putExtra("secondName", secondName)
            intent.putExtra("lastName", lastName)
            intent.putExtra("birthDate", birthDate)
            intent.putExtra("pesel", pesel)
            startActivity(intent)
        }
    }
}