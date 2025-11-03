package com.coinlet.app

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.coinlet.R
import com.coinlet.databinding.ActivityDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Dashboard : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnTransfer.setOnClickListener {
            startActivity(Intent(this, TransferActivity::class.java))
        }

        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, TransferHistory::class.java))
        }

        binding.btnUserProfile.setOnClickListener {view ->
            showPopup(view)
        }

        val userId = FirebaseAuth.getInstance().currentUser!!.uid

        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .document(userId)
            .collection("accounts")
            .get()
            .addOnSuccessListener { snapshot ->
                if(!snapshot.isEmpty()){
                    val account = snapshot.documents[0]
                    val balance = account.getDouble("balance") ?: 0.0
                    val currency = account.getString("currency") ?: "PLN"

                    binding.balanceTextView.text = "$balance $currency"
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Błąd odczytu", Toast.LENGTH_SHORT).show()
            }


    }

    private fun showPopup(view: View) {
        val popup = PopupMenu(this, view)
        popup.inflate(R.menu.popupmenu)

        popup.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item: MenuItem? ->

            when (item!!.itemId) {
                R.id.action_settings -> {
                    Toast.makeText(this, item.title, Toast.LENGTH_SHORT).show()
                }
                R.id.action_logout -> {
                SplashScreen.auth.signOut()
                startActivity(Intent(this, SplashScreen::class.java))

                Toast.makeText(this, "Bezpiecznie zostałeś wylogowany!", Toast.LENGTH_LONG).show()                }
            }

            true
        })

        popup.show()
    }
}