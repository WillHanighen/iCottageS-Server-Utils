package com.icottage.serverutils.iCottageSServerUtils.combat

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.Zombie
import org.bukkit.inventory.ItemStack
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages combat logging prevention
 */
class CombatManager(private val plugin: JavaPlugin) {
    // Map of player UUIDs to their combat timers
    private val combatPlayers = ConcurrentHashMap<UUID, Long>()
    
    // Map of player UUIDs to their combat logger NPCs
    private val combatLoggers = ConcurrentHashMap<UUID, UUID>()
    
    // Configuration values
    private val combatTime: Int = plugin.config.getInt("combat.time-seconds", 10)
    private val enabled: Boolean = plugin.config.getBoolean("combat.enabled", true)
    private val actionBarEnabled: Boolean = plugin.config.getBoolean("combat.action-bar-enabled", true)
    private val punishLogout: Boolean = plugin.config.getBoolean("combat.punish-logout", true)
    
    // Task ID for the combat timer update task
    private var timerTaskId: Int = -1
    
    /**
     * Initialize the combat manager
     */
    fun initialize() {
        if (!enabled) {
            plugin.logger.info("Combat logging prevention is disabled in config")
            return
        }
        
        // Start the timer task to update combat timers
        timerTaskId = plugin.server.scheduler.scheduleSyncRepeatingTask(plugin, Runnable {
            updateCombatTimers()
        }, 20L, 20L) // Run every second
        
        plugin.logger.info("Combat manager initialized")
    }
    
