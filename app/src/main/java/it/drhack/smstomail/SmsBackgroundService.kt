package it.drhack.smstomail

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SmsBackgroundService : Service() {
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "smstomail_channel"
        private const val CHANNEL_NAME = "SMS to Email"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Verifichiamo se il servizio è stato avviato dopo il riavvio del dispositivo
        val isFromBootReceiver = intent?.getBooleanExtra("bootCompleted", false) ?: false

        // Per Android 15 e superiori, gestire diversamente l'avvio dal boot
        if (isFromBootReceiver && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Invece di avviare un servizio in primo piano dopo il boot,
            // registriamo solo la capacità di ricevere SMS e terminiamo il servizio
            // L'app risponderà ai nuovi SMS quando arriveranno attraverso SmsReceiver

            // Qui potremmo anche schedulare un WorkManager per operazioni periodiche
            // se necessario, ma non avviamo un servizio in primo piano

            stopSelf()
            return START_NOT_STICKY
        }

        // Recuperiamo i dati dell'SMS, se presenti
        val sender = intent?.getStringExtra("sender")
        val message = intent?.getStringExtra("message")

        // Se non ci sono informazioni sull'SMS non eseguiamo alcuna elaborazione
        if (sender.isNullOrEmpty() || message.isNullOrEmpty()) {
            stopSelf()
            return START_NOT_STICKY
        }

        val safeSender = sender
        val safeMessage = message

        // Procedura normale per l'elaborazione degli SMS

        // Crea e mostra una notifica per il foreground service
        val notification = createNotification("SMS ricevuto", "Elaborazione SMS da $safeSender...")
        startForeground(NOTIFICATION_ID, notification)

        // Elabora l'SMS in un coroutine scope
        CoroutineScope(Dispatchers.IO).launch {
            processSms(safeSender, safeMessage)
            // Ferma il servizio dopo l'elaborazione
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Canale per le notifiche di SMS inoltrati via email"
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String, content: String) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle(title)
        .setContentText(content)
        .setSmallIcon(R.drawable.ic_notification)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
        )
        .build()

    private suspend fun processSms(sender: String, message: String) {
        val db = AppDatabase.getInstance(this)
        val filters = db.filterDao().getAllFilters()
        val processor = SmsFilterProcessor(filters)

        if (processor.shouldProcessSms(sender, message)) {
            val config = db.emailConfigDao().getConfig() ?: return

            try {
                val emailSender = EmailSender(config.email, config.password)
                val result = emailSender.sendEmail(
                    config.destination,
                    "Nuovo SMS da $sender",
                    message
                )

                // Aggiorna la notifica
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val updatedNotification = createNotification(
                    "SMS inoltrato",
                    "SMS da $sender inoltrato con successo"
                )
                notificationManager.notify(NOTIFICATION_ID, updatedNotification)

                // Notifica il risultato all'app
                withContext(Dispatchers.Main) {
                    val resultIntent = Intent("it.drhack.smstomail.SMS_RESULT").apply {
                        putExtra("sms_message", "Da: $sender\n$message")
                        putExtra("mail_result", "Email inviata: $result")
                    }
                    LocalBroadcastManager.getInstance(this@SmsBackgroundService)
                        .sendBroadcast(resultIntent)
                }
            } catch (e: Exception) {
                // Notifica l'errore
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val errorNotification = createNotification(
                    "Errore inoltro SMS",
                    "Errore nell'invio dell'email: ${e.message}"
                )
                notificationManager.notify(NOTIFICATION_ID, errorNotification)

                // Notifica il risultato all'app
                withContext(Dispatchers.Main) {
                    val resultIntent = Intent("it.drhack.smstomail.SMS_RESULT").apply {
                        putExtra("sms_message", "Da: $sender\n$message")
                        putExtra("mail_result", "Errore invio email: ${e.message}")
                    }
                    LocalBroadcastManager.getInstance(this@SmsBackgroundService)
                        .sendBroadcast(resultIntent)
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

