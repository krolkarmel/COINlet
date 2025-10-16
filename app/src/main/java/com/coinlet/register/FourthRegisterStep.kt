package com.coinlet.register

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.coinlet.R
import com.coinlet.databinding.ActivityFourthRegisterStepBinding

class FourthRegisterStep : AppCompatActivity() {
    private lateinit var binding: ActivityFourthRegisterStepBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityFourthRegisterStepBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        binding.btnNext.setOnClickListener {
            val city = binding.cityInput.text.toString()
            val street = binding.streetInput.text.toString()
            val postalCode = binding.postalCodeInput.text.toString()
            val houseNumber = binding.houseNumberInput.text.toString()
            val country = binding.countryInput.text.toString()
            val nationality = intent.getStringExtra("nationality") ?: ""
            val phoneNumber = intent.getStringExtra("phoneNumber") ?: ""
            val firstName = intent.getStringExtra("firstName") ?: ""
            val secondName = intent.getStringExtra("secondName") ?: ""
            val lastName = intent.getStringExtra("lastName") ?: ""
            val birthDate = intent.getStringExtra("birthDate") ?: ""
            val pesel = intent.getStringExtra("pesel") ?: ""
            val email = intent.getStringExtra("email") ?: ""
            val intent = Intent(this, FifthRegisterStep::class.java)
            intent.putExtra("nationality", nationality)
            intent.putExtra("phoneNumber", phoneNumber)
            intent.putExtra("firstName", firstName)
            intent.putExtra("secondName", secondName)
            intent.putExtra("lastName", lastName)
            intent.putExtra("birthDate", birthDate)
            intent.putExtra("pesel", pesel)
            intent.putExtra("email", email)
            intent.putExtra("city", city)
            intent.putExtra("street", street)
            intent.putExtra("postalCode", postalCode)
            intent.putExtra("houseNumber", houseNumber)
            intent.putExtra("country", country)
            startActivity(intent)
        }


    }
}