package it.drhack.smstomail

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

// Converter per la cifratura della password
import it.drhack.smstomail.EncryptedStringConverter

@Entity(tableName = "email_config")
data class EmailConfig(
    @PrimaryKey val id: Int = 0,
    val email: String,
    @field:TypeConverters(EncryptedStringConverter::class)
    val password: String,
    val destination: String,
    val maxSmsToKeep: Int = 100,  // Valore predefinito: 100 messaggi
    val smtpHost: String = "smtp.gmail.com",  // Valore predefinito per Gmail
    val smtpPort: String = "587",  // Porta predefinita per TLS
    val smtpUseTls: Boolean = true,  // Usa TLS per default
    val signature: String = "by SMS to Mail"  // Firma predefinita
)

