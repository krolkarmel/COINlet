package com.coinlet.register

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.coinlet.R
import com.coinlet.app.SplashScreen
import com.coinlet.databinding.ActivityFifthRegisterStepBinding
import com.coinlet.model.Address
import com.coinlet.model.UserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FifthRegisterStep : AppCompatActivity() {
    private lateinit var binding: ActivityFifthRegisterStepBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

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
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()



        binding.btnEndRegister.setOnClickListener {
            val email = intent.getStringExtra("email") ?: ""
            val password = binding.passwordInput.text.toString()

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
                isVerified = false,
            )


            if(email.isNotEmpty() && password.isNotEmpty()){
                auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
                    if(it.isSuccessful){


                        val userId = FirebaseAuth.getInstance().currentUser!!.uid

                        db.collection("users").document(userId).set(userData)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Pomyślnie utworzono konto!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, FinishRegisterStep::class.java))
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Wystąpił błąd przy tworzeniu konta! Błąd Firestore", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, SplashScreen::class.java))
                                finish()
                            }
                    }
                }.addOnFailureListener {
                    Toast.makeText(this, "Błąd Auth:", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this, SplashScreen::class.java))
                }
            }
        }

    }
}