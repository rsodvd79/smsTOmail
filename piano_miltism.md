# Piano di implementazione — Supporto Multi-SIM (slot)

## Obiettivo

Su dispositivi dual/multi-SIM, identificare **lo slot SIM** (SIM 1, SIM 2, …) su cui è arrivato ogni SMS, mostrarlo nell'email inoltrata e nella cronologia, e permettere di **filtrare per slot**. Se in un filtro lo slot non è specificato, il filtro vale per **tutti gli slot**.

**Fuori scope:** nome operatore/numero della SIM (richiederebbe `READ_PHONE_STATE`). Si usa solo l'indice di slot, disponibile senza permessi aggiuntivi.

---

## 1. Rilevamento dello slot — `SmsReceiver`

L'intent `SMS_RECEIVED` include extra aggiunti dal framework (API 22+):

- `SubscriptionManager.EXTRA_SLOT_INDEX` (`"android.telephony.extra.SLOT_INDEX"`) — chiave standard
- fallback legacy: extra `"slot"` e `"phone"` (usati su alcune ROM/versioni)

Logica:

```kotlin
private fun extractSimSlot(intent: Intent): Int {
    val slot = intent.getIntExtra(SubscriptionManager.EXTRA_SLOT_INDEX,
        intent.getIntExtra("slot",
            intent.getIntExtra("phone", -1)))
    return slot // -1 = sconosciuto (single SIM vecchie API o extra assente)
}
```

- Valore `-1` = slot non determinabile → trattato come "sconosciuto".
- Lo slot viene passato a `SmsForwarder.handleIncomingSms(context, sender, message, simSlot)`.
- Nessun nuovo permesso nel manifest.

## 2. Modello dati — Room (versione 7 → 8)

### 2.1 `Filter`
Nuovo campo:

```kotlin
val simSlot: Int? = null   // null = qualsiasi slot
```

- `null` = il filtro si applica a tutti gli slot (comportamento retrocompatibile).
- `0`, `1`, … = il filtro si applica solo agli SMS arrivati su quello slot.

### 2.2 `SmsLogEntry`
Nuovo campo:

```kotlin
val simSlot: Int? = null   // null = sconosciuto / non rilevato
```

### 2.3 Migrazione `MIGRATION_7_8` in `AppDatabase`

```sql
ALTER TABLE filters ADD COLUMN simSlot INTEGER;      -- nullable, default NULL
ALTER TABLE sms_log ADD COLUMN simSlot INTEGER;      -- nullable, default NULL
```

- Aggiornare `@Database(version = 8)` e registrare la migrazione in `addMigrations(...)`.
- Rigenerare lo schema esportato in `app/schemas/it.drhack.smstomail.AppDatabase/8.json` (build kapt).
- Nessun nuovo TypeConverter necessario (`Int?` è nativo per Room).

## 3. Logica di filtraggio — `SmsFilterProcessor`

Firma aggiornata:

```kotlin
fun shouldProcessSms(sender: String, message: String, simSlot: Int): Boolean
```

Regola di match dello slot, integrata nella funzione `matches(filter)` esistente (logica AND con sender e keyword):

```kotlin
val slotMatch = filter.simSlot == null || filter.simSlot == simSlot
```

Semantica:

- **Filtro senza slot (`null`)** → matcha SMS da qualsiasi slot (incluso slot sconosciuto `-1`).
- **Filtro con slot specificato** → matcha solo se lo slot dell'SMS coincide. Un SMS con slot sconosciuto (`-1`) **non** matcha un filtro con slot specifico.
- Le priorità restano invariate: EXCLUDE prevale su INCLUDE; nessun filtro = inoltra tutto.

## 4. Flusso di inoltro — `SmsForwarder`

- `handleIncomingSms(context, sender, message, simSlot: Int = -1)`.
- Passa `simSlot` a `shouldProcessSms(...)`.
- Oggetto email arricchito quando lo slot è noto:
  - slot noto: `"Nuovo SMS da $sender [SIM ${simSlot + 1}]"` (slot 0 → "SIM 1")
  - slot sconosciuto: oggetto invariato `"Nuovo SMS da $sender"`
- `log(...)` salva `simSlot` in `SmsLogEntry` (`null` se `-1`).
- `SmsBackgroundService.processSms` aggiornato per propagare l'eventuale extra `simSlot` dell'intent (default `-1`).

