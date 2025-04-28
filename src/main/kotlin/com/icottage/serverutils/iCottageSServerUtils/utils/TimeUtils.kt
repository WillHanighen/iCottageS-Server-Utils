package com.icottage.serverutils.iCottageSServerUtils.utils

import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Utility class for handling time-related operations
 */
object TimeUtils {
    private val TIME_PATTERN = Pattern.compile("(\\d+)([smhdwy])")
    
    /**
     * Parse a time string like "1d2h30m" into milliseconds
     * 
     * @param timeString The time string to parse
     * @return The time in milliseconds, or -1 if the format is invalid
     */
    fun parseTimeString(timeString: String): Long {
        if (timeString.equals("permanent", ignoreCase = true) || 
            timeString.equals("perm", ignoreCase = true) || 
            timeString.equals("forever", ignoreCase = true)) {
            return Long.MAX_VALUE
        }
        
        var totalMillis = 0L
        val matcher = TIME_PATTERN.matcher(timeString.lowercase())
        var found = false
        
        while (matcher.find()) {
            found = true
            val amount = matcher.group(1).toLong()
            val unit = matcher.group(2)
            
            val millis = when (unit) {
                "y" -> TimeUnit.DAYS.toMillis(amount * 365)
                "w" -> TimeUnit.DAYS.toMillis(amount * 7)
                "d" -> TimeUnit.DAYS.toMillis(amount)
                "h" -> TimeUnit.HOURS.toMillis(amount)
                "m" -> TimeUnit.MINUTES.toMillis(amount)
                "s" -> TimeUnit.SECONDS.toMillis(amount)
                else -> 0
            }
            
            totalMillis += millis
        }
        
        return if (found) totalMillis else -1
    }
    
    /**
     * Format milliseconds into a human-readable string (e.g., "1d 2h 30m")
     * 
     * @param millis The time in milliseconds
     * @return A formatted time string
     */
    fun formatTime(millis: Long): String {
        if (millis == Long.MAX_VALUE) {
            return "permanent"
        }
        
        if (millis <= 0) {
            return "0s"
        }
        
        val days = TimeUnit.MILLISECONDS.toDays(millis)
        val hours = TimeUnit.MILLISECONDS.toHours(millis) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        
        val sb = StringBuilder()
        
        if (days > 0) {
            sb.append("${days}d ")
        }
        
        if (hours > 0) {
            sb.append("${hours}h ")
        }
        
        if (minutes > 0) {
            sb.append("${minutes}m ")
        }
        
        if (seconds > 0 || sb.isEmpty()) {
            sb.append("${seconds}s")
        }
        
        return sb.toString().trim()
    }
    
    /**
     * Get a timestamp for a duration from now
     * 
     * @param durationMillis The duration in milliseconds
     * @return A timestamp in milliseconds
     */
    fun getTimestamp(durationMillis: Long): Long {
        return if (durationMillis == Long.MAX_VALUE) {
            Long.MAX_VALUE
        } else {
            System.currentTimeMillis() + durationMillis
        }
    }
    
    /**
     * Check if a timestamp has expired
     * 
     * @param timestamp The timestamp to check
     * @return True if the timestamp has expired, false otherwise
     */
    fun hasExpired(timestamp: Long): Boolean {
        return timestamp != Long.MAX_VALUE && timestamp <= System.currentTimeMillis()
    }
    
    /**
     * Get the remaining time from a timestamp
     * 
     * @param timestamp The timestamp to check
     * @return The remaining time in milliseconds
     */
    fun getRemainingTime(timestamp: Long): Long {
        if (timestamp == Long.MAX_VALUE) {
            return Long.MAX_VALUE
        }
        
        val remaining = timestamp - System.currentTimeMillis()
        return if (remaining < 0) 0 else remaining
    }
}
