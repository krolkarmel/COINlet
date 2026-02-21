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
import androidx.recyclerview.widget.LinearLayoutManager
import com.coinlet.R
import com.coinlet.databinding.ActivityDashboardBinding
import com.coinlet.model.Transactions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlin.jvm.java

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

        binding.rvLastTransactions.layoutManager = LinearLayoutManager(this)
        val lastAdapter = TransferHistoryAdapter()
        binding.rvLastTransactions.adapter = lastAdapter

        loadLastTransactions(lastAdapter)

        loadIban()

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
                    startActivity(Intent(this, ActivityUserProfile::class.java))
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
    private fun loadIban() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .document(uid)
            .collection("accounts")
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val doc = snapshot.documents[0]
                    val iban = doc.getString("accountNumber") ?: ""
                    binding.ibanTextView.text =
                        if (iban.isNotBlank()) "IBAN: $iban" else "IBAN: —"
                } else {
                    binding.ibanTextView.text = "IBAN: —"
                }
            }
            .addOnFailureListener {
                binding.ibanTextView.text = "IBAN: —"
            }
    }

    private fun loadLastTransactions(adapter: TransferHistoryAdapter) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(uid)
            .collection("accounts")
            .limit(1)
            .get()
            .addOnSuccessListener { accountsSnap ->
                if (accountsSnap.isEmpty) {
                    adapter.setData(emptyList())
                    return@addOnSuccessListener
                }

                val accountRef = accountsSnap.documents.first().reference

                accountRef.collection("transactions")
                    .orderBy("date", Query.Direction.DESCENDING)
                    .limit(3)
                    .get()
                    .addOnSuccessListener { txSnap ->
                        val list = txSnap.documents.mapNotNull { it.toObject(Transactions::class.java) }
                        adapter.setData(list)
                    }
            }
    }


}