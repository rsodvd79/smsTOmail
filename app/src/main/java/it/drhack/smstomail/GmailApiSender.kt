package it.drhack.smstomail

import android.content.Context
import android.util.Base64
import android.util.Log
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Invia email tramite Gmail REST API usando OAuth 2.0.
 * Non richiede credenziali SMTP: si autentica con l'account Google già connesso.
 */
class GmailApiSender(
    private val context: Context,
    private val signature: String
) {
    companion object {
        private const val TAG = "GmailApiSender"
        private const val GMAIL_SCOPE = "oauth2:https://www.googleapis.com/auth/gmail.send"
        private const val GMAIL_API_URL = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send"
    }

    suspend fun sendEmail(recipient: String, subject: String, body: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val signedInAccount = GoogleSignIn.getLastSignedInAccount(context)
                    ?: return@withContext "Errore: nessun account Google connesso. Apri l'app e riconfigura."

                val googleAccount = signedInAccount.account
                    ?: return@withContext "Errore: impossibile ottenere l'account Google."

                val token = try {
                    GoogleAuthUtil.getToken(context, googleAccount, GMAIL_SCOPE)
                } catch (e: UserRecoverableAuthException) {
                    Log.e(TAG, "Autorizzazione richiesta", e)
                    return@withContext "Errore di autorizzazione Gmail: apri l'app per rinnovare l'accesso."
                } catch (e: GoogleAuthException) {
                    Log.e(TAG, "Errore autenticazione Google", e)
                    return@withContext "Errore di autenticazione Google: ${e.message}"
                }

                val finalBody = if (signature.isNotBlank()) "$body\n\n$signature" else body
                val rawMessage = buildRfc2822Message(
                    from = signedInAccount.email ?: "",
                    to = recipient,
                    subject = subject,
                    body = finalBody
                )
                val encoded = Base64.encodeToString(
                    rawMessage.toByteArray(Charsets.UTF_8),
                    Base64.URL_SAFE or Base64.NO_WRAP
                )

                val url = URL(GMAIL_API_URL)
                val connection = url.openConnection() as HttpURLConnection
                try {
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Authorization", "Bearer $token")
                    connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    connection.doOutput = true
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000

                    val jsonBody = "{\"raw\":\"$encoded\"}"
                    connection.outputStream.use { os ->
                        os.write(jsonBody.toByteArray(Charsets.UTF_8))
                    }

                    val responseCode = connection.responseCode
                    if (responseCode in 200..299) {
                        Log.d(TAG, "Email inviata con successo a $recipient via Gmail API")
                        "Email inviata con successo"
                    } else {
                        val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: ""
                        Log.e(TAG, "Errore Gmail API: $responseCode - $errorBody")
                        "Errore Gmail API ($responseCode)"
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Errore generico invio Gmail", e)
                "Errore generico: ${e.message}"
            }
        }
    }

    private fun buildRfc2822Message(from: String, to: String, subject: String, body: String): String {
        return "From: $from\r\n" +
               "To: $to\r\n" +
               "Subject: $subject\r\n" +
               "Content-Type: text/plain; charset=UTF-8\r\n" +
               "\r\n" +
               body
    }
}
