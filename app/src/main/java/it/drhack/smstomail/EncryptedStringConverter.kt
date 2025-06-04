package it.drhack.smstomail

import androidx.room.TypeConverter

class EncryptedStringConverter {
    @TypeConverter
    fun encrypt(value: String): String = CryptoManager.encrypt(value)

    @TypeConverter
    fun decrypt(value: String): String = CryptoManager.decrypt(value)
}
