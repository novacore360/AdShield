package com.adshield.detector.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.adshield.detector.util.SecureKeyManager
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [AdEvent::class, AllowlistEntry::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun adEventDao(): AdEventDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: build(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun build(context: Context): AppDatabase {
            // Loads native SQLCipher libs and derives the encryption key from
            // Android Keystore-backed storage (see SecureKeyManager). The
            // resulting .db file on disk is fully encrypted at rest.
            SQLiteDatabase.loadLibs(context)
            val passphrase = SecureKeyManager.getOrCreateDbPassphraseBytes(context)
            val factory = SupportFactory(passphrase)

            return Room.databaseBuilder(context, AppDatabase::class.java, "adshield_encrypted.db")
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
