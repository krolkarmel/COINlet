package com.coinlet.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.coinlet.R
import com.coinlet.databinding.ActivityTransferHistoryBinding
import com.coinlet.model.Transactions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.view.View
class TransferHistory : AppCompatActivity() {
    private lateinit var binding: ActivityTransferHistoryBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityTransferHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnBack.setOnClickListener {
            startActivity(Intent(this, Dashboard::class.java))
        }

        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        val adapter = TransferHistoryAdapter()
        binding.rvHistory.adapter = adapter

        loadHistory(adapter)
    }

    private fun loadHistory(adapter: TransferHistoryAdapter) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .document(uid)
            .collection("accounts")
            .limit(1)
            .get()
            .addOnSuccessListener { accountsSnap ->
                if (accountsSnap.isEmpty) {
                    showEmpty(adapter, empty = true)
                    return@addOnSuccessListener
                }

                val accountRef = accountsSnap.documents.first().reference

                accountRef.collection("transactions")
                    .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { txSnap ->
                        val list = txSnap.documents.mapNotNull { it.toObject(Transactions::class.java) }

                        adapter.setData(list)
                        showEmpty(adapter, empty = list.isEmpty())
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, it.message ?: "Błąd pobierania historii", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, it.message ?: "Błąd pobierania konta", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showEmpty(adapter: TransferHistoryAdapter, empty: Boolean) {
        binding.emptyState.visibility = if (empty) View.VISIBLE else View.GONE
        binding.rvHistory.visibility = if (empty) View.GONE else View.VISIBLE
    }

}