    /**
     * Update all combat timers and send action bar messages
     */
    private fun updateCombatTimers() {
        val currentTime = System.currentTimeMillis()
        val iterator = combatPlayers.entries.iterator()
        
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val playerUuid = entry.key
            val combatEndTime = entry.value
            
            // Check if combat has ended
            if (currentTime >= combatEndTime) {
                // Remove player from combat
                iterator.remove()
                
                // Notify player if they're online
                val player = plugin.server.getPlayer(playerUuid)
                if (player != null && player.isOnline) {
                    player.removeMetadata("in_combat", plugin)
                    player.sendMessage(Component.text("You are no longer in combat.").color(NamedTextColor.GREEN))
                    
                    if (actionBarEnabled) {
                        player.sendActionBar(Component.text("Combat Ended").color(NamedTextColor.GREEN))
                    }
                }
            } else {
                // Player is still in combat, update action bar
                val player = plugin.server.getPlayer(playerUuid)
                if (player != null && player.isOnline && actionBarEnabled) {
                    val timeLeft = ((combatEndTime - currentTime) / 1000).toInt()
                    val color = when {
                        timeLeft <= 3 -> NamedTextColor.RED
                        timeLeft <= 5 -> NamedTextColor.YELLOW
                        else -> NamedTextColor.GOLD
                    }
                    player.sendActionBar(Component.text("Combat: $timeLeft seconds").color(color))
                }
            }
        }
    }
    
    /**
     * Put a player in combat
     */
    fun tagPlayerCombat(player: Player) {
        if (!enabled) {
            plugin.logger.info("Combat logging prevention is disabled, not tagging ${player.name}")
            return
        }
        
        if (player.gameMode == GameMode.CREATIVE || player.gameMode == GameMode.SPECTATOR) {
            plugin.logger.info("Player ${player.name} is in creative/spectator mode, not tagging")
            return
        }
        
        val wasInCombat = isInCombat(player)
        val combatEndTime = System.currentTimeMillis() + (combatTime * 1000)
        
        // Update combat timer
        combatPlayers[player.uniqueId] = combatEndTime
        plugin.logger.info("Tagged ${player.name} in combat until ${combatEndTime}")
        
        // Set metadata for other systems to check
        player.setMetadata("in_combat", FixedMetadataValue(plugin, true))
        
        // Notify player if they weren't already in combat
        if (!wasInCombat) {
            player.sendMessage(Component.text("You are now in combat! Do not log out for $combatTime seconds.").color(NamedTextColor.RED))
        }
    }
    
    /**
     * Check if a player is in combat
     */
    fun isInCombat(player: Player): Boolean {
        val inCombatMap = combatPlayers.containsKey(player.uniqueId)
        val inCombatMeta = player.hasMetadata("in_combat")
        val inCombat = inCombatMap || inCombatMeta
        
        if (inCombatMap != inCombatMeta) {
            // Synchronize the two combat tracking methods if they're out of sync
            if (inCombatMap && !inCombatMeta) {
                player.setMetadata("in_combat", FixedMetadataValue(plugin, true))
            } else if (!inCombatMap && inCombatMeta) {
                combatPlayers[player.uniqueId] = System.currentTimeMillis() + (combatTime * 1000)
            }
        }
        
        plugin.logger.info("Checking if ${player.name} is in combat: $inCombat (map: $inCombatMap, meta: $inCombatMeta)")
        return inCombat
    }
    
    /**
     * Handle player logout during combat
     */
    fun handleCombatLogout(player: Player) {
        if (!enabled) {
            plugin.logger.info("Combat logging prevention is disabled, not creating combat logger for ${player.name}")
            return
        }
        
        if (!punishLogout) {
            plugin.logger.info("Combat logging punishment is disabled, not creating combat logger for ${player.name}")
            return
        }
        
        // Check both the map and the metadata to ensure we catch all combat cases
        val inCombatMap = combatPlayers.containsKey(player.uniqueId)
        val inCombatMeta = player.hasMetadata("in_combat")
        
        if (!inCombatMap && !inCombatMeta) {
            plugin.logger.info("Player ${player.name} is not in combat (map: $inCombatMap, meta: $inCombatMeta), not creating combat logger")
            return
        }
        
        // Create a combat logger NPC directly using Bukkit API
        plugin.logger.info("Creating combat logger for ${player.name} who logged out during combat")
        
        // Force create the zombie at the player's location
        val location = player.location
        val world = player.world
        
        // Spawn the zombie directly using the world object
        val zombie = world.spawn(location, Zombie::class.java)
        
        // Configure the zombie
        zombie.setAI(false) // Disable AI as requested
        zombie.isBaby = false
        zombie.isCustomNameVisible = true
        zombie.customName(Component.text("Combat Logger: ${player.name}").color(NamedTextColor.RED))
        
        // Prevent vanilla drops
        zombie.removeWhenFarAway = false
        zombie.canPickupItems = false
        zombie.setLootTable(null) // Prevent vanilla drops like rotten flesh
        
        // Set the zombie's health to match the player's
        zombie.health = player.health.coerceAtLeast(1.0)
        zombie.absorptionAmount = player.absorptionAmount
        
        // Make the zombie not burn in sunlight and not despawn
        zombie.setMetadata("combat_logger", FixedMetadataValue(plugin, player.uniqueId.toString()))
        zombie.isPersistent = true
        zombie.removeWhenFarAway = false
        zombie.addPotionEffect(PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 1, false, false))
        
        // Copy player equipment to the zombie
        zombie.equipment?.setItemInMainHand(player.inventory.itemInMainHand.clone())
        zombie.equipment?.setItemInOffHand(player.inventory.itemInOffHand.clone())
        zombie.equipment?.helmet = player.inventory.helmet?.clone()
        zombie.equipment?.chestplate = player.inventory.chestplate?.clone()
        zombie.equipment?.leggings = player.inventory.leggings?.clone()
        zombie.equipment?.boots = player.inventory.boots?.clone()
        
        // Set drop chances to 100% for all equipment
        zombie.equipment?.itemInMainHandDropChance = 1.0f
        zombie.equipment?.itemInOffHandDropChance = 1.0f
        zombie.equipment?.helmetDropChance = 1.0f
        zombie.equipment?.chestplateDropChance = 1.0f
        zombie.equipment?.leggingsDropChance = 1.0f
        zombie.equipment?.bootsDropChance = 1.0f
        
        // Store the player's inventory in the zombie's metadata
        val inventoryItems = arrayOfNulls<ItemStack>(player.inventory.size)
        for (i in 0 until player.inventory.size) {
            val item = player.inventory.getItem(i)
            if (item != null) {
                inventoryItems[i] = item.clone()
            }
        }
        zombie.setMetadata("player_inventory", FixedMetadataValue(plugin, inventoryItems))
        
        // Store the combat logger in the map
        combatLoggers[player.uniqueId] = zombie.uniqueId
        
        plugin.logger.info("Created combat logger for ${player.name} with UUID ${zombie.uniqueId}")
    }
    
    /**
     * Create a combat logger NPC for a player - This method is no longer used, see handleCombatLogout
     */
    private fun createCombatLogger(player: Player): Boolean {
        // This method is kept for reference but is no longer used
        // The zombie creation logic has been moved directly into handleCombatLogout
        // to avoid any timing issues with player data being cleared
        return false
    }
    
    /**
     * Handle a player reconnecting after combat logging
     */
    fun handlePlayerReconnect(player: Player) {
        // First check if this player was killed while offline (from file)
        val wasKilledOffline = checkIfPlayerWasKilledOffline(player.uniqueId)
        
        if (wasKilledOffline) {
            plugin.logger.info("Player ${player.name} was killed while offline (from persistent storage)")
            killPlayerOnReconnect(player)
            return
        }
        
        val combatLoggerUuid = combatLoggers[player.uniqueId]
        if (combatLoggerUuid != null) {
            plugin.logger.info("Player ${player.name} reconnecting, looking for combat logger with UUID $combatLoggerUuid")
            
            // Check if this is our special "killed" UUID
            if (combatLoggerUuid == UUID.fromString("00000000-0000-0000-0000-000000000000")) {
                plugin.logger.info("Player ${player.name} had a combat logger that was killed (from memory)")
                killPlayerOnReconnect(player)
                combatLoggers.remove(player.uniqueId)
                return
            }
            
            // Find the combat logger entity
            val entity = findEntityByUuid(combatLoggerUuid)
            
            if (entity != null && entity is Zombie) {
                plugin.logger.info("Found combat logger for ${player.name}, syncing health and teleporting")
                
                // Combat logger still exists, sync health and remove it
                try {
                    // Store the zombie's health for later use
                    val zombieHealth = entity.health
                    
                    // Restore player inventory from zombie metadata if available
                    if (entity.hasMetadata("player_inventory")) {
                        val inventoryItems = entity.getMetadata("player_inventory").firstOrNull()?.value() as? Array<*>
                        if (inventoryItems != null) {
                            // Clear player's current inventory first
                            player.inventory.clear()
                            
                            // Restore items from the stored inventory
                            for (i in 0 until inventoryItems.size) {
                                val item = inventoryItems[i]
                                if (item is ItemStack) {
                                    player.inventory.setItem(i, item.clone())
                                }
                            }
                            plugin.logger.info("Restored inventory for ${player.name} from combat logger")
                        }
                    }
                    
                    // Teleport player to combat logger location
                    player.teleport(entity.location)
                    
                    // Remove the combat logger
                    entity.remove()
                    
                    // Set player health after teleporting and removing the zombie
                    // Make sure health is at least 1 to prevent instant death
                    player.health = zombieHealth.coerceAtLeast(1.0)
                    
                    player.sendMessage(Component.text("Your combat logger has survived! Your health has been synchronized.").color(NamedTextColor.YELLOW))
                } catch (e: Exception) {
                    plugin.logger.severe("Error handling combat logger reconnect for ${player.name}: ${e.message}")
                    e.printStackTrace()
                    
                    // Fallback: set health to at least 1 to prevent instant death
                    try {
                        player.health = player.health.coerceAtLeast(1.0)
                    } catch (ex: Exception) {
                        plugin.logger.severe("Failed to set player health: ${ex.message}")
                    }
                }
            } else {
                // If we can't find the entity, assume it was killed
                plugin.logger.info("Combat logger for ${player.name} not found, assuming it was killed")
                killPlayerOnReconnect(player)
            }
            
            // Remove from the map
            combatLoggers.remove(player.uniqueId)
        }
    }
    
    /**
     * Kill a player who is reconnecting after their combat logger was killed
     */
    private fun killPlayerOnReconnect(player: Player) {
        try {
            // Clear inventory first before killing to prevent duplication
            player.inventory.clear()
            
            // Send message before killing the player
            player.sendMessage(Component.text("Your combat logger was killed while you were offline!").color(NamedTextColor.RED))
            
            // Use a delayed task to ensure the player is fully loaded before killing them
            plugin.server.scheduler.scheduleSyncDelayedTask(plugin, {
                try {
                    // Make sure inventory is still clear
                    player.inventory.clear()
                    
                    // Kill the player using multiple methods to ensure it works
                    player.health = 0.0
                    plugin.logger.info("Successfully killed player ${player.name} after combat logger death")
                } catch (ex: Exception) {
                    plugin.logger.severe("Failed to kill player ${player.name} using health=0: ${ex.message}")
                    
                    // Try alternative methods
                    try {
                        player.damage(1000.0) // Deal massive damage
                        plugin.logger.info("Used damage method to kill player ${player.name}")
                    } catch (ex2: Exception) {
                        plugin.logger.severe("Failed to kill player ${player.name} using damage: ${ex2.message}")
                        
                        // Last resort
                        try {
                            // Use the Bukkit API directly
                            player.setHealth(0.0)
                            plugin.logger.info("Used setHealth method to kill player ${player.name}")
                        } catch (ex3: Exception) {
                            plugin.logger.severe("All methods to kill player ${player.name} failed: ${ex3.message}")
                        }
                    }
                }
            }, 10L) // Wait 10 ticks (1/2 second) to ensure player is fully loaded
        } catch (e: Exception) {
            plugin.logger.severe("Error applying death penalty to ${player.name}: ${e.message}")
        }
    }
    
    /**
     * Check if a player was killed while offline (from persistent storage)
     */
    private fun checkIfPlayerWasKilledOffline(playerUuid: UUID): Boolean {
        try {
            val killedPlayersFile = java.io.File(plugin.dataFolder, "killed_combat_loggers.txt")
            if (!killedPlayersFile.exists()) {
                return false
            }
            
            // Read the file and check if this player's UUID is in it
            val reader = java.io.BufferedReader(java.io.FileReader(killedPlayersFile))
            var line: String?
            
            while (reader.readLine().also { line = it } != null) {
                if (line?.trim() == playerUuid.toString()) {
                    reader.close()
                    
                    // Remove this player from the file
                    removePlayerFromKilledFile(playerUuid)
                    
                    return true
                }
            }
            
            reader.close()
            return false
        } catch (e: Exception) {
            plugin.logger.severe("Error checking if player was killed offline: ${e.message}")
            return false
        }
    }
    
    /**
     * Remove a player from the killed combat loggers file
     */
    private fun removePlayerFromKilledFile(playerUuid: UUID) {
        try {
            val killedPlayersFile = java.io.File(plugin.dataFolder, "killed_combat_loggers.txt")
            if (!killedPlayersFile.exists()) {
                return
            }
            
            // Read all lines except the one with this player's UUID
            val reader = java.io.BufferedReader(java.io.FileReader(killedPlayersFile))
            val lines = reader.lines().filter { it.trim() != playerUuid.toString() }.toList()
            reader.close()
            
            // Write the filtered lines back to the file
            val writer = java.io.FileWriter(killedPlayersFile, false) // false to overwrite
            for (line in lines) {
                writer.write("$line\n")
            }
            writer.close()
            
            plugin.logger.info("Removed player $playerUuid from killed combat loggers file")
        } catch (e: Exception) {
            plugin.logger.severe("Error removing player from killed combat loggers file: ${e.message}")
        }
    }
    
    /**
     * Check if a UUID is likely to be a valid entity UUID (not a random UUID used to mark killed loggers)
     */
    private fun isValidEntityUuid(uuid: UUID): Boolean {
        // This is a heuristic - entity UUIDs in Minecraft have certain patterns
        // Random UUIDs we generate for killed loggers are unlikely to match these patterns
        return uuid.version() == 4 && uuid.variant() == 2
    }
    
    /**
     * Find an entity by UUID
     */
    private fun findEntityByUuid(uuid: UUID): org.bukkit.entity.Entity? {
        plugin.logger.info("Searching for entity with UUID $uuid")
        
        // First try the more efficient Bukkit method if available
        try {
            val entity = Bukkit.getEntity(uuid)
            if (entity != null) {
                plugin.logger.info("Found entity with UUID $uuid using Bukkit.getEntity")
                return entity
            }
        } catch (e: Exception) {
            // Some versions of Bukkit might not have this method, fall back to manual search
            plugin.logger.info("Bukkit.getEntity not available or failed, falling back to manual search")
        }
        
        // Fall back to manual search
        for (world in Bukkit.getWorlds()) {
            for (entity in world.entities) {
                if (entity.uniqueId == uuid) {
                    plugin.logger.info("Found entity with UUID $uuid in world ${world.name} through manual search")
                    return entity
                }
            }
        }
        
        plugin.logger.info("No entity found with UUID $uuid")
        return null
    }
    
    /**
     * Handle a combat logger being killed
     */
    fun handleCombatLoggerKilled(zombie: Zombie) {
        try {
            val playerUuidString = zombie.getMetadata("combat_logger").firstOrNull()?.asString() ?: return
            val playerUuid = UUID.fromString(playerUuidString)
            
            plugin.logger.info("Combat logger for player $playerUuidString is being killed")
            
            // Check if this zombie has already been processed to prevent double drops
            if (zombie.hasMetadata("combat_logger_processed")) {
                plugin.logger.info("Combat logger for player $playerUuidString has already been processed, skipping item drops")
                return
            }
            
            // Mark this zombie as processed to prevent double drops
            zombie.setMetadata("combat_logger_processed", FixedMetadataValue(plugin, true))
            
            // Cancel the default death drops
            try {
                // This will be called in the EntityDeathEvent handler to cancel vanilla drops
                zombie.setMetadata("cancel_drops", FixedMetadataValue(plugin, true))
            } catch (e: Exception) {
                plugin.logger.warning("Failed to set cancel_drops metadata: ${e.message}")
            }
            
            // Drop the player's inventory items
            if (zombie.hasMetadata("player_inventory")) {
                val inventoryItems = zombie.getMetadata("player_inventory").firstOrNull()?.value() as? Array<*>
                if (inventoryItems != null) {
                    for (item in inventoryItems) {
                        if (item is ItemStack) {
                            zombie.world.dropItemNaturally(zombie.location, item)
                        }
                    }
                }
            }
            
            // Store a special value to mark the player as killed
            // Use "KILLED" as a string instead of a UUID to make it more reliable
            combatLoggers[playerUuid] = UUID.fromString("00000000-0000-0000-0000-000000000000")
            
            // Save this information to a persistent file to ensure it survives server restarts
            try {
                val killedPlayersFile = java.io.File(plugin.dataFolder, "killed_combat_loggers.txt")
                if (!killedPlayersFile.exists()) {
                    killedPlayersFile.createNewFile()
                }
                
                // Append this player's UUID to the file
                val writer = java.io.FileWriter(killedPlayersFile, true) // true for append mode
                writer.write("$playerUuid\n")
                writer.close()
                
                plugin.logger.info("Saved killed combat logger info for player $playerUuidString to file")
            } catch (e: Exception) {
                plugin.logger.severe("Failed to save killed combat logger info to file: ${e.message}")
            }
            
            plugin.logger.info("Combat logger for player $playerUuidString was killed and marked for death on reconnect")
        } catch (e: Exception) {
            plugin.logger.severe("Error handling combat logger death: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Clean up resources when the plugin is disabled
     */
    fun shutdown() {
        if (timerTaskId != -1) {
            plugin.server.scheduler.cancelTask(timerTaskId)
            timerTaskId = -1
        }
        
        // Remove all combat loggers
        for (loggerUuid in combatLoggers.values) {
            val entity = findEntityByUuid(loggerUuid)
            entity?.remove()
        }
        
        combatLoggers.clear()
        combatPlayers.clear()
    }
}
