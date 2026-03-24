package com.coinlet.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.coinlet.R
import com.coinlet.applock.AppLockPrefs
import com.coinlet.applock.BiometricSettings
import com.coinlet.databinding.ActivityDashboardBinding
import com.coinlet.model.Transactions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class Dashboard : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var lastAdapter: TransferHistoryAdapter
    private var rawIbanToCopy: String? = null
    private var isUserVerified: Boolean = false

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
            if (isUserVerified) {
                startActivity(Intent(this, TransferActivity::class.java))
            } else {
                Toast.makeText(
                    this,
                    "Przelewy są dostępne tylko dla zweryfikowanych kont",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, TransferHistory::class.java))
        }

        binding.btnUserProfile.setOnClickListener { view ->
            showPopup(view)
        }

        binding.ibanTextView.setOnClickListener {
            copyIbanToClipboard()
        }

        binding.rvLastTransactions.layoutManager = LinearLayoutManager(this)
        lastAdapter = TransferHistoryAdapter()
        binding.rvLastTransactions.adapter = lastAdapter

        loadLastTransactions(lastAdapter)
        loadIban()
        loadBalance()
        loadVerificationStatus()
    }

    override fun onResume() {
        super.onResume()
        loadBalance()
        loadIban()
        loadLastTransactions(lastAdapter)
        loadVerificationStatus()
    }

    private fun formatIbanForDisplay(iban: String): String {
        val cleanIban = iban.replace(" ", "")

        if (cleanIban.length <= 2) return cleanIban

        val firstPart = cleanIban.take(2)
        val remainingPart = cleanIban.drop(2)

        return "$firstPart ${remainingPart.chunked(4).joinToString(" ")}"
    }

    private fun copyIbanToClipboard() {
        val iban = rawIbanToCopy

        if (iban.isNullOrBlank()) {
            Toast.makeText(this, "Brak numeru IBAN do skopiowania", Toast.LENGTH_SHORT).show()
            return
        }

        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("IBAN", iban)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(this, "IBAN skopiowany", Toast.LENGTH_SHORT).show()
    }

    private fun loadBalance() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .document(userId)
            .collection("accounts")
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
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

    private fun loadVerificationStatus() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                isUserVerified = doc.getBoolean("isVerified") ?: false
                binding.btnTransfer.isEnabled = isUserVerified
                binding.btnTransfer.alpha = if (isUserVerified) 1.0f else 0.5f
            }
            .addOnFailureListener {
                isUserVerified = false
                binding.btnTransfer.isEnabled = false
                binding.btnTransfer.alpha = 0.5f
            }
    }

    private fun showPopup(anchor: View) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->

                val name = doc.getString("firstName")
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: currentUser.displayName
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }
                    ?: "Użytkowniku"

                val isVerified = doc.getBoolean("isVerified") ?: false

                val popup = PopupMenu(this, anchor)
                popup.inflate(R.menu.popupmenu)

                try {
                    val field = PopupMenu::class.java.getDeclaredField("mPopup")
                    field.isAccessible = true
                    val mPopup = field.get(popup)
                    val setForceShowIcon =
                        mPopup.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                    setForceShowIcon.invoke(mPopup, true)
                } catch (_: Exception) {
                }

                val miWelcome = popup.menu.findItem(R.id.label_welcome)
                val miVerified = popup.menu.findItem(R.id.label_verified)

                miWelcome.title = "Witaj,\n$name"
                miVerified.title =
                    if (isVerified) "Konto\nzweryfikowane" else "Konto\nniezweryfikowane"
                miVerified.setIcon(if (isVerified) R.drawable.verified else R.drawable.noverified)

                val lockPrefs = AppLockPrefs(this)
                val parts = mutableListOf<String>()
                if (lockPrefs.isFingerprintEnabled()) parts.add("odcisk")
                if (lockPrefs.isFaceEnabled()) parts.add("twarz")

                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.action_biometrics -> {
                            startActivity(Intent(this, BiometricSettings::class.java))
                            true
                        }

                        R.id.action_settings -> {
                            startActivity(Intent(this, ActivityUserProfile::class.java))
                            true
                        }

                        R.id.action_change_pin -> {
                            startActivity(Intent(this, ChangePinActivity::class.java))
                            true
                        }

                        R.id.action_logout -> {
                            SplashScreen.auth.signOut()
                            startActivity(Intent(this, SplashScreen::class.java))
                            Toast.makeText(
                                this,
                                "Bezpiecznie zostałeś wylogowany!",
                                Toast.LENGTH_LONG
                            ).show()
                            finish()
                            true
                        }

                        else -> true
                    }
                }

                popup.show()
            }
            .addOnFailureListener {
                val popup = PopupMenu(this, anchor)
                popup.inflate(R.menu.popupmenu)
                popup.show()
            }
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
                    val iban = (doc.getString("accountNumber") ?: "").replace(" ", "")

                    if (iban.isNotBlank()) {
                        rawIbanToCopy = iban
                        binding.ibanTextView.text = "IBAN: ${formatIbanForDisplay(iban)}"
                    } else {
                        rawIbanToCopy = null
                        binding.ibanTextView.text = "IBAN: —"
                    }
                } else {
                    rawIbanToCopy = null
                    binding.ibanTextView.text = "IBAN: —"
                }
            }
            .addOnFailureListener {
                rawIbanToCopy = null
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
                        val list =
                            txSnap.documents.mapNotNull { it.toObject(Transactions::class.java) }
                        adapter.setData(list)
                    }
            }
    }
}