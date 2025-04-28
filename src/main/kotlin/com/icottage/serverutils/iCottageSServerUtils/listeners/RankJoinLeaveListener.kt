package com.icottage.serverutils.iCottageSServerUtils.listeners

import com.icottage.serverutils.iCottageSServerUtils.ranks.RankManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

/**
 * Listener for player join and leave events to format messages with player ranks
 */
class RankJoinLeaveListener(private val rankManager: RankManager) : Listener {
    
    /**
     * Formats join messages to include player rank
     * Format: [+] [Rank] Username
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val rank = rankManager.getPlayerRank(player)
        
        // Create the rank prefix component
        val prefixComponent = LegacyComponentSerializer.legacyAmpersand()
            .deserialize(rank.getFormattedPrefix())
        
        // Format: [+] [Rank] Username
        val formattedMessage = Component.text()
            .append(Component.text("[+] ").color(NamedTextColor.GREEN))
            .append(prefixComponent)
            .append(Component.text(" "))
            .append(Component.text(player.name))
            .build()
        
        // Set the join message
        event.joinMessage(formattedMessage)
    }
    
    /**
     * Formats leave messages to include player rank
     * Format: [-] [Rank] Username
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val rank = rankManager.getPlayerRank(player)
        
        // Create the rank prefix component
        val prefixComponent = LegacyComponentSerializer.legacyAmpersand()
            .deserialize(rank.getFormattedPrefix())
        
        // Format: [-] [Rank] Username
        val formattedMessage = Component.text()
            .append(Component.text("[-] ").color(NamedTextColor.RED))
            .append(prefixComponent)
            .append(Component.text(" "))
            .append(Component.text(player.name))
            .build()
        
        // Set the quit message
        event.quitMessage(formattedMessage)
    }
}
