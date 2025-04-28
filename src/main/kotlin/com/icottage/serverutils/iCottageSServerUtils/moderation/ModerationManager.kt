package com.icottage.serverutils.iCottageSServerUtils.moderation

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.icottage.serverutils.iCottageSServerUtils.utils.TimeUtils
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages moderation actions like mutes, bans, warnings, and kicks
 */
class ModerationManager(private val plugin: JavaPlugin) : Listener {
    private val mutes = ConcurrentHashMap<UUID, MuteEntry>()
    private val bans = ConcurrentHashMap<UUID, BanEntry>()
    private val warnings = ConcurrentHashMap<UUID, MutableList<WarnEntry>>()
    private val kicks = ConcurrentHashMap<UUID, MutableList<KickEntry>>()
    
    private val mutesFile = File(plugin.dataFolder, "mutes.json")
    private val bansFile = File(plugin.dataFolder, "bans.json")
    private val warningsFile = File(plugin.dataFolder, "warnings.json")
    private val kicksFile = File(plugin.dataFolder, "kicks.json")
    
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    
    /**
     * Initialize the moderation manager
     */
    fun initialize() {
        plugin.dataFolder.mkdirs()
        
        // Register events
        plugin.server.pluginManager.registerEvents(this, plugin)
        
        // Load data
        loadMutes()
        loadBans()
        loadWarnings()
        loadKicks()
        
        // Schedule cleanup task
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, Runnable {
            cleanupExpiredEntries()
        }, 20 * 60, 20 * 60) // Run every minute
        
