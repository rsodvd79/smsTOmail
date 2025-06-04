package it.drhack.smstomail

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.TypeConverters

// Per assicurare la cifratura dei campi sensibili
import it.drhack.smstomail.EncryptedStringConverter

@Dao
@TypeConverters(EncryptedStringConverter::class)
interface EmailConfigDao {
    @Query("SELECT * FROM email_config WHERE id = 0 LIMIT 1")
    suspend fun getConfig(): EmailConfig?

    @Insert
    suspend fun insertConfig(config: EmailConfig)

    @Update
    suspend fun updateConfig(config: EmailConfig)
}

