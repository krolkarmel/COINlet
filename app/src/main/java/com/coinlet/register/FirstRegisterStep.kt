package com.coinlet.register

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.coinlet.R
import com.coinlet.Validator
import com.coinlet.databinding.ActivityFirstRegisterStepBinding


class FirstRegisterStep : AppCompatActivity() {
    private lateinit var binding: ActivityFirstRegisterStepBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityFirstRegisterStepBinding.inflate(layoutInflater)
        setContentView(binding.root)


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }




        val spinner = findViewById<Spinner>(R.id.spinner)

        val arrayAdapter = ArrayAdapter.createFromResource(
            this@FirstRegisterStep,
            R.array.languages,
            android.R.layout.simple_spinner_dropdown_item
        )
        spinner.adapter = arrayAdapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                p0: AdapterView<*>?,
                p1: View?,
                p2: Int,
                p3: Long
            ) {
                //
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                //
            }
        }


    val spinner2 = findViewById<Spinner>(R.id.spinnerNationality)

    val arrayAdapter2 = ArrayAdapter.createFromResource(
        this@FirstRegisterStep,
        R.array.nationality,
        android.R.layout.simple_spinner_dropdown_item
    )
    spinner2.adapter = arrayAdapter2
    spinner2.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(
            p0: AdapterView<*>?,
            p1: View?,
            p2: Int,
            p3: Long
        ) {
            //
        }

        override fun onNothingSelected(p0: AdapterView<*>?) {
            //
        }
    }

            binding.btnNext.setOnClickListener {
                if(validateForm()) {
                    val nationality = binding.spinnerNationality.selectedItem.toString()
                    val intent = Intent(this, SecondStepRegister::class.java)
                    intent.putExtra("nationality", nationality)
                    startActivity(intent)
                }
        }


}
    private fun validateForm(): Boolean{
        var isValid = true

        if(!Validator.validateCheckbox(binding.checkBoxAge.isChecked)){
            binding.checkBoxAge.error = "Wymagane jest ukończony 18 rok życia!"
//            Toast.makeText(this, "Wymagane jest ukończony 18 rok życia.", Toast.LENGTH_SHORT).show()
            isValid = false;
        }
        else{
            binding.checkBoxAge.error = null
        }
        return isValid
    }
}