package com.icottage.serverutils.iCottageSServerUtils.listeners

import com.icottage.serverutils.iCottageSServerUtils.ranks.RankManager
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

/**
 * Listener for chat events to format messages with player ranks
 */
class RankChatListener(private val rankManager: RankManager) : Listener {
    
    /**
     * Formats chat messages to include player rank
     * Format: [Rank] Username: Message
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerChat(event: AsyncChatEvent) {
        val player = event.player
        val rank = rankManager.getPlayerRank(player)
        
        // Create the rank prefix component
        val prefixComponent = rank.getPrefixComponent()
        
        // Get the original message
        val originalMessage = event.message()
        
        // Get the player's name with the rank's color
        val nameColor = rank.getColor() ?: NamedTextColor.WHITE
        val playerName = Component.text(player.name, nameColor)
        
        // Format: [Rank] Username: Message
        val formattedMessage = Component.text()
            .append(prefixComponent)
            .append(Component.text(" "))
            .append(playerName)
            .append(Component.text(": ", NamedTextColor.GRAY))
            .append(originalMessage)
            .build()
        
        // Set the formatted message
        event.renderer { _, _, message, _ -> formattedMessage }
    }
}
