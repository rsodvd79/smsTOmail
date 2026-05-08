package it.drhack.smstomail


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            if (messages.isNotEmpty()) {
                val sender = messages[0].originatingAddress ?: "Numero sconosciuto"
                val bodyBuilder = StringBuilder()
                for (message in messages) {
                    bodyBuilder.append(message.messageBody)
                }
                val messageBody = bodyBuilder.toString()

                // goAsync() impedisce ad Android di terminare il processo prima che
                // la coroutine abbia completato l'invio email e il salvataggio nel DB.
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getInstance(context)
                        val filters = db.filterDao().getAllFilters()
                        val processor = SmsFilterProcessor(filters)

                        if (processor.shouldProcessSms(sender, messageBody)) {
                            val config = db.emailConfigDao().getConfig()
                            if (config != null) {
                                val result = if (config.authMode == EmailConfig.AUTH_MODE_GMAIL_OAUTH) {
                                    GmailApiSender(context, config.signature).sendEmail(
                                        config.destination,
                                        "Nuovo SMS da $sender",
                                        messageBody
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
                                        messageBody
                                    )
                                }
                                val emailSuccess = result.startsWith("Email inviata con successo")
                                db.smsLogDao().insert(
                                    SmsLogEntry(
                                        timestamp = Date(),
                                        sender = sender,
                                        message = messageBody,
                                        emailSent = emailSuccess,
                                        emailResult = result
                                    )
                                )
                            }
                        } else {
                            db.smsLogDao().insert(
                                SmsLogEntry(
                                    timestamp = Date(),
                                    sender = sender,
                                    message = messageBody,
                                    emailSent = false,
                                    emailResult = "SMS filtrato: non corrisponde ai filtri configurati"
                                )
                            )
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}

