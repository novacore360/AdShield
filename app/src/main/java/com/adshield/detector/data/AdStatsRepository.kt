package com.adshield.detector.data

import android.content.Context
import com.adshield.detector.AdShieldApplication
import kotlinx.coroutines.flow.Flow

class AdStatsRepository(private val dao: AdEventDao) {

    suspend fun recordEvent(
        packageName: String,
        heuristicScore: Int,
        windowType: String,
        wasWhileAppInBackground: Boolean,
        actionTaken: String
    ) {
        if (dao.isAllowlisted(packageName)) return
        dao.insert(
            AdEvent(
                packageName = packageName,
                timestampMillis = System.currentTimeMillis(),
                heuristicScore = heuristicScore,
                windowType = windowType,
                wasWhileAppInBackground = wasWhileAppInBackground,
                actionTaken = actionTaken
            )
        )
        // Keep local storage bounded — auto-prune anything older than 30 days.
        dao.pruneOlderThan(System.currentTimeMillis() - THIRTY_DAYS_MS)
    }

    fun recentEvents(limit: Int = 200): Flow<List<AdEvent>> = dao.recentEvents(limit)

    fun eventsForPackage(pkg: String): Flow<List<AdEvent>> = dao.eventsForPackage(pkg)

    fun rankedSuspects(windowMillis: Long = SEVEN_DAYS_MS): Flow<List<PackageCount>> =
        dao.rankedSuspects(System.currentTimeMillis() - windowMillis)

    fun eventCountToday(): Flow<Int> = dao.eventCountSince(startOfTodayMillis())

    fun eventCountLast7Days(): Flow<Int> = dao.eventCountSince(System.currentTimeMillis() - SEVEN_DAYS_MS)

    suspend fun clearAllData() = dao.clearAll()

    suspend fun addToAllowlist(pkg: String) =
        dao.addToAllowlist(AllowlistEntry(pkg, System.currentTimeMillis()))

    suspend fun removeFromAllowlist(pkg: String, addedAt: Long) =
        dao.removeFromAllowlist(AllowlistEntry(pkg, addedAt))

    fun allowlist(): Flow<List<AllowlistEntry>> = dao.allowlist()

    private fun startOfTodayMillis(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    companion object {
        private const val SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000
        private const val THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000

        @Volatile private var INSTANCE: AdStatsRepository? = null

        fun getInstance(context: Context): AdStatsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AdStatsRepository(
                    (context.applicationContext as AdShieldApplication).database.adEventDao()
                ).also { INSTANCE = it }
            }
        }
    }
}
