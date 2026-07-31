package it.drhack.smstomail

import android.content.Context
import java.util.Date

/**
 * Logica condivisa di elaborazione e inoltro SMS via email.
 * Usata sia da SmsReceiver che da SmsBackgroundService per evitare duplicazioni.
 */
object SmsForwarder {

    /**
     * Esito dell'elaborazione di un SMS.
     *
     * @param forwarded true se l'SMS ha superato i filtri ed è stato tentato l'invio
     * @param emailSent true se l'email è stata inviata con successo
     * @param result Messaggio descrittivo dell'esito
     */
    data class Outcome(
        val forwarded: Boolean,
        val emailSent: Boolean,
        val result: String
    )

    /**
     * Applica i filtri, invia l'email (SMTP o Gmail API in base alla configurazione)
     * e registra sempre l'esito in sms_log.
     */
    suspend fun handleIncomingSms(context: Context, sender: String, message: String): Outcome {
        val db = AppDatabase.getInstance(context)
        val filters = db.filterDao().getAllFilters()
        val processor = SmsFilterProcessor(filters)

        if (!processor.shouldProcessSms(sender, message)) {
            val result = "SMS filtrato: non corrisponde ai filtri configurati"
            log(db, sender, message, false, result)
            return Outcome(forwarded = false, emailSent = false, result = result)
        }

        val config = db.emailConfigDao().getConfig()
        if (config == null) {
            val result = "Configurazione email mancante: SMS non inoltrato"
            log(db, sender, message, false, result)
            return Outcome(forwarded = false, emailSent = false, result = result)
        }

        val result = try {
            if (config.authMode == EmailConfig.AUTH_MODE_GMAIL_OAUTH) {
                GmailApiSender(context, config.signature).sendEmail(
                    config.destination,
                    "Nuovo SMS da $sender",
                    message
                )
            } else {
                EmailSender(
                    config.email,
                    config.password.value,
                    config.smtpHost,
                    config.smtpPort,
                    config.smtpUseTls,
                    config.signature
                ).sendEmail(
                    config.destination,
                    "Nuovo SMS da $sender",
                    message
                )
            }
        } catch (e: Exception) {
            "Errore invio email: ${e.message}"
        }

        val emailSuccess = result.startsWith("Email inviata con successo")
        log(db, sender, message, emailSuccess, result)

        // Notifica immediata all'utente in caso di errore di autenticazione/autorizzazione
        if (!emailSuccess && isAuthFailure(result)) {
            NotificationHelper.showAuthErrorNotification(context, result)
        }

        return Outcome(forwarded = true, emailSent = emailSuccess, result = result)
    }

    private fun isAuthFailure(result: String): Boolean =
        result.startsWith("Errore di autenticazione") ||
            result.startsWith("Errore di autorizzazione Gmail")

    private suspend fun log(
        db: AppDatabase,
        sender: String,
        message: String,
        emailSent: Boolean,
        result: String
    ) {
        db.smsLogDao().insert(
            SmsLogEntry(
                timestamp = Date(),
                sender = sender,
                message = message,
                emailSent = emailSent,
                emailResult = result
            )
        )
    }
}
