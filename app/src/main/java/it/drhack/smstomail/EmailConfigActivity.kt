package it.drhack.smstomail

import android.os.Bundle
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import it.drhack.smstomail.ui.theme.SmsTOmailTheme

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
                title = { Text(stringResource(R.string.email_config_title)) },
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
                            contentDescription = stringResource(R.string.back_button_description)
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
                label = { Text(stringResource(R.string.email_sender_label)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text(stringResource(R.string.password_label)) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password
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
                        stringResource(R.string.gmail_warning_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        stringResource(R.string.gmail_warning_message),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.gmail_create_app_password),
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
                label = { Text(stringResource(R.string.email_destination_label)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onSaveClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save_button))
            }
            if (saved) {
                Text(stringResource(R.string.config_saved_message), color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
