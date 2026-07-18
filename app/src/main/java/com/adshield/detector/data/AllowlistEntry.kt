package com.adshield.detector.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Apps the user has explicitly marked as "trusted" — AdShield will not flag or
 * act against overlays from these packages even if heuristics fire.
 */
@Entity(tableName = "allowlist")
data class AllowlistEntry(
    @PrimaryKey val packageName: String,
    val addedAtMillis: Long
)
