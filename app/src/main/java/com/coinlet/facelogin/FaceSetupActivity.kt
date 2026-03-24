package com.coinlet.facelogin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.coinlet.R
import com.coinlet.databinding.ActivityFaceSetupBinding
import java.io.File

class FaceSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFaceSetupBinding

    private val prefs by lazy { getSharedPreferences("security_prefs", MODE_PRIVATE) }
    private val keyEnabled = "face_login_enabled"

    // WAŻNE: ten sam plik co w FaceEnrollMfnActivity i FaceLoginMfnActivity
    private val modelFileName = "mfn_template.bin"

    private val enrollLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (hasModel()) {
                setCheckboxSilently(true)
                prefs.edit().putBoolean(keyEnabled, true).apply()
                binding.errorTextView.visibility = View.GONE
            } else {
                setCheckboxSilently(false)
                prefs.edit().putBoolean(keyEnabled, false).apply()
                binding.errorTextView.text =
                    "Nie zarejestrowano twarzy – logowanie twarzą wyłączone."
                binding.errorTextView.visibility = View.VISIBLE
            }
            refreshUi()
        }

    private val faceCheckedChangeListener =
        CompoundButton.OnCheckedChangeListener { _, isChecked ->
            binding.errorTextView.visibility = View.GONE

            if (isChecked) {
                if (hasModel()) {
                    refreshUi()
                } else {
                    enrollLauncher.launch(Intent(this, FaceEnrollMfnActivity::class.java))
                }
            } else {
                prefs.edit().putBoolean(keyEnabled, false).apply()
                refreshUi()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityFaceSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.faceSetupRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnBack.setOnClickListener { finish() }

        val fromOnboarding = intent.getBooleanExtra("fromOnboarding", false)
        val enabledFromPrefs = prefs.getBoolean(keyEnabled, false)

        val initialChecked = when {
            fromOnboarding && !prefs.contains(keyEnabled) -> false
            else -> enabledFromPrefs && hasModel()
        }

        setCheckboxSilently(initialChecked)
        binding.checkboxEnableFaceLogin.setOnCheckedChangeListener(faceCheckedChangeListener)

        binding.btnContinue.setOnClickListener {
            val wantsEnabled = binding.checkboxEnableFaceLogin.isChecked

            if (wantsEnabled && !hasModel()) {
                binding.errorTextView.text =
                    "Najpierw zarejestruj twarz, aby włączyć logowanie twarzą."
                binding.errorTextView.visibility = View.VISIBLE
                enrollLauncher.launch(Intent(this, FaceEnrollMfnActivity::class.java))
                return@setOnClickListener
            }

            prefs.edit().putBoolean(keyEnabled, wantsEnabled).apply()
            goToDashboard()
        }

        binding.btnSkip.setOnClickListener {
            prefs.edit().putBoolean(keyEnabled, false).apply()
            goToDashboard()
        }

        if (fromOnboarding && !hasModel()) {
            binding.errorTextView.visibility = View.GONE
        }

        refreshUi()
    }

    override fun onResume() {
        super.onResume()

        // Fallback: gdyby użytkownik wrócił po rejestracji bez callbacka,
        // a plik już istnieje, od razu zaznacz checkbox.
        if (hasModel()) {
            binding.errorTextView.visibility = View.GONE
            if (!binding.checkboxEnableFaceLogin.isChecked) {
                setCheckboxSilently(true)
            }
        }

        refreshUi()
    }

    private fun hasModel(): Boolean = File(filesDir, modelFileName).exists()

    private fun setCheckboxSilently(checked: Boolean) {
        binding.checkboxEnableFaceLogin.setOnCheckedChangeListener(null)
        binding.checkboxEnableFaceLogin.isChecked = checked
        binding.checkboxEnableFaceLogin.setOnCheckedChangeListener(faceCheckedChangeListener)
    }

    private fun refreshUi() {
        val enabled = binding.checkboxEnableFaceLogin.isChecked
        val model = hasModel()

        if (model) {
            binding.errorTextView.visibility = View.GONE
        }

        binding.infoTextView.text = when {
            !enabled -> "Logowanie twarzą jest wyłączone. Możesz korzystać z PIN."
            enabled && model -> "Logowanie twarzą jest włączone. Wzorzec twarzy zapisany lokalnie ✅"
            else -> "Włączone, ale brak wzorca. Zarejestruj twarz, aby aktywować logowanie."
        }

        binding.btnContinue.isEnabled = (!enabled) || model
        binding.btnContinue.alpha = if (binding.btnContinue.isEnabled) 1.0f else 0.5f
    }

    private fun goToDashboard() {
        val intent = Intent(this, com.coinlet.app.Dashboard::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}