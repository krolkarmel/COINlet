//package com.coinlet.facelogin
//
//import android.Manifest
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.graphics.Bitmap
//import android.graphics.BitmapFactory
//import android.graphics.ImageFormat
//import android.graphics.Rect
//import android.graphics.YuvImage
//import android.os.Bundle
//import android.os.SystemClock
//import android.widget.Toast
//import androidx.activity.enableEdgeToEdge
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.appcompat.app.AppCompatActivity
//import androidx.camera.core.CameraSelector
//import androidx.camera.core.ImageAnalysis
//import androidx.camera.core.ImageProxy
//import androidx.camera.core.Preview
//import androidx.camera.lifecycle.ProcessCameraProvider
//import androidx.core.content.ContextCompat
//import androidx.core.view.ViewCompat
//import androidx.core.view.WindowInsetsCompat
//import com.coinlet.R
//import com.coinlet.app.Dashboard
//import com.coinlet.applock.LockActivity
//import com.coinlet.databinding.ActivityFaceLoginBinding
//import org.bytedeco.opencv.global.opencv_core.CV_8UC4
//import org.bytedeco.opencv.global.opencv_imgproc.COLOR_RGBA2GRAY
//import org.bytedeco.opencv.global.opencv_imgproc.cvtColor
//import org.bytedeco.opencv.global.opencv_imgproc.equalizeHist
//import org.bytedeco.opencv.global.opencv_imgproc.resize
//import org.bytedeco.opencv.opencv_core.Mat
//import org.bytedeco.opencv.opencv_core.RectVector
//import org.bytedeco.opencv.opencv_core.Size
//import org.bytedeco.opencv.opencv_core.Rect as CvRect
//import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer
//import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier
//import java.io.ByteArrayOutputStream
//import java.io.File
//import java.io.FileOutputStream
//import java.nio.ByteBuffer
//import java.util.concurrent.Executors
//
//class FaceLoginActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityFaceLoginBinding
//    private val cameraExecutor = Executors.newSingleThreadExecutor()
//
//    private lateinit var faceCascade: CascadeClassifier
//    private lateinit var recognizer: LBPHFaceRecognizer
//
//    private val modelFileName = "lbph_model.yml"
//
//    private var cameraStarted = false
//    private var verifying = false
//    private var attempts = 0
//    private var lastAttemptMs = 0L
//
//    // startowy próg – potem dopracujesz testami na Xiaomi
//    private val threshold = 49.0
//    private val maxAttempts = 5
//
//    private val requestCameraPermission =
//        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
//            if (granted) ensureCameraStarted()
//            else Toast.makeText(this, "Brak uprawnień do kamery", Toast.LENGTH_LONG).show()
//        }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//
//        binding = ActivityFaceLoginBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }
//
//        binding.btnBack.setOnClickListener { finish() }
//
//        initCascade()
//
//        binding.statusTextView.text = "Status: Oczekiwanie…"
//
//        binding.btnStartVerify.setOnClickListener {
//            if (!modelExists()) {
//                binding.statusTextView.text = "Status: Brak wzorca – zarejestruj twarz"
//                return@setOnClickListener
//            }
//
//            ensureCameraPermissionAndStart()
//
//            verifying = !verifying
//            binding.btnStartVerify.text = if (verifying) "Stop" else "Start"
//            attempts = 0
//            binding.statusTextView.text = if (verifying) "Status: Weryfikacja…" else "Status: Zatrzymano"
//
//            if (verifying) {
//                loadModelIfNeeded()
//            }
//        }
//
//        binding.btnUsePin.setOnClickListener {
//            val intent = Intent(this, LockActivity::class.java).apply {
//                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
//            }
//            startActivity(intent)
//            finish()
//        }
//    }
//
//    private fun modelExists(): Boolean = File(filesDir, modelFileName).exists()
//
//    private fun ensureCameraPermissionAndStart() {
//        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
//                PackageManager.PERMISSION_GRANTED
//
//        if (granted) ensureCameraStarted()
//        else requestCameraPermission.launch(Manifest.permission.CAMERA)
//    }
//
//    private fun ensureCameraStarted() {
//        if (cameraStarted) return
//        cameraStarted = true
//        startCamera()
//    }
//
//    private fun startCamera() {
//        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
//        cameraProviderFuture.addListener({
//            val cameraProvider = cameraProviderFuture.get()
//
//            val preview = Preview.Builder().build().apply {
//                setSurfaceProvider(binding.previewView.surfaceProvider)
//            }
//
//            val analysis = ImageAnalysis.Builder()
//                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                .build()
//
//            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
//                analyzeFrame(imageProxy)
//            }
//
//            try {
//                cameraProvider.unbindAll()
//                cameraProvider.bindToLifecycle(
//                    this,
//                    CameraSelector.DEFAULT_FRONT_CAMERA,
//                    preview,
//                    analysis
//                )
//            } catch (_: Exception) {
//                binding.statusTextView.text = "Status: Błąd kamery"
//            }
//        }, ContextCompat.getMainExecutor(this))
//    }
//
//    private fun loadModelIfNeeded() {
//        if (::recognizer.isInitialized) return
//        recognizer = LBPHFaceRecognizer.create()
//        recognizer.read(File(filesDir, modelFileName).absolutePath)
//    }
//
//    private fun analyzeFrame(imageProxy: ImageProxy) {
//        try {
//            if (!verifying) return
//
//            val now = SystemClock.elapsedRealtime()
//            if (now - lastAttemptMs < 350) return
//            lastAttemptMs = now
//
//            val bmp = imageProxyToBitmap(imageProxy) ?: return
//
//            val rgba = bitmapToRgbaMat(bmp)
//            val gray = Mat()
//            cvtColor(rgba, gray, COLOR_RGBA2GRAY)
//            equalizeHist(gray, gray)
//
//            val faces = RectVector()
//            faceCascade.detectMultiScale(gray, faces, 1.1, 3, 0, Size(80, 80), Size())
//
//            val count = faces.size().toInt()
//            if (count != 1) {
//                binding.previewView.post {
//                    binding.statusTextView.text =
//                        if (count == 0) "Status: Brak twarzy"
//                        else "Status: Wiele twarzy ($count)"
//                }
//                rgba.release(); gray.release()
//                return
//            }
//
//            val r = faces.get(0)
//            val x = r.x().coerceAtLeast(0)
//            val y = r.y().coerceAtLeast(0)
//            val w = r.width().coerceAtMost(gray.cols() - x)
//            val h = r.height().coerceAtMost(gray.rows() - y)
//
//            val faceRoi = Mat(gray, CvRect(x, y, w, h))
//            val face200 = Mat()
//            resize(faceRoi, face200, Size(200, 200))
//
//            val predictedLabel = IntArray(1)
//            val confidence = DoubleArray(1)
//            recognizer.predict(face200, predictedLabel, confidence)
//
//            val distance = confidence[0]
//            val accept = (predictedLabel[0] == 1) && (distance <= threshold)
//
//            attempts++
//
//            binding.previewView.post {
//                binding.statusTextView.text =
//                    if (accept) "Status: Zalogowano ✅"
//                    else "Status: Odrzucono ❌ (próba $attempts/$maxAttempts)"
//            }
//
//            faceRoi.release()
//            face200.release()
//            rgba.release()
//            gray.release()
//
//            if (accept) {
//                verifying = false
//                runOnUiThread {
//                    startActivity(Intent(this, Dashboard::class.java))
//                    finish()
//                }
//            } else if (attempts >= maxAttempts) {
//                verifying = false
//                runOnUiThread {
//                    binding.btnStartVerify.text = "Start"
//                    binding.statusTextView.text = "Status: Zbyt wiele prób – użyj PIN"
//                }
//            }
//
//        } catch (_: Exception) {
//            binding.previewView.post { binding.statusTextView.text = "Status: Błąd weryfikacji" }
//        } finally {
//            imageProxy.close()
//        }
//    }
//
//    private fun initCascade() {
//        val input = resources.openRawResource(R.raw.haarcascade_frontalface_default)
//        val cascadeFile = File(filesDir, "haarcascade_frontalface_default.xml")
//        FileOutputStream(cascadeFile).use { out -> input.copyTo(out) }
//        input.close()
//
//        faceCascade = CascadeClassifier(cascadeFile.absolutePath)
//        if (faceCascade.empty()) {
//            throw IllegalStateException("Nie udało się załadować kaskady")
//        }
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        cameraExecutor.shutdown()
//    }
//
//    // ======== ImageProxy -> Bitmap helpers (skopiowane z Enroll) ========
//
//    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
//        val yuvImage = imageProxyToYuvImage(image) ?: return null
//        val out = ByteArrayOutputStream()
//        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 90, out)
//        val bytes = out.toByteArray()
//        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
//    }
//
//    private fun imageProxyToYuvImage(image: ImageProxy): YuvImage? {
//        val nv21 = yuv420888ToNv21(image) ?: return null
//        return YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
//    }
//
//    private fun yuv420888ToNv21(image: ImageProxy): ByteArray? {
//        val yPlane = image.planes[0]
//        val uPlane = image.planes[1]
//        val vPlane = image.planes[2]
//
//        val yBuffer = yPlane.buffer
//        val uBuffer = uPlane.buffer
//        val vBuffer = vPlane.buffer
//
//        val ySize = yBuffer.remaining()
//        val uSize = uBuffer.remaining()
//        val vSize = vBuffer.remaining()
//
//        val nv21 = ByteArray(ySize + uSize + vSize)
//        yBuffer.get(nv21, 0, ySize)
//
//        val rowStride = uPlane.rowStride
//        val pixelStride = uPlane.pixelStride
//
//        val height = image.height
//
//        val uBytes = ByteArray(uSize)
//        val vBytes = ByteArray(vSize)
//        uBuffer.get(uBytes)
//        vBuffer.get(vBytes)
//
//        val chromaHeight = height / 2
//        val chromaWidth = image.width / 2
//
//        var outputOffset = ySize
//        for (row in 0 until chromaHeight) {
//            for (col in 0 until chromaWidth) {
//                val uIndex = row * rowStride + col * pixelStride
//                val vIndex = row * vPlane.rowStride + col * vPlane.pixelStride
//                nv21[outputOffset++] = vBytes[vIndex]
//                nv21[outputOffset++] = uBytes[uIndex]
//            }
//        }
//        return nv21
//    }
//
//    private fun bitmapToRgbaMat(bitmap: Bitmap): Mat {
//        val bmp = if (bitmap.config != Bitmap.Config.ARGB_8888) {
//            bitmap.copy(Bitmap.Config.ARGB_8888, false)
//        } else bitmap
//
//        val bytes = ByteArray(bmp.byteCount)
//        val buffer = ByteBuffer.wrap(bytes)
//        bmp.copyPixelsToBuffer(buffer)
//
//        val mat = Mat(bmp.height, bmp.width, CV_8UC4)
//        mat.data().put(bytes, 0, bytes.size)
//        return mat
//    }
//}
