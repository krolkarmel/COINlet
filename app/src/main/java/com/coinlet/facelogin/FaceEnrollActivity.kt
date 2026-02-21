package com.coinlet.facelogin

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
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
import org.bytedeco.opencv.global.opencv_core.CV_32SC1
import org.bytedeco.opencv.global.opencv_core.CV_8UC4
import org.bytedeco.opencv.global.opencv_imgproc.COLOR_RGBA2GRAY
import org.bytedeco.opencv.global.opencv_imgproc.cvtColor
import org.bytedeco.opencv.global.opencv_imgproc.equalizeHist
import org.bytedeco.opencv.global.opencv_imgproc.resize
import org.bytedeco.opencv.opencv_core.Mat
import org.bytedeco.opencv.opencv_core.MatVector
import org.bytedeco.opencv.opencv_core.RectVector
import org.bytedeco.opencv.opencv_core.Size
import org.bytedeco.opencv.opencv_core.Rect as CvRect
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import org.bytedeco.javacpp.indexer.IntIndexer

class FaceEnrollActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFaceEnrollBinding
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private lateinit var faceCascade: CascadeClassifier

    private var cameraStarted = false
    private var isCollecting = false

    private val targetSamples = 20
    private var samplesCollected = 0
    private var lastSampleTimeMs = 0L

    private val samples = mutableListOf<Mat>()
    private val modelFileName = "lbph_model.yml"

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) ensureCameraStarted()
            else Toast.makeText(this, "Brak uprawnień do kamery", Toast.LENGTH_LONG).show()
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

        binding.btnBack.setOnClickListener { finish() }

        initCascade()
        binding.progressTextView.text = "0/$targetSamples"
        binding.statusTextView.text = "Status: Oczekiwanie na start…"

        binding.btnStartStop.setOnClickListener {
            ensureCameraPermissionAndStart()

            // toggle collect
            isCollecting = !isCollecting
            binding.btnStartStop.text = if (isCollecting) "Stop" else "Start"

            if (isCollecting) {
                resetCollection()
                binding.statusTextView.text = "Status: Zbieranie próbek…"
            } else {
                binding.statusTextView.text = "Status: Zatrzymano"
            }
        }
    }

    private fun resetCollection() {
        // zwolnij stare próbki (jeśli były)
        for (m in samples) m.release()
        samples.clear()

        samplesCollected = 0
        lastSampleTimeMs = 0L
        binding.progressTextView.text = "0/$targetSamples"
    }

    private fun ensureCameraPermissionAndStart() {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

        if (granted) ensureCameraStarted()
        else requestCameraPermission.launch(Manifest.permission.CAMERA)
    }

    private fun ensureCameraStarted() {
        if (cameraStarted) return
        cameraStarted = true
        startCameraPreview()
    }

    private fun startCameraPreview() {
        binding.statusTextView.text = "Status: Uruchamianie kamery…"

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

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                    analysis
                )
                binding.statusTextView.text = "Status: Kamera działa"
            } catch (_: Exception) {
                binding.statusTextView.text = "Status: Błąd kamery"
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun analyzeFrame(imageProxy: ImageProxy) {
        try {
            val bmp = imageProxyToBitmap(imageProxy) ?: return

            val rgba = bitmapToRgbaMat(bmp)
            val gray = Mat()
            cvtColor(rgba, gray, COLOR_RGBA2GRAY)
            equalizeHist(gray, gray)

            val faces = RectVector()
            faceCascade.detectMultiScale(gray, faces, 1.1, 3, 0, Size(80, 80), Size())
            val count = faces.size().toInt()

            binding.previewView.post {
                binding.statusTextView.text = when (count) {
                    0 -> "Status: Brak twarzy"
                    1 -> if (isCollecting) "Status: Wykryto twarz (zbieranie…)" else "Status: Wykryto twarz"
                    else -> "Status: Wiele twarzy ($count)"
                }
            }

            if (isCollecting && count == 1 && samplesCollected < targetSamples) {
                val now = SystemClock.elapsedRealtime()
                if (now - lastSampleTimeMs > 350) {
                    lastSampleTimeMs = now

                    val r = faces.get(0)
                    val x = r.x().coerceAtLeast(0)
                    val y = r.y().coerceAtLeast(0)
                    val w = r.width().coerceAtMost(gray.cols() - x)
                    val h = r.height().coerceAtMost(gray.rows() - y)

                    val faceRoi = Mat(gray, CvRect(x, y, w, h))
                    val face200 = Mat()
                    resize(faceRoi, face200, Size(200, 200))

                    // zapisz próbkę do treningu (clone!)
                    samples.add(face200.clone())

                    samplesCollected++
                    binding.previewView.post {
                        binding.progressTextView.text = "$samplesCollected/$targetSamples"
                    }

                    faceRoi.release()
                    face200.release()

                    if (samplesCollected == targetSamples) {
                        isCollecting = false
                        binding.previewView.post {
                            binding.btnStartStop.text = "Start"
                            binding.statusTextView.text = "Status: Trening modelu…"
                        }

                        // trening w wątku executor (żeby nie blokować UI)
                        cameraExecutor.execute {
                            val ok = trainAndSaveModel()
                            binding.previewView.post {
                                binding.statusTextView.text =
                                    if (ok) "Status: Zarejestrowano twarz ✅"
                                    else "Status: Błąd treningu ❌"
                            }
                        }
                    }
                }
            }

            rgba.release()
            gray.release()
        } catch (_: Exception) {
            binding.previewView.post { binding.statusTextView.text = "Status: Błąd analizy" }
        } finally {
            imageProxy.close()
        }
    }

    private fun trainAndSaveModel(): Boolean {
        return try {
            if (samples.isEmpty()) return false

            val recognizer = LBPHFaceRecognizer.create()

            // MatVector – bez named args
            val matVector = MatVector(samples.size.toLong())
            for (i in samples.indices) {
                matVector.put(i.toLong(), samples[i])
            }

            // Labels – wypełnij przez IntIndexer (pewne w Kotlin + bytedeco)
            val labels = Mat(samples.size, 1, CV_32SC1)
            val indexer: IntIndexer = labels.createIndexer()
            for (i in samples.indices) {
                indexer.put(i.toLong(), 0L, 1)
            }
            indexer.release()

            recognizer.train(matVector, labels)

            val modelFile = File(filesDir, modelFileName)
            recognizer.write(modelFile.absolutePath)

            labels.release()
            for (m in samples) m.release()
            samples.clear()

            true
        } catch (e: Exception) {
            false
        }
    }




    private fun initCascade() {
        val input = resources.openRawResource(R.raw.haarcascade_frontalface_default)
        val cascadeFile = File(filesDir, "haarcascade_frontalface_default.xml")
        FileOutputStream(cascadeFile).use { out -> input.copyTo(out) }
        input.close()

        faceCascade = CascadeClassifier(cascadeFile.absolutePath)
        if (faceCascade.empty()) {
            throw IllegalStateException("Nie udało się załadować kaskady. Sprawdź res/raw/haarcascade_frontalface_default.xml")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // sprzątanie próbek jeśli user wyjdzie w trakcie
        for (m in samples) m.release()
        samples.clear()
        cameraExecutor.shutdown()
    }

    // ======== ImageProxy -> Bitmap helpers ========

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

    private fun bitmapToRgbaMat(bitmap: Bitmap): Mat {
        val bmp = if (bitmap.config != Bitmap.Config.ARGB_8888) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else bitmap

        val bytes = ByteArray(bmp.byteCount)
        val buffer = ByteBuffer.wrap(bytes)
        bmp.copyPixelsToBuffer(buffer)

        val mat = Mat(bmp.height, bmp.width, CV_8UC4)
        mat.data().put(bytes, 0, bytes.size)
        return mat
    }
}
