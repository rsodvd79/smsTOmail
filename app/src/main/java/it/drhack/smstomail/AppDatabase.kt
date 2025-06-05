package it.drhack.smstomail

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// Converter per criptare le stringhe sensibili
import it.drhack.smstomail.EncryptedStringConverter
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Filter::class, EmailConfig::class, SmsLogEntry::class], version = 6)
@TypeConverters(FilterTypeConverter::class, DateConverter::class, EncryptedStringConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun filterDao(): FilterDao
    abstract fun emailConfigDao(): EmailConfigDao
    abstract fun smsLogDao(): SmsLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migrazione dalla versione 2 alla 3
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Creiamo una tabella temporanea con la nuova struttura
                database.execSQL(
                    "CREATE TABLE filters_new (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "sender TEXT NOT NULL DEFAULT '', " +
                    "keyword TEXT NOT NULL DEFAULT '', " +
                    "filterType INTEGER NOT NULL DEFAULT 0)"
                )

                // Copiamo i dati dalla vecchia tabella alla nuova
                // Il campo 'word' viene mappato a 'keyword' e aggiungiamo valori predefiniti per gli altri campi
                database.execSQL(
                    "INSERT INTO filters_new (id, keyword) " +
                    "SELECT id, word FROM filters"
                )

                // Eliminiamo la vecchia tabella
                database.execSQL("DROP TABLE filters")

                // Rinominiamo la nuova tabella con il nome originale
                database.execSQL("ALTER TABLE filters_new RENAME TO filters")
            }
        }

        // Migrazione dalla versione 3 alla 4
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Creazione della tabella per i log degli SMS
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS sms_log (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "timestamp INTEGER NOT NULL, " +
                    "sender TEXT NOT NULL, " +
                    "message TEXT NOT NULL, " +
                    "emailSent INTEGER NOT NULL, " +
                    "emailResult TEXT)"
                )
            }
        }

        // Migrazione dalla versione 4 alla 5
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Aggiungi il campo maxSmsToKeep alla tabella email_config
                database.execSQL(
                    "ALTER TABLE email_config ADD COLUMN maxSmsToKeep INTEGER NOT NULL DEFAULT 100"
                )
            }
        }

        // Migrazione dalla versione 5 alla 6
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Aggiungiamo i nuovi campi per la configurazione SMTP ed altre impostazioni
                database.execSQL(
                    "ALTER TABLE email_config ADD COLUMN smtpHost TEXT NOT NULL DEFAULT 'smtp.gmail.com'"
                )
                database.execSQL(
                    "ALTER TABLE email_config ADD COLUMN smtpPort TEXT NOT NULL DEFAULT '587'"
                )
                database.execSQL(
                    "ALTER TABLE email_config ADD COLUMN smtpUseTls INTEGER NOT NULL DEFAULT 1"
                )
                database.execSQL(
                    "ALTER TABLE email_config ADD COLUMN signature TEXT NOT NULL DEFAULT 'by SMS to Mail'"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "filters_db"
                )
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

