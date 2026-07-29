package it.drhack.smstomail

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException
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
    private val smtpHost: String,
    private val smtpPort: String,
    private val useTls: Boolean,
    private val signature: String,
) {

    companion object {
        private const val TAG = "EmailSender"
        private const val DEBUG = false // Imposta su true per attivare il debug JavaMail

        // Retry con backoff esponenziale per errori transient (timeout, EOF, connessione)
        private const val MAX_ATTEMPTS = 3
        private const val RETRY_BASE_DELAY_MS = 2000L
    }

    /**
     * Invia un'email al destinatario specificato.
     * Gli errori transient di rete vengono ritentati fino a [MAX_ATTEMPTS] volte
     * con backoff esponenziale; gli errori di autenticazione non vengono ritentati.
     *
     * @param recipient L'indirizzo email del destinatario
     * @param subject L'oggetto dell'email
     * @param body Il contenuto dell'email
     * @return Una stringa che indica l'esito dell'operazione
     */
    suspend fun sendEmail(recipient: String, subject: String, body: String): String {
        return withContext(Dispatchers.IO) {
            var lastError: MessagingException? = null
            for (attempt in 1..MAX_ATTEMPTS) {
                try {
                    doSend(recipient, subject, body)
                    Log.d(TAG, "Email inviata con successo (tentativo $attempt)")
                    return@withContext "Email inviata con successo"
                } catch (e: MessagingException) {
                    Log.e(TAG, "Errore nell'invio dell'email (tentativo $attempt/$MAX_ATTEMPTS)", e)
                    lastError = e
                    if (!isTransient(e) || attempt == MAX_ATTEMPTS) {
                        return@withContext describeError(e)
                    }
                    // Backoff esponenziale: 2s, 4s, ...
                    delay(RETRY_BASE_DELAY_MS * (1L shl (attempt - 1)))
                } catch (e: Exception) {
                    Log.e(TAG, "Errore generico", e)
                    return@withContext "Errore generico: ${e.message}"
                }
            }
            describeError(lastError!!)
        }
    }

    private fun doSend(recipient: String, subject: String, body: String) {
        // Log solo in modalità debug: non esporre indirizzi/credenziali in produzione
        if (DEBUG) {
            Log.d(TAG, "Tentativo di invio email da: $email a: $recipient")
            Log.d(TAG, "Server SMTP: $smtpHost:$smtpPort, TLS: $useTls")
        }

        val props = Properties().apply {
            put("mail.smtp.host", smtpHost)
            put("mail.smtp.port", smtpPort)
            put("mail.smtp.auth", "true")

            // Debug più dettagliato
            if (DEBUG) {
                put("mail.debug", "true")
                put("mail.debug.auth", "true")
            }

            // Configura in base alla porta e al tipo di connessione
            if (smtpPort == "465") {
                // Configurazione SSL diretta per Aruba (porta 465)
                put("mail.smtp.socketFactory.port", smtpPort)
                put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                put("mail.smtp.ssl.enable", "true") // Forza SSL a true per porta 465
                put("mail.smtp.ssl.trust", smtpHost) // Fidati solo dell'host SMTP configurato
                put("mail.smtp.ssl.protocols", "TLSv1.2") // Specifica il protocollo SSL

                // Disattiva STARTTLS per connessioni SSL dirette
                put("mail.smtp.starttls.enable", "false")
                put("mail.smtp.starttls.required", "false")
            } else {
                // Configurazione STARTTLS (es. Gmail porta 587)
                put("mail.smtp.starttls.enable", useTls.toString())
                put("mail.smtp.ssl.trust", smtpHost)
            }

            // Impostazioni timeout per evitare blocchi
            put("mail.smtp.connectiontimeout", "15000")
            put("mail.smtp.timeout", "15000")
            put("mail.smtp.writetimeout", "15000")
        }

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                // Assicurati che non ci siano spazi nelle credenziali
                val trimmedEmail = email.trim()
                val trimmedPassword = password.trim()
                if (DEBUG) Log.d(TAG, "Autenticazione con utente: $trimmedEmail")
                return PasswordAuthentication(trimmedEmail, trimmedPassword)
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
    }

    /**
     * Determina se l'errore è transient (rete/timeout) e quindi ritentabile.
     * Gli errori di autenticazione non sono mai transient.
     */
    private fun isTransient(e: MessagingException): Boolean {
        if (isAuthError(e)) return false
        val msg = e.message.orEmpty()
        return e.nextException is IOException ||
            msg.contains("[EOF]") ||
            msg.contains("timeout", ignoreCase = true) ||
            msg.contains("timed out", ignoreCase = true) ||
            msg.contains("Couldn't connect", ignoreCase = true) ||
            msg.contains("Connection reset", ignoreCase = true)
    }

    private fun isAuthError(e: MessagingException): Boolean {
        val msg = e.message.orEmpty()
        return msg.contains("534-5.7.9") ||
            msg.contains("535-5.7.8") ||
            msg.contains("BadCredentials") ||
            msg.contains("InvalidSecondFactor")
    }

    private fun describeError(e: MessagingException): String {
        // Verifica se l'errore è relativo alla necessità di una password specifica per app
        return if (isAuthError(e)) {
            "Errore di autenticazione: Per Gmail è necessario utilizzare una password specifica per app. " +
            "Visita https://myaccount.google.com/security e crea una password specifica per questa app."
        } else if (e.message?.contains("[EOF]") == true) {
            "Errore di connessione al server SMTP: Il server ha chiuso la connessione prematuramente. " +
            "Verifica che host, porta e impostazioni SSL/TLS siano corrette."
        } else {
            "Errore: ${e.message}"
        }
    }
}
