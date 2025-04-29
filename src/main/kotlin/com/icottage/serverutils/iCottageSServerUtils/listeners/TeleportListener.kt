package com.icottage.serverutils.iCottageSServerUtils.listeners

import com.icottage.serverutils.iCottageSServerUtils.teleport.TeleportManager
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.plugin.java.JavaPlugin

/**
 * Listener for teleport-related events
 */
class TeleportListener(private val plugin: JavaPlugin, private val teleportManager: TeleportManager) : Listener {
    
    /**
     * Handle player damage events to cancel teleports
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerDamage(event: EntityDamageEvent) {
        if (event.entity !is Player) {
            return
        }
        
        val player = event.entity as Player
        teleportManager.handlePlayerDamage(player.uniqueId)
    }
}
