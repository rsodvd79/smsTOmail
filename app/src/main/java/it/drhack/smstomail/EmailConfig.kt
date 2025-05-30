package it.drhack.smstomail

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "email_config")
data class EmailConfig(
    @PrimaryKey val id: Int = 0,
    val email: String,
    val password: String,
    val destination: String
)