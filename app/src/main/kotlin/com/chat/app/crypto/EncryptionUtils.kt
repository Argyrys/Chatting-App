package com.chat.app.crypto

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object EncryptionUtils {
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"

    private var sharedKey: String = "default-secret-key"
    private var secretKey: SecretKeySpec? = null

    fun setSharedKey(key: String) {
        sharedKey = key
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(key.toByteArray(Charsets.UTF_8))
        secretKey = SecretKeySpec(keyBytes.copyOf(32), ALGORITHM)
    }

    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return plainText

        val key = secretKey ?: run {
            setSharedKey(sharedKey)
            secretKey!!
        }

        val iv = ByteArray(16)
        java.security.SecureRandom().nextBytes(iv)
        val ivSpec = IvParameterSpec(iv)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val combined = iv + encrypted
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(cipherText: String): String {
        if (cipherText.isEmpty()) return cipherText

        val key = secretKey ?: run {
            setSharedKey(sharedKey)
            secretKey!!
        }

        return try {
            val combined = Base64.decode(cipherText, Base64.NO_WRAP)
            val iv = combined.copyOfRange(0, 16)
            val encrypted = combined.copyOfRange(16, combined.size)

            val ivSpec = IvParameterSpec(iv)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
            val decrypted = cipher.doFinal(encrypted)
            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            cipherText
        }
    }

    fun isEncrypted(text: String): Boolean {
        return try {
            val bytes = Base64.decode(text, Base64.NO_WRAP)
            bytes.size > 16
        } catch (e: Exception) {
            false
        }
    }
}
