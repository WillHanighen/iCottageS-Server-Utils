package com.icottage.serverutils.iCottageSServerUtils.afk

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChatEvent
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerAnimationEvent
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Manages AFK (Away From Keyboard) status for players
 */
class AfkManager(private val plugin: JavaPlugin) : Listener {
    
    // Maps player UUIDs to their last activity time
    private val lastActivityTime = ConcurrentHashMap<UUID, Long>()
    
    // Maps player UUIDs to their AFK status
    private val afkPlayers = ConcurrentHashMap<UUID, Boolean>()
    
    // Maps player UUIDs to their manual AFK status (set by command)
    private val manualAfkPlayers = ConcurrentHashMap<UUID, Boolean>()
    
    // Default time in minutes before a player is considered AFK
    private var afkTimeoutMinutes = 5L
    
    // Task ID for the AFK checker
    private var afkCheckerTaskId = -1
    
    /**
     * Initialize the AFK manager
     */
    fun initialize() {
        // Register events
        Bukkit.getPluginManager().registerEvents(this, plugin)
        
        // Start the AFK checker task
        startAfkChecker()
        
        // Initialize all online players
        Bukkit.getOnlinePlayers().forEach { player ->
            lastActivityTime[player.uniqueId] = System.currentTimeMillis()
            afkPlayers[player.uniqueId] = false
        }
        
        plugin.logger.info("AFK manager initialized with timeout of $afkTimeoutMinutes minutes")
    }
    
    /**
     * Start the AFK checker task
     */
    private fun startAfkChecker() {
        // Cancel any existing task
        if (afkCheckerTaskId != -1) {
            Bukkit.getScheduler().cancelTask(afkCheckerTaskId)
        }
        
        // Start a new task that runs every 30 seconds
        afkCheckerTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, {
            checkAfkPlayers()
        }, 20L * 30L, 20L * 30L) // Run every 30 seconds (20 ticks = 1 second)
        
