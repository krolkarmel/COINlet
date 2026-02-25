package com.coinlet.facelogin

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
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
    private val modelFileName = "lbph_model.yml"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val engine = com.coinlet.facelogin.MobileFaceNetEngine(this)
        android.util.Log.d("MFN", "Engine init OK, input=${engine.inputSize}, emb=${engine.embeddingDim}")

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
        if (fromOnboarding && !prefs.contains(keyEnabled)) {
            binding.checkboxEnableFaceLogin.isChecked = true
        } else {
            binding.checkboxEnableFaceLogin.isChecked = enabledFromPrefs
        }

        binding.checkboxEnableFaceLogin.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !hasModel()) {
                binding.errorTextView.visibility = View.GONE
                startActivity(Intent(this, FaceEnrollActivity::class.java))
            }
            refreshUi()
        }

        binding.btnContinue.setOnClickListener {
            val wantsEnabled = binding.checkboxEnableFaceLogin.isChecked

            if (wantsEnabled && !hasModel()) {
                binding.errorTextView.text =
                    "Najpierw zarejestruj twarz, aby włączyć logowanie twarzą."
                binding.errorTextView.visibility = View.VISIBLE
                startActivity(Intent(this, FaceEnrollActivity::class.java))
                return@setOnClickListener
            }

            prefs.edit().putBoolean(keyEnabled, wantsEnabled).apply()
            goToDashboard()
        }

        binding.btnSkip.setOnClickListener {
            prefs.edit().putBoolean(keyEnabled, false).apply()
            goToDashboard()
        }

        if (fromOnboarding && binding.checkboxEnableFaceLogin.isChecked && !hasModel()) {
            startActivity(Intent(this, FaceEnrollActivity::class.java))
        }

        refreshUi()
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    private fun hasModel(): Boolean = File(filesDir, modelFileName).exists()

    private fun refreshUi() {
        val enabled = binding.checkboxEnableFaceLogin.isChecked
        val model = hasModel()

        binding.errorTextView.visibility = View.GONE

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