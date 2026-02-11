package com.coinlet.register

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.coinlet.R
import com.coinlet.Validator
import com.coinlet.app.SplashScreen
import com.coinlet.databinding.ActivityFifthRegisterStepBinding
import com.coinlet.model.Accounts
import com.coinlet.model.Address
import com.coinlet.model.UserData
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.security.SecureRandom

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
            if (!validateForm()) return@setOnClickListener

            val email = intent.getStringExtra("email") ?: ""
            val password = binding.passwordInput.text.toString()

            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Brak email lub hasła.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = auth.currentUser
            if (user == null) {
                Toast.makeText(this, "Brak sesji rejestracji. Zacznij od początku.", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, SplashScreen::class.java))
                finish()
                return@setOnClickListener
            }

            val userData = UserData(
                email = email,
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

            // ✅ Jeśli provider email/password już jest, nie linkuj drugi raz
            val alreadyHasEmailProvider = user.providerData.any { it.providerId == "password" }
            if (alreadyHasEmailProvider) {
                saveUserProfileAndAccount(userData)
                return@setOnClickListener
            }

            val emailCredential = EmailAuthProvider.getCredential(email, password)

            user.linkWithCredential(emailCredential)
                .addOnSuccessListener {
                    saveUserProfileAndAccount(userData)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "Błąd Auth: ${e.javaClass.simpleName}: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                    startActivity(Intent(this, SplashScreen::class.java))
                    finish()
                }
        }
    }

    private fun saveUserProfileAndAccount(userData: UserData) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Brak UID po rejestracji.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, SplashScreen::class.java))
            finish()
            return
        }

        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"

        val payload: MutableMap<String, Any> = hashMapOf(
            "email" to userData.email,
            "nationality" to userData.nationality,
            "phoneNumber" to userData.phoneNumber,
            "firstName" to userData.firstName,
            "secondName" to userData.secondName,
            "lastName" to userData.lastName,
            "birthDate" to userData.birthDate,
            "pesel" to userData.pesel,
            "address" to userData.address,
            "isVerified" to userData.isVerified,

            "appLockEnabled" to false,
            "trustedDevices" to hashMapOf<String, Any>(),

            "trustedDevices.$deviceId" to true,
            "needsAppLockSetup" to true

        )

        db.collection("users").document(userId)
            .set(payload, SetOptions.merge())
            .addOnSuccessListener {

                val accountRef = db.collection("users")
                    .document(userId)
                    .collection("accounts")
                    .document()

                val account = Accounts(
                    accountId = accountRef.id,
                    userId = userId,
                    accountNumber = generateAccountNumber(),
                    balance = 100.0,
                    currency = "PLN"
                )

                accountRef.set(account)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Pomyślnie utworzono konto!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, FinishRegisterStep::class.java))
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Błąd konta: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this, SplashScreen::class.java))
                        finish()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Firestore error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, SplashScreen::class.java))
                finish()
            }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        if (!Validator.isPasswordValid(binding.passwordInput.text.toString())) {
            binding.passwordInput.error =
                "Hasło musi składać się z conajmniej ośmiu znaków, jednej cyfry i jednej wielkiej litery!"
            isValid = false
        } else {
            binding.passwordInput.error = null
        }

        if (!Validator.doPasswordsMatch(
                binding.passwordInput.text.toString(),
                binding.passwordConfirmInput.text.toString()
            )
        ) {
            binding.passwordConfirmInput.error = "Hasła nie są identyczne"
            isValid = false
        } else {
            binding.passwordConfirmInput.error = null
        }

        return isValid
    }

    private fun generateAccountNumber(): String {
        val r = SecureRandom()
        val controlDigits = "11"
        val bankCode = "1750"
        val rest = buildString { repeat(20) { append(r.nextInt(10)) } }
        return controlDigits + bankCode + rest
    }
}
