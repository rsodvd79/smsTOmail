package it.drhack.smstomail

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object CryptoManager {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val ALIAS = "SmsToMailKey"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    // Dimensione IV standard per AES/GCM (Android Keystore genera sempre IV di 12 byte)
    private const val GCM_IV_SIZE = 12
    private const val GCM_TAG_BITS = 128

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val parameterSpec = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(parameterSpec)
        return keyGenerator.generateKey()
    }

    fun encrypt(input: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val iv = cipher.iv
        check(iv.size == GCM_IV_SIZE) { "IV inatteso di ${iv.size} byte (attesi $GCM_IV_SIZE)" }
        val cipherText = cipher.doFinal(input.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(input: String): String {
        val decoded = Base64.decode(input, Base64.NO_WRAP)
        require(decoded.size > GCM_IV_SIZE) { "Dati cifrati malformati: ${decoded.size} byte" }
        val iv = decoded.copyOfRange(0, GCM_IV_SIZE)
        val cipherText = decoded.copyOfRange(GCM_IV_SIZE, decoded.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
        val plain = cipher.doFinal(cipherText)
        return String(plain, Charsets.UTF_8)
    }
}
