package com.icottage.serverutils.iCottageSServerUtils.scoreboard

import com.icottage.serverutils.iCottageSServerUtils.ranks.RankManager
import com.icottage.serverutils.iCottageSServerUtils.stats.PlayerStatsManager
import com.icottage.serverutils.iCottageSServerUtils.utils.TimeUtils
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard
import java.text.DecimalFormat
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages player scoreboards
 */
class ScoreboardManager(
    private val plugin: JavaPlugin,
    private val rankManager: RankManager,
    private val statsManager: PlayerStatsManager
) {
    private val playerScoreboards = ConcurrentHashMap<UUID, Scoreboard>()
    private val kdrFormat = DecimalFormat("0.00")
    private val serverName = plugin.config.getString("scoreboard.server-name", "iCottage Server") ?: "iCottage Server"
    private val serverAddress = plugin.config.getString("scoreboard.server-address", "play.icottage.com") ?: "play.icottage.com"
    private val updateInterval = plugin.config.getLong("scoreboard.update-interval", 20L)
    private val showRank = plugin.config.getBoolean("scoreboard.show-rank", true)
    private val showKills = plugin.config.getBoolean("scoreboard.show-kills", true)
    private val showDeaths = plugin.config.getBoolean("scoreboard.show-deaths", true)
    private val showKDR = plugin.config.getBoolean("scoreboard.show-kdr", true)
    private val showPlaytime = plugin.config.getBoolean("scoreboard.show-playtime", true)
    private val showOnlinePlayers = plugin.config.getBoolean("scoreboard.show-online-players", true)
    private val showServerTPS = plugin.config.getBoolean("scoreboard.show-server-tps", true)
    private val showServerAddress = plugin.config.getBoolean("scoreboard.show-server-address", true)
    
    /**
     * Initialize the scoreboard manager
     */
    fun initialize() {
        // Schedule scoreboard update task
        plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            updateAllScoreboards()
        }, updateInterval, updateInterval)
        
        plugin.logger.info("Scoreboard manager initialized")
    }
    
    /**
     * Set up a player's scoreboard
     */
    fun setupScoreboard(player: Player) {
        val scoreboard = Bukkit.getScoreboardManager().newScoreboard
        val objective = scoreboard.registerNewObjective(
            "stats",
            Criteria.DUMMY,
            Component.text(serverName).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD)
        )
        
        objective.displaySlot = DisplaySlot.SIDEBAR
        playerScoreboards[player.uniqueId] = scoreboard
        player.scoreboard = scoreboard
        
        updateScoreboard(player)
    }
    
    /**
     * Remove a player's scoreboard
     */
    fun removeScoreboard(player: Player) {
        playerScoreboards.remove(player.uniqueId)
    }
    
    /**
     * Update all player scoreboards
     */
    private fun updateAllScoreboards() {
        for (player in Bukkit.getOnlinePlayers()) {
            updateScoreboard(player)
        }
    }
    
    /**
     * Update a player's scoreboard
     */
    private fun updateScoreboard(player: Player) {
        val scoreboard = playerScoreboards[player.uniqueId] ?: return
        val objective = scoreboard.getObjective("stats") ?: return
        
        // Clear existing scores
        for (entry in scoreboard.entries) {
            scoreboard.resetScores(entry)
        }
        
        // Get player stats
        val stats = statsManager.getPlayerStats(player)
        
        // Add lines to scoreboard
        var score = 15
        
        // Add empty line
        objective.getScore("§8⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯").score = score--
        
        // Add player name
        objective.getScore("§ePlayer: §f${player.name}").score = score--
        
        // Add rank if enabled
        if (showRank) {
            val rank = rankManager.getPlayerRankByUUID(player.uniqueId)
            // Convert the rank display name to a legacy string with proper color codes
            val rankDisplayComponent = rank.getDisplayNameComponent()
            val rankDisplay = LegacyComponentSerializer.legacySection().serialize(rankDisplayComponent)
            objective.getScore("§eRank: $rankDisplay").score = score--
        }
        
        // Add empty line
        objective.getScore("§8⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯ ").score = score--
        
        // Add stats if enabled
        if (showKills) {
            objective.getScore("§eKills: §f${stats.kills}").score = score--
        }
        
        if (showDeaths) {
            objective.getScore("§eDeaths: §f${stats.deaths}").score = score--
        }
        
        if (showKDR) {
            objective.getScore("§eKDR: §f${kdrFormat.format(stats.kdr)}").score = score--
        }
        
        if (showPlaytime) {
            // Calculate current session time
            val currentSession = (System.currentTimeMillis() - stats.lastJoinTime) / 1000
            val totalPlaytime = stats.playTimeSeconds + currentSession
            
            objective.getScore("§ePlaytime: §f${formatPlaytime(totalPlaytime)}").score = score--
        }
        
        // Add empty line
        objective.getScore("§8⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯  ").score = score--
        
        // Add server info if enabled
        if (showOnlinePlayers) {
            val onlineCount = Bukkit.getOnlinePlayers().size
            val maxPlayers = Bukkit.getMaxPlayers()
            objective.getScore("§eOnline: §f$onlineCount/$maxPlayers").score = score--
        }
        
        if (showServerTPS) {
            val tps = Bukkit.getTPS()[0]
            val tpsFormatted = String.format("%.1f", tps)
            val tpsColor = when {
                tps > 18.0 -> "§a" // Green
                tps > 15.0 -> "§e" // Yellow
                else -> "§c" // Red
            }
            objective.getScore("§eTPS: $tpsColor$tpsFormatted").score = score--
        }
        
        // Add empty line
        objective.getScore("§8⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯   ").score = score--
        
        // Add server address if enabled
        if (showServerAddress) {
            objective.getScore("§e$serverAddress").score = score--
        }
        
        // Add empty line
        objective.getScore("§8⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯   ").score = score--
        objective.getScore("§b$serverAddress").score = score
    }
    
    /**
     * Format playtime in a human-readable format
     */
    private fun formatPlaytime(seconds: Long): String {
        if (seconds < 60) {
            return "$seconds seconds"
        }
        
        val minutes = seconds / 60
        if (minutes < 60) {
            return "$minutes minutes"
        }
        
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        if (hours < 24) {
            return "$hours hours, $remainingMinutes minutes"
        }
        
        val days = hours / 24
        val remainingHours = hours % 24
        return "$days days, $remainingHours hours"
    }
    

}
