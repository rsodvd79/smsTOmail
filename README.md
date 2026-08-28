# smsTOmail

> Sincronizza gli SMS ricevuti tra il telefono e i tuoi altri dispositivi tramite email.

![Android](https://img.shields.io/badge/Android-7.0%2B-brightgreen?logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-2.x-purple?logo=kotlin)
![License](https://img.shields.io/badge/License-MIT-blue)
![Version](https://img.shields.io/badge/Version-2026.08.28-orange)

---

## Screenshot

| Schermata principale | Configurazione Email | Gestione Filtri |
|:---:|:---:|:---:|
| ![Main](screen_shot/Screenshot_20250531_163420.png) | ![Config](screen_shot/Screenshot_20260823_191309.png) | ![Filters](screen_shot/Screenshot_20260823_191402.png) |

---

## Video

[![Guarda il video su YouTube](https://img.youtube.com/vi/BZoDvigArZ4/0.jpg)](https://youtu.be/BZoDvigArZ4)

---

## Funzionalità

- 📨 **Inoltro automatico SMS** — ogni SMS tradizionale (GSM) ricevuto viene inviato via email in tempo reale
- 🔍 **Filtri avanzati** — includi o escludi SMS per mittente e/o parola chiave
- 🔒 **Password cifrata** — la password SMTP è protetta con AES-256/GCM tramite Android Keystore
- 📋 **Cronologia SMS** — log degli SMS ricevuti con stato dell'invio email
- ⚙️ **SMTP configurabile** — compatibile con Gmail (porta 587 STARTTLS) e provider con SSL diretto (porta 465, es. Aruba)
- 🔄 **Retry SMTP** — fino a tre tentativi con backoff esponenziale per errori temporanei di rete
- 🔔 **Notifiche di errore** — avvisi immediati in caso di problemi di autenticazione email
- 🌍 **Multilingua** — interfaccia in italiano, inglese, spagnolo, portoghese (Brasile), francese e tedesco

---

## Requisiti

| Componente | Versione minima |
|---|---|
| Android | 7.0 (API 24) |
| Android Studio | 2024.1 (Koala) o superiore |
| JDK | 11 o superiore |
| Gradle | fornito dal wrapper (9.3.1) |

---

## Build e installazione

```bash
# Clona il repository
git clone https://github.com/rsodvd79/smsTOmail.git
cd smsTOmail

# Build APK debug
./gradlew app:assembleDebug          # Linux/macOS
gradlew.bat app:assembleDebug        # Windows

# Test unitari
./gradlew app:testDebugUnitTest

# Test strumentati (richiede dispositivo/emulatore connesso)
./gradlew app:connectedDebugAndroidTest

# Lint
./gradlew app:lintDebug
```

> **Nota Windows:** se `JAVA_HOME` punta a un JDK non valido, impostarlo esplicitamente prima del comando:
> ```powershell
> $env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
> ```

---

## Compatibilità

| API | Note |
|---|---|
| API 24–28 | Supporto completo |
| API 29+ (Android 10+) | Foreground service avviato con tipo `DATA_SYNC` esplicito |
| API 33+ (Android 13+) | Richiesta runtime del permesso `POST_NOTIFICATIONS` |
| API 35+ (Android 15+) | Edge-to-edge obbligatorio — tutte le Activity chiamano `enableEdgeToEdge()` |
| API 37 (Android 16+) | Testato su emulatore; stabile su dispositivo fisico |

---

## Configurazione dell'app

### 1. Permessi richiesti

Al primo avvio l'app chiede i permessi necessari:

| Permesso | Motivo |
|---|---|
| `RECEIVE_SMS` | Intercettare gli SMS tradizionali (GSM) in arrivo |
| `INTERNET` | Inviare email via SMTP |
| `POST_NOTIFICATIONS` | Mostrare notifiche di errore (Android 13+) |

### 2. Configurazione email

Dalla schermata **Configurazione Email** puoi scegliere tra due modalità di invio:

#### 🔵 Modalità SMTP (Senza Cloud Console)

Configurazione manuale del server di posta in uscita:

| Campo | Descrizione |
|---|---|
| Email mittente | Account da cui partono le email di inoltro |
| Password | Password SMTP (vedi sotto per Gmail) |
| Host SMTP | Es. `smtp.gmail.com` |
| Porta SMTP | `587` (STARTTLS) oppure `465` (SSL diretto) |
| Usa TLS | Attivo per porta 587; disattivare per porta 465 |

L'app verifica che mittente e destinatario abbiano un formato email valido e che la porta SMTP sia compresa tra `1` e `65535`.

#### 🟢 Modalità Gmail API (Con Cloud Console)

Autenticazione OAuth 2.0: l'utente accede con il proprio account Google. Non è necessario inserire password SMTP.

**Prerequisiti (operazione una-tantum per lo sviluppatore):**
1. Crea un progetto su [console.cloud.google.com](https://console.cloud.google.com)
2. Abilita la **Gmail API**
3. Crea credenziali **OAuth 2.0** di tipo *Android* (con l'SHA-1 del certificato di firma)

**Per l'utente finale:**  
Tocca **Accedi con Google**, scegli l'account Gmail, autorizza l'accesso. Fine.

#### Campi comuni (entrambe le modalità)

| Campo | Descrizione |
|---|---|
| Email destinatario | Indirizzo a cui ricevere gli SMS inoltrati |
| Firma | Testo aggiunto in fondo a ogni email |
| Max SMS in cronologia | Numero massimo di voci nel log locale (minimo `1`) |

#### Gmail — Password specifica per app (solo modalità SMTP)

Gmail richiede una **password specifica per app** al posto della password dell'account:

1. Vai su [myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords)
2. Crea una nuova password per "smsTOmail"
3. Usa quella password nel campo **Password** dell'app

### 3. Filtri SMS

Dalla schermata **Gestione Filtri** puoi creare regole per decidere quali SMS inoltrare:

| Tipo | Comportamento |
|---|---|
| **INCLUDI** | Solo gli SMS che corrispondono a questo filtro vengono inoltrati |
| **ESCLUDI** | Gli SMS che corrispondono a questo filtro vengono bloccati |

I campi **Mittente** e **Parola chiave** sono entrambi opzionali: lasciare un campo vuoto significa "qualsiasi valore". Il mittente usa un match parziale case-insensitive; la parola chiave usa un match case-insensitive nel testo dell'SMS. I filtri ESCLUDI hanno sempre **precedenza** sugli INCLUDI. Non è possibile inserire due filtri con lo stesso mittente, parola chiave e tipo.

> Se non viene configurato alcun filtro, **tutti gli SMS vengono inoltrati**.

> ⚠️ **Limitazione:** l'app intercetta solo gli **SMS tradizionali (GSM/SS7)** tramite il broadcast `SMS_RECEIVED`. I messaggi **RCS** (Rich Communication Services), **MMS** e i messaggi in-app (WhatsApp, Telegram, ecc.) **non vengono intercettati**.

---

## Architettura

```
SmsReceiver (BroadcastReceiver)
    └─► SmsForwarder           — flusso condiviso: filtri, invio e log
            └─► SmsFilterProcessor — valuta i filtri INCLUDE/EXCLUDE
            └─► EmailSender        — JavaMail SMTP, retry transienti
            └─► GmailApiSender     — Gmail REST API + OAuth 2.0
            └─► Room (AppDatabase)
            ├─ FilterDao       — regole di filtraggio
            ├─ EmailConfigDao  — configurazione email (riga unica, id=0)
            └─ SmsLogDao       — cronologia SMS + stato invio

SmsBackgroundService (ForegroundService)
    └─► SmsForwarder           — percorso alternativo per SMS passati esplicitamente al servizio

NotificationHelper
    └─► notifica ad alta priorità in caso di errore di autenticazione/autorizzazione email

BootReceiver
    └─► su Android 14+ non avvia servizi dal boot; SmsReceiver riceve comunque gli SMS dal manifest

UI: Jetpack Compose
    ├─ MainActivity            — cronologia SMS + navigazione
    ├─ EmailConfigActivity     — configurazione SMTP
    └─ FilterActivity          — gestione filtri
```

**Sicurezza:** la password SMTP è cifrata con AES-256-GCM tramite Android Keystore prima di essere salvata in SQLite. Il database è escluso da Auto Backup e trasferimento dispositivo: la chiave Keystore non è ripristinabile, quindi un backup del database conterrebbe dati indecifrabili. Le connessioni SMTP SSL si fidano esclusivamente dell'host configurato; gli indirizzi email e le credenziali non vengono loggati in produzione. I messaggi Gmail API sono costruiti con header sanificati e body codificato MIME.

---

## Changelog

### 2026.08.26
- Revisione della scheda Google Play per conformità alle policy sui permessi SMS: nuova descrizione in [`store_listing_it.txt`](store_listing_it.txt), senza riferimenti a backup e codici di autenticazione/OTP, incentrata sulla sincronizzazione degli SMS tra dispositivi (caso d'uso dichiarato in Play Console).
- Nessuna modifica funzionale al codice: solo aggiornamento del numero di versione.

### 2026.08.21
- Aggiunta una schermata di **Prominent Disclosure** mostrata prima della richiesta di sistema per il permesso `RECEIVE_SMS`, per conformità con le policy Google Play sui permessi sensibili: spiega esplicitamente che il permesso serve solo a sincronizzare via email gli SMS in arrivo con altri dispositivi dell'utente.
- Resi più espliciti (IT/EN) i messaggi mostrati quando il permesso SMS non viene concesso.

### 2026.07.31
- Release build con R8 abilitato (`minify` + `shrinkResources`): il `mapping.txt` per il deoffuscamento è incluso nell'App Bundle e l'app è più piccola. Aggiunte regole ProGuard per JavaMail.
- Configurata l'estrazione dei simboli di debug nativi (`debugSymbolLevel = SYMBOL_TABLE`, richiede NDK). Nota: l'unica libreria nativa (`libandroidx.graphics.path.so` di Compose) è distribuita già strippata da Google, quindi il relativo avviso di Play Console non è eliminabile.
- Fix: la notifica di errore di autenticazione email (`NotificationHelper`) ora viene effettivamente mostrata quando l'invio fallisce per credenziali/autorizzazione non valide (prima non veniva mai emessa).
- Fix: il database Room è escluso da Auto Backup e trasferimento dispositivo — dopo un restore la chiave Android Keystore non esiste più e la password salvata sarebbe stata indecifrabile e usata silenziosamente come testo corrotto.
- Fix: salvando la configurazione in modalità Gmail API, una porta SMTP invalida viene normalizzata al default `587` invece di essere persistita così com'è.
- Rimosso codice morto: broadcast locale `SMS_RESULT` senza ricevente, `SmsResultFragment` mai usato, dipendenza `localbroadcastmanager` e controllo irraggiungibile su `InvalidSecondFactor` in `MainActivity`.

### 2026.07.29
- Aggiunta validazione di mittente/destinatario email, porta SMTP (`1`–`65535`) e limite della cronologia SMS (minimo `1`).
- Impedito l'inserimento di filtri duplicati; documentato il match parziale case-insensitive del mittente.
- Introdotto `SmsForwarder`, che centralizza filtri, invio SMTP/Gmail API e salvataggio nel log per `SmsReceiver` e `SmsBackgroundService`.
- Aggiunto retry SMTP per errori transienti: massimo tre tentativi con backoff esponenziale.
- Rafforzata la sicurezza SMTP e Gmail API: trust SSL limitato all'host configurato, log sensibili disabilitati in produzione e messaggi RFC 2822 sanificati/codificati.
- Corretto l'avvio da boot su Android 14+: nessun servizio viene avviato senza lavoro da eseguire.

### 2026.05.08
- Aggiunta scelta della modalità di invio email nella schermata **Configurazione Email**:
  - **SMTP (Senza Cloud Console)** — configurazione manuale del server SMTP (comportamento precedente)
  - **Gmail API (Con Cloud Console)** — autenticazione OAuth 2.0 tramite account Google; nessuna password SMTP richiesta
- Nuovo file `GmailApiSender.kt`: invia email via Gmail REST API usando un token OAuth ottenuto da `GoogleAuthUtil`
- Aggiornato `SmsReceiver` per instradare l'invio a `GmailApiSender` o `EmailSender` in base alla modalità configurata
- Schema Room aggiornato a versione 7 (nuovi campi `authMode` e `oauthAccount` in `email_config`)
- Fix: dopo il login Google la UI aggiornava correttamente l'account connesso (rimosso controllo errato su `resultCode`)
- Fix: `CancellationException` nelle coroutine di `MainActivity` non viene più loggata come errore

### 2026.05.07
- Versione aggiornata a 2026.05.07

### 2026.05.06
- Fix: aggiunto `enableEdgeToEdge()` in `EmailConfigActivity` e `FilterActivity` (necessario per Android 15+ / API 35+)
- Fix: `startForeground()` in `SmsBackgroundService` ora specifica `FOREGROUND_SERVICE_TYPE_DATA_SYNC` su API 29+ (obbligatorio con `foregroundServiceType` dichiarato nel manifest)
- Fix: sostituito `launchWhenStarted` (deprecato) con `repeatOnLifecycle(Lifecycle.State.STARTED)` in `MainActivity`

---

## Privacy

La app non invia dati a server di terze parti. Gli SMS vengono inoltrati direttamente dal dispositivo al server SMTP configurato dall'utente. Consulta [`privacy_policy_it.html`](privacy_policy_it.html) per i dettagli.

---

## Licenza

Distribuito sotto licenza **MIT**. Vedi [LICENSE](LICENSE) per i dettagli.
