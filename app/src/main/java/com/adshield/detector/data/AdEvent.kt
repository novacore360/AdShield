package com.adshield.detector.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single suspected ad / unwanted-overlay occurrence, logged locally.
 * Nothing here is ever transmitted anywhere — there is no network code in this app.
 */
@Entity(tableName = "ad_events")
data class AdEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val timestampMillis: Long,
    val heuristicScore: Int,
    val windowType: String,
    val wasWhileAppInBackground: Boolean,
    val actionTaken: String // "logged_only", "dismissed_back", "dismissed_home", "none"
)
