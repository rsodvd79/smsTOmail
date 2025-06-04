package it.drhack.smstomail

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * Classe di utilità per gestire le notifiche nell'app
 */
object NotificationHelper {
    private const val NOTIFICATION_CHANNEL_ID = "email_error_channel"
    private const val AUTH_ERROR_NOTIFICATION_ID = 1001

    /**
     * Crea un canale di notifica per Android 8.0 e versioni successive
     */
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Errori Email",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifiche per errori di invio email"
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Mostra una notifica per errori di autenticazione email
     */
    fun showAuthErrorNotification(context: Context, message: String) {
        createNotificationChannel(context)

        // Crea intent per aprire l'activity principale
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(context, 0, intent, pendingFlags)

        // Costruisci la notifica
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Assicurati di avere questa icona nelle risorse
            .setContentTitle("Errore di autenticazione email")
            .setContentText("Problema con le credenziali Gmail")
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Mostra la notifica
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(AUTH_ERROR_NOTIFICATION_ID, notification)
    }
}
