package it.drhack.smstomail

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, SmsBackgroundService::class.java)
            serviceIntent.putExtra("bootCompleted", true)

            // Per Android 15 (SDK 35) e superiori, non avviare direttamente un servizio in primo piano
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14 o superiore
                // Invece di avviare direttamente il servizio in primo piano, avviamo un servizio normale
                // che poi può programmare un lavoro o utilizzare WorkManager
                context.startService(serviceIntent)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Per Android 8-14 possiamo ancora usare startForegroundService
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
