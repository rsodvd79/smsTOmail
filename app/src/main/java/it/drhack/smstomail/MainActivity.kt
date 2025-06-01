package it.drhack.smstomail

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.drhack.smstomail.ui.theme.SmsTOmailTheme
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.app.AlertDialog
import android.util.Log
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.collectLatest

class MainActivity : FragmentActivity() {
    private var emailConfig: EmailConfig? = null
    private var lastSmsMessage: String? = null
    private var lastEmailResult: String? = null
    private var smsLogEntries by mutableStateOf<List<SmsLogEntry>>(emptyList())

    // Lista dei permessi necessari per l'app
    private val requiredPermissions = arrayOf(
        android.Manifest.permission.RECEIVE_SMS,
        android.Manifest.permission.INTERNET,
        android.Manifest.permission.ACCESS_NETWORK_STATE
    )

    // Permessi per servizi in foreground (necessari solo per Android 14+)
    private val foregroundServicePermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        arrayOf(
            android.Manifest.permission.FOREGROUND_SERVICE,
            android.Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC
        )
    } else {
        arrayOf(android.Manifest.permission.FOREGROUND_SERVICE)
    }

    // Registrazione per la richiesta dei permessi
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (!allGranted) {
            showPermissionDialog()
        } else {
            checkForegroundServicePermissions()
        }
    }

    private val requestForegroundServicePermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (!allGranted) {
            showPermissionDialog()
        } else {
            initializeApp()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Controlla i permessi prima di inizializzare l'app
        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val permissionsNotGranted = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsNotGranted.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsNotGranted)
        } else {
            checkForegroundServicePermissions()
        }
    }

    private fun checkForegroundServicePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionsNotGranted = foregroundServicePermissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }.toTypedArray()

            if (permissionsNotGranted.isNotEmpty()) {
                requestForegroundServicePermissions.launch(permissionsNotGranted)
            } else {
                initializeApp()
            }
        } else {
            initializeApp()
        }
    }

    private fun showPermissionDialog() {
        val builder = AlertDialog.Builder(this)
        builder.apply {
            setTitle(getString(R.string.permission_dialog_title))
            setMessage(getString(R.string.permission_dialog_message))
            setPositiveButton(getString(R.string.permission_dialog_settings_button)) { _, _ ->
                // Apri le impostazioni dell'app
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            setNegativeButton(getString(R.string.permission_dialog_exit_button)) { _, _ ->
                // Chiudi l'app
                finish()
            }
        }
        builder.create().show()
    }

    private fun initializeApp() {
        // Aggiungo log per tracciare l'esecuzione
        Log.d("MainActivity", "Inizializzazione dell'app in corso...")

        // Controlla se la configurazione email esiste
        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            try {
                Log.d("MainActivity", "Recupero configurazione dal database...")
                val config = db.emailConfigDao().getConfig()

                // Se la configurazione non esiste, mostra la schermata di configurazione email
                if (config == null) {
                    Log.d("MainActivity", "Nessuna configurazione trovata, avvio EmailConfigActivity")
                    val intent = Intent(this@MainActivity, EmailConfigActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // La configurazione esiste, mostra la schermata principale
                    Log.d("MainActivity", "Configurazione trovata, mostra la schermata principale")
                    emailConfig = config

                    // Carica i log degli SMS
                    db.smsLogDao().getAllLogs().collectLatest { logs ->
                        smsLogEntries = logs
                    }

                    // Assicuriamoci di essere nel thread principale per aggiornare l'UI
                    if (!isDestroyed && !isFinishing) {
                        setContent {
                            SmsTOmailTheme {
                                MainScreen(
                                    config = emailConfig,
                                    lastSmsMessage = lastSmsMessage,
                                    lastEmailResult = lastEmailResult,
                                    smsLogEntries = smsLogEntries,
                                    onEditConfig = {
                                        val intent = Intent(this@MainActivity, EmailConfigActivity::class.java)
                                        startActivity(intent)
                                    },
                                    onManageFilters = {
                                        val intent = Intent(this@MainActivity, FilterActivity::class.java)
                                        startActivity(intent)
                                    },
                                    onClearLogs = {
                                        lifecycleScope.launch {
                                            db.smsLogDao().deleteAll()
                                        }
                                    }
                                )
                            }
                        }

                        // Avvia anche il servizio di ascolto SMS se necessario
                        startSmsListenerService()
                    } else {
                        Log.e("MainActivity", "Activity distrutta o in chiusura, impossibile aggiornare l'UI")
                    }
                }
            } catch (e: Exception) {
                // Gestione degli errori
                Log.e("MainActivity", "Errore durante l'inizializzazione dell'app", e)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Aggiorna la configurazione quando l'attività riprende
        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            val config = db.emailConfigDao().getConfig()
            if (config != null) {
                emailConfig = config
                // Ricrea il content solo se necessario
                if (emailConfig != null) {
                    setContent {
                        SmsTOmailTheme {
                            MainScreen(
                                config = emailConfig,
                                lastSmsMessage = lastSmsMessage,
                                lastEmailResult = lastEmailResult,
                                smsLogEntries = smsLogEntries,
                                onEditConfig = {
                                    val intent = Intent(this@MainActivity, EmailConfigActivity::class.java)
                                    startActivity(intent)
                                },
                                onManageFilters = {
                                    val intent = Intent(this@MainActivity, FilterActivity::class.java)
                                    startActivity(intent)
                                },
                                onClearLogs = {
                                    lifecycleScope.launch {
                                        db.smsLogDao().deleteAll()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Avvia il servizio di ascolto degli SMS
    private fun startSmsListenerService() {
        try {
            Log.d("MainActivity", "Avvio del servizio di ascolto SMS")
            val serviceIntent = Intent(this, SmsBackgroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            Log.d("MainActivity", "Servizio di ascolto SMS avviato con successo")
        } catch (e: Exception) {
            Log.e("MainActivity", "Errore nell'avvio del servizio di ascolto SMS", e)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    config: EmailConfig?,
    lastSmsMessage: String?,
    lastEmailResult: String?,
    smsLogEntries: List<SmsLogEntry>,
    onEditConfig: () -> Unit,
    onManageFilters: () -> Unit,
    onClearLogs: () -> Unit
) {
    // Verifichiamo se c'è un errore di autenticazione Gmail
    val hasAuthError = lastEmailResult?.contains("password specifica per app") == true ||
                      lastEmailResult?.contains("InvalidSecondFactor") == true

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("SMS to Mail") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mostra un banner di errore per problemi di autenticazione Gmail
            if (hasAuthError) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Problema di autenticazione Gmail",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Gmail richiede una password specifica per app. La password normale del tuo account Google non funzionerà.",
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onEditConfig,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Text("Aggiorna configurazione email")
                        }
                    }
                }
            }

            // Info sulla configurazione email
            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Configurazione Email",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    config?.let {
                        Text("Email mittente: ${it.email}")
                        Text("Email destinatario: ${it.destination}")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onEditConfig,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Modifica")
                        Spacer(Modifier.width(4.dp))
                        Text("Modifica")
                    }
                }
            }

            // Pulsante per gestire i filtri
            Button(
                onClick = onManageFilters,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Filtri")
                Spacer(Modifier.width(8.dp))
                Text("Gestione Filtri")
            }

            // Lista degli SMS ricevuti
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxSize()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cronologia SMS ricevuti",
                            style = MaterialTheme.typography.titleMedium
                        )

                        IconButton(
                            onClick = onClearLogs
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Svuota cronologia",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (smsLogEntries.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Nessun SMS ricevuto", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(smsLogEntries) { entry ->
                                SmsLogItem(entry)
                                Divider()
                            }
                        }
                    }
                }
            }

            // Informazioni
            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Informazioni",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("L'app è in esecuzione e pronta a ricevere SMS.")
                    Text("Gli SMS che corrispondono ai filtri impostati verranno inoltrati via email.")
                }
            }
        }
    }
}

