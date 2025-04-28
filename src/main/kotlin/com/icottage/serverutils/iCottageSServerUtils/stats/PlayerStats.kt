package com.icottage.serverutils.iCottageSServerUtils.stats

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Represents a player's statistics
 */
data class PlayerStatsData(
    val uuid: UUID,
    val playerName: String,
    var kills: Int = 0,
    var deaths: Int = 0,
    var joinCount: Int = 0,
    var playTimeSeconds: Long = 0,
    var lastJoinTime: Long = 0
) {
    val kdr: Double
        get() = if (deaths == 0) kills.toDouble() else kills.toDouble() / deaths.toDouble()
}

/**
 * Manages player statistics
 */
class PlayerStatsManager(private val plugin: JavaPlugin) {
    private val stats = ConcurrentHashMap<UUID, PlayerStatsData>()
    private val statsFile = File(plugin.dataFolder, "player_stats.json")
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    
    /**
     * Initialize the stats manager
     */
    fun initialize() {
        plugin.dataFolder.mkdirs()
        loadStats()
        
        // Schedule auto-save task
        plugin.server.scheduler.runTaskTimerAsynchronously(plugin, Runnable {
            saveStats()
        }, 20 * 60 * 5, 20 * 60 * 5) // Save every 5 minutes
        
        plugin.logger.info("Player stats manager initialized")
    }
    
    /**
     * Get a player's stats, creating a new entry if needed
     */
    fun getPlayerStats(player: Player): PlayerStatsData {
        return stats.computeIfAbsent(player.uniqueId) {
            PlayerStatsData(player.uniqueId, player.name)
        }
    }
    
    /**
     * Record a player kill
     */
    fun recordKill(player: Player) {
        val playerStats = getPlayerStats(player)
        playerStats.kills++
        saveStats()
    }
    
    /**
     * Record a player death
     */
    fun recordDeath(player: Player) {
        val playerStats = getPlayerStats(player)
        playerStats.deaths++
        saveStats()
    }
    
    /**
     * Record a player join
     */
    fun recordJoin(player: Player) {
        val playerStats = getPlayerStats(player)
        playerStats.joinCount++
        playerStats.lastJoinTime = System.currentTimeMillis()
        saveStats()
    }
    
    /**
     * Record player leave and update playtime
     */
    fun recordLeave(player: Player) {
        val playerStats = getPlayerStats(player)
        val sessionTime = (System.currentTimeMillis() - playerStats.lastJoinTime) / 1000
        playerStats.playTimeSeconds += sessionTime
        saveStats()
    }
    
    /**
     * Get the top players by kills
     */
    fun getTopKillers(limit: Int): List<PlayerStatsData> {
        return stats.values.sortedByDescending { it.kills }.take(limit)
    }
    
    /**
     * Get the top players by KDR
     */
    fun getTopKDR(limit: Int): List<PlayerStatsData> {
        return stats.values.sortedByDescending { it.kdr }.take(limit)
    }
    
    /**
     * Get the top players by playtime
     */
    fun getTopPlaytime(limit: Int): List<PlayerStatsData> {
        return stats.values.sortedByDescending { it.playTimeSeconds }.take(limit)
    }
    
    /**
     * Load stats from file
     */
    private fun loadStats() {
        if (!statsFile.exists()) {
            return
        }
        
        try {
            FileReader(statsFile).use { reader ->
                val type = object : TypeToken<Map<UUID, PlayerStatsData>>() {}.type
                val loadedStats: Map<UUID, PlayerStatsData> = gson.fromJson(reader, type)
                stats.clear()
                stats.putAll(loadedStats)
                plugin.logger.info("Loaded stats for ${stats.size} players")
            }
        } catch (e: Exception) {
            plugin.logger.severe("Failed to load player stats: ${e.message}")
        }
    }
    
    /**
     * Save stats to file
     */
    fun saveStats() {
        try {
            FileWriter(statsFile).use { writer ->
                gson.toJson(stats, writer)
            }
        } catch (e: Exception) {
            plugin.logger.severe("Failed to save player stats: ${e.message}")
        }
    }
}