        plugin.logger.info("Moderation manager initialized")
    }
    
    /**
     * Mute a player
     */
    fun mutePlayer(target: OfflinePlayer, reason: String, duration: Long, issuer: CommandSender): Boolean {
        val targetUUID = target.uniqueId
        val targetName = target.name ?: "Unknown"
        val issuerUUID = if (issuer is Player) issuer.uniqueId else UUID(0, 0)
        val issuerName = if (issuer is Player) issuer.name else "Console"
        
        val expiration = TimeUtils.getTimestamp(duration)
        val muteEntry = MuteEntry(targetUUID, targetName, reason, expiration, issuerUUID, issuerName)
        
        mutes[targetUUID] = muteEntry
        saveMutes()
        
        // Notify the target if they're online
        val targetPlayer = Bukkit.getPlayer(targetUUID)
        if (targetPlayer != null && targetPlayer.isOnline) {
            val formattedTime = if (duration == Long.MAX_VALUE) "permanently" else "for ${TimeUtils.formatTime(duration)}"
            targetPlayer.sendMessage(Component.text("You have been muted $formattedTime").color(NamedTextColor.RED))
            targetPlayer.sendMessage(Component.text("Reason: $reason").color(NamedTextColor.RED))
        }
        
        return true
    }
    
    /**
     * Unmute a player
     */
    fun unmutePlayer(target: OfflinePlayer, issuer: CommandSender): Boolean {
        val targetUUID = target.uniqueId
        
        if (!mutes.containsKey(targetUUID)) {
            return false
        }
        
        mutes.remove(targetUUID)
        saveMutes()
        
        // Notify the target if they're online
        val targetPlayer = Bukkit.getPlayer(targetUUID)
        if (targetPlayer != null && targetPlayer.isOnline) {
            targetPlayer.sendMessage(Component.text("You have been unmuted").color(NamedTextColor.GREEN))
        }
        
        return true
    }
    
    /**
     * Check if a player is muted
     */
    fun isMuted(player: Player): Boolean {
        val muteEntry = mutes[player.uniqueId] ?: return false
        
        // Check if the mute has expired
        if (TimeUtils.hasExpired(muteEntry.expiration)) {
            mutes.remove(player.uniqueId)
            saveMutes()
            return false
        }
        
        return true
    }
    
    /**
     * Get a player's mute entry
     */
    fun getMuteEntry(player: OfflinePlayer): MuteEntry? {
        return mutes[player.uniqueId]
    }
    
    /**
     * Ban a player
     */
    fun banPlayer(target: OfflinePlayer, reason: String, duration: Long, issuer: CommandSender): Boolean {
        val targetUUID = target.uniqueId
        val targetName = target.name ?: "Unknown"
        val issuerUUID = if (issuer is Player) issuer.uniqueId else UUID(0, 0)
        val issuerName = if (issuer is Player) issuer.name else "Console"
        
        val expiration = TimeUtils.getTimestamp(duration)
        val banEntry = BanEntry(targetUUID, targetName, reason, expiration, issuerUUID, issuerName)
        
        bans[targetUUID] = banEntry
        saveBans()
        
        // Kick the player if they're online
        val targetPlayer = Bukkit.getPlayer(targetUUID)
        if (targetPlayer != null && targetPlayer.isOnline) {
            val formattedTime = if (duration == Long.MAX_VALUE) "permanently" else "for ${TimeUtils.formatTime(duration)}"
            targetPlayer.kick(Component.text("You have been banned $formattedTime\nReason: $reason").color(NamedTextColor.RED))
        }
        
        return true
    }
    
    /**
     * Unban a player
     */
    fun unbanPlayer(target: OfflinePlayer, issuer: CommandSender): Boolean {
        val targetUUID = target.uniqueId
        
        if (!bans.containsKey(targetUUID)) {
            return false
        }
        
        bans.remove(targetUUID)
        saveBans()
        
        return true
    }
    
    /**
     * Check if a player is banned
     */
    fun isBanned(player: OfflinePlayer): Boolean {
        val banEntry = bans[player.uniqueId] ?: return false
        
        // Check if the ban has expired
        if (TimeUtils.hasExpired(banEntry.expiration)) {
            bans.remove(player.uniqueId)
            saveBans()
            return false
        }
        
        return true
    }
    
    /**
     * Get a player's ban entry
     */
    fun getBanEntry(player: OfflinePlayer): BanEntry? {
        return bans[player.uniqueId]
    }
    
    /**
     * Warn a player
     */
    fun warnPlayer(target: OfflinePlayer, reason: String, issuer: CommandSender): Boolean {
        val targetUUID = target.uniqueId
        val targetName = target.name ?: "Unknown"
        val issuerUUID = if (issuer is Player) issuer.uniqueId else UUID(0, 0)
        val issuerName = if (issuer is Player) issuer.name else "Console"
        
        val warnEntry = WarnEntry(targetUUID, targetName, reason, issuerUUID, issuerName)
        
        val playerWarnings = warnings.computeIfAbsent(targetUUID) { mutableListOf() }
        playerWarnings.add(warnEntry)
        saveWarnings()
        
        // Notify the target if they're online
        val targetPlayer = Bukkit.getPlayer(targetUUID)
        if (targetPlayer != null && targetPlayer.isOnline) {
            targetPlayer.sendMessage(Component.text("You have been warned").color(NamedTextColor.YELLOW))
            targetPlayer.sendMessage(Component.text("Reason: $reason").color(NamedTextColor.YELLOW))
        }
        
        // Check for auto-punishments based on warning count
        val config = plugin.config
        val enableAutoPunishments = config.getBoolean("moderation.warnings.auto-punish.enabled", false)
        
        if (enableAutoPunishments) {
            val warningCount = playerWarnings.size
            
            // Check for mute threshold
            val muteThreshold = config.getInt("moderation.warnings.auto-punish.mute-threshold", 0)
            if (muteThreshold > 0 && warningCount == muteThreshold) {
                val muteDuration = TimeUtils.parseTimeString(config.getString("moderation.warnings.auto-punish.mute-duration", "1h") ?: "1h")
                val muteReason = "Automatic mute after $warningCount warnings"
                mutePlayer(target, muteReason, muteDuration, Bukkit.getConsoleSender())
                
                Bukkit.broadcast(
                    Component.text("$targetName has been automatically muted after receiving $warningCount warnings").color(NamedTextColor.YELLOW),
                    "serverutils.moderation.notify"
                )
            }
            
            // Check for ban threshold
            val banThreshold = config.getInt("moderation.warnings.auto-punish.ban-threshold", 0)
            if (banThreshold > 0 && warningCount == banThreshold) {
                val banDuration = TimeUtils.parseTimeString(config.getString("moderation.warnings.auto-punish.ban-duration", "1d") ?: "1d")
                val banReason = "Automatic ban after $warningCount warnings"
                banPlayer(target, banReason, banDuration, Bukkit.getConsoleSender())
                
                Bukkit.broadcast(
                    Component.text("$targetName has been automatically banned after receiving $warningCount warnings").color(NamedTextColor.YELLOW),
                    "serverutils.moderation.notify"
                )
            }
        }
        
        return true
    }
    
    /**
     * Get a player's warnings
     */
    fun getWarnings(player: OfflinePlayer): List<WarnEntry> {
        return warnings[player.uniqueId] ?: emptyList()
    }
    
    /**
     * Clear a player's warnings
     */
    fun clearWarnings(target: OfflinePlayer, issuer: CommandSender): Boolean {
        val targetUUID = target.uniqueId
        
        if (!warnings.containsKey(targetUUID)) {
            return false
        }
        
        warnings.remove(targetUUID)
        saveWarnings()
        
        return true
    }
    
    /**
     * Kick a player
     */
    fun kickPlayer(target: Player, reason: String, issuer: CommandSender): Boolean {
        val targetUUID = target.uniqueId
        val targetName = target.name
        val issuerUUID = if (issuer is Player) issuer.uniqueId else UUID(0, 0)
        val issuerName = if (issuer is Player) issuer.name else "Console"
        
        val kickEntry = KickEntry(targetUUID, targetName, reason, issuerUUID, issuerName)
        
        val playerKicks = kicks.computeIfAbsent(targetUUID) { mutableListOf() }
        playerKicks.add(kickEntry)
        saveKicks()
        
        // Kick the player
        target.kick(Component.text("You have been kicked\nReason: $reason").color(NamedTextColor.RED))
        
        return true
    }
    
    /**
     * Get a player's kick history
     */
    fun getKickHistory(player: OfflinePlayer): List<KickEntry> {
        return kicks[player.uniqueId] ?: emptyList()
    }
    
    /**
     * Handle player chat event to check for mutes
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerChat(event: AsyncPlayerChatEvent) {
        val player = event.player
        
        if (isMuted(player)) {
            val muteEntry = mutes[player.uniqueId]!!
            val remainingTime = TimeUtils.getRemainingTime(muteEntry.expiration)
            val formattedTime = if (remainingTime == Long.MAX_VALUE) "permanently" else "for ${TimeUtils.formatTime(remainingTime)}"
            
            player.sendMessage(Component.text("You are muted $formattedTime").color(NamedTextColor.RED))
            player.sendMessage(Component.text("Reason: ${muteEntry.reason}").color(NamedTextColor.RED))
            
            event.isCancelled = true
        }
    }
    
    /**
     * Handle player login event to check for bans
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerLogin(event: PlayerLoginEvent) {
        val player = event.player
        
        if (isBanned(player)) {
            val banEntry = bans[player.uniqueId]!!
            val remainingTime = TimeUtils.getRemainingTime(banEntry.expiration)
            val formattedTime = if (remainingTime == Long.MAX_VALUE) "permanently" else "for ${TimeUtils.formatTime(remainingTime)}"
            
            event.disallow(
                PlayerLoginEvent.Result.KICK_BANNED,
                Component.text("You are banned $formattedTime\nReason: ${banEntry.reason}").color(NamedTextColor.RED)
            )
        }
    }
    
    /**
     * Clean up expired mutes and bans
     */
    private fun cleanupExpiredEntries() {
        val currentTime = System.currentTimeMillis()
        var muteCount = 0
        var banCount = 0
        
        // Clean up mutes
        val mutesToRemove = mutableListOf<UUID>()
        for ((uuid, muteEntry) in mutes) {
            if (muteEntry.expiration != Long.MAX_VALUE && muteEntry.expiration <= currentTime) {
                mutesToRemove.add(uuid)
                muteCount++
            }
        }
        
        for (uuid in mutesToRemove) {
            mutes.remove(uuid)
        }
        
        // Clean up bans
        val bansToRemove = mutableListOf<UUID>()
        for ((uuid, banEntry) in bans) {
            if (banEntry.expiration != Long.MAX_VALUE && banEntry.expiration <= currentTime) {
                bansToRemove.add(uuid)
                banCount++
            }
        }
        
        for (uuid in bansToRemove) {
            bans.remove(uuid)
        }
        
        // Save changes if needed
        if (muteCount > 0) {
            saveMutes()
            plugin.logger.info("Cleaned up $muteCount expired mutes")
        }
        
        if (banCount > 0) {
            saveBans()
            plugin.logger.info("Cleaned up $banCount expired bans")
        }
    }
    
    /**
     * Load mutes from file
     */
    private fun loadMutes() {
        if (!mutesFile.exists()) {
            return
        }
        
        try {
            FileReader(mutesFile).use { reader ->
                val type = object : TypeToken<Map<UUID, MuteEntry>>() {}.type
                val loadedMutes: Map<UUID, MuteEntry> = gson.fromJson(reader, type)
                mutes.clear()
                mutes.putAll(loadedMutes)
                plugin.logger.info("Loaded ${mutes.size} mutes")
            }
        } catch (e: Exception) {
            plugin.logger.severe("Failed to load mutes: ${e.message}")
        }
    }
    
    /**
     * Save mutes to file
     */
    private fun saveMutes() {
        try {
            FileWriter(mutesFile).use { writer ->
                gson.toJson(mutes, writer)
            }
        } catch (e: Exception) {
            plugin.logger.severe("Failed to save mutes: ${e.message}")
        }
    }
    
    /**
     * Load bans from file
     */
    private fun loadBans() {
        if (!bansFile.exists()) {
            return
        }
        
        try {
            FileReader(bansFile).use { reader ->
                val type = object : TypeToken<Map<UUID, BanEntry>>() {}.type
                val loadedBans: Map<UUID, BanEntry> = gson.fromJson(reader, type)
                bans.clear()
                bans.putAll(loadedBans)
                plugin.logger.info("Loaded ${bans.size} bans")
            }
        } catch (e: Exception) {
            plugin.logger.severe("Failed to load bans: ${e.message}")
        }
    }
    
    /**
     * Save bans to file
     */
    private fun saveBans() {
        try {
            FileWriter(bansFile).use { writer ->
                gson.toJson(bans, writer)
            }
        } catch (e: Exception) {
            plugin.logger.severe("Failed to save bans: ${e.message}")
        }
    }
    
    /**
     * Load warnings from file
     */
    private fun loadWarnings() {
        if (!warningsFile.exists()) {
            return
        }
        
        try {
            FileReader(warningsFile).use { reader ->
                val type = object : TypeToken<Map<UUID, List<WarnEntry>>>() {}.type
                val loadedWarnings: Map<UUID, List<WarnEntry>> = gson.fromJson(reader, type)
                warnings.clear()
                
                for ((uuid, warnList) in loadedWarnings) {
                    warnings[uuid] = warnList.toMutableList()
                }
                
                plugin.logger.info("Loaded warnings for ${warnings.size} players")
            }
        } catch (e: Exception) {
            plugin.logger.severe("Failed to load warnings: ${e.message}")
        }
    }
    
    /**
     * Save warnings to file
     */
    private fun saveWarnings() {
        try {
            FileWriter(warningsFile).use { writer ->
                gson.toJson(warnings, writer)
            }
        } catch (e: Exception) {
            plugin.logger.severe("Failed to save warnings: ${e.message}")
        }
    }
    
    /**
     * Load kicks from file
     */
    private fun loadKicks() {
        if (!kicksFile.exists()) {
            return
        }
        
        try {
            FileReader(kicksFile).use { reader ->
                val type = object : TypeToken<Map<UUID, List<KickEntry>>>() {}.type
                val loadedKicks: Map<UUID, List<KickEntry>> = gson.fromJson(reader, type)
                kicks.clear()
                
                for ((uuid, kickList) in loadedKicks) {
                    kicks[uuid] = kickList.toMutableList()
                }
                
                plugin.logger.info("Loaded kicks for ${kicks.size} players")
            }
        } catch (e: Exception) {
            plugin.logger.severe("Failed to load kicks: ${e.message}")
        }
    }
    
    /**
     * Save kicks to file
     */
    private fun saveKicks() {
        try {
            FileWriter(kicksFile).use { writer ->
                gson.toJson(kicks, writer)
            }
        } catch (e: Exception) {
            plugin.logger.severe("Failed to save kicks: ${e.message}")
        }
    }
}
