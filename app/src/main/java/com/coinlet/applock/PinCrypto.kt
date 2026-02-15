package com.coinlet.applock
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinCrypto {
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256

    fun generateSalt(bytes: Int = 16): ByteArray {
        val salt = ByteArray(bytes)
        SecureRandom().nextBytes(salt)
        return salt
    }

    fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return skf.generateSecret(spec).encoded
    }

    fun toB64(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.NO_WRAP)

    fun fromB64(str: String): ByteArray =
        Base64.decode(str, Base64.NO_WRAP)

    fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
        return diff == 0
    }
}
