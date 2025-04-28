package com.icottage.serverutils.iCottageSServerUtils.listeners

import com.icottage.serverutils.iCottageSServerUtils.tablist.TablistManager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

/**
 * Listener for tablist-related events
 */
class TablistListener(private val tablistManager: TablistManager) : Listener {
    
    /**
     * Update a player's tablist when they join
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        // Update the player's tablist
        tablistManager.updateTabList(event.player)
    }
}
