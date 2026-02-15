package com.coinlet.applock

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.coinlet.R
import com.coinlet.Validator
import com.coinlet.databinding.ActivityEnterPinBinding

class EnterPinActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEnterPinBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityEnterPinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.pinRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnConfirmPin.setOnClickListener {
            if (!validateForm()) return@setOnClickListener

            val enteredPin = binding.textInputPin.text.toString()

            val i = Intent(this, ConfirmPinActivity::class.java).apply {
                putExtra("enteredPin", enteredPin)
            }
            startActivity(i)

        }
    }

    private fun validateForm(): Boolean {
        val pin = binding.textInputPin.text.toString()
        return if (!Validator.isPinGood(pin)) {
            binding.textInputPin.error = "PIN jest niezgodny z regulaminem!"
            false
        } else {
            binding.textInputPin.error = null
            true
        }
    }
}
