package com.icottage.serverutils.iCottageSServerUtils.listeners

import com.icottage.serverutils.iCottageSServerUtils.stats.PlayerStatsManager
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

/**
 * Listener for player statistics events
 */
class StatsListener(private val statsManager: PlayerStatsManager) : Listener {
    
    /**
     * Handle player death events to track kills and deaths
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val victim = event.entity
        
        // Record death for the victim
        statsManager.recordDeath(victim)
        
        // Record kill for the killer if it was a player
        val killer = victim.killer
        if (killer is Player) {
            statsManager.recordKill(killer)
        }
    }
    
    /**
     * Handle player join events to track join count and session time
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        statsManager.recordJoin(event.player)
    }
    
    /**
     * Handle player quit events to update playtime
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        statsManager.recordLeave(event.player)
    }
}
