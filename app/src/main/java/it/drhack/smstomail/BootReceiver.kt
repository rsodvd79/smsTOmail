package it.drhack.smstomail

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Su Android 14+ non avviamo alcun servizio dal boot:
            // - startService() da background lancerebbe IllegalStateException
            // - il servizio a boot non ha comunque lavoro da svolgere (si auto-ferma)
            // - la ricezione SMS è garantita da SmsReceiver registrato nel manifest
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                return
            }

            val serviceIntent = Intent(context, SmsBackgroundService::class.java)
            serviceIntent.putExtra("bootCompleted", true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Per Android 8-13 possiamo ancora usare startForegroundService
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
