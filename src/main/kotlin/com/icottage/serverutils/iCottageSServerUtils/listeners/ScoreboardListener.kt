package com.icottage.serverutils.iCottageSServerUtils.listeners

import com.icottage.serverutils.iCottageSServerUtils.scoreboard.ScoreboardManager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

/**
 * Listener for scoreboard-related events
 */
class ScoreboardListener(private val scoreboardManager: ScoreboardManager) : Listener {
    
    /**
     * Set up a scoreboard for players when they join
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        // Set up the player's scoreboard
        scoreboardManager.setupScoreboard(event.player)
    }
    
    /**
     * Clean up scoreboard when players leave
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        // Remove the player's scoreboard
        scoreboardManager.removeScoreboard(event.player)
    }
}
