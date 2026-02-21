package com.coinlet.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.coinlet.R
import com.coinlet.databinding.ActivityUserProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ActivityUserProfile : AppCompatActivity() {

    private lateinit var binding: ActivityUserProfileBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnBack.setOnClickListener { finish() }

        setEditable(false)
        loadUserData()

        binding.btnEdit.setOnClickListener {
            if (!isEditMode) {
                // start edycji
                isEditMode = true
                setEditable(true)
                binding.btnEdit.text = "Zapisz"
            } else {
                // zapis
                saveUserData()
            }
        }
    }

    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Brak zalogowanego użytkownika", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener

                binding.tvFirstName.setText(doc.getString("firstName") ?: "")
                binding.tvLastName.setText(doc.getString("lastName") ?: "")
                binding.tvEmail.setText(doc.getString("email") ?: "")
                binding.tvPhone.setText(doc.getString("phoneNumber") ?: "")
                binding.tvBirthDate.setText(doc.getString("birthDate") ?: "")
                binding.tvNationality.setText(doc.getString("nationality") ?: "")
                binding.tvPesel.setText(doc.getString("pesel") ?: "")

                val address = doc.get("address") as? Map<*, *>
                val street = address?.get("street") as? String ?: ""
                val house = address?.get("houseNumber") as? String ?: ""
                val postal = address?.get("postalCode") as? String ?: ""
                val city = address?.get("city") as? String ?: ""
                val country = address?.get("country") as? String ?: ""

                binding.tvStreetHouse.setText(listOf(street, house).filter { it.isNotBlank() }.joinToString(" "))
                binding.tvPostalCity.setText(listOf(postal, city).filter { it.isNotBlank() }.joinToString(" "))
                binding.tvCountry.setText(country)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Błąd odczytu danych", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveUserData() {
        val uid = auth.currentUser?.uid ?: return

        // prosta walidacja (minimum)
        val firstName = binding.tvFirstName.text?.toString()?.trim().orEmpty()
        val lastName = binding.tvLastName.text?.toString()?.trim().orEmpty()
        val phone = binding.tvPhone.text?.toString()?.trim().orEmpty()
        val birthDate = binding.tvBirthDate.text?.toString()?.trim().orEmpty()
        val nationality = binding.tvNationality.text?.toString()?.trim().orEmpty()
        val pesel = binding.tvPesel.text?.toString()?.trim().orEmpty()

        val streetHouse = binding.tvStreetHouse.text?.toString()?.trim().orEmpty()
        val postalCity = binding.tvPostalCity.text?.toString()?.trim().orEmpty()
        val country = binding.tvCountry.text?.toString()?.trim().orEmpty()

        if (firstName.isBlank()) {
            binding.tvFirstName.error = "Wpisz imię"
            return
        }
        if (lastName.isBlank()) {
            binding.tvLastName.error = "Wpisz nazwisko"
            return
        }
        if (pesel.isNotBlank() && pesel.length != 11) {
            binding.tvPesel.error = "PESEL musi mieć 11 cyfr"
            return
        }

        // parsowanie adresu z 2 pól (prosto, bez kombinowania):
        // "Ulica 12" => street="Ulica", houseNumber="12" (jeśli się da)
        val (street, houseNumber) = splitStreetHouse(streetHouse)

        // "12-345 Miasto" => postalCode="12-345", city="Miasto" (jeśli się da)
        val (postalCode, city) = splitPostalCity(postalCity)

        binding.btnEdit.isEnabled = false

        val updates = hashMapOf<String, Any>(
            "firstName" to firstName,
            "lastName" to lastName,
            "phoneNumber" to phone,
            "birthDate" to birthDate,
            "nationality" to nationality,
            "pesel" to pesel,

            // aktualizacja pól mapy address bez nadpisywania całej mapy
            "address.street" to street,
            "address.houseNumber" to houseNumber,
            "address.postalCode" to postalCode,
            "address.city" to city,
            "address.country" to country
        )

        db.collection("users").document(uid)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Zapisano zmiany", Toast.LENGTH_SHORT).show()
                isEditMode = false
                setEditable(false)
                binding.btnEdit.text = "Edytuj dane"
                binding.btnEdit.isEnabled = true
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Błąd zapisu: ${e.message}", Toast.LENGTH_LONG).show()
                binding.btnEdit.isEnabled = true
            }
    }

    private fun setEditable(editable: Boolean) {
        binding.tvFirstName.isEnabled = editable
        binding.tvLastName.isEnabled = editable

        // email raczej nie edytujemy tutaj (bo to FirebaseAuth), ale jak chcesz to da się osobno
        binding.tvEmail.isEnabled = false

        binding.tvPhone.isEnabled = editable
        binding.tvBirthDate.isEnabled = editable
        binding.tvNationality.isEnabled = editable
        binding.tvPesel.isEnabled = editable

        binding.tvStreetHouse.isEnabled = editable
        binding.tvPostalCity.isEnabled = editable
        binding.tvCountry.isEnabled = editable
    }

    private fun splitStreetHouse(input: String): Pair<String, String> {
        // bardzo prosty split: ostatni token jako numer jeśli wygląda jak numer
        val parts = input.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (parts.isEmpty()) return "" to ""
        if (parts.size == 1) return parts[0] to ""

        val last = parts.last()
        val isNumberLike = last.any { it.isDigit() }
        return if (isNumberLike) {
            parts.dropLast(1).joinToString(" ") to last
        } else {
            input to ""
        }
    }

    private fun splitPostalCity(input: String): Pair<String, String> {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return "" to ""

        val parts = trimmed.split(Regex("\\s+"), limit = 2)
        if (parts.size == 1) return parts[0] to ""

        val first = parts[0]
        val rest = parts[1]
        val looksLikePostal = Regex("^\\d{2}-\\d{3}$").matches(first) || Regex("^\\d{5}$").matches(first)

        return if (looksLikePostal) first to rest else "" to trimmed
    }
}
