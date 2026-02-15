package com.coinlet.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.coinlet.R
import com.coinlet.databinding.ActivityTransferBinding
import com.coinlet.model.Transactions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

class TransferActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransferBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityTransferBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        FirebaseFirestore.setLoggingEnabled(true)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnSendTransfer.setOnClickListener { sendTransfer() }
    }

    private fun validateTransfer(receiver: String, iban: String, amount: String): Boolean {
        if (receiver.isEmpty()) {
            binding.etReceiver.error = "Podaj odbiorcę"
            return false
        }
        if (iban.length < 15) {
            binding.etIban.error = "Niepoprawny IBAN"
            return false
        }
        if (amount.isEmpty() || amount.toDoubleOrNull() == null) {
            binding.etAmount.error = "Niepoprawna kwota"
            return false
        }
        return true
    }

    private fun sendTransfer() {
        val senderUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val receiverName = binding.etReceiver.text.toString().trim()
        val receiverIban = binding.etIban.text.toString().trim()
        val amountText = binding.etAmount.text.toString().trim()
        val title = binding.etTitle.text.toString().trim()

        if (!validateTransfer(receiverName, receiverIban, amountText)) return
        val amount = amountText.toDouble()

        db.collection("users")
            .document(senderUserId)
            .collection("accounts")
            .limit(1)
            .get()
            .addOnSuccessListener { senderSnap ->
                if (senderSnap.isEmpty) {
                    Toast.makeText(this, "Brak konta nadawcy", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val senderAccountRef = senderSnap.documents.first().reference

                findAccountByIban(
                    db = db,
                    iban = receiverIban,
                    onSuccess = { receiverAccountRef ->
                        db.runTransaction { tr ->
                            val senderFresh = tr.get(senderAccountRef)
                            val receiverFresh = tr.get(receiverAccountRef)

                            val senderBalance = senderFresh.getDoubleSafe("balance")
                            val receiverBalance = receiverFresh.getDoubleSafe("balance")

                            if (senderBalance < amount) throw Exception("Brak środków")

                            tr.update(senderAccountRef, "balance", senderBalance - amount)
                            tr.update(receiverAccountRef, "balance", receiverBalance + amount)

                            // outgoing u nadawcy
                            val outgoingTx = Transactions(
                                amount = amount,
                                receiverName = receiverName,
                                receiverIban = receiverIban,
                                title = title,
                                type = "outgoing"
                            )
                            tr.set(senderAccountRef.collection("transactions").document(), outgoingTx)

                            val incomingTx = Transactions(
                                amount = amount,
                                receiverName = receiverName,
                                receiverIban = receiverIban,
                                title = title,
                                type = "incoming"
                            )
                            tr.set(receiverAccountRef.collection("transactions").document(), incomingTx)

                            null
                        }.addOnSuccessListener {
                            Toast.makeText(this, "Przelew wykonany", Toast.LENGTH_SHORT).show()
                            finish()
                        }.addOnFailureListener { e ->
                            Toast.makeText(this, e.message ?: "Błąd przelewu", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onError = { msg ->
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, e.message ?: "Błąd pobierania konta", Toast.LENGTH_SHORT).show()
            }
    }

    private fun findAccountByIban(
        db: FirebaseFirestore,
        iban: String,
        onSuccess: (DocumentReference) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collectionGroup("accounts")
            .whereEqualTo("accountNumber", iban)
            .limit(1)
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    onError("Nie znaleziono konta o podanym IBAN")
                    return@addOnSuccessListener
                }
                onSuccess(snap.documents.first().reference)
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Błąd wyszukiwania konta po IBAN")
            }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.getDoubleSafe(field: String): Double {
        val any = get(field)
        return when (any) {
            is Long -> any.toDouble()
            is Double -> any
            is Int -> any.toDouble()
            else -> 0.0
        }
    }
}
