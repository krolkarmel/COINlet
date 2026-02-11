package com.coinlet.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.coinlet.R
import com.coinlet.applock.AppLockPrefs
import com.coinlet.applock.EnterPinActivity
import com.coinlet.applock.LockActivity
import com.coinlet.databinding.ActivitySplashScreenBinding
import com.coinlet.login.Login
import com.coinlet.register.FirstRegisterStep
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SplashScreen : AppCompatActivity() {

    private lateinit var binding: ActivitySplashScreenBinding
    private lateinit var db: FirebaseFirestore

    companion object {
        lateinit var auth: FirebaseAuth
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val user = auth.currentUser

        if (user == null || user.isAnonymous) {
            showSplashScreen()
        } else {
            routeLoggedUser(user.uid)
        }
    }

    private fun showSplashScreen() {
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnRegister.setOnClickListener {
            startActivity(Intent(this, FirstRegisterStep::class.java))
        }

        binding.btnLogin.setOnClickListener {
            startActivity(Intent(this, Login::class.java))
        }
    }

    private fun routeLoggedUser(userId: String) {
        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { snapshot ->

                if (!snapshot.exists()) {
                    auth.signOut()
                    showSplashScreen()
                    return@addOnSuccessListener
                }

                val appLockEnabled = snapshot.getBoolean("appLockEnabled") ?: false
                val needsAppLockSetup = snapshot.getBoolean("needsAppLockSetup") ?: false
                val trustedDevices = snapshot.get("trustedDevices") as? Map<*, *>

                val deviceId = Settings.Secure.getString(
                    contentResolver,
                    Settings.Secure.ANDROID_ID
                ) ?: "unknown"

                val isTrusted = trustedDevices?.get(deviceId) == true

                val prefs = AppLockPrefs(this)
                val hasLocalLock = prefs.isPinSet() || prefs.isBiometricsEnabled()

                if (needsAppLockSetup && !hasLocalLock) {
                    startActivity(Intent(this, EnterPinActivity::class.java))
                    finish()
                    return@addOnSuccessListener
                }

                val target = when {
                    !appLockEnabled -> Dashboard::class.java

                    isTrusted -> Dashboard::class.java

                    !isTrusted && hasLocalLock -> LockActivity::class.java

                    else -> EnterPinActivity::class.java
                }

                startActivity(Intent(this, target))
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Błąd odczytu danych: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, Dashboard::class.java))
                finish()
            }
    }
}
