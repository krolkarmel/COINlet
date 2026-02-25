package com.coinlet.facelogin

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

class FaceNetEngine(private val context: Context) {

    private val inputSize = 160
    private val embeddingDim = 128

    private val interpreter: Interpreter by lazy {
        val modelBytes = context.assets.open("facenet.tflite").use { it.readBytes() }
        val bb = ByteBuffer.allocateDirect(modelBytes.size).order(ByteOrder.nativeOrder())
        bb.put(modelBytes)
        bb.rewind()
        Interpreter(bb)
    }

    fun embeddingFromBitmap(rgbBitmap160: Bitmap): FloatArray {
        val input = bitmapToFloatBuffer(rgbBitmap160, inputSize)
        val output = Array(1) { FloatArray(embeddingDim) }
        interpreter.run(input, output)
        return l2Normalize(output[0])
    }

    fun saveTemplate(file: File, emb: FloatArray) {
        DataOutputStream(BufferedOutputStream(FileOutputStream(file))).use { dos ->
            dos.writeInt(emb.size)
            for (v in emb) dos.writeFloat(v)
        }
    }

    fun loadTemplate(file: File): FloatArray {
        DataInputStream(BufferedInputStream(FileInputStream(file))).use { dis ->
            val n = dis.readInt()
            val out = FloatArray(n)
            for (i in 0 until n) out[i] = dis.readFloat()
            return out
        }
    }

    fun cosine(a: FloatArray, b: FloatArray): Float {
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

    private fun bitmapToFloatBuffer(bmp: Bitmap, size: Int): ByteBuffer {
        val input = ByteBuffer.allocateDirect(1 * size * size * 3 * 4).order(ByteOrder.nativeOrder())
        val pixels = IntArray(size * size)
        bmp.getPixels(pixels, 0, size, 0, 0, size, size)

        for (px in pixels) {
            val r = (px shr 16) and 0xFF
            val g = (px shr 8) and 0xFF
            val b = px and 0xFF

            input.putFloat((r - 127.5f) / 128f)
            input.putFloat((g - 127.5f) / 128f)
            input.putFloat((b - 127.5f) / 128f)
        }
        input.rewind()
        return input
    }

    private fun l2Normalize(x: FloatArray): FloatArray {
        var sum = 0f
        for (v in x) sum += v * v
        val norm = sqrt(sum) + 1e-10f
        return FloatArray(x.size) { i -> x[i] / norm }
    }
}