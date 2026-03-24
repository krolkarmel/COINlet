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
import com.google.firebase.firestore.FirebaseFirestore

class ThirdRegisterStep : AppCompatActivity() {

    private lateinit var binding: ActivityThirdRegisterStepBinding
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityThirdRegisterStepBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = FirebaseFirestore.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnNext.setOnClickListener {
            if (validateForm()) {
                checkEmailUniquenessAndContinue()
            }
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        val email = binding.emailInput.text.toString().trim()
        val firstName = binding.firstNameInput.text.toString().trim()
        val secondName = binding.secondNameInput.text.toString().trim()
        val lastName = binding.lastNameInput.text.toString().trim()
        val birthDate = binding.birthDateInput.text.toString().trim()
        val pesel = binding.peselInput.text.toString().trim()

        if (!Validator.isEmailValid(email)) {
            binding.emailInput.error = "Niepoprawny adres email"
            isValid = false
        } else {
            binding.emailInput.error = null
        }

        if (!Validator.isFirstNameValid(firstName)) {
            binding.firstNameInput.error = "Pierwsze imię składa się tylko z liter oraz zaczyna się z dużej litery!"
            isValid = false
        } else {
            binding.firstNameInput.error = null
        }

        if (!Validator.isSecondNameValid(secondName)) {
            binding.secondNameInput.error = "Drugie imię składa się tylko z liter oraz zaczyna się z dużej litery!"
            isValid = false
        } else {
            binding.secondNameInput.error = null
        }

        if (!Validator.isLastNameValid(lastName)) {
            binding.lastNameInput.error = "Nazwisko składa się tylko z liter oraz zaczyna się z dużej litery!"
            isValid = false
        } else {
            binding.lastNameInput.error = null
        }

        if (!Validator.isBirthDateValid(birthDate)) {
            binding.birthDateInput.error = "Niepoprawna data urodzenia!"
            isValid = false
        } else {
            binding.birthDateInput.error = null
        }

        if (!Validator.isPeselValid(pesel)) {
            binding.peselInput.error = "Niepoprawny numer pesel!"
            isValid = false
        } else {
            binding.peselInput.error = null
        }

        return isValid
    }

    private fun checkEmailUniquenessAndContinue() {
        val email = binding.emailInput.text.toString().trim().lowercase()

        binding.btnNext.isEnabled = false
        binding.btnNext.alpha = 0.5f

        db.collection("users")
            .whereEqualTo("email", email)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                binding.btnNext.isEnabled = true
                binding.btnNext.alpha = 1.0f

                if (!snapshot.isEmpty) {
                    binding.emailInput.error = "Ten adres e-mail jest już zajęty"
                    return@addOnSuccessListener
                }

                binding.emailInput.error = null

                val nationality = intent.getStringExtra("nationality") ?: ""
                val phoneNumber = intent.getStringExtra("phoneNumber") ?: ""
                val firstName = binding.firstNameInput.text.toString().trim()
                val secondName = binding.secondNameInput.text.toString().trim()
                val lastName = binding.lastNameInput.text.toString().trim()
                val birthDate = binding.birthDateInput.text.toString().trim()
                val pesel = binding.peselInput.text.toString().trim()

                val nextIntent = Intent(this, IdRegisterStep::class.java).apply {
                    putExtra("nationality", nationality)
                    putExtra("phoneNumber", phoneNumber)
                    putExtra("firstName", firstName)
                    putExtra("secondName", secondName)
                    putExtra("lastName", lastName)
                    putExtra("birthDate", birthDate)
                    putExtra("pesel", pesel)
                    putExtra("email", email)
                }
                startActivity(nextIntent)
            }
            .addOnFailureListener {
                binding.btnNext.isEnabled = true
                binding.btnNext.alpha = 1.0f
                binding.emailInput.error = "Nie udało się sprawdzić adresu e-mail"
            }
    }
}