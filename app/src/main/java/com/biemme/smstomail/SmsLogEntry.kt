package com.biemme.smstomail

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "sms_log")
data class SmsLogEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Date,
    val sender: String,
    val message: String,
    val emailSent: Boolean,
    val emailResult: String?
)
