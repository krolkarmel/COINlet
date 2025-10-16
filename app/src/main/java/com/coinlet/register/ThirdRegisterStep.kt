package com.coinlet.register

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.coinlet.R
import com.coinlet.databinding.ActivityThirdRegisterStepBinding

class ThirdRegisterStep : AppCompatActivity() {
    private lateinit var binding: ActivityThirdRegisterStepBinding

    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityThirdRegisterStepBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        binding.btnNext.setOnClickListener {
            val nationality = intent.getStringExtra("nationality") ?: ""
            val phoneNumber = intent.getStringExtra("phoneNumber") ?: ""
            val firstName = binding.firstNameInput.text.toString()
            val secondName = binding.secondNameInput.text.toString()
            val lastName = binding.lastNameInput.text.toString()
            val birthDate = binding.birthDateInput.text.toString()
            val pesel = binding.peselInput.text.toString()
            val email = binding.emailInput.text.toString()
            val intent = Intent(this, IdRegisterStep::class.java)
            intent.putExtra("nationality", nationality)
            intent.putExtra("phoneNumber", phoneNumber)
            intent.putExtra("firstName", firstName)
            intent.putExtra("secondName", secondName)
            intent.putExtra("lastName", lastName)
            intent.putExtra("birthDate", birthDate)
            intent.putExtra("pesel", pesel)
            intent.putExtra("email", email)
            startActivity(intent)
        }


    }
}