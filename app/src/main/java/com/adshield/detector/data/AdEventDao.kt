package com.adshield.detector.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class PackageCount(val packageName: String, val count: Int, val lastSeen: Long, val maxScore: Int)

@Dao
interface AdEventDao {

    @Insert
    suspend fun insert(event: AdEvent)

    @Query("SELECT * FROM ad_events ORDER BY timestampMillis DESC LIMIT :limit")
    fun recentEvents(limit: Int = 200): Flow<List<AdEvent>>

    @Query("SELECT * FROM ad_events WHERE packageName = :pkg ORDER BY timestampMillis DESC")
    fun eventsForPackage(pkg: String): Flow<List<AdEvent>>

    @Query(
        """
        SELECT packageName, COUNT(*) as count, MAX(timestampMillis) as lastSeen, MAX(heuristicScore) as maxScore
        FROM ad_events
        WHERE timestampMillis >= :sinceMillis
        GROUP BY packageName
        ORDER BY count DESC
        """
    )
    fun rankedSuspects(sinceMillis: Long): Flow<List<PackageCount>>

    @Query("SELECT COUNT(*) FROM ad_events WHERE timestampMillis >= :sinceMillis")
    fun eventCountSince(sinceMillis: Long): Flow<Int>

    @Query("DELETE FROM ad_events WHERE timestampMillis < :beforeMillis")
    suspend fun pruneOlderThan(beforeMillis: Long)

    @Query("DELETE FROM ad_events")
    suspend fun clearAll()

    @Insert
    suspend fun addToAllowlist(entry: AllowlistEntry)

    @Delete
    suspend fun removeFromAllowlist(entry: AllowlistEntry)

    @Query("SELECT * FROM allowlist")
    fun allowlist(): Flow<List<AllowlistEntry>>

    @Query("SELECT EXISTS(SELECT 1 FROM allowlist WHERE packageName = :pkg)")
    suspend fun isAllowlisted(pkg: String): Boolean
}
