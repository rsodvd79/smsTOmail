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
    val password: EncryptedValue,
    val destination: String,
    val maxSmsToKeep: Int = 100,
    val smtpHost: String = "smtp.gmail.com",
    val smtpPort: String = "587",
    val smtpUseTls: Boolean = true,
    val signature: String = "by SMS to Mail",
    val authMode: String = AUTH_MODE_SMTP,
    val oauthAccount: String = ""
) {
    companion object {
        const val AUTH_MODE_SMTP = "SMTP"
        const val AUTH_MODE_GMAIL_OAUTH = "GMAIL_OAUTH"
    }
}

