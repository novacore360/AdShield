package com.adshield.detector.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import java.util.Base64

/**
 * Generates and stores the passphrase used to encrypt the local SQLCipher database.
 *
 * - The passphrase is 256-bit random, generated once on first launch.
 * - It is stored only inside EncryptedSharedPreferences, whose own key lives in the
 *   Android Keystore (hardware-backed on most devices) — never in plaintext, never
 *   hardcoded in source, never sent anywhere (this app has no INTERNET permission).
 */
object SecureKeyManager {

    private const val PREFS_NAME = "adshield_secure_prefs"
    private const val KEY_DB_PASSPHRASE = "db_passphrase_v1"

    /** Returns the raw 32-byte key used directly as the SQLCipher passphrase. */
    fun getOrCreateDbPassphraseBytes(context: Context): ByteArray {
        val prefs = encryptedPrefs(context)
        val existing = prefs.getString(KEY_DB_PASSPHRASE, null)
        if (existing != null) {
            return Base64.getDecoder().decode(existing)
        }

        val randomBytes = ByteArray(32)
        SecureRandom().nextBytes(randomBytes)
        val encoded = Base64.getEncoder().encodeToString(randomBytes)
        prefs.edit().putString(KEY_DB_PASSPHRASE, encoded).apply()
        return randomBytes
    }

    private fun encryptedPrefs(context: Context) : android.content.SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}