@Composable
fun SmsLogItem(entry: SmsLogEntry) {
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ITALIAN)
    val formattedDate = dateFormatter.format(entry.timestamp)

    Column(
        modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = entry.sender,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = formattedDate,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = entry.message,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Stato: ",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = if (entry.emailSent) "Email inviata" else "Email non inviata",
                color = if (entry.emailSent)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        entry.emailResult?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true, name = "MainScreen - Dati completi", device = "id:pixel_8")
@Composable
fun MainScreenPreview() {
    val mockConfig = EmailConfig(
        id = 1,
        email = "esempio@gmail.com",
        password = "password",
        destination = "destinatario@email.com"
    )

    val mockSmsLogEntries = listOf(
        SmsLogEntry(
            id = 1,
            sender = "+39123456789",
            message = "Questo è un esempio di SMS ricevuto che verrebbe inoltrato via email secondo i filtri configurati.",
            timestamp = Date(),
            emailSent = true,
            emailResult = "Email inviata con successo"
        ),
        SmsLogEntry(
            id = 2,
            sender = "+39987654321",
            message = "Un altro SMS di esempio con un testo più lungo per vedere come si comporta l'interfaccia con messaggi di lunghezza maggiore.",
            timestamp = Date(System.currentTimeMillis() - 3600000), // 1 ora fa
            emailSent = false,
            emailResult = "Errore di invio: il mittente non corrisponde ai filtri"
        )
    )

    SmsTOmailTheme {
        MainScreen(
            config = mockConfig,
            lastSmsMessage = "Ultimo SMS: +39123456789 - Messaggio di prova",
            lastEmailResult = null,
            smsLogEntries = mockSmsLogEntries,
            onEditConfig = {},
            onManageFilters = {},
            onClearLogs = {}
        )
    }
}

@Preview(showBackground = true, name = "MainScreen - Errore Auth", device = "id:pixel_8")
@Composable
fun MainScreenErrorPreview() {
    val mockConfig = EmailConfig(
        id = 1,
        email = "esempio@gmail.com",
        password = "password",
        destination = "destinatario@email.com"
    )

    SmsTOmailTheme {
        MainScreen(
            config = mockConfig,
            lastSmsMessage = null,
            lastEmailResult = "Errore: La password inserita è scorretta. Gmail richiede una password specifica per app",
            smsLogEntries = emptyList(),
            onEditConfig = {},
            onManageFilters = {},
            onClearLogs = {}
        )
    }
}

@Preview(showBackground = true, name = "MainScreen - Nessun SMS", device = "id:pixel_8")
@Composable
fun MainScreenEmptyPreview() {
    val mockConfig = EmailConfig(
        id = 1,
        email = "esempio@gmail.com",
        password = "password",
        destination = "destinatario@email.com"
    )

    SmsTOmailTheme {
        MainScreen(
            config = mockConfig,
            lastSmsMessage = null,
            lastEmailResult = null,
            smsLogEntries = emptyList(),
            onEditConfig = {},
            onManageFilters = {},
            onClearLogs = {}
        )
    }
}
