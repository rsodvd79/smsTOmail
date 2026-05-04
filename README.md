# smsTOmail

> Inoltra automaticamente gli SMS ricevuti via email — configura una volta, dimentica per sempre.

![Android](https://img.shields.io/badge/Android-7.0%2B-brightgreen?logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-2.x-purple?logo=kotlin)
![License](https://img.shields.io/badge/License-MIT-blue)
![Version](https://img.shields.io/badge/Version-2025.06.04-orange)

---

## Screenshot

| Schermata principale | Configurazione Email | Gestione Filtri |
|:---:|:---:|:---:|
| ![Main](screen_shot/Screenshot_20250531_163207.png) | ![Config](screen_shot/Screenshot_20250531_163250.png) | ![Filters](screen_shot/Screenshot_20250531_163420.png) |

---

## Funzionalità

- 📨 **Inoltro automatico SMS** — ogni SMS ricevuto viene inviato via email in tempo reale
- 🔍 **Filtri avanzati** — includi o escludi SMS per mittente e/o parola chiave
- 🔒 **Password cifrata** — la password SMTP è protetta con AES-256/GCM tramite Android Keystore
- 📋 **Cronologia SMS** — log degli SMS ricevuti con stato dell'invio email
- ⚙️ **SMTP configurabile** — compatibile con Gmail (porta 587 STARTTLS) e provider con SSL diretto (porta 465, es. Aruba)
- 🔔 **Notifiche di errore** — avvisi immediati in caso di problemi di autenticazione email
- 🌍 **Multilingua** — interfaccia in italiano e inglese

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
> $env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.11'
> ```

---

## Configurazione dell'app

### 1. Permessi richiesti

Al primo avvio l'app chiede i permessi necessari:

| Permesso | Motivo |
|---|---|
| `RECEIVE_SMS` | Intercettare gli SMS in arrivo |
| `INTERNET` | Inviare email via SMTP |
| `POST_NOTIFICATIONS` | Mostrare notifiche di errore (Android 13+) |

### 2. Configurazione email

Dalla schermata **Configurazione Email** inserisci:

| Campo | Descrizione |
|---|---|
| Email mittente | Account da cui partono le email di inoltro |
| Password | Password SMTP (vedi sotto per Gmail) |
| Email destinatario | Indirizzo a cui ricevere gli SMS inoltrati |
| Host SMTP | Es. `smtp.gmail.com` |
| Porta SMTP | `587` (STARTTLS) oppure `465` (SSL diretto) |
| Usa TLS | Attivo per porta 587; disattivare per porta 465 |
| Firma | Testo aggiunto in fondo a ogni email |
| Max SMS in cronologia | Numero massimo di voci nel log locale |

#### Gmail — Password specifica per app

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

I campi **Mittente** e **Parola chiave** sono entrambi opzionali: lasciare un campo vuoto significa "qualsiasi valore". I filtri ESCLUDI hanno sempre **precedenza** sugli INCLUDI.

> Se non viene configurato alcun filtro, **tutti gli SMS vengono inoltrati**.

---

## Architettura

```
SmsReceiver (BroadcastReceiver)
    └─► SmsFilterProcessor     — valuta i filtri INCLUDE/EXCLUDE
    └─► EmailSender            — invia via JavaMail SMTP
    └─► Room (AppDatabase)
            ├─ FilterDao       — regole di filtraggio
            ├─ EmailConfigDao  — configurazione SMTP (riga unica, id=0)
            └─ SmsLogDao       — cronologia SMS + stato invio

SmsBackgroundService (ForegroundService)
    └─► path alternativo (avviato da BootReceiver al riavvio)

UI: Jetpack Compose
    ├─ MainActivity            — cronologia SMS + navigazione
    ├─ EmailConfigActivity     — configurazione SMTP
    └─ FilterActivity          — gestione filtri
```

**Sicurezza:** la password SMTP è cifrata con AES-256-GCM tramite Android Keystore prima di essere salvata in SQLite.

---

## Privacy

La app non invia dati a server di terze parti. Gli SMS vengono inoltrati direttamente dal dispositivo al server SMTP configurato dall'utente. Consulta [`privacy_policy_it.html`](privacy_policy_it.html) per i dettagli.

---

## Licenza

Distribuito sotto licenza **MIT**. Vedi [LICENSE](LICENSE) per i dettagli.

