package com.icottage.serverutils.iCottageSServerUtils.teleport

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Manages teleport requests between players
 */
class TeleportManager(private val plugin: JavaPlugin) {
    
    // Maps player UUIDs to a map of target UUIDs and their teleport requests
    private val teleportRequests = ConcurrentHashMap<UUID, MutableMap<UUID, TeleportRequest>>()
    
    // Maps player UUIDs to their teleport cooldowns
    private val teleportCooldowns = ConcurrentHashMap<UUID, Long>()
    
    // Maps player UUIDs to their pending teleports
    private val pendingTeleports = ConcurrentHashMap<UUID, PendingTeleport>()
    
    // Maps target UUIDs to sender UUIDs for pending teleports
    private val pendingTeleportTargets = ConcurrentHashMap<UUID, UUID>()
    
    // Default cooldown in seconds
    private val defaultCooldown = 60L
    
    // Default expiration time in seconds
    private val defaultExpirationTime = 60L
    
    // Teleport delay in seconds
    private val teleportDelay = 5L
    
    /**
     * Initialize the teleport manager
     */
    fun initialize() {
        plugin.logger.info("Teleport manager initialized")
    }
    
    /**
     * Send a teleport request from one player to another
     */
    fun sendTeleportRequest(sender: Player, targetName: String): Boolean {
        // Find the target player
        val target = Bukkit.getPlayer(targetName)
        
        if (target == null) {
            sender.sendMessage(Component.text("Player $targetName is not online").color(NamedTextColor.RED))
            return false
        }
        
        if (target.uniqueId == sender.uniqueId) {
            sender.sendMessage(Component.text("You cannot teleport to yourself").color(NamedTextColor.RED))
            return false
        }
        
        // Check if the sender is on cooldown
        val currentTime = System.currentTimeMillis()
        val cooldownTime = teleportCooldowns[sender.uniqueId]
        
        if (cooldownTime != null && currentTime < cooldownTime) {
            val remainingSeconds = TimeUnit.MILLISECONDS.toSeconds(cooldownTime - currentTime)
            sender.sendMessage(Component.text("You must wait $remainingSeconds seconds before sending another teleport request").color(NamedTextColor.RED))
            return false
        }
        
        // Check if there's already a pending request to this player
        val senderRequests = teleportRequests.computeIfAbsent(sender.uniqueId) { ConcurrentHashMap() }
        if (senderRequests.containsKey(target.uniqueId)) {
            sender.sendMessage(Component.text("You already have a pending teleport request to ${target.name}").color(NamedTextColor.RED))
            return false
        }
        
        // Create a new teleport request
        val expirationTime = currentTime + TimeUnit.SECONDS.toMillis(defaultExpirationTime)
        val request = TeleportRequest(sender.uniqueId, target.uniqueId, expirationTime)
        
        // Schedule a task to expire the request
        val task = Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            cancelRequest(sender.uniqueId, target.uniqueId)
            sender.sendMessage(Component.text("Your teleport request to ${target.name} has expired").color(NamedTextColor.RED))
        }, defaultExpirationTime * 20L) // Convert to ticks (20 ticks = 1 second)
        
        request.task = task
        
        // Store the request
        senderRequests[target.uniqueId] = request
        
        // Set cooldown
        teleportCooldowns[sender.uniqueId] = currentTime + TimeUnit.SECONDS.toMillis(defaultCooldown)
        
        // Send messages
        sender.sendMessage(Component.text("Teleport request sent to ${target.name}").color(NamedTextColor.GREEN))
        
        // Send interactive message to target
        target.sendMessage(
            Component.text("${sender.name} has requested to teleport to you. ").color(NamedTextColor.GOLD)
                .append(
                    Component.text("[Accept]").color(NamedTextColor.GREEN)
                        .decorate(TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/tpaccept ${sender.name}"))
                )
                .append(Component.text(" ").color(NamedTextColor.WHITE))
                .append(
                    Component.text("[Deny]").color(NamedTextColor.RED)
                        .decorate(TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/tpdeny ${sender.name}"))
                )
        )
        
        return true
    }
    
    /**
     * Accept a teleport request
     */
    fun acceptTeleportRequest(target: Player, senderName: String): Boolean {
        val sender = Bukkit.getPlayer(senderName)
        
        if (sender == null) {
            target.sendMessage(Component.text("Player $senderName is not online").color(NamedTextColor.RED))
            return false
        }
        
        // Check if there's a pending request from this player
        val senderRequests = teleportRequests[sender.uniqueId] ?: return false
        val request = senderRequests[target.uniqueId]
        
        if (request == null) {
            target.sendMessage(Component.text("You don't have a pending teleport request from ${sender.name}").color(NamedTextColor.RED))
            return false
        }
        
        // Cancel the expiration task
        request.task?.cancel()
        
        // Remove the request
        senderRequests.remove(target.uniqueId)
        
        // Check if the player already has a pending teleport
        if (pendingTeleports.containsKey(sender.uniqueId)) {
            cancelPendingTeleport(sender.uniqueId, "You already have a pending teleport.")
        }
        
        // Check if the target player is already involved in a teleport
        if (pendingTeleportTargets.containsKey(target.uniqueId) || pendingTeleports.containsKey(target.uniqueId)) {
            sender.sendMessage(Component.text("${target.name} is already involved in another teleport").color(NamedTextColor.RED))
            return false
        }
        
        // Start the teleport delay
        startTeleportDelay(sender, target)
        
        // Send messages
        sender.sendMessage(
            Component.text("Teleporting to ${target.name} in $teleportDelay seconds. ").color(NamedTextColor.GREEN)
                .append(Component.text("Don't move or take damage! ").color(NamedTextColor.GOLD))
                .append(
                    Component.text("[Cancel]").color(NamedTextColor.RED)
                        .decorate(TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/tpcancel"))
                )
        )
        target.sendMessage(
            Component.text("${sender.name} will teleport to you in $teleportDelay seconds. ").color(NamedTextColor.GREEN)
                .append(Component.text("Don't move or take damage! ").color(NamedTextColor.GOLD))
        )
        
        return true
    }
    
    /**
     * Deny a teleport request
     */
    fun denyTeleportRequest(target: Player, senderName: String): Boolean {
        val sender = Bukkit.getPlayer(senderName)
        
        if (sender == null) {
            target.sendMessage(Component.text("Player $senderName is not online").color(NamedTextColor.RED))
            return false
        }
        
        // Check if there's a pending request from this player
        val senderRequests = teleportRequests[sender.uniqueId] ?: return false
        val request = senderRequests[target.uniqueId]
        
        if (request == null) {
            target.sendMessage(Component.text("You don't have a pending teleport request from ${sender.name}").color(NamedTextColor.RED))
            return false
        }
        
        // Cancel the expiration task
        request.task?.cancel()
        
        // Remove the request
        senderRequests.remove(target.uniqueId)
        
        // Send messages
        sender.sendMessage(Component.text("${target.name} denied your teleport request").color(NamedTextColor.RED))
        target.sendMessage(Component.text("You denied ${sender.name}'s teleport request").color(NamedTextColor.RED))
        
        return true
    }
    
    /**
     * Cancel a teleport request
     */
    private fun cancelRequest(senderUUID: UUID, targetUUID: UUID) {
        val senderRequests = teleportRequests[senderUUID] ?: return
        val request = senderRequests[targetUUID] ?: return
        
        // Cancel the task if it exists
        request.task?.cancel()
        
        // Remove the request
        senderRequests.remove(targetUUID)
    }
    
    /**
     * Cancel all teleport requests for a player
     */
    fun cancelAllRequests(playerUUID: UUID) {
        // Cancel all outgoing requests
        val outgoingRequests = teleportRequests[playerUUID]
        outgoingRequests?.forEach { (_, request) ->
            request.task?.cancel()
        }
        teleportRequests.remove(playerUUID)
        
        // Cancel all incoming requests
        teleportRequests.forEach { (senderUUID, requests) ->
            val request = requests[playerUUID]
            if (request != null) {
                request.task?.cancel()
                requests.remove(playerUUID)
            }
        }
    }
    
    /**
     * Start the teleport delay for a player
     */
    private fun startTeleportDelay(sender: Player, target: Player) {
        val senderUUID = sender.uniqueId
        val targetUUID = target.uniqueId
        val senderStartLocation = sender.location.clone()
        val targetStartLocation = target.location.clone()
        val destination = target.location.clone()
        
        // Create a countdown task that runs every second
        var secondsLeft = teleportDelay.toInt()
        
        val task = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            if (secondsLeft <= 0) {
                // Time's up, teleport the player
                completeTeleport(senderUUID, destination)
                return@Runnable
            }
            
            // Check if sender is still online
            val senderPlayer = Bukkit.getPlayer(senderUUID)
            if (senderPlayer == null) {
                cancelPendingTeleport(senderUUID, "Teleport cancelled: You disconnected!")
                return@Runnable
            }
            
            // Check if target is still online
            val targetPlayer = Bukkit.getPlayer(targetUUID)
            if (targetPlayer == null) {
                cancelPendingTeleport(senderUUID, "Teleport cancelled: Target player disconnected!")
                return@Runnable
            }
            
            // Check if sender has moved
            if (hasPlayerMoved(senderPlayer, senderStartLocation)) {
                cancelPendingTeleport(senderUUID, "Teleport cancelled: You moved!")
                return@Runnable
            }
            
            // Check if target has moved
            if (hasPlayerMoved(targetPlayer, targetStartLocation)) {
                cancelPendingTeleport(senderUUID, "Teleport cancelled: Target player moved!")
                targetPlayer.sendMessage(Component.text("Teleport cancelled: You moved!").color(NamedTextColor.RED))
                return@Runnable
            }
            
            // Update action bar with countdown for both players
            senderPlayer.sendActionBar(
                Component.text("Teleporting in $secondsLeft seconds...").color(NamedTextColor.GOLD)
            )
            targetPlayer.sendActionBar(
                Component.text("${senderPlayer.name} arriving in $secondsLeft seconds...").color(NamedTextColor.GOLD)
            )
            
            secondsLeft--
        }, 0L, 20L) // Run every second (20 ticks)
        
        // Store the pending teleport
        pendingTeleports[senderUUID] = PendingTeleport(
            senderUUID,
            targetUUID,
            destination,
            senderStartLocation,
            targetStartLocation,
            task,
            System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(teleportDelay)
        )
        
        // Store the target association
        pendingTeleportTargets[targetUUID] = senderUUID
    }
    
    /**
     * Check if a player has moved from their starting location
     */
    private fun hasPlayerMoved(player: Player, startLocation: Location): Boolean {
        val currentLocation = player.location
        
        // Check if the player has moved to a different world
        if (currentLocation.world != startLocation.world) {
            return true
        }
        
        // Check if the player has moved more than 0.5 blocks in any direction
        return currentLocation.distance(startLocation) > 0.5
    }
    
    /**
     * Complete a pending teleport
     */
    private fun completeTeleport(playerUUID: UUID, destination: Location) {
        val pendingTeleport = pendingTeleports.remove(playerUUID) ?: return
        pendingTeleport.task.cancel()
        
        // Clean up target association
        pendingTeleportTargets.remove(pendingTeleport.targetUUID)
        
        val player = Bukkit.getPlayer(playerUUID) ?: return
        player.teleport(destination)
        player.sendMessage(Component.text("Teleport complete!").color(NamedTextColor.GREEN))
        
        // Notify the target player
        val targetPlayer = Bukkit.getPlayer(pendingTeleport.targetUUID)
        targetPlayer?.sendMessage(Component.text("${player.name} has teleported to you.").color(NamedTextColor.GREEN))
    }
    
    /**
     * Cancel a pending teleport
     */
    fun cancelPendingTeleport(playerUUID: UUID, reason: String) {
        val pendingTeleport = pendingTeleports.remove(playerUUID) ?: return
        pendingTeleport.task.cancel()
        
        // Clean up target association
        pendingTeleportTargets.remove(pendingTeleport.targetUUID)
        
        val player = Bukkit.getPlayer(playerUUID) ?: return
        player.sendMessage(Component.text(reason).color(NamedTextColor.RED))
        
        // Notify the target player
        val targetPlayer = Bukkit.getPlayer(pendingTeleport.targetUUID)
        targetPlayer?.sendMessage(Component.text("Teleport from ${player.name} has been cancelled.").color(NamedTextColor.RED))
    }
    
    /**
     * Check if a player has a pending teleport
     */
    fun hasPendingTeleport(playerUUID: UUID): Boolean {
        return pendingTeleports.containsKey(playerUUID)
    }
    
    /**
     * Handle player damage event
     */
    fun handlePlayerDamage(playerUUID: UUID) {
        // Check if this player is the sender in a teleport
        if (pendingTeleports.containsKey(playerUUID)) {
            cancelPendingTeleport(playerUUID, "Teleport cancelled: You took damage!")
            return
        }
        
        // Check if this player is the target in a teleport
        val senderUUID = pendingTeleportTargets[playerUUID]
        if (senderUUID != null) {
            val senderPlayer = Bukkit.getPlayer(senderUUID) ?: return
            val targetPlayer = Bukkit.getPlayer(playerUUID) ?: return
            
            cancelPendingTeleport(senderUUID, "Teleport cancelled: Target player took damage!")
            targetPlayer.sendMessage(Component.text("Teleport cancelled: You took damage!").color(NamedTextColor.RED))
        }
    }
    
    /**
     * Data class to represent a teleport request
     */
    data class TeleportRequest(
        val senderUUID: UUID,
        val targetUUID: UUID,
        val expirationTime: Long,
        var task: BukkitTask? = null
    )
    
    /**
     * Data class to represent a pending teleport
     */
    data class PendingTeleport(
        val playerUUID: UUID,
        val targetUUID: UUID,
        val destination: Location,
        val senderStartLocation: Location,
        val targetStartLocation: Location,
        val task: BukkitTask,
        val expirationTime: Long
    )
    
    /**
     * Get a list of UUIDs of players who have sent teleport requests to the target player
     */
    fun getPendingRequestSenders(targetUUID: UUID): List<UUID> {
        val senders = mutableListOf<UUID>()
        
        teleportRequests.forEach { (senderUUID, requests) ->
            if (requests.containsKey(targetUUID)) {
                senders.add(senderUUID)
            }
        }
        
        return senders
    }
}
