package it.drhack.smstomail

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsLogDao {
    @Insert
    suspend fun insertRaw(smsLogEntry: SmsLogEntry): Long

    @Transaction
    suspend fun insert(smsLogEntry: SmsLogEntry) {
        insertRaw(smsLogEntry)
        // Dopo l'inserimento, controllo se è necessario eliminare i messaggi più vecchi
        enforceMaxSmsLimit()
    }

    @Query("SELECT COUNT(*) FROM sms_log")
    suspend fun getCount(): Int

    @Query("DELETE FROM sms_log WHERE id IN (SELECT id FROM sms_log ORDER BY timestamp ASC LIMIT :limitToDelete)")
    suspend fun deleteOldestMessages(limitToDelete: Int)

    suspend fun enforceMaxSmsLimit() {
        // Ottengo la configurazione email
        val config = getEmailConfig()
        val maxSmsToKeep = config?.maxSmsToKeep ?: 100

        // Conto quanti messaggi ci sono nel database
        val currentCount = getCount()

        // Se abbiamo superato il limite, elimino i messaggi più vecchi
        if (currentCount > maxSmsToKeep) {
            val toDelete = currentCount - maxSmsToKeep
            deleteOldestMessages(toDelete)
        }
    }

    @Query("SELECT * FROM email_config LIMIT 1")
    suspend fun getEmailConfig(): EmailConfig?

    @Query("SELECT * FROM sms_log ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<SmsLogEntry>>

    @Query("DELETE FROM sms_log")
    suspend fun deleteAll()
}
