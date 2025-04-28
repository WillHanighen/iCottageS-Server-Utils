package com.icottage.serverutils.iCottageSServerUtils.integration

import com.icottage.serverutils.iCottageSServerUtils.ranks.Rank
import com.icottage.serverutils.iCottageSServerUtils.ranks.RankManager
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.CompletableFuture

/**
 * Simple integration with LuckPerms for role permissions
 */
class LuckPermsIntegration(
    private val plugin: JavaPlugin,
    private val rankManager: RankManager
) {
    private var enabled = false
    private var syncPermissions = true
    private var syncWeight = true
    private var syncPlayers = true
    private var autoSyncOnChange = true
    
    /**
     * Initializes the LuckPerms integration
     */
    fun initialize() {
        try {
            if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
                plugin.logger.warning("LuckPerms not found, integration disabled")
                return
            }
            
            // Load configuration options
            val config = plugin.config
            syncPermissions = config.getBoolean("luckperms.sync.permissions", true)
            syncWeight = config.getBoolean("luckperms.sync.weight", true)
            syncPlayers = config.getBoolean("luckperms.sync.players", true)
            autoSyncOnChange = config.getBoolean("luckperms.auto-sync-on-change", true)
            
            enabled = true
            plugin.logger.info("LuckPerms integration enabled with settings:")
            plugin.logger.info(" - Sync permissions: $syncPermissions")
            plugin.logger.info(" - Sync weight: $syncWeight")
            plugin.logger.info(" - Sync players: $syncPlayers")
            plugin.logger.info(" - Auto-sync on change: $autoSyncOnChange")
            
            // Only set up listeners if auto-sync is enabled
            if (autoSyncOnChange) {
                // Listen for rank changes
                rankManager.setRankChangeListener { rank, _ ->
                    createGroupInLuckPerms(rank)
                }
                
                // Listen for player rank changes if player sync is enabled
                if (syncPlayers) {
                    rankManager.setPlayerRankChangeListener { player, rankName ->
                        setPlayerGroup(player, rankName)
                    }
                }
            }
            
        } catch (e: Exception) {
            plugin.logger.severe("Failed to initialize LuckPerms integration: ${e.message}")
            enabled = false
        }
    }
    
    /**
     * Creates or updates a group in LuckPerms
     */
    private fun createGroupInLuckPerms(rank: Rank) {
        if (!enabled) return
        
        try {
            // Always create the group
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp creategroup ${rank.name}")
            
            // Set weight if enabled
            if (syncWeight) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp group ${rank.name} setweight ${rank.weight}")
            }
            
            // Set permissions if enabled
            if (syncPermissions) {
                for (permission in rank.permissions) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp group ${rank.name} permission set $permission true")
                }
            }
            
            plugin.logger.info("Created/updated LuckPerms group: ${rank.name}")
        } catch (e: Exception) {
            plugin.logger.warning("Error creating group in LuckPerms: ${e.message}")
        }
    }
    
    /**
     * Sets a player's group in LuckPerms
     */
    private fun setPlayerGroup(player: Player, rankName: String) {
        if (!enabled || !syncPlayers) return
        
        try {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user ${player.name} parent set $rankName")
            plugin.logger.info("Set player ${player.name} to group $rankName in LuckPerms")
        } catch (e: Exception) {
            plugin.logger.warning("Error setting player's group in LuckPerms: ${e.message}")
        }
    }
    
    /**
     * Exports ranks to LuckPerms
     */
    fun exportRanksToLuckPerms(): CompletableFuture<Void> {
        val future = CompletableFuture<Void>()
        
        if (!enabled) {
            return CompletableFuture.completedFuture(null)
        }
        
        try {
            for (rank in rankManager.getAllRanks()) {
                createGroupInLuckPerms(rank)
            }
            future.complete(null)
        } catch (e: Exception) {
            future.completeExceptionally(e)
        }
        
        return future
    }
    
    /**
     * Imports LuckPerms groups as ranks
     */
    fun importLuckPermsGroups(): CompletableFuture<Void> {
        // Just sync existing ranks to LuckPerms
        return exportRanksToLuckPerms()
    }
    
    /**
     * Checks if LuckPerms integration is enabled
     */
    fun isEnabled(): Boolean {
        return enabled
    }
}
