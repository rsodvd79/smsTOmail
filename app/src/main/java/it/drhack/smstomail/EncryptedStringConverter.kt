package it.drhack.smstomail

import androidx.room.TypeConverter

class EncryptedStringConverter {
    @TypeConverter
    fun fromEncrypted(value: String): EncryptedValue {
        return try {
            EncryptedValue(CryptoManager.decrypt(value))
        } catch (e: Exception) {
            // Fallback: password salvata in chiaro prima che la cifratura fosse attiva
            EncryptedValue(value)
        }
    }

    @TypeConverter
    fun toEncrypted(value: EncryptedValue): String {
        return CryptoManager.encrypt(value.value)
    }
}
