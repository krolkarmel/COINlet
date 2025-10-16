package com.coinlet.register

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.coinlet.R
import com.coinlet.databinding.ActivitySecondStepRegisterBinding

class SecondStepRegister : AppCompatActivity() {
    private lateinit var binding: ActivitySecondStepRegisterBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySecondStepRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }



        binding.btnSendCode.setOnClickListener {
            val nationality = intent.getStringExtra("nationality") ?: ""
            val phoneNumber = binding.phoneNumberInputText.text.toString()
            val intent = Intent(this, ConfirmCodeRegisterStep::class.java)
            intent.putExtra("nationality", nationality)
            intent.putExtra("phoneNumber", phoneNumber)
            startActivity(intent)
        }


    }
}