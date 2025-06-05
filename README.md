# smsTOmail

smsTOmail è un'applicazione Android che inoltra automaticamente gli SMS ricevuti tramite e-mail.

## Prerequisiti

- **Android Studio**: consigliata la versione 2024.1 (Koala) o superiore.
- **JDK**: 11 o superiore.
- **Gradle**: il progetto utilizza il wrapper con la versione 8.11.1.

## Configurazione del progetto

1. Clonare il repository:
   ```bash
   git clone <repo-url>
   cd smsTOmail
   ```
2. Aprire la cartella del progetto con Android Studio oppure usare il wrapper Gradle:
   ```bash
   ./gradlew assembleDebug
   ```

## Esecuzione dei test

Per avviare i test unitari:
```bash
./gradlew test
```
Per gli instrumented test (su dispositivo/emulatore):
```bash
./gradlew connectedAndroidTest
```

## Configurazione dell'email

All'avvio dell'app occorre configurare un account email dal quale inviare i messaggi. Per gli account Gmail è necessario generare una "Password specifica per app" e usarla al posto della password normale.

La schermata "Configurazione Email" permette di inserire:
- Indirizzo mittente e relativa password
- Indirizzo destinatario
- Host e porta SMTP
- Numero massimo di SMS da conservare
- Firma da aggiungere alle email

## Gestione dei filtri

Dal menu è possibile accedere alla "Gestione Filtri SMS" per specificare mittente e/o parole chiave da includere o escludere. Se non viene impostato alcun filtro, tutti gli SMS saranno inoltrati.

## Note aggiuntive

- Nella cartella `screen_shot` sono presenti alcune immagini di esempio dell'app.
- Il file `privacy_policy_it.html` contiene la policy sulla privacy in italiano.

## Licenza

Questo progetto è distribuito con licenza MIT. Per maggiori dettagli consulta il file [LICENSE](LICENSE).
