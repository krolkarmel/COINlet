package com.coinlet.facelogin

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.coinlet.R
import com.coinlet.app.Dashboard
import com.coinlet.applock.LockActivity
import com.coinlet.databinding.ActivityFaceLoginBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max

class FaceLoginMfnActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFaceLoginBinding
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private lateinit var engine: MobileFaceNetEngine
    private lateinit var template: FloatArray

    private val templateFileName = "mfn_template.bin"

    private var cameraStarted = false
    private var verifying = false
    private var attempts = 0
    private var lastAttemptMs = 0L

    private var isFrontCamera = true

    private val maxAttempts = 5

    private val rejectTh = 0.50f
    private val acceptTh = 0.60f

    private val minFaceSizePx = 160
    private val maxYawDeg = 18f
    private val maxRollDeg = 18f

    private val cropScale = 1.35f

    private val detector by lazy {
        val opts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .enableTracking()
            .build()
        FaceDetection.getClient(opts)
    }

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) ensureCameraStarted()
            else Toast.makeText(this, "Brak uprawnień do kamery", Toast.LENGTH_LONG).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityFaceLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnBack.setOnClickListener { finish() }

        engine = MobileFaceNetEngine(this)

        val tplFile = File(filesDir, templateFileName)
        if (!tplFile.exists()) {
            binding.statusTextView.text = "Status: Brak wzorca – zarejestruj twarz"
        } else {
            template = engine.loadTemplate(tplFile)
            binding.statusTextView.text = "Status: Oczekiwanie…"
        }

        binding.btnStartVerify.setOnClickListener {
            val file = File(filesDir, templateFileName)
            if (!file.exists()) {
                setStatus("Status: Brak wzorca – zarejestruj twarz")
                return@setOnClickListener
            }
            template = engine.loadTemplate(file)

            ensureCameraPermissionAndStart()

            verifying = !verifying
            binding.btnStartVerify.text = if (verifying) "Stop" else "Start"
            attempts = 0
            setStatus(if (verifying) "Status: Weryfikacja…" else "Status: Zatrzymano")
        }

        binding.btnUsePin.setOnClickListener {
            startActivity(Intent(this, LockActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
            finish()
        }
    }

    private fun setStatus(msg: String) = runOnUiThread {
        binding.statusTextView.text = msg
    }

    private fun ensureCameraPermissionAndStart() {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        if (granted) ensureCameraStarted() else requestCameraPermission.launch(Manifest.permission.CAMERA)
    }

    private fun ensureCameraStarted() {
        if (cameraStarted) return
        cameraStarted = true
        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                analyzeFrame(imageProxy)
            }

            val selector = try {
                if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    isFrontCamera = true
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    isFrontCamera = false
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
            } catch (_: Exception) {
                isFrontCamera = false
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, selector, preview, analysis)
            } catch (_: Exception) {
                setStatus("Status: Błąd kamery")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun analyzeFrame(imageProxy: ImageProxy) {
        try {
            if (!verifying) return

            val now = SystemClock.elapsedRealtime()
            if (now - lastAttemptMs < 350) return
            lastAttemptMs = now

            val rotation = imageProxy.imageInfo.rotationDegrees

            val raw = imageProxyToBitmap(imageProxy) ?: return
            val bmp = rotateAndMirror(raw, rotation, mirror = isFrontCamera)

            val image = InputImage.fromBitmap(bmp, 0)
            detector.process(image)
                .addOnSuccessListener { faces ->
                    if (!verifying) return@addOnSuccessListener

                    if (faces.size != 1) {
                        setStatus(
                            if (faces.isEmpty()) "Status: Brak twarzy"
                            else "Status: Wiele twarzy (${faces.size})"
                        )
                        return@addOnSuccessListener
                    }

                    val face = faces[0]
                    val bb = face.boundingBox

                    if (bb.width() < minFaceSizePx || bb.height() < minFaceSizePx) {
                        setStatus("Status: Podejdź bliżej")
                        return@addOnSuccessListener
                    }

                    val yaw = abs(face.headEulerAngleY)
                    val roll = abs(face.headEulerAngleZ)
                    if (yaw > maxYawDeg || roll > maxRollDeg) {
                        setStatus("Status: Patrz prosto w kamerę")
                        return@addOnSuccessListener
                    }

                    val bb2 = squareExpandRect(bb, bmp.width, bmp.height, cropScale)
                    val crop = cropBitmapSafe(bmp, bb2)
                    if (crop == null) {
                        setStatus("Status: Zły kadr")
                        return@addOnSuccessListener
                    }

                    val emb = engine.embeddingFromBitmap(crop)
                    val sim = engine.cosine(template, emb)
                    val simTxt = "%.3f".format(sim)

                    when {
                        sim >= acceptTh -> {
                            verifying = false
                            runOnUiThread {
                                binding.statusTextView.text = "Status: Odblokowano ✅ "
                                startActivity(Intent(this, Dashboard::class.java))
                                finish()
                            }
                        }

                        sim < rejectTh -> {
                            attempts++
                            setStatus("Status: Odrzucono ❌  ($attempts/$maxAttempts)")
                            if (attempts >= maxAttempts) {
                                verifying = false
                                runOnUiThread {
                                    binding.btnStartVerify.text = "Start"
                                    binding.statusTextView.text = "Status: Zbyt wiele prób – użyj PIN"
                                }
                            }
                        }

                        else -> {
                            // ✅ Szara strefa: retry (bez naliczania próby)
                            setStatus("Status: Niepewne…  — podejdź bliżej / lepsze światło")
                        }
                    }
                }
                .addOnFailureListener {
                    setStatus("Status: Błąd detekcji")
                }
        } finally {
            imageProxy.close()
        }
    }

    private fun cropBitmapSafe(src: Bitmap, rect: Rect): Bitmap? {
        val left = rect.left.coerceAtLeast(0)
        val top = rect.top.coerceAtLeast(0)
        val right = rect.right.coerceAtMost(src.width)
        val bottom = rect.bottom.coerceAtMost(src.height)
        val w = right - left
        val h = bottom - top
        if (w <= 0 || h <= 0) return null
        return Bitmap.createBitmap(src, left, top, w, h)
    }

    private fun squareExpandRect(r: Rect, imgW: Int, imgH: Int, scale: Float): Rect {
        val cx = r.centerX()
        val cy = r.centerY()
        val size = (max(r.width(), r.height()) * scale).toInt()

        val left = (cx - size / 2).coerceAtLeast(0)
        val top = (cy - size / 2).coerceAtLeast(0)
        val right = (cx + size / 2).coerceAtMost(imgW)
        val bottom = (cy + size / 2).coerceAtMost(imgH)
        return Rect(left, top, right, bottom)
    }

    private fun rotateAndMirror(src: Bitmap, rotationDegrees: Int, mirror: Boolean): Bitmap {
        var out = src
        if (rotationDegrees != 0) {
            val m = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            out = Bitmap.createBitmap(out, 0, 0, out.width, out.height, m, true)
        }
        if (mirror) {
            val m2 = Matrix().apply { postScale(-1f, 1f, out.width / 2f, out.height / 2f) }
            out = Bitmap.createBitmap(out, 0, 0, out.width, out.height, m2, true)
        }
        return out
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        val yuvImage = imageProxyToYuvImage(image) ?: return null
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 90, out)
        val bytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun imageProxyToYuvImage(image: ImageProxy): YuvImage? {
        val nv21 = yuv420888ToNv21(image) ?: return null
        return YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
    }

    private fun yuv420888ToNv21(image: ImageProxy): ByteArray? {
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)

        val rowStride = uPlane.rowStride
        val pixelStride = uPlane.pixelStride
        val height = image.height

        val uBytes = ByteArray(uSize)
        val vBytes = ByteArray(vSize)
        uBuffer.get(uBytes)
        vBuffer.get(vBytes)

        val chromaHeight = height / 2
        val chromaWidth = image.width / 2

        var outputOffset = ySize
        for (row in 0 until chromaHeight) {
            for (col in 0 until chromaWidth) {
                val uIndex = row * rowStride + col * pixelStride
                val vIndex = row * vPlane.rowStride + col * vPlane.pixelStride
                nv21[outputOffset++] = vBytes[vIndex]
                nv21[outputOffset++] = uBytes[uIndex]
            }
        }
        return nv21
    }
}