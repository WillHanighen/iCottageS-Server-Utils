package com.icottage.serverutils.iCottageSServerUtils.messaging

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages private messaging between players
 */
class MessagingManager(private val plugin: JavaPlugin) {
    
    // Maps player UUIDs to the UUID of the last player they messaged
    private val lastMessagedPlayer = ConcurrentHashMap<UUID, UUID>()
    
    // Maps player UUIDs to a set of player UUIDs they are ignoring
    private val ignoredPlayers = ConcurrentHashMap<UUID, MutableSet<UUID>>()
    
    /**
     * Initialize the messaging manager
     */
    fun initialize() {
        plugin.logger.info("Messaging manager initialized")
    }
    
    /**
     * Send a private message from one player to another
     */
    fun sendPrivateMessage(sender: Player, targetName: String, message: String): Boolean {
        // Find the target player
        val target = Bukkit.getPlayer(targetName)
        
        if (target == null) {
            sender.sendMessage(Component.text("Player $targetName is not online").color(NamedTextColor.RED))
            return false
        }
        
        // Check if the target is ignoring the sender
        if (isIgnoring(target.uniqueId, sender.uniqueId)) {
            sender.sendMessage(Component.text("You cannot message this player").color(NamedTextColor.RED))
            return false
        }
        
        // Send the message
        sender.sendMessage(
            Component.text("To ").color(NamedTextColor.GRAY)
                .append(Component.text(target.name).color(NamedTextColor.GOLD))
                .append(Component.text(": ").color(NamedTextColor.GRAY))
                .append(Component.text(message).color(NamedTextColor.WHITE))
        )
        
        target.sendMessage(
            Component.text("From ").color(NamedTextColor.GRAY)
                .append(Component.text(sender.name).color(NamedTextColor.GOLD))
                .append(Component.text(": ").color(NamedTextColor.GRAY))
                .append(Component.text(message).color(NamedTextColor.WHITE))
        )
        
        // Update last messaged player
        lastMessagedPlayer[sender.uniqueId] = target.uniqueId
        lastMessagedPlayer[target.uniqueId] = sender.uniqueId
        
        return true
    }
    
    /**
     * Reply to the last player who messaged this player
     */
    fun replyToLastMessage(sender: Player, message: String): Boolean {
        val lastMessagedUUID = lastMessagedPlayer[sender.uniqueId]
        
        if (lastMessagedUUID == null) {
            sender.sendMessage(Component.text("You have not messaged anyone yet").color(NamedTextColor.RED))
            return false
        }
        
        val target = Bukkit.getPlayer(lastMessagedUUID)
        
        if (target == null) {
            sender.sendMessage(Component.text("The player you last messaged is no longer online").color(NamedTextColor.RED))
            return false
        }
        
        // Check if the target is ignoring the sender
        if (isIgnoring(target.uniqueId, sender.uniqueId)) {
            sender.sendMessage(Component.text("You cannot message this player").color(NamedTextColor.RED))
            return false
        }
        
        // Send the message
        sender.sendMessage(
            Component.text("To ").color(NamedTextColor.GRAY)
                .append(Component.text(target.name).color(NamedTextColor.GOLD))
                .append(Component.text(": ").color(NamedTextColor.GRAY))
                .append(Component.text(message).color(NamedTextColor.WHITE))
        )
        
        target.sendMessage(
            Component.text("From ").color(NamedTextColor.GRAY)
                .append(Component.text(sender.name).color(NamedTextColor.GOLD))
                .append(Component.text(": ").color(NamedTextColor.GRAY))
                .append(Component.text(message).color(NamedTextColor.WHITE))
        )
        
        // Update last messaged player
        lastMessagedPlayer[sender.uniqueId] = target.uniqueId
        lastMessagedPlayer[target.uniqueId] = sender.uniqueId
        
        return true
    }
    
    /**
     * Add a player to another player's ignore list
     */
    fun ignorePlayer(player: Player, targetName: String): Boolean {
        val target = Bukkit.getPlayer(targetName)
        
        if (target == null) {
            player.sendMessage(Component.text("Player $targetName is not online").color(NamedTextColor.RED))
            return false
        }
        
        if (target.uniqueId == player.uniqueId) {
            player.sendMessage(Component.text("You cannot ignore yourself").color(NamedTextColor.RED))
            return false
        }
        
        // Get or create the ignore set for this player
        val ignoreSet = ignoredPlayers.computeIfAbsent(player.uniqueId) { HashSet() }
        
        // Add the target to the ignore list
        if (ignoreSet.contains(target.uniqueId)) {
            player.sendMessage(Component.text("You are already ignoring ${target.name}").color(NamedTextColor.RED))
            return false
        }
        
        ignoreSet.add(target.uniqueId)
        player.sendMessage(Component.text("You are now ignoring ${target.name}").color(NamedTextColor.GREEN))
        return true
    }
    
    /**
     * Remove a player from another player's ignore list
     */
    fun unignorePlayer(player: Player, targetName: String): Boolean {
        // Get the ignore set for this player
        val ignoreSet = ignoredPlayers[player.uniqueId] ?: return false
        
        // Try to find the player by name
        val targetUUID = Bukkit.getOfflinePlayer(targetName).uniqueId
        
        // Remove the target from the ignore list
        if (!ignoreSet.contains(targetUUID)) {
            player.sendMessage(Component.text("You are not ignoring $targetName").color(NamedTextColor.RED))
            return false
        }
        
        ignoreSet.remove(targetUUID)
        player.sendMessage(Component.text("You are no longer ignoring $targetName").color(NamedTextColor.GREEN))
        return true
    }
    
    /**
     * Check if a player is ignoring another player
     */
    fun isIgnoring(playerUUID: UUID, targetUUID: UUID): Boolean {
        val ignoreSet = ignoredPlayers[playerUUID] ?: return false
        return ignoreSet.contains(targetUUID)
    }
    
    /**
     * Get a list of players that a player is ignoring
     */
    fun getIgnoredPlayers(playerUUID: UUID): List<UUID> {
        val ignoreSet = ignoredPlayers[playerUUID] ?: return emptyList()
        return ignoreSet.toList()
    }
}
