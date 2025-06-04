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
class EmailSender(
    private val email: String,
    private val password: String,
    private val smtpHost: String = "smtp.gmail.com",
    private val smtpPort: String = "587",
    private val useTls: Boolean = true,
    private val signature: String = "by SMS to Mail"
) {

    companion object {
        private const val TAG = "EmailSender"
        private const val DEBUG = false // Imposta su true per attivare il debug JavaMail
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
                    put("mail.smtp.host", smtpHost)
                    put("mail.smtp.port", smtpPort)
                    put("mail.smtp.auth", "true")

                    // Configura in base alla porta e al tipo di connessione
                    if (smtpPort == "465") {
                        // Configurazione SSL diretta (es. Aruba)
                        put("mail.smtp.socketFactory.port", smtpPort)
                        put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                        put("mail.smtp.ssl.enable", useTls.toString())
                        put("mail.smtp.ssl.trust", smtpHost)
                    } else {
                        // Configurazione STARTTLS (es. Gmail porta 587)
                        put("mail.smtp.starttls.enable", useTls.toString())
                    }

                    // Impostazioni timeout per evitare blocchi
                    put("mail.smtp.connectiontimeout", "10000")
                    put("mail.smtp.timeout", "10000")
                }

                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(email, password)
                    }
                })

                // Per debug
                session.debug = DEBUG

                // Aggiungi la firma al corpo del messaggio se presente
                val finalBody = if (signature.isNotBlank()) {
                    "$body\n\n$signature"
                } else {
                    body
                }

                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(email))
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient))
                    setSubject(subject)
                    setText(finalBody)
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
                } else if (e.message?.contains("[EOF]") == true) {
                    "Errore di connessione al server SMTP: Il server ha chiuso la connessione prematuramente. " +
                    "Verifica che host, porta e impostazioni SSL/TLS siano corrette."
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
