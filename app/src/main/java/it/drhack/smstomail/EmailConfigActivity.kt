package it.drhack.smstomail

import android.os.Bundle
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
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

        var authMode by remember { mutableStateOf(EmailConfig.AUTH_MODE_SMTP) }
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var destination by remember { mutableStateOf("") }
        var maxSmsToKeep by remember { mutableStateOf("100") }
        var smtpHost by remember { mutableStateOf("smtp.gmail.com") }
        var smtpPort by remember { mutableStateOf("587") }
        var smtpUseTls by remember { mutableStateOf(true) }
        var signature by remember { mutableStateOf("by SMS to Mail") }
        var oauthAccount by remember { mutableStateOf("") }
        var oauthSignInError by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()
        var saved by remember { mutableStateOf(false) }
        var testResult by remember { mutableStateOf<String?>(null) }
        var isTestingEmail by remember { mutableStateOf(false) }

        val gso = remember {
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(Scope("https://www.googleapis.com/auth/gmail.send"))
                .build()
        }
        val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

        val signInLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                oauthAccount = account.email ?: ""
                oauthSignInError = null
            } catch (e: ApiException) {
                // Status 12501 = utente ha annullato, non è un errore da mostrare
                if (e.statusCode != 12501) {
                    oauthSignInError = context.getString(R.string.oauth_sign_in_error, e.statusCode.toString())
                }
            }
        }

        LaunchedEffect(Unit) {
            val config = db.emailConfigDao().getConfig()
            config?.let {
                authMode = it.authMode
                email = it.email
                password = it.password.value
                destination = it.destination
                maxSmsToKeep = it.maxSmsToKeep.toString()
                smtpHost = it.smtpHost
                smtpPort = it.smtpPort
                smtpUseTls = it.smtpUseTls
                signature = it.signature
                oauthAccount = it.oauthAccount
            }
            if (oauthAccount.isEmpty()) {
                GoogleSignIn.getLastSignedInAccount(context)?.email?.let { oauthAccount = it }
            }
        }

        EmailConfigScreenContent(
            authMode = authMode,
            email = email,
            password = password,
            destination = destination,
            maxSmsToKeep = maxSmsToKeep,
            smtpHost = smtpHost,
            smtpPort = smtpPort,
            smtpUseTls = smtpUseTls,
            signature = signature,
            oauthAccount = oauthAccount,
            oauthSignInError = oauthSignInError,
            saved = saved,
            testResult = testResult,
            isTestingEmail = isTestingEmail,
            onAuthModeChange = { authMode = it },
            onEmailChange = { email = it },
            onPasswordChange = { password = it },
            onDestinationChange = { destination = it },
            onSmtpHostChange = { smtpHost = it },
            onSmtpPortChange = { newPort ->
                if (newPort.isEmpty() || newPort.all { it.isDigit() }) smtpPort = newPort
            },
            onSmtpUseTlsChange = { smtpUseTls = it },
            onSignatureChange = { signature = it },
            onMaxSmsToKeepChange = { newValue ->
                if (newValue.isEmpty() || newValue.all { it.isDigit() }) maxSmsToKeep = newValue
            },
            onSignInClick = { signInLauncher.launch(googleSignInClient.signInIntent) },
            onSignOutClick = {
                googleSignInClient.signOut().addOnSuccessListener { oauthAccount = "" }
            },
            onSaveClick = {
                scope.launch {
                    if (authMode == EmailConfig.AUTH_MODE_GMAIL_OAUTH && oauthAccount.isEmpty()) {
                        testResult = context.getString(R.string.oauth_required_sign_in)
                        return@launch
                    }
                    val finalEmail = if (authMode == EmailConfig.AUTH_MODE_GMAIL_OAUTH) oauthAccount else email
                    val maxSms = maxSmsToKeep.toIntOrNull() ?: 100
                    val config = EmailConfig(
                        id = 0,
                        email = finalEmail,
                        password = EncryptedValue(if (authMode == EmailConfig.AUTH_MODE_GMAIL_OAUTH) "" else password),
                        destination = destination,
                        maxSmsToKeep = maxSms,
                        smtpHost = smtpHost,
                        smtpPort = smtpPort,
                        smtpUseTls = smtpUseTls,
                        signature = signature,
                        authMode = authMode,
                        oauthAccount = oauthAccount
                    )
                    if (db.emailConfigDao().getConfig() == null) {
                        db.emailConfigDao().insertConfig(config)
                    } else {
                        db.emailConfigDao().updateConfig(config)
                    }
                    saved = true
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
                        if (destination.isBlank()) {
                            testResult = context.getString(R.string.test_email_empty_fields)
                        } else if (authMode == EmailConfig.AUTH_MODE_GMAIL_OAUTH) {
                            if (oauthAccount.isEmpty()) {
                                testResult = context.getString(R.string.oauth_required_sign_in)
                            } else {
                                val sender = GmailApiSender(context, signature)
                                testResult = sender.sendEmail(
                                    destination,
                                    context.getString(R.string.test_email_subject),
                                    context.getString(R.string.test_email_body)
                                )
                            }
                        } else {
                            if (email.isBlank() || password.isBlank()) {
                                testResult = context.getString(R.string.test_email_empty_fields)
                            } else {
                                val emailSender = EmailSender(email, password, smtpHost, smtpPort, smtpUseTls, signature)
                                testResult = emailSender.sendEmail(
                                    destination,
                                    context.getString(R.string.test_email_subject),
                                    context.getString(R.string.test_email_body)
                                )
                            }
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
                authMode = EmailConfig.AUTH_MODE_SMTP,
                email = "esempio@gmail.com",
                password = "password123",
                destination = "destinatario@email.com",
                maxSmsToKeep = "100",
                smtpHost = "smtp.gmail.com",
                smtpPort = "587",
                smtpUseTls = true,
                signature = "by SMS to Mail",
                oauthAccount = "",
                oauthSignInError = null,
                saved = false,
                testResult = null,
                isTestingEmail = false,
                onAuthModeChange = {},
                onEmailChange = {},
                onPasswordChange = {},
                onDestinationChange = {},
                onMaxSmsToKeepChange = {},
                onSmtpHostChange = {},
                onSmtpPortChange = {},
                onSmtpUseTlsChange = {},
                onSignatureChange = {},
                onSignInClick = {},
                onSignOutClick = {},
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
    authMode: String,
    email: String,
    password: String,
    destination: String,
    maxSmsToKeep: String,
    smtpHost: String,
    smtpPort: String,
    smtpUseTls: Boolean,
    signature: String,
    oauthAccount: String,
    oauthSignInError: String?,
    saved: Boolean,
    testResult: String?,
    isTestingEmail: Boolean,
    onAuthModeChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onDestinationChange: (String) -> Unit,
    onMaxSmsToKeepChange: (String) -> Unit,
    onSmtpHostChange: (String) -> Unit,
    onSmtpPortChange: (String) -> Unit,
    onSmtpUseTlsChange: (Boolean) -> Unit,
    onSignatureChange: (String) -> Unit,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
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
                    IconButton(onClick = { (context as? Activity)?.finish() }) {
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
            // ── Selettore modalità ──────────────────────────────────────────
            Text(
                text = stringResource(R.string.auth_mode_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = authMode == EmailConfig.AUTH_MODE_SMTP,
                    onClick = { onAuthModeChange(EmailConfig.AUTH_MODE_SMTP) },
                    label = { Text(stringResource(R.string.auth_mode_smtp)) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = authMode == EmailConfig.AUTH_MODE_GMAIL_OAUTH,
                    onClick = { onAuthModeChange(EmailConfig.AUTH_MODE_GMAIL_OAUTH) },
                    label = { Text(stringResource(R.string.auth_mode_gmail_oauth)) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Sezione SMTP ────────────────────────────────────────────────
            if (authMode == EmailConfig.AUTH_MODE_SMTP) {
                Text(
                    text = stringResource(R.string.account_settings_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Switch(checked = smtpUseTls, onCheckedChange = onSmtpUseTlsChange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.smtp_use_tls_label),
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

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
            }

            // ── Sezione Gmail OAuth ─────────────────────────────────────────
            if (authMode == EmailConfig.AUTH_MODE_GMAIL_OAUTH) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (oauthAccount.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.oauth_connected_account, oauthAccount),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            OutlinedButton(
                                onClick = onSignOutClick,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.oauth_sign_out_button))
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.oauth_not_connected),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Button(
                                onClick = onSignInClick,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.oauth_sign_in_button))
                            }
                        }

                        oauthSignInError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }

            // ── Impostazioni generali (sempre visibili) ─────────────────────
            Text(
                text = stringResource(R.string.general_settings_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            OutlinedTextField(
                value = maxSmsToKeep,
                onValueChange = onMaxSmsToKeepChange,
                label = { Text(stringResource(R.string.max_sms_to_keep_label)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = destination,
                onValueChange = onDestinationChange,
                label = { Text(stringResource(R.string.email_destination_label)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = signature,
                onValueChange = onSignatureChange,
                label = { Text(stringResource(R.string.signature_label)) },
                placeholder = { Text(stringResource(R.string.signature_hint)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Button(onClick = onSaveClick, modifier = Modifier.fillMaxWidth()) {
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
