package it.drhack.smstomail

import androidx.room.TypeConverter

class EncryptedStringConverter {
    @TypeConverter
    fun fromEncrypted(value: String): EncryptedValue {
        return EncryptedValue(CryptoManager.decrypt(value))
    }

    @TypeConverter
    fun toEncrypted(value: EncryptedValue): String {
        return CryptoManager.encrypt(value.value)
    }
}
