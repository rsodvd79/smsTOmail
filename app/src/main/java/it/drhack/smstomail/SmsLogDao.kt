package it.drhack.smstomail

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsLogDao {
    @Insert
    suspend fun insert(smsLogEntry: SmsLogEntry)

    @Query("SELECT * FROM sms_log ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<SmsLogEntry>>

    @Query("DELETE FROM sms_log")
    suspend fun deleteAll()
}
