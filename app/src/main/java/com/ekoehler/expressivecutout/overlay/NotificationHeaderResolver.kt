package com.ekoehler.expressivecutout.overlay

import android.content.Context

/**
 * Modular helper responsible for resolving and formatting notification header metadata
 * (the application name and elapsed relative timestamp) displayed across the island cutout.
 */
object NotificationHeaderResolver {

    /**
     * Resolves the user-facing display label for an application from [Context.getPackageManager].
     * Returns null if [packageName] is null/blank or if the package cannot be found.
     */
    fun resolveAppName(context: Context, packageName: String?): String? {
        if (packageName.isNullOrBlank()) return null
        return runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        }.getOrNull()
    }

    /**
     * Resolves the notification arrival timestamp. If [postTime] is greater than 0, it is preserved;
     * otherwise, [fallbackMs] (defaulting to current wall-clock time) is returned.
     */
    fun resolvePostTimeMs(postTime: Long, fallbackMs: Long = System.currentTimeMillis()): Long {
        return if (postTime > 0L) postTime else fallbackMs
    }

    /**
     * Formats an elapsed timestamp into a human-readable relative duration string
     * (e.g. "Now", "5s ago", "2m ago", "2h ago", "1d ago").
     */
    fun formatRelativeTime(postTimeMs: Long, nowMs: Long = System.currentTimeMillis()): String {
        val elapsedSeconds = ((nowMs - postTimeMs) / 1000L).coerceAtLeast(0L)
        return when {
            elapsedSeconds < 5L -> "Now"
            elapsedSeconds < 60L -> "${elapsedSeconds}s ago"
            elapsedSeconds < 3600L -> "${elapsedSeconds / 60L}m ago"
            elapsedSeconds < 86400L -> "${elapsedSeconds / 3600L}h ago"
            else -> "${elapsedSeconds / 86400L}d ago"
        }
    }

    /**
     * Combines the resolved app name and relative timestamp according to visibility preferences.
     * When both are active, they are joined with " • ".
     */
    fun formatHeader(
        appName: String?,
        relativeTime: String?,
        showAppName: Boolean,
        showTimestamp: Boolean,
    ): String? {
        val showApp = showAppName && !appName.isNullOrBlank()
        val showTime = showTimestamp && !relativeTime.isNullOrBlank()
        return when {
            showApp && showTime -> "$appName • $relativeTime"
            showApp -> appName
            showTime -> relativeTime
            else -> null
        }
    }

    /**
     * Convenience function to resolve relative timestamp and format the notification header text.
     */
    fun resolveHeader(
        appName: String?,
        postTimeMs: Long?,
        showAppName: Boolean,
        showTimestamp: Boolean,
        nowMs: Long = System.currentTimeMillis(),
    ): String? {
        val relativeTime = postTimeMs?.let { formatRelativeTime(it, nowMs) }
        return formatHeader(
            appName = appName,
            relativeTime = relativeTime,
            showAppName = showAppName,
            showTimestamp = showTimestamp,
        )
    }

    /**
     * Resolves corner placement for app name and timestamp based on horizontal cutout position:
     * - offsetXDp == 0 (Center cutout): app name on the left top corner, timestamp on the right top corner.
     * - offsetXDp < 0 (Top-left cutout): combined header formatted on the top-right corner.
     * - offsetXDp > 0 (Top-right cutout): combined header formatted on the top-left corner.
     */
    fun resolveHeaderPlacement(
        appName: String?,
        relativeTime: String?,
        showAppName: Boolean,
        showTimestamp: Boolean,
        offsetXDp: Int,
    ): ResolvedHeaderPlacement {
        val showApp = showAppName && !appName.isNullOrBlank()
        val showTime = showTimestamp && !relativeTime.isNullOrBlank()
        if (!showApp && !showTime) {
            return ResolvedHeaderPlacement(leftText = null, rightText = null)
        }

        return when {
            offsetXDp == 0 -> ResolvedHeaderPlacement(
                leftText = if (showApp) appName else null,
                rightText = if (showTime) relativeTime else null,
            )
            offsetXDp < 0 -> ResolvedHeaderPlacement(
                leftText = null,
                rightText = formatHeader(appName, relativeTime, showAppName, showTimestamp),
            )
            else -> ResolvedHeaderPlacement(
                leftText = formatHeader(appName, relativeTime, showAppName, showTimestamp),
                rightText = null,
            )
        }
    }

    /**
     * Convenience function to resolve relative timestamp and corner placement.
     */
    fun resolveHeaderPlacement(
        appName: String?,
        postTimeMs: Long?,
        showAppName: Boolean,
        showTimestamp: Boolean,
        offsetXDp: Int,
        nowMs: Long = System.currentTimeMillis(),
    ): ResolvedHeaderPlacement {
        val relativeTime = postTimeMs?.let { formatRelativeTime(it, nowMs) }
        return resolveHeaderPlacement(
            appName = appName,
            relativeTime = relativeTime,
            showAppName = showAppName,
            showTimestamp = showTimestamp,
            offsetXDp = offsetXDp,
        )
    }
}

/**
 * Encapsulates the resolved left and right header strings for corner positioning.
 */
data class ResolvedHeaderPlacement(
    val leftText: String? = null,
    val rightText: String? = null,
)
