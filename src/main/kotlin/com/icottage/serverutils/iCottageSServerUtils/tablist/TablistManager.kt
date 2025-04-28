package com.icottage.serverutils.iCottageSServerUtils.tablist

import com.icottage.serverutils.iCottageSServerUtils.ranks.RankManager
import com.icottage.serverutils.iCottageSServerUtils.stats.PlayerStatsManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit

import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.text.DecimalFormat
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages the custom tab list display
 */
class TablistManager(
    private val plugin: JavaPlugin,
    private val rankManager: RankManager,
    private val statsManager: PlayerStatsManager
) {
    private val kdrFormat = DecimalFormat("0.00")
    private val updateInterval = plugin.config.getLong("tablist.update-interval", 20L)
    private val headerEnabled = plugin.config.getBoolean("tablist.header.enabled", true)
    private val footerEnabled = plugin.config.getBoolean("tablist.footer.enabled", true)
    private val showPlayerStats = plugin.config.getBoolean("tablist.show-player-stats", true)
    private val serverName = plugin.config.getString("scoreboard.server-name", "Your Server") ?: "Your Server"
    private val serverAddress = plugin.config.getString("scoreboard.server-address", "play.yourserver.com") ?: "play.yourserver.com"
    
    /**
     * Initialize the tab list manager
     */
    fun initialize() {
        // Schedule tab list update task
        plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            updateAllTabLists()
        }, updateInterval, updateInterval)
        
        plugin.logger.info("Tab list manager initialized")
    }
    
    /**
     * Update all players' tab lists
     */
    private fun updateAllTabLists() {
        for (player in Bukkit.getOnlinePlayers()) {
            updateTabList(player)
        }
    }
    
    /**
     * Update a player's tab list
     */
    fun updateTabList(player: Player) {
        // Update player name format in tab list
        updatePlayerListName(player)
        
        // Update header and footer
        if (headerEnabled || footerEnabled) {
            val header = if (headerEnabled) createHeader() else Component.empty()
            val footer = if (footerEnabled) createFooter(player) else Component.empty()
            player.sendPlayerListHeaderAndFooter(header, footer)
        }
    }
    
    /**
     * Update a player's list name in the tab list
     */
    private fun updatePlayerListName(player: Player) {
        val rank = rankManager.getPlayerRankByUUID(player.uniqueId)
        
        // If player stats are enabled, show KDR in the tab list
        if (showPlayerStats) {
            val stats = statsManager.getPlayerStats(player)
            val kdr = kdrFormat.format(stats.kdr)
            
            player.playerListName(Component.text()
                .append(rank.getDisplayNameComponent())
                .append(Component.text(" "))
                .append(Component.text(player.name))
                .append(Component.text(" ["))
                .append(Component.text("K:${stats.kills} D:${stats.deaths} KDR:$kdr", NamedTextColor.GRAY))
                .append(Component.text("]"))
                .build())
        } else {
            player.playerListName(Component.text()
                .append(rank.getDisplayNameComponent())
                .append(Component.text(" "))
                .append(Component.text(player.name))
                .build())
        }
    }
    
    /**
     * Create the tab list header
     */
    private fun createHeader(): Component {
        val onlinePlayers = Bukkit.getOnlinePlayers().size
        val maxPlayers = Bukkit.getMaxPlayers()
        
        return Component.text()
            .append(Component.text("⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯", NamedTextColor.DARK_GRAY))
            .append(Component.newline())
            .append(Component.text(serverName, NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(Component.newline())
            .append(Component.text("Online: ", NamedTextColor.YELLOW)
                .append(Component.text("$onlinePlayers/$maxPlayers", NamedTextColor.WHITE)))
            .append(Component.newline())
            .append(Component.text("⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯", NamedTextColor.DARK_GRAY))
            .build()
    }
    
    /**
     * Create the tab list footer
     */
    private fun createFooter(player: Player): Component {
        val stats = statsManager.getPlayerStats(player)
        val tps = Bukkit.getTPS()[0]
        val tpsColor = when {
            tps >= 18.0 -> NamedTextColor.GREEN
            tps >= 15.0 -> NamedTextColor.YELLOW
            else -> NamedTextColor.RED
        }
        
        // Calculate current session time
        val currentSession = (System.currentTimeMillis() - stats.lastJoinTime) / 1000
        val totalPlaytime = stats.playTimeSeconds + currentSession
        
        return Component.text()
            .append(Component.text("⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯", NamedTextColor.DARK_GRAY))
            .append(Component.newline())
            .append(Component.text("Your Stats: ", NamedTextColor.YELLOW))
            .append(Component.newline())
            .append(Component.text("Kills: ", NamedTextColor.YELLOW)
                .append(Component.text("${stats.kills}", NamedTextColor.WHITE)))
            .append(Component.text(" | ", NamedTextColor.GRAY))
            .append(Component.text("Deaths: ", NamedTextColor.YELLOW)
                .append(Component.text("${stats.deaths}", NamedTextColor.WHITE)))
            .append(Component.text(" | ", NamedTextColor.GRAY))
            .append(Component.text("KDR: ", NamedTextColor.YELLOW)
                .append(Component.text("${kdrFormat.format(stats.kdr)}", NamedTextColor.WHITE)))
            .append(Component.newline())
            .append(Component.text("Playtime: ", NamedTextColor.YELLOW)
                .append(Component.text(formatPlaytime(totalPlaytime), NamedTextColor.WHITE)))
            .append(Component.newline())
            .append(Component.text("TPS: ", NamedTextColor.YELLOW)
                .append(Component.text(String.format("%.1f", tps.coerceAtMost(20.0)), tpsColor)))
            .append(Component.newline())
            .append(Component.text(serverAddress, NamedTextColor.AQUA))
            .append(Component.newline())
            .append(Component.text("⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯", NamedTextColor.DARK_GRAY))
            .build()
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
