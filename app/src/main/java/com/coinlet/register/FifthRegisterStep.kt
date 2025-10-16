package com.coinlet.register

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.coinlet.R
import com.coinlet.databinding.ActivityFifthRegisterStepBinding
import com.coinlet.model.Address
import com.coinlet.model.UserData

class FifthRegisterStep : AppCompatActivity() {
    private lateinit var binding: ActivityFifthRegisterStepBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityFifthRegisterStepBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        binding.btnEndRegister.setOnClickListener {
            val userData = UserData(
                nationality = intent.getStringExtra("nationality") ?: "",
                phoneNumber = intent.getStringExtra("phoneNumber") ?: "",
                firstName = intent.getStringExtra("firstName") ?: "",
                secondName = intent.getStringExtra("secondName") ?: "",
                lastName = intent.getStringExtra("lastName") ?: "",
                birthDate = intent.getStringExtra("birthDate") ?: "",
                pesel = intent.getStringExtra("pesel") ?: "",
                address = Address(
                    city = intent.getStringExtra("city") ?: "",
                    street = intent.getStringExtra("street") ?: "",
                    postalCode = intent.getStringExtra("postalCode") ?: "",
                    houseNumber = intent.getStringExtra("houseNumber") ?: "",
                    country = intent.getStringExtra("country") ?: "",
                ),
                password = binding.passwordInput.text.toString(),
                isVerified = false,
            )





            val intent = Intent(this, FinishRegisterStep::class.java)
            startActivity(intent)
        }




    }
}