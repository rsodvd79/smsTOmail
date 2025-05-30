package it.drhack.smstomail


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import androidx.localbroadcastmanager.content.LocalBroadcastManager
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

                CoroutineScope(Dispatchers.IO).launch {
                    val db = AppDatabase.getInstance(context)
                    val filters = db.filterDao().getAllFilters()
                    val processor = SmsFilterProcessor(filters)

                    if (processor.shouldProcessSms(sender, messageBody)) {
                        val config = db.emailConfigDao().getConfig()
                        config?.let {
                            try {
                                val emailSender = EmailSender(it.email, it.password)
                                val result = emailSender.sendEmail(
                                    it.destination,
                                    "Nuovo SMS da $sender",
                                    messageBody
                                )

                                val smsLogEntry = SmsLogEntry(
                                    timestamp = Date(),
                                    sender = sender,
                                    message = messageBody,
                                    emailSent = true,
                                    emailResult = "Email inviata con successo"
                                )
                                db.smsLogDao().insert(smsLogEntry)
                            } catch (e: Exception) {
                                val smsLogEntry = SmsLogEntry(
                                    timestamp = Date(),
                                    sender = sender,
                                    message = messageBody,
                                    emailSent = false,
                                    emailResult = "Errore invio email: ${e.message}"
                                )
                                db.smsLogDao().insert(smsLogEntry)
                            }
                        }
                    } else {
                        val smsLogEntry = SmsLogEntry(
                            timestamp = Date(),
                            sender = sender,
                            message = messageBody,
                            emailSent = false,
                            emailResult = "SMS filtrato: non corrisponde ai filtri configurati"
                        )
                        db.smsLogDao().insert(smsLogEntry)
                    }
                }
            }
        }
    }
}