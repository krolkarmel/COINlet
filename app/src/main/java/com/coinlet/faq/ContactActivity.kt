package com.coinlet.faq

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.coinlet.R
import com.coinlet.databinding.ActivityContactBinding

class ContactActivity : AppCompatActivity() {

    private lateinit var binding: ActivityContactBinding

    private val supportEmail = "coinlet.support@mail.com"
    private val supportPhone = "+48 33 123 11 33"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityContactBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnBack.setOnClickListener { finish() }

        binding.btnEmail.setOnClickListener {
            showEmailDialog()
        }

        binding.btnPhone.setOnClickListener {
            showPhoneDialog()
        }
    }

    private fun showEmailDialog() {
        AlertDialog.Builder(this)
            .setTitle("Kontakt e-mail")
            .setMessage(supportEmail)
            .setPositiveButton("Kopiuj") { _, _ ->
                copyToClipboard("Email", supportEmail)
            }
            .setNegativeButton("Zamknij", null)
            .show()
    }

    private fun showPhoneDialog() {
        AlertDialog.Builder(this)
            .setTitle("Kontakt telefoniczny")
            .setMessage(supportPhone)
            .setPositiveButton("Kopiuj") { _, _ ->
                copyToClipboard("Telefon", supportPhone)
            }
            .setNegativeButton("Zamknij", null)
            .show()
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(this, "$label skopiowano", Toast.LENGTH_SHORT).show()
    }
}