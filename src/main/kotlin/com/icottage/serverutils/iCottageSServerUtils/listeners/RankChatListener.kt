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
        val prefixComponent = LegacyComponentSerializer.legacyAmpersand()
            .deserialize(rank.getFormattedPrefix())
        
        // Get the original message
        val originalMessage = event.message()
        
        // Format: [Rank] Username: Message
        val formattedMessage = Component.text()
            .append(prefixComponent)
            .append(Component.text(" "))
            .append(Component.text(player.name))
            .append(Component.text(": "))
            .append(originalMessage)
            .build()
        
        // Set the formatted message
        event.renderer { _, _, message, _ -> formattedMessage }
    }
}
