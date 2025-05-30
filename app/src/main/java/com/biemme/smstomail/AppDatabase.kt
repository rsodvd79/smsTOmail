package com.biemme.smstomail

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Filter::class, EmailConfig::class, SmsLogEntry::class], version = 4)
@TypeConverters(FilterTypeConverter::class, DateConverter::class)
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

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "filters_db"
                )
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

