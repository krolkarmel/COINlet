package com.coinlet.register

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.coinlet.R
import com.coinlet.databinding.ActivityIdRegisterStepBinding
import androidx.core.content.FileProvider
import java.io.File
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.content.ContextCompat

class IdRegisterStep : AppCompatActivity() {
    private lateinit var binding: ActivityIdRegisterStepBinding
    private enum class Side { PHOTO_1, PHOTO_2 }
    private var currentSide: Side = Side.PHOTO_1
    private var photo1Uri: Uri? = null
    private var photo2Uri: Uri? = null
    private var pendingCameraUri: Uri? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityIdRegisterStepBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        binding.btnTakePhoto.setOnClickListener {
            chooseSide { side ->
                runWithCameraPermission {
                    startCameraFor(side)
                }
            }
        }

        binding.btnPickFromGallery.setOnClickListener {
            chooseSide {
                pickFromGalleryLauncher.launch("image/*")
            }
        }

        updateNextButtonState()

        binding.btnNext.setOnClickListener {
            if (photo1Uri == null || photo2Uri == null){
                return@setOnClickListener
                Toast.makeText(this, "Dodaj oba zdjęcia", Toast.LENGTH_SHORT).show()
            }

            val nationality = intent.getStringExtra("nationality") ?: ""
            val phoneNumber = intent.getStringExtra("phoneNumber") ?: ""
            val firstName = intent.getStringExtra("firstName") ?: ""
            val secondName = intent.getStringExtra("secondName") ?: ""
            val lastName = intent.getStringExtra("lastName") ?: ""
            val birthDate = intent.getStringExtra("birthDate") ?: ""
            val pesel = intent.getStringExtra("pesel") ?: ""
            val email = intent.getStringExtra("email") ?: ""
            val intent = Intent(this, FourthRegisterStep::class.java)
            intent.putExtra("nationality", nationality)
            intent.putExtra("phoneNumber", phoneNumber)
            intent.putExtra("firstName", firstName)
            intent.putExtra("secondName", secondName)
            intent.putExtra("lastName", lastName)
            intent.putExtra("birthDate", birthDate)
            intent.putExtra("pesel", pesel)
            intent.putExtra("email", email)
            intent.putExtra("idPhoto1Uri", photo1Uri.toString())
            intent.putExtra("idPhoto2Uri", photo2Uri.toString())
            startActivity(intent)
        }
    }

    private fun updateNextButtonState(){
        binding.btnNext.isEnabled = photo1Uri != null && photo2Uri != null
    }
    private fun chooseSide(onChosen: (Side) -> Unit) {
        val options = arrayOf("Zdjęcie 1", "Zdjęcie 2")

        AlertDialog.Builder(this)
            .setTitle("Wybierz stronę dowodu")
            .setItems(options) { _, which ->
                val side = if (which == 0) Side.PHOTO_1 else Side.PHOTO_2
                currentSide = side
                onChosen(side)
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (!success) return@registerForActivityResult
        val uri = pendingCameraUri ?: return@registerForActivityResult
        onImageChosen(uri)
    }

    private val pickFromGalleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onImageChosen(it) }
    }

    private fun onImageChosen(uri: Uri) {
        when (currentSide) {
            Side.PHOTO_1 -> {
                photo1Uri = uri
                binding.ivFront.setImageURI(uri)
            }
            Side.PHOTO_2 -> {
                photo2Uri = uri
                binding.ivBack.setImageURI(uri)
            }
        }
        updateNextButtonState()
    }

    private fun createTempImageUri(): Uri {
        val dir = File(cacheDir, "images").apply { mkdirs() }
        val file = File(dir, "id_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
    }

    private var afterCameraPermission: (() -> Unit)? = null

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                afterCameraPermission?.invoke()
            } else {
                Toast.makeText(this, "Brak dostępu do aparatu", Toast.LENGTH_SHORT).show()
            }
            afterCameraPermission = null
        }

    private fun runWithCameraPermission(action: () -> Unit) {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

        if (granted) {
            action()
        } else {
            afterCameraPermission = action
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    private fun startCameraFor(side: Side) {
        val uri = createTempImageUri()
        pendingCameraUri = uri
        currentSide = side
        takePictureLauncher.launch(uri)
    }
}