        plugin.logger.info("AFK checker task started")
    }
    
    /**
     * Check all online players for AFK status
     */
    private fun checkAfkPlayers() {
        val currentTime = System.currentTimeMillis()
        val afkThreshold = currentTime - TimeUnit.MINUTES.toMillis(afkTimeoutMinutes)
        
        Bukkit.getOnlinePlayers().forEach { player ->
            val uuid = player.uniqueId
            val lastActivity = lastActivityTime[uuid] ?: currentTime
            
            // Skip players who are manually AFK
            if (manualAfkPlayers[uuid] == true) {
                return@forEach
            }
            
            // Check if player has been inactive for too long
            if (lastActivity < afkThreshold && !isAfk(uuid)) {
                setAfk(player, true)
            }
        }
    }
    
    /**
     * Set a player's AFK status
     */
    fun setAfk(player: Player, afk: Boolean, manual: Boolean = false) {
        val uuid = player.uniqueId
        
        // Update AFK status
        afkPlayers[uuid] = afk
        
        // If this is a manual change, update that too
        if (manual) {
            manualAfkPlayers[uuid] = afk
        }
        
        // If no longer AFK, update activity time
        if (!afk) {
            updateActivity(uuid)
            manualAfkPlayers.remove(uuid)
        }
        
        // Broadcast message
        val message = if (afk) {
            Component.text("${player.name} is now AFK").color(NamedTextColor.GRAY)
        } else {
            Component.text("${player.name} is no longer AFK").color(NamedTextColor.GRAY)
        }
        
        Bukkit.getOnlinePlayers().forEach { p ->
            p.sendMessage(message)
        }
        
        // Update player display name
        updatePlayerDisplayName(player)
    }
    
    /**
     * Update a player's display name to show AFK status
     */
    private fun updatePlayerDisplayName(player: Player) {
        val isAfk = isAfk(player.uniqueId)
        
        if (isAfk) {
            // Add AFK prefix to display name
            val afkPrefix = Component.text("[AFK] ").color(NamedTextColor.GRAY)
            player.displayName(afkPrefix.append(Component.text(player.name)))
        } else {
            // Reset display name
            player.displayName(Component.text(player.name))
        }
    }
    
    /**
     * Check if a player is AFK
     */
    fun isAfk(uuid: UUID): Boolean {
        return afkPlayers[uuid] ?: false
    }
    
    /**
     * Update a player's last activity time
     */
    fun updateActivity(uuid: UUID) {
        lastActivityTime[uuid] = System.currentTimeMillis()
        
        // If player was AFK and it wasn't manual, set them as no longer AFK
        val player = Bukkit.getPlayer(uuid)
        if (player != null && isAfk(uuid) && manualAfkPlayers[uuid] != true) {
            setAfk(player, false)
            plugin.logger.info("${player.name} is no longer AFK due to activity")
        }
    }
    
    /**
     * Get the AFK status text for a player
     */
    fun getAfkStatusText(uuid: UUID): Component? {
        if (!isAfk(uuid)) {
            return null
        }
        
        return Component.text(" [AFK]").color(NamedTextColor.GRAY)
    }
    
    /**
     * Set the AFK timeout in minutes
     */
    fun setAfkTimeout(minutes: Long) {
        afkTimeoutMinutes = minutes
        plugin.logger.info("AFK timeout set to $afkTimeoutMinutes minutes")
    }
    
    /**
     * Handle player join event
     */
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val uuid = event.player.uniqueId
        
        // Reset AFK status
        afkPlayers[uuid] = false
        manualAfkPlayers[uuid] = false
        
        // Update activity time
        updateActivity(uuid)
    }
    
    /**
     * Handle player quit event
     */
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val uuid = event.player.uniqueId
        
        // Clean up
        afkPlayers.remove(uuid)
        manualAfkPlayers.remove(uuid)
        lastActivityTime.remove(uuid)
    }
    
    /**
     * Handle player move event
     */
    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        // Only count significant movement (not just looking around)
        if (event.from.x != event.to.x || event.from.y != event.to.y || event.from.z != event.to.z) {
            val uuid = event.player.uniqueId
            lastActivityTime[uuid] = System.currentTimeMillis()
            
            // If player was AFK, set them as no longer AFK regardless of manual status
            if (isAfk(uuid)) {
                setAfk(event.player, false)
                plugin.logger.info("${event.player.name} is no longer AFK due to movement")
            }
        }
    }
    
    /**
     * Handle player interact event
     */
    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val uuid = event.player.uniqueId
        lastActivityTime[uuid] = System.currentTimeMillis()
        
        // If player was AFK, set them as no longer AFK regardless of manual status
        if (isAfk(uuid)) {
            setAfk(event.player, false)
            plugin.logger.info("${event.player.name} is no longer AFK due to interaction")
        }
    }
    
    /**
     * Handle player chat event
     */
    @EventHandler
    fun onPlayerChat(event: PlayerChatEvent) {
        val uuid = event.player.uniqueId
        lastActivityTime[uuid] = System.currentTimeMillis()
        
        // If player was AFK and it wasn't manual, set them as no longer AFK
        if (isAfk(uuid) && manualAfkPlayers[uuid] != true) {
            setAfk(event.player, false)
            plugin.logger.info("${event.player.name} is no longer AFK due to chat")
        }
    }
    
    /**
     * Handle player command event
     */
    @EventHandler
    fun onPlayerCommand(event: PlayerCommandPreprocessEvent) {
        // Don't count /afk command as activity
        if (!event.message.startsWith("/afk")) {
            val uuid = event.player.uniqueId
            lastActivityTime[uuid] = System.currentTimeMillis()
            
            // If player was AFK and it wasn't manual, set them as no longer AFK
            if (isAfk(uuid) && manualAfkPlayers[uuid] != true) {
                setAfk(event.player, false)
                plugin.logger.info("${event.player.name} is no longer AFK due to command")
            }
        }
    }
    
    /**
     * Handle player animation event (arm swing, attack)
     */
    @EventHandler
    fun onPlayerAnimation(event: PlayerAnimationEvent) {
        val uuid = event.player.uniqueId
        lastActivityTime[uuid] = System.currentTimeMillis()
        
        // If player was AFK, set them as no longer AFK regardless of manual status
        if (isAfk(uuid)) {
            setAfk(event.player, false)
            plugin.logger.info("${event.player.name} is no longer AFK due to animation/attack")
        }
    }
    
    /**
     * Handle player attacking entities
     */
    @EventHandler
    fun onEntityDamage(event: EntityDamageByEntityEvent) {
        if (event.damager !is Player) {
            return
        }
        
        val player = event.damager as Player
        val uuid = player.uniqueId
        lastActivityTime[uuid] = System.currentTimeMillis()
        
        // If player was AFK, set them as no longer AFK regardless of manual status
        if (isAfk(uuid)) {
            setAfk(player, false)
            plugin.logger.info("${player.name} is no longer AFK due to attacking entity")
        }
    }
    
    /**
     * Handle block breaking
     */
    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val uuid = event.player.uniqueId
        lastActivityTime[uuid] = System.currentTimeMillis()
        
        // If player was AFK, set them as no longer AFK regardless of manual status
        if (isAfk(uuid)) {
            setAfk(event.player, false)
            plugin.logger.info("${event.player.name} is no longer AFK due to breaking block")
        }
    }
    
    /**
     * Handle block placing
     */
    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        val uuid = event.player.uniqueId
        lastActivityTime[uuid] = System.currentTimeMillis()
        
        // If player was AFK, set them as no longer AFK regardless of manual status
        if (isAfk(uuid)) {
            setAfk(event.player, false)
            plugin.logger.info("${event.player.name} is no longer AFK due to placing block")
        }
    }
}
