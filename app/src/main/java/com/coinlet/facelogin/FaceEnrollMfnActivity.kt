package com.coinlet.facelogin

import android.Manifest
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
import com.coinlet.databinding.ActivityFaceEnrollBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

class FaceEnrollMfnActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFaceEnrollBinding
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private lateinit var engine: MobileFaceNetEngine

    private var cameraStarted = false
    private var isCameraStarting = false
    private var isCollecting = false
    private var isEnrollmentFinished = false
    private var samplesCollected = 0
    private var lastSampleTimeMs = 0L

    private var isFrontCamera = true

    private val targetSamples = 14
    private val embeddings = mutableListOf<FloatArray>()

    private val templateFileName = "mfn_template.bin"

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
            if (granted) {
                binding.statusTextView.text = "Status: Uruchamianie kamery..."
                ensureCameraStarted()
            } else {
                binding.statusTextView.text = "Status: Brak uprawnień do kamery"
                Toast.makeText(this, "Brak uprawnień do kamery", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityFaceEnrollBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        engine = MobileFaceNetEngine(this)

        binding.btnBack.setOnClickListener { finish() }

        binding.progressTextView.text = "0/$targetSamples"
        binding.statusTextView.text = "Status: Uruchamianie kamery..."

        ensureCameraPermissionAndStart()
    }

    override fun onResume() {
        super.onResume()
        if (hasCameraPermission() && !isEnrollmentFinished) {
            ensureCameraStarted()
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun resetCollection() {
        embeddings.clear()
        samplesCollected = 0
        lastSampleTimeMs = 0L
        binding.progressTextView.text = "0/$targetSamples"
    }

    private fun startAutoCollection() {
        if (isEnrollmentFinished) return

        resetCollection()
        isCollecting = true
        binding.statusTextView.text =
            "Status: Zbieranie próbek... (patrz prosto, podejdź bliżej)"
    }

    private fun ensureCameraPermissionAndStart() {
        if (hasCameraPermission()) {
            ensureCameraStarted()
        } else {
            binding.statusTextView.text = "Status: Oczekiwanie na zgodę kamery"
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun ensureCameraStarted() {
        if (cameraStarted || isCameraStarting) return
        isCameraStarting = true
        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
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

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, selector, preview, analysis)

                cameraStarted = true
                isCameraStarting = false
                binding.statusTextView.text = "Status: Kamera działa"

                startAutoCollection()
            } catch (_: Exception) {
                cameraStarted = false
                isCameraStarting = false
                binding.statusTextView.text = "Status: Błąd kamery"
                Toast.makeText(this, "Nie udało się uruchomić kamery", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun analyzeFrame(imageProxy: ImageProxy) {
        try {
            if (!isCollecting || isEnrollmentFinished) return

            val now = SystemClock.elapsedRealtime()
            if (now - lastSampleTimeMs < 450) return
            lastSampleTimeMs = now

            val rotation = imageProxy.imageInfo.rotationDegrees

            val raw = imageProxyToBitmap(imageProxy) ?: return
            val bmp = rotateAndMirror(raw, rotation, mirror = isFrontCamera)

            val image = InputImage.fromBitmap(bmp, 0)
            detector.process(image)
                .addOnSuccessListener { faces ->
                    if (!isCollecting || isEnrollmentFinished) return@addOnSuccessListener

                    if (faces.size != 1) {
                        binding.statusTextView.text =
                            if (faces.isEmpty()) "Status: Brak twarzy"
                            else "Status: Wiele twarzy (${faces.size})"
                        return@addOnSuccessListener
                    }

                    val face = faces[0]
                    val bb = face.boundingBox

                    if (bb.width() < minFaceSizePx || bb.height() < minFaceSizePx) {
                        binding.statusTextView.text = "Status: Podejdź bliżej"
                        return@addOnSuccessListener
                    }

                    val yaw = abs(face.headEulerAngleY)
                    val roll = abs(face.headEulerAngleZ)
                    if (yaw > maxYawDeg || roll > maxRollDeg) {
                        binding.statusTextView.text = "Status: Patrz prosto w kamerę"
                        return@addOnSuccessListener
                    }

                    val bb2 = squareExpandRect(bb, bmp.width, bmp.height, cropScale)
                    val crop = cropBitmapSafe(bmp, bb2)
                    if (crop == null) {
                        binding.statusTextView.text = "Status: Zły kadr"
                        return@addOnSuccessListener
                    }

                    val emb = engine.embeddingFromBitmap(crop)
                    embeddings.add(emb)

                    samplesCollected++
                    binding.progressTextView.text = "$samplesCollected/$targetSamples"
                    binding.statusTextView.text = "Status: Próbka zapisana"

                    if (samplesCollected >= targetSamples) {
                        isCollecting = false
                        isEnrollmentFinished = true
                        binding.statusTextView.text = "Status: Budowanie wzorca..."

                        val template = buildRobustTemplate(embeddings)
                        engine.saveTemplate(File(filesDir, templateFileName), template)

                        setResult(RESULT_OK)
                        binding.statusTextView.text = "Status: Zarejestrowano ✅"
                        Toast.makeText(this, "Twarz została zarejestrowana", Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener {
                    if (!isEnrollmentFinished) {
                        binding.statusTextView.text = "Status: Błąd detekcji"
                    }
                }
        } finally {
            imageProxy.close()
        }
    }

    private fun buildRobustTemplate(samples: List<FloatArray>): FloatArray {
        if (samples.isEmpty()) return FloatArray(192)

        val mean1 = l2Normalize(average(samples))

        val scored = samples.map { s -> s to cosine(mean1, s) }
            .sortedByDescending { it.second }

        val keep = max(8, (samples.size * 0.75f).toInt())
        val kept = scored.take(keep).map { it.first }

        val mean2 = average(kept)
        return l2Normalize(mean2)
    }

    private fun average(list: List<FloatArray>): FloatArray {
        val n = list[0].size
        val out = FloatArray(n)
        for (e in list) {
            for (i in 0 until n) {
                out[i] += e[i]
            }
        }
        for (i in 0 until n) out[i] /= list.size.toFloat()
        return out
    }

    private fun cosine(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        var na = 0f
        var nb = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            na += a[i] * a[i]
            nb += b[i] * b[i]
        }
        return dot / (sqrt(na) * sqrt(nb) + 1e-8f)
    }

    private fun l2Normalize(x: FloatArray): FloatArray {
        var sum = 0f
        for (v in x) sum += v * v
        val norm = sqrt(sum) + 1e-10f
        return FloatArray(x.size) { i -> x[i] / norm }
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