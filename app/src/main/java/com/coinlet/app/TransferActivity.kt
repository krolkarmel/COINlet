package com.coinlet.app

import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.coinlet.R
import com.coinlet.applock.AppLockPrefs
import com.coinlet.applock.PinCrypto
import com.coinlet.databinding.ActivityTransferBinding
import com.coinlet.model.Transactions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

class TransferActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransferBinding

    private var isTransferInProgress = false
    private var pinAttempts = 0

    private val pinThreshold = 500.0
    private val maxPinAttempts = 3

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

        binding.btnBack.setOnClickListener { finish() }
        binding.btnSendTransfer.setOnClickListener {
            if (!isTransferInProgress) {
                sendTransfer()
            }
        }
    }

    private fun clearErrors() {
        binding.etReceiver.error = null
        binding.etIban.error = null
        binding.etAmount.error = null
        binding.etTitle.error = null
    }

    private fun setTransferButtonEnabled(enabled: Boolean) {
        binding.btnSendTransfer.isEnabled = enabled
        binding.btnSendTransfer.alpha = if (enabled) 1.0f else 0.5f
    }

    private fun normalizeIban(value: String): String {
        return value.uppercase().filter { !it.isWhitespace() && it != '-' }
    }

    private fun validateTransfer(
        receiver: String,
        iban: String,
        amountText: String,
        title: String
    ): Boolean {
        clearErrors()

        var isValid = true

        if (receiver.isBlank()) {
            binding.etReceiver.error = "Podaj odbiorcę"
            isValid = false
        }

        if (iban.isBlank()) {
            binding.etIban.error = "Podaj numer IBAN"
            isValid = false
        } else if (iban.length !in 26..34) {
            binding.etIban.error = "Niepoprawny IBAN"
            isValid = false
        }

        val parsedAmount = amountText.replace(",", ".").toDoubleOrNull()
        if (amountText.isBlank()) {
            binding.etAmount.error = "Podaj kwotę"
            isValid = false
        } else if (parsedAmount == null) {
            binding.etAmount.error = "Niepoprawna kwota"
            isValid = false
        } else if (parsedAmount <= 0.0) {
            binding.etAmount.error = "Kwota musi być większa od 0"
            isValid = false
        }

        if (title.isBlank()) {
            binding.etTitle.error = "Podaj tytuł przelewu"
            isValid = false
        }

        return isValid
    }

    private fun sendTransfer() {
        val senderUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (senderUserId == null) {
            Toast.makeText(this, "Brak zalogowanego użytkownika", Toast.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseFirestore.getInstance()

        val receiverName = binding.etReceiver.text.toString().trim()
        val receiverIban = normalizeIban(binding.etIban.text.toString().trim())
        val amountText = binding.etAmount.text.toString().trim()
        val title = binding.etTitle.text.toString().trim()

        if (!validateTransfer(receiverName, receiverIban, amountText, title)) return

        val amount = amountText.replace(",", ".").toDoubleOrNull()
        if (amount == null) {
            binding.etAmount.error = "Niepoprawna kwota"
            return
        }

        isTransferInProgress = true
        setTransferButtonEnabled(false)

        db.collection("users")
            .document(senderUserId)
            .collection("accounts")
            .limit(1)
            .get()
            .addOnSuccessListener { senderSnap ->
                if (senderSnap.isEmpty) {
                    resetTransferState()
                    Toast.makeText(this, "Brak konta nadawcy", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val senderAccountDoc = senderSnap.documents.first()
                val senderAccountRef = senderAccountDoc.reference
                val senderIban = normalizeIban(senderAccountDoc.getString("accountNumber") ?: "")

                if (senderIban.isBlank()) {
                    resetTransferState()
                    Toast.makeText(this, "Nie udało się odczytać numeru konta nadawcy", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                if (senderIban == receiverIban) {
                    resetTransferState()
                    binding.etIban.error = "Nie można wykonać przelewu na własny rachunek"
                    Toast.makeText(this, "Nie można wykonać przelewu na własny rachunek", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val proceedWithTransfer = {
                    executeTransfer(
                        db = db,
                        senderAccountRef = senderAccountRef,
                        receiverName = receiverName,
                        receiverIban = receiverIban,
                        amount = amount,
                        title = title
                    )
                }

                if (amount > pinThreshold) {
                    showPinConfirmationDialog(
                        onVerified = proceedWithTransfer,
                        onCancelled = { resetTransferState() }
                    )
                } else {
                    proceedWithTransfer()
                }
            }
            .addOnFailureListener { e ->
                resetTransferState()
                Toast.makeText(this, e.message ?: "Błąd pobierania konta", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showPinConfirmationDialog(
        onVerified: () -> Unit,
        onCancelled: () -> Unit
    ) {
        pinAttempts = 0

        val input = EditText(this).apply {
            hint = "Wpisz PIN"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            filters = arrayOf(InputFilter.LengthFilter(4))
        }

        if (!isPinConfigured()) {
            Toast.makeText(this, "PIN nie jest ustawiony. Ustaw PIN ponownie.", Toast.LENGTH_SHORT).show()
            onCancelled()
            return
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Potwierdzenie PIN")
            .setMessage("Przelewy powyżej 500 zł wymagają potwierdzenia kodem PIN.")
            .setView(input)
            .setCancelable(true)
            .setPositiveButton("Potwierdź", null)
            .setNegativeButton("Anuluj") { d, _ ->
                d.dismiss()
                Toast.makeText(this, "Operacja anulowana", Toast.LENGTH_SHORT).show()
                onCancelled()
            }
            .create()

        dialog.setOnCancelListener {
            Toast.makeText(this, "Operacja anulowana", Toast.LENGTH_SHORT).show()
            onCancelled()
        }

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val enteredPin = input.text.toString().trim()

                if (enteredPin.length != 4) {
                    input.error = "PIN musi mieć 4 cyfry"
                    return@setOnClickListener
                }

                if (verifyPinWithStoredHash(enteredPin)) {
                    dialog.dismiss()
                    Toast.makeText(this, "PIN potwierdzony", Toast.LENGTH_SHORT).show()
                    onVerified()
                } else {
                    pinAttempts++
                    val attemptsLeft = maxPinAttempts - pinAttempts

                    if (attemptsLeft > 0) {
                        input.error = "Nieprawidłowy PIN"
                        input.text.clear()
                        Toast.makeText(
                            this,
                            "Nieprawidłowy PIN. Pozostało prób: $attemptsLeft",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        dialog.dismiss()
                        Toast.makeText(
                            this,
                            "3 błędne próby PIN. Operacja anulowana.",
                            Toast.LENGTH_LONG
                        ).show()
                        onCancelled()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun isPinConfigured(): Boolean {
        val prefs = AppLockPrefs(this)
        return prefs.getPinHash() != null && prefs.getPinSalt() != null
    }

    private fun verifyPinWithStoredHash(enteredPin: String): Boolean {
        val prefs = AppLockPrefs(this)

        val storedHashB64 = prefs.getPinHash() ?: return false
        val storedSaltB64 = prefs.getPinSalt() ?: return false

        val salt = PinCrypto.fromB64(storedSaltB64)
        val storedHash = PinCrypto.fromB64(storedHashB64)
        val typedHash = PinCrypto.hashPin(enteredPin, salt)

        return PinCrypto.constantTimeEquals(typedHash, storedHash)
    }

    private fun executeTransfer(
        db: FirebaseFirestore,
        senderAccountRef: DocumentReference,
        receiverName: String,
        receiverIban: String,
        amount: Double,
        title: String
    ) {
        findAccountByIban(
            db = db,
            iban = receiverIban,
            onSuccess = { receiverAccountRef ->
                db.runTransaction { tr ->
                    val senderFresh = tr.get(senderAccountRef)
                    val receiverFresh = tr.get(receiverAccountRef)

                    val senderBalance = senderFresh.getDoubleSafe("balance")
                    val receiverBalance = receiverFresh.getDoubleSafe("balance")

                    if (senderBalance < amount) {
                        throw Exception("Brak środków na koncie")
                    }

                    tr.update(senderAccountRef, "balance", senderBalance - amount)
                    tr.update(receiverAccountRef, "balance", receiverBalance + amount)

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
                    setResult(RESULT_OK)
                    finish()
                }.addOnFailureListener { e ->
                    resetTransferState()
                    Toast.makeText(this, e.message ?: "Błąd przelewu", Toast.LENGTH_SHORT).show()
                }
            },
            onError = { msg ->
                resetTransferState()
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun resetTransferState() {
        isTransferInProgress = false
        setTransferButtonEnabled(true)
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