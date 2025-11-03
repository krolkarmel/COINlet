package com.coinlet.register

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.coinlet.R
import com.coinlet.Validator
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
            if (validateForm()) {
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
    private fun validateForm(): Boolean{
        var isValid = true;
        if(!Validator.isEmailValid(binding.emailInput.text.toString())){
            binding.emailInput.error = "Niepoprawny adres email"
            isValid = false
        }
        else{
            binding.emailInput.error = null
        }
        if(!Validator.isFirstNameValid(binding.firstNameInput.text.toString())){
            binding.firstNameInput.error = "Pierwsze imię składa się tylko z liter oraz zaczyna się z dużej litery!"
            isValid = false
        }
        else{
            binding.firstNameInput.error = null
        }
        if(!Validator.isSecondNameValid(binding.secondNameInput.text.toString())){
            binding.secondNameInput.error = "Drugie imię składa się tylko z liter oraz zaczyna się z dużej litery!"
            isValid = false
        }
        else{
            binding.secondNameInput.error = null
        }
        if(!Validator.isLastNameValid(binding.lastNameInput.text.toString())){
            binding.lastNameInput.error = "Nazwisko składa się tylko z liter oraz zaczyna się z dużej litery!"
            isValid = false
        }
        else{
            binding.lastNameInput.error = null
        }
        if(!Validator.isBirthDateValid(binding.birthDateInput.text.toString())){
            binding.birthDateInput.error = "Niepoprawna data urodzenia!"
            isValid = false
        }
        else{
            binding.birthDateInput.error = null
        }
        if(!Validator.isPeselValid(binding.peselInput.text.toString())){
            binding.peselInput.error = "Niepoprawny numer pesel!"
            isValid = false
        }
        else{
            binding.peselInput.error = null
        }
        return isValid
    }
}