## 5. UI

### 5.1 `FilterActivity` — creazione filtri

- Nuovo controllo sotto i campi Mittente/Parola chiave: riga di `FilterChip`/`RadioButton` con opzioni:
  - **Tutte le SIM** (default → `simSlot = null`)
  - **SIM 1** (`simSlot = 0`)
  - **SIM 2** (`simSlot = 1`)
- Le opzioni SIM 1/SIM 2 sono statiche (i dispositivi con 3+ SIM fisiche sono rarissimi; eventuale estensione futura).
- Nella lista dei filtri attivi, mostrare l'indicazione dello slot quando presente: `"SIM: 2"`.
- Il controllo duplicati (`isDuplicate`) include anche `simSlot` nel confronto.
- Il pulsante "Aggiungi" resta abilitato con sender/keyword valorizzati; un filtro con **solo** lo slot (sender e keyword vuoti) è ammesso e va abilitato anche in quel caso (`newKeyword.isNotBlank() || newSender.isNotBlank() || simSlot != null`).

### 5.2 `MainActivity` — cronologia

- In `SmsLogItem`, accanto al mittente, mostrare un badge `SIM n` se `entry.simSlot != null`.

### 5.3 Stringhe

Nuove risorse in `res/values/strings.xml` (IT) e `res/values-en/strings.xml` (EN):

| Chiave | IT | EN |
|---|---|---|
| `sim_slot_label` | SIM | SIM |
| `sim_slot_any` | Tutte le SIM | All SIMs |
| `sim_slot_n` | SIM %1$d | SIM %1$d |
| `email_subject_with_sim` | Nuovo SMS da %1$s [SIM %2$d] | (solo IT: l'oggetto email non è localizzato oggi — mantenere coerenza con l'attuale stringa hardcoded) |

> Nota: l'oggetto email attuale è hardcoded in italiano in `SmsForwarder`; mantenere lo stesso stile (stringa hardcoded) o migrare a risorsa, ma in modo coerente in un unico punto.

## 6. Ordine di lavoro

1. **Room**: campi `simSlot` in `Filter` e `SmsLogEntry`, `MIGRATION_7_8`, version 8, build per rigenerare `8.json`.
2. **`SmsFilterProcessor`**: nuovo parametro `simSlot` + regola `slotMatch`.
3. **`SmsReceiver`**: estrazione slot dagli extra dell'intent.
4. **`SmsForwarder`**: propagazione slot, oggetto email, log.
5. **`SmsBackgroundService`**: extra `simSlot` opzionale.
6. **UI filtri** (`FilterActivity`): selettore slot, duplicati, lista.
7. **UI cronologia** (`MainActivity`): badge SIM.
8. **Stringhe** IT + EN.
9. **README**: sezione filtri e changelog.

## 7. Test e verifica

- **Unit test** su `SmsFilterProcessor` (nuova classe di test):
  - filtro senza slot matcha slot 0, 1 e -1;
  - filtro con slot 0 matcha solo slot 0 (non 1, non -1);
  - EXCLUDE con slot prevale su INCLUDE generico;
  - nessun filtro → inoltra sempre, qualunque slot.
- **Migrazione**: test strumentato `MigrationTestHelper` 7→8 (o verifica manuale su device con dati esistenti: filtri e log preesistenti devono avere `simSlot = NULL` e comportarsi come prima).
- **Manuale su device dual-SIM**: SMS su SIM 1 e SIM 2 → verifica oggetto email, badge in cronologia e filtri per slot.
- Build: `gradlew.bat app:assembleDebug` + `gradlew.bat app:testDebugUnitTest`.

## 8. Rischi e note

- **Affidabilità degli extra**: `EXTRA_SLOT_INDEX` è popolato dal framework su API 22+; su ROM molto vecchie o custom può mancare → fallback `-1` (sconosciuto), l'app degrada senza errori al comportamento attuale.
- **Retrocompatibilità filtri**: i filtri esistenti migrano con `simSlot = NULL` → continuano a valere per tutti gli slot (nessun cambio di comportamento).
- **eSIM**: le eSIM hanno comunque uno slot index (fisico o logico); nessuna gestione speciale richiesta.
- **Nessun nuovo permesso**: l'indice di slot dagli extra dell'intent non richiede `READ_PHONE_STATE`.
