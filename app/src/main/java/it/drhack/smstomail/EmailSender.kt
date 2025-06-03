package it.drhack.smstomail

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

/**
 * Classe che si occupa di inviare email attraverso un server SMTP
 */
class EmailSender(private val email: String, private val password: String) {

    companion object {
        private const val TAG = "EmailSender"
        private const val SMTP_HOST = "smtp.gmail.com"
        private const val SMTP_PORT = "587"
    }

    /**
     * Invia un'email al destinatario specificato
     *
     * @param recipient L'indirizzo email del destinatario
     * @param subject L'oggetto dell'email
     * @param body Il contenuto dell'email
     * @return Una stringa che indica l'esito dell'operazione
     */
    suspend fun sendEmail(recipient: String, subject: String, body: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val props = Properties().apply {
                    put("mail.smtp.host", SMTP_HOST)
                    put("mail.smtp.port", SMTP_PORT)
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.starttls.enable", "true")
                }

                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(email, password)
                    }
                })

                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(email))
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient))
                    setSubject(subject)
                    setText(body)
                }

                Transport.send(message)
                Log.d(TAG, "Email inviata con successo a $recipient")
                "Email inviata con successo"
            } catch (e: MessagingException) {
                Log.e(TAG, "Errore nell'invio dell'email", e)

                // Verifica se l'errore è relativo alla necessità di una password specifica per app
                if (e.message?.contains("534-5.7.9") == true ||
                    e.message?.contains("535-5.7.8") == true ||
                    e.message?.contains("BadCredentials") == true ||
                    e.message?.contains("InvalidSecondFactor") == true) {
                    "Errore di autenticazione: Per Gmail è necessario utilizzare una password specifica per app. " +
                    "Visita https://myaccount.google.com/security e crea una password specifica per questa app."
                } else {
                    "Errore: ${e.message}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Errore generico", e)
                "Errore generico: ${e.message}"
            }
        }
    }
}
