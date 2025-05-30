package com.biemme.smstomail

import android.os.Bundle
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.biemme.smstomail.ui.theme.SmsTOmailTheme

class EmailConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EmailConfigScreen()
        }
    }

    @Composable
    fun EmailConfigScreen() {
        val context = this
        val db = remember { AppDatabase.getInstance(context) }
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var destination by remember { mutableStateOf("") }
        val scope = rememberCoroutineScope()
        var saved by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            val config = db.emailConfigDao().getConfig()
            config?.let {
                email = it.email
                password = it.password
                destination = it.destination
            }
        }

        EmailConfigScreenContent(
            email = email,
            password = password,
            destination = destination,
            saved = saved,
            onEmailChange = { email = it },
            onPasswordChange = { password = it },
            onDestinationChange = { destination = it },
            onSaveClick = {
                scope.launch {
                    val config = EmailConfig(0, email, password, destination)
                    if (db.emailConfigDao().getConfig() == null) {
                        db.emailConfigDao().insertConfig(config)
                    } else {
                        db.emailConfigDao().updateConfig(config)
                    }
                    saved = true
                    // Torna alla MainActivity dopo il salvataggio
                    (context as? Activity)?.finish()
                }
            },
            onGoogleHelpClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://myaccount.google.com/apppasswords"))
                context.startActivity(intent)
            }
        )
    }
}

@Preview(showBackground = true, name = "EmailConfigScreen Preview")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailConfigScreenPreview() {
    SmsTOmailTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            EmailConfigScreenContent(
                email = "esempio@gmail.com",
                password = "password123",
                destination = "destinatario@email.com",
                saved = false,
                onEmailChange = {},
                onPasswordChange = {},
                onDestinationChange = {},
                onSaveClick = {},
                onGoogleHelpClick = {}
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailConfigScreenContent(
    email: String,
    password: String,
    destination: String,
    saved: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onDestinationChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onGoogleHelpClick: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Configurazione Email") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                navigationIcon = {
                    IconButton(onClick = {
                        (context as? Activity)?.finish()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Torna indietro"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Email mittente") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Password
                )
            )

            // Aggiungiamo un avviso per gli utenti Gmail
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "IMPORTANTE per account Gmail:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "Google richiede una 'Password specifica per app' e non la password standard del tuo account.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Clicca qui per creare una password specifica per app",
                        style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.Underline),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.clickable(onClick = onGoogleHelpClick)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = destination,
                onValueChange = onDestinationChange,
                label = { Text("Email destinatario") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onSaveClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Salva")
            }
            if (saved) {
                Text("Configurazione salvata!", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
