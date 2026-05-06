package it.drhack.smstomail

import android.os.Bundle
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import it.drhack.smstomail.ui.theme.SmsTOmailTheme

class EmailConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
        var maxSmsToKeep by remember { mutableStateOf("100") }
        var smtpHost by remember { mutableStateOf("smtp.gmail.com") }
        var smtpPort by remember { mutableStateOf("587") }
        var smtpUseTls by remember { mutableStateOf(true) }
        var signature by remember { mutableStateOf("by SMS to Mail") }
        val scope = rememberCoroutineScope()
        var saved by remember { mutableStateOf(false) }
        var testResult by remember { mutableStateOf<String?>(null) }
        var isTestingEmail by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            val config = db.emailConfigDao().getConfig()
            config?.let {
                email = it.email
                password = it.password.value
                destination = it.destination
                maxSmsToKeep = it.maxSmsToKeep.toString()
                smtpHost = it.smtpHost
                smtpPort = it.smtpPort
                smtpUseTls = it.smtpUseTls
                signature = it.signature
            }
        }

        EmailConfigScreenContent(
            email = email,
            password = password,
            destination = destination,
            maxSmsToKeep = maxSmsToKeep,
            smtpHost = smtpHost,
            smtpPort = smtpPort,
            smtpUseTls = smtpUseTls,
            signature = signature,
            saved = saved,
            testResult = testResult,
            isTestingEmail = isTestingEmail,
            onEmailChange = { email = it },
            onPasswordChange = { password = it },
            onDestinationChange = { destination = it },
            onSmtpHostChange = { smtpHost = it },
            onSmtpPortChange = { newPort ->
                if (newPort.isEmpty() || newPort.all { it.isDigit() }) {
                    smtpPort = newPort
                }
            },
            onSmtpUseTlsChange = { smtpUseTls = it },
            onSignatureChange = { signature = it },
            onMaxSmsToKeepChange = { newValue ->
                // Accetta solo valori numerici positivi
                if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                    maxSmsToKeep = newValue
                }
            },
            onSaveClick = {
                scope.launch {
                    val maxSms = maxSmsToKeep.toIntOrNull() ?: 100
                    val config = EmailConfig(
                        0,
                        email,
                        EncryptedValue(password),
                        destination,
                        maxSms,
                        smtpHost,
                        smtpPort,
                        smtpUseTls,
                        signature
                    )
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
            },
            onTestEmailClick = {
                scope.launch {
                    isTestingEmail = true
                    testResult = null

                    try {
                        if (email.isBlank() || password.isBlank() || destination.isBlank()) {
                            testResult = context.getString(R.string.test_email_empty_fields)
                        } else {
                            val emailSender = EmailSender(email, password, smtpHost, smtpPort, smtpUseTls, signature)
                            var testBody = context.getString(R.string.test_email_body)
                            testResult = emailSender.sendEmail(
                                destination,
                                context.getString(R.string.test_email_subject),
                                testBody
                            )
                        }
                    } catch (e: Exception) {
                        testResult = context.getString(R.string.test_email_error, e.message)
                    } finally {
                        isTestingEmail = false
                    }
                }
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
                maxSmsToKeep = "100",
                smtpHost = "smtp.gmail.com",
                smtpPort = "587",
                smtpUseTls = true,
                signature = "by SMS to Mail",
                saved = false,
                testResult = null,
                isTestingEmail = false,
                onEmailChange = {},
                onPasswordChange = {},
                onDestinationChange = {},
                onMaxSmsToKeepChange = {},
                onSmtpHostChange = {},
                onSmtpPortChange = {},
                onSmtpUseTlsChange = {},
                onSignatureChange = {},
                onSaveClick = {},
                onGoogleHelpClick = {},
                onTestEmailClick = {}
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
    maxSmsToKeep: String,
    smtpHost: String,
    smtpPort: String,
    smtpUseTls: Boolean,
    signature: String,
    saved: Boolean,
    testResult: String?,
    isTestingEmail: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onDestinationChange: (String) -> Unit,
    onMaxSmsToKeepChange: (String) -> Unit,
    onSmtpHostChange: (String) -> Unit,
    onSmtpPortChange: (String) -> Unit,
    onSmtpUseTlsChange: (Boolean) -> Unit,
    onSignatureChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onGoogleHelpClick: () -> Unit,
    onTestEmailClick: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var passwordVisible by remember { mutableStateOf(false) }

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
                .verticalScroll(scrollState)
        ) {
            Text(
                text = stringResource(R.string.account_settings_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

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
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (passwordVisible) stringResource(R.string.password_hide) else stringResource(R.string.password_show)
                        )
                    }
                }
            )

            Text(
                text = stringResource(R.string.smtp_settings_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            OutlinedTextField(
                value = smtpHost,
                onValueChange = onSmtpHostChange,
                label = { Text(stringResource(R.string.smtp_host_label)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = smtpPort,
                onValueChange = onSmtpPortChange,
                label = { Text(stringResource(R.string.smtp_port_label)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                )
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                Switch(
                    checked = smtpUseTls,
                    onCheckedChange = onSmtpUseTlsChange
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.smtp_use_tls_label),
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

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

            Text(
                text = stringResource(R.string.general_settings_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            OutlinedTextField(
                value = maxSmsToKeep,
                onValueChange = onMaxSmsToKeepChange,
                label = { Text(stringResource(R.string.max_sms_to_keep_label)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                )
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = destination,
                onValueChange = onDestinationChange,
                label = { Text(stringResource(R.string.email_destination_label)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            // Campo per la firma
            OutlinedTextField(
                value = signature,
                onValueChange = onSignatureChange,
                label = { Text(stringResource(R.string.signature_label)) },
                placeholder = { Text(stringResource(R.string.signature_hint)) },
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

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onTestEmailClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isTestingEmail
            ) {
                Text(stringResource(R.string.test_email_button))
            }

            testResult?